package com.engagePlus.report.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HaravanExportController {

    @Value("${haravan.access-token}")
    private String accessToken;

    @Value("${haravan.shop-domain}")
    private String shopDomain;

    @Value("${haravan.cookie}")
    private String haravanCookie;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/export-order")
    public ResponseEntity<String> exportOrdersToJson() throws IOException {
        String baseUrl = "https://" + shopDomain +
                "/admin/orders.json?query=250602EB7JX2Y8&page=1&limit=50&shipment_status=delivered";

        // Tạo RestTemplate để gửi request
        RestTemplate restTemplate = new RestTemplate();

        // Gắn header Authorization
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Gửi GET request và nhận phản hồi kiểu String (JSON)
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        // Trả về JSON nhận được
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    @GetMapping("/export-orders")
    public ResponseEntity<byte[]> exportOrdersToExcel() throws IOException {
        String baseUrl = "https://" + shopDomain +
                "/admin/orders.json?status=any&limit=250&shipment_status=delivered&page=";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        List<Map<String, Object>> allOrders = new ArrayList<>();
        int page = 1;

        ObjectMapper mapper = new ObjectMapper();

        while (true) {
            String url = baseUrl + page;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode ordersNode = root.get("orders");
            if (ordersNode == null || !ordersNode.isArray() || ordersNode.size() == 0) break;

            for (JsonNode orderNode : ordersNode) {
                allOrders.add(mapper.convertValue(orderNode, new TypeReference<Map<String, Object>>() {}));
            }

            page++;
        }

        // Ghi Excel bằng SXSSFWorkbook
        SXSSFWorkbook workbook = new SXSSFWorkbook(100); // giữ 100 dòng trong bộ nhớ
        Sheet sheet = workbook.createSheet("Orders");

        String[] headersRow = { "order_name", "order_number", "total_price", "sku", "product_name", "barcode", "quantity" };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headersRow.length; i++) {
            header.createCell(i).setCellValue(headersRow[i]);
        }

        int rowIndex = 1;
        for (Map<String, Object> order : allOrders) {
            String orderName = String.valueOf(order.get("name"));
            String orderNumber = String.valueOf(order.get("order_number"));
            double totalPrice = Double.parseDouble(String.valueOf(order.get("total_price")));

            List<Map<String, Object>> lineItems = (List<Map<String, Object>>) order.get("line_items");

            for (Map<String, Object> item : lineItems) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(orderName);
                row.createCell(1).setCellValue(orderNumber);
                row.createCell(2).setCellValue(totalPrice);
                row.createCell(3).setCellValue(String.valueOf(item.getOrDefault("sku", "")));
                row.createCell(4).setCellValue(String.valueOf(item.getOrDefault("title", "")));
                row.createCell(5).setCellValue(String.valueOf(item.getOrDefault("barcode", "")));
                row.createCell(6).setCellValue(Integer.parseInt(String.valueOf(item.get("quantity"))));
            }
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.dispose(); // xóa file tạm của SXSSF
        workbook.close();

        byte[] excelBytes = bos.toByteArray();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        responseHeaders.setContentDisposition(ContentDisposition.attachment()
                .filename("haravan-orders.xlsx").build());

        return new ResponseEntity<>(excelBytes, responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/export-products")
    public ResponseEntity<byte[]> exportProductExcel() throws IOException {
        String baseUrl = "https://" + shopDomain + "/admin/products.json?limit=50&page=";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<List<Object>> excelData = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        int page = 1;
        while (true) {
            String url = baseUrl + page;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode products = root.get("products");

            if (products == null || !products.isArray() || products.size() == 0) {
                break; // hết sản phẩm
            }

            for (JsonNode product : products) {
                String productTitle = product.path("title").asText();
                String productType = product.path("product_type").asText();
                String vendor = product.path("vendor").asText();
                long productId = product.path("id").asLong();

                JsonNode variants = product.path("variants");
                if (variants != null && variants.isArray()) {
                    for (JsonNode variant : variants) {
                        List<Object> row = new ArrayList<>();
                        row.add(productId);
                        row.add(productTitle);
                        row.add(productType);
                        row.add(vendor);
                        row.add(variant.path("id").asLong());
                        row.add(variant.path("sku").asText(""));
                        row.add(variant.path("barcode").asText(""));
                        row.add(variant.path("price").asDouble());
                        row.add(variant.path("inventory_quantity").asInt());
                        row.add(variant.path("grams").asDouble());
                        excelData.add(row);
                    }
                }
            }

            page++; // chuyển trang
        }

        // Header
        List<List<String>> head = Arrays.asList(
                Collections.singletonList("Product ID"),
                Collections.singletonList("Product Title"),
                Collections.singletonList("Product Type"),
                Collections.singletonList("Vendor"),
                Collections.singletonList("Variant ID"),
                Collections.singletonList("SKU"),
                Collections.singletonList("Barcode"),
                Collections.singletonList("Price"),
                Collections.singletonList("Inventory Quantity"),
                Collections.singletonList("Grams")
        );

        // Ghi Excel
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        EasyExcel.write(outStream)
                .head(head)
                .sheet("Products")
                .doWrite(excelData);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.xlsx");
        responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outStream.toByteArray());
    }
}