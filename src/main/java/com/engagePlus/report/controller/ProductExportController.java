package com.engagePlus.report.controller;

import com.engagePlus.report.service.ProductExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/export")
public class ProductExportController {

    private final ProductExportService productExportService;

    public ProductExportController(ProductExportService productExportService) {
        this.productExportService = productExportService;
    }

    @GetMapping("/products")
    public ResponseEntity<byte[]> exportProducts() throws IOException {
        byte[] excelData = productExportService.fetchProductsAndExportExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.xlsx");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}

