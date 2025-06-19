package com.engagePlus.report.controller;

import com.engagePlus.report.service.OrderExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/export")
public class OrderExportController {

    private final OrderExportService orderExportService;

    public OrderExportController(OrderExportService orderExportService) {
        this.orderExportService = orderExportService;
    }

    @GetMapping("/orders")
    public ResponseEntity<byte[]> exportOrders() throws IOException {
        try {
            byte[] excelData = orderExportService.fetchOrdersAndExportExcel();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            headers.add("X-Status", "success");
            headers.add("X-Message", "Export thành công");

            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Status", "error");
            headers.add("X-Message", "Lỗi khi export: " + e.getMessage());

            return ResponseEntity.status(500).headers(headers).body(null);
        }
    }
}
