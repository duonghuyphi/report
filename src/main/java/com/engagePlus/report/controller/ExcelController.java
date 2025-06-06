package com.engagePlus.report.controller;

import com.engagePlus.report.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/excel")
public class ExcelController {
    @Autowired
    private ExcelService excelService;

    @GetMapping("/report-expanded")
    public List<Map<String, Object>> getExpandedReport(@RequestParam String report, @RequestParam String gift) throws IOException {
        InputStream reportStream = new FileInputStream("path/to/" + report);
        InputStream giftStream = new FileInputStream("path/to/" + gift);

        return ExcelService.expandComboProducts(reportStream, giftStream);
    }
    @GetMapping("/excel-data")
    public List<Map<String, Object>> getExcelData(@RequestParam String filename) {
        return excelService.getAllExcelData(filename);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            excelService.readExcelAndGenerateTable(file);
            return ResponseEntity.ok(Map.of("message", "File uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete-table")
    public ResponseEntity<Map<String, String>> deleteTable(@RequestParam String tableName) {
        Map<String, String> response = new HashMap<>();
        try {
            excelService.deleteTable(tableName);
            response.put("message", "Đã xóa bảng: " + tableName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Lỗi khi xóa bảng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/expand-combo")
    public ResponseEntity<List<Map<String, Object>>> expandCombo() {
        List<Map<String, Object>> result = excelService.expandComboRows();
        return ResponseEntity.ok(result);
    }

}
