package com.engagePlus.report.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OrderExportService {
    @Value("${haravan.shop-domain}")
    private String shopDomain;

    @Value("${haravan.access-token}")
    private String accessToken;

    @Value("${haravan.cookie}")
    private String haravanCookie;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] fetchOrdersAndExportExcel() throws IOException {
        List<List<String>> data = Collections.synchronizedList(new ArrayList<>());
        Set<String> headerSet = Collections.synchronizedSet(new LinkedHashSet<>());

        ExecutorService executor = Executors.newFixedThreadPool(10); // Tùy cấu hình máy
        int page = 1;

        while (true) {
            String url = "https://" + shopDomain + "/admin/orders.json?limit=250&shipment_status=delivered&page=" + page;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.add("Cookie", haravanCookie);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode orders = objectMapper.readTree(response.getBody()).get("orders");
            if (orders == null || !orders.isArray() || orders.size() == 0) break;

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (JsonNode order : orders) {
                Long orderId = order.get("id").asLong();
                String orderNumber = order.get("order_number").asText("");
                String createAt = order.get("created_at").asText("");
                String totalPrice = order.get("total_price").asText("");

                for (JsonNode item : order.get("line_items")) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        Map<String, String> row = new LinkedHashMap<>();
                        row.put("order_id", String.valueOf(orderId));
                        row.put("order_number", orderNumber);
                        row.put("create_at", createAt);
                        row.put("total_price", totalPrice);
                        row.put("line_item_id", item.get("id").asText(""));
                        row.put("sku_combo", item.get("sku").asText());

                        try {
                            String ec_url = "https://" + shopDomain + "/admin/call/com_api/orders/" + orderId + "/" + item.get("id") + "/combo";
                            ResponseEntity<String> ec_response = restTemplate.exchange(ec_url, HttpMethod.GET, entity, String.class);

                            JsonNode dataItem = null;
                            String ecBody = ec_response.getBody();

                            if (ecBody != null && !ecBody.isEmpty()) {
                                JsonNode ecNode = objectMapper.readTree(ecBody);
                                JsonNode dataArray = ecNode.get("data");
                                if (dataArray != null && dataArray.isArray() && dataArray.size() > 0) {
                                    dataItem = dataArray.get(0);
                                }
                            }

                            if (dataItem == null) {
                                row.put("sku", item.get("sku").asText(""));
                                row.put("product_name", item.get("name").asText(""));
                                row.put("barcode", item.get("barcode").asText(""));
                            } else {
                                row.put("sku", dataItem.get("sku").asText(""));
                                row.put("product_name", dataItem.get("productName").asText(""));
                                row.put("barcode", dataItem.get("barcode").asText(""));
                            }

                            row.put("quantity", item.get("quantity").asText(""));
                            row.put("variant_id", item.get("variant_id").asText(""));
                            row.put("price_item", item.get("price").asText(""));

                            synchronized (headerSet) {
                                headerSet.addAll(row.keySet());
                            }

                            data.add(new ArrayList<>(row.values()));

                        } catch (Exception ex) {
                            System.err.println("Lỗi khi xử lý combo: orderId=" + orderId + ", lineItem=" + item.get("id") + " | " + ex.getMessage());
                        }
                    }, executor);

                    futures.add(future);
                }
            }

            // Chờ tất cả future hoàn thành trước khi qua page tiếp theo
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            page++;
        }

        executor.shutdown(); // Tắt Executor sau khi hoàn thành

        // Tạo header
        List<List<String>> head = new ArrayList<>();
        for (String key : headerSet) {
            head.add(Collections.singletonList(key));
        }

        // Style Excel
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        WriteFont headFont = new WriteFont();
        headFont.setFontHeightInPoints((short) 12);
        headFont.setBold(true);
        headFont.setFontName("Calibri");
        headStyle.setWriteFont(headFont);

        WriteCellStyle contentStyle = new WriteCellStyle();
        WriteFont contentFont = new WriteFont();
        contentFont.setFontName("Calibri");
        contentFont.setFontHeightInPoints((short) 11);
        contentStyle.setWriteFont(contentFont);

        HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(headStyle, contentStyle);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out)
                    .head(head)
                    .registerWriteHandler(styleStrategy)
                    .sheet("Orders")
                    .doWrite(data);
            return out.toByteArray();
        }
    }
}
