package com.engagePlus.report.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.stream.Collectors;

@Service
public class ProductExportService {

    @Value("${haravan.shop-domain}")
    private String shopDomain;

    @Value("${haravan.access-token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] fetchProductsAndExportExcel() throws IOException {
        List<List<String>> data = new ArrayList<>();
        Set<String> headerSet = new LinkedHashSet<>(); // để lưu tên cột duy nhất

        int page = 1;
        while (true) {
            String url = "https://" + shopDomain + "/admin/products.json?limit=250&page=" + page;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode products = objectMapper.readTree(response.getBody()).get("products");
            if (products == null || !products.isArray() || products.size() == 0) break;

            for (JsonNode product : products) {
                String productTitle = product.get("title").asText("");
                String productType = product.get("product_type").asText("");
                String vendor = product.get("vendor").asText("");
                long productId = product.get("id").asLong();

                JsonNode variants = product.get("variants");
                for (JsonNode variant : variants) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("product_id", String.valueOf(productId));
                    row.put("title", productTitle);
                    row.put("product_type", productType);
                    row.put("vendor", vendor);
                    row.put("variant_id", variant.get("id").asText());
                    row.put("sku", variant.get("sku").asText(""));
                    row.put("barcode", variant.get("barcode").asText(""));
                    row.put("inventory_quantity", variant.get("inventory_quantity").asText(""));
                    row.put("grams", variant.get("grams").asText(""));

                    headerSet.addAll(row.keySet()); // đảm bảo thứ tự cột cố định
                    data.add(new ArrayList<>(row.values()));
                }
            }

            page++;
        }

        // Header (dòng đầu tiên)
        List<List<String>> head = new ArrayList<>();
        for (String key : headerSet) {
            head.add(Collections.singletonList(key));
        }

        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); // nền xám
        WriteFont headFont = new WriteFont();
        headFont.setFontHeightInPoints((short) 12);
        headFont.setBold(true);
        headFont.setFontName("Calibri"); // font giống nội dung
        headStyle.setWriteFont(headFont);

        WriteCellStyle contentStyle = new WriteCellStyle();
        WriteFont contentFont = new WriteFont();
        contentFont.setFontName("Calibri"); // giống header
        contentFont.setFontHeightInPoints((short) 11);
        contentStyle.setWriteFont(contentFont);

        HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(headStyle, contentStyle);


        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out)
                    .head(head)
                    .registerWriteHandler(styleStrategy)
                    .sheet("Products")
                    .doWrite(data);
            return out.toByteArray();
        }
    }
}

