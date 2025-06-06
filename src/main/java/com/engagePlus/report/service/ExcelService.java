package com.engagePlus.report.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getAllExcelData(String tableName) {
        String normalizedTable = toSqlTableName(tableName); // xử lý tên file thành tên bảng
        String sql = "SELECT * FROM " + normalizedTable;
        return jdbcTemplate.queryForList(sql);
    }

    public void readExcelAndGenerateTable(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();

        // B1: Tạo tên bảng từ tên file
        String originalFilename = file.getOriginalFilename();
        String tableName = toSqlTableName(originalFilename);

        List<Map<Integer, String>> excelData = EasyExcel.read(inputStream)
                .autoCloseStream(true)
                .headRowNumber(0)
                .sheet()
                .doReadSync();

        if (excelData.isEmpty()) {
            throw new IllegalArgumentException("File Excel rỗng");
        }

        Map<Integer, String> headerRow = excelData.get(0);
        List<String> columnNames = headerRow.values().stream()
                .map(this::toSqlColumn)
                .collect(Collectors.toList());

        // B2: Tạo bảng
        String createTableSql = generateCreateTableSQL(tableName, columnNames);
        jdbcTemplate.execute(createTableSql);

        // B3: Insert dữ liệu
        for (int i = 1; i < excelData.size(); i++) {
            Map<Integer, String> rowMap = excelData.get(i);
            List<String> rowValues = columnNames.stream()
                    .map(col -> rowMap.getOrDefault(columnNames.indexOf(col), ""))
                    .collect(Collectors.toList());
            String insertSql = generateInsertSQL(tableName, columnNames, rowValues);
            jdbcTemplate.execute(insertSql);
        }
    }

    private String generateCreateTableSQL(String tableName, List<String> columnNames) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");
        for (String col : columnNames) {
            sql.append(col).append(" TEXT, ");
        }
        sql.delete(sql.length() - 2, sql.length()); // remove last comma
        sql.append(");");
        return sql.toString();
    }

    private String generateInsertSQL(String tableName, List<String> columns, List<String> values) {
        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
        sql.append(String.join(",", columns)).append(") VALUES (");

        for (int i = 0; i < columns.size(); i++) {
            String value = (i < values.size()) ? values.get(i) : "";
            value = (value == null) ? "" : value.replace("'", "''"); // Escape SQL
            sql.append("'").append(value).append("',");
        }

        sql.deleteCharAt(sql.length() - 1); // remove trailing comma
        sql.append(");");
        return sql.toString();
    }

    private String toSqlColumn(String header) {
        String normalized = Normalizer.normalize(header, Normalizer.Form.NFD)
                .replaceAll("đ", "d")       // xử lý chữ đ
                .replaceAll("Đ", "D")       // xử lý chữ Đ
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "") // bỏ dấu
                .replaceAll("[^a-zA-Z0-9]", "_") // thay ký tự đặc biệt bằng _
                .replaceAll("_+", "_")      // gom nhiều dấu _ thành 1
                .replaceAll("^_|_$", "")    // xóa _ đầu/cuối
                .toLowerCase();

        return normalized;
    }


    private String toSqlTableName(String filename) {
        if (filename == null) return "unknown_table";
        String nameWithoutExtension = filename.replaceAll("\\.[^.]+$", "");
        return toSqlColumn(nameWithoutExtension);
    }

    public static List<Map<String, Object>> expandComboProducts(InputStream reportStream, InputStream giftStream) throws IOException {
        List<Map<String, Object>> reportData = EasyExcelFactory.read(reportStream).sheet().doReadSync();
        List<Map<String, Object>> giftData = EasyExcelFactory.read(giftStream).sheet().doReadSync();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : reportData) {
            String barcode = String.valueOf(row.get("Barcode")).trim();

            Optional<Map<String, Object>> matchingGift = giftData.stream()
                    .filter(g -> barcode.equals(String.valueOf(g.get("SKU Sản phẩm")).trim()))
                    .findFirst();

            if (matchingGift.isPresent()) {
                Map<String, Object> giftRow = matchingGift.get();
                for (int i = 1; i <= 4; i++) {
                    String giftBarcode = String.valueOf(giftRow.get("Tên sản phẩm " + i));
                    String giftQtyStr = String.valueOf(giftRow.get("Số lượng " + i));

                    if (giftBarcode != null && !giftBarcode.isBlank()) {
                        Map<String, Object> newRow = new HashMap<>(row);
                        newRow.put("Barcode", giftBarcode);
                        try {
                            newRow.put("Số sản phẩm", Integer.parseInt(giftQtyStr));
                        } catch (NumberFormatException ignored) {}
                        result.add(newRow);
                    }
                }
            } else {
                result.add(row);
            }
        }

        return result;
    }
    public void deleteTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Tên bảng không hợp lệ");
        }

        String safeTableName = toSqlTableName(tableName);
        String sql = "DROP TABLE " + safeTableName;
        jdbcTemplate.execute(sql);
    }

    public List<Map<String, Object>> expandComboRows() {
        List<Map<String, Object>> report = fetchAllData("report");
        List<Map<String, Object>> gift = fetchAllData("gift");

        // Map để tra cứu nhanh theo SKU sản phẩm
        Map<String, List<String>> giftBarcodeMap = new HashMap<>();
        for (Map<String, Object> g : gift) {
            String sku = (g.get("sku_san_pham") + "").trim();

            List<String> barcodes = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                Object code = g.get("barcode_" + i);
                if (code != null && !code.toString().trim().isEmpty()) {
                    barcodes.add(code.toString().trim());
                }
            }

            giftBarcodeMap.put(sku, barcodes);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : report) {
            String barcode = (row.get("barcode") + "").trim();

            if (giftBarcodeMap.containsKey(barcode)) {
                List<String> children = giftBarcodeMap.get(barcode);
                for (String childBarcode : children) {
                    Map<String, Object> newRow = new LinkedHashMap<>(row);
                    newRow.put("barcode", childBarcode); // Chỉ thay đổi cột "Barcode"
                    result.add(newRow);
                }
            } else {
                result.add(row); // Không phải combo, giữ nguyên
            }
        }

        return result;
    }


    public List<Map<String, Object>> fetchAllData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

}
