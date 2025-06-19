package com.engagePlus.report.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    public List<Map<String, Object>> getAllExcelData(String tableName) {
        String normalizedTable = toSqlTableName(tableName); // xử lý tên file thành tên bảng
        String sql = "SELECT * FROM " + normalizedTable;
        return jdbcTemplate.queryForList(sql);
    }

    public void readExcelAndGenerateTable(MultipartFile file, String table_Name) throws IOException {
        InputStream inputStream = file.getInputStream();

        // B1: Tạo tên bảng từ tên file
        String tableName = toSqlTableName(table_Name);

        // B2: Đọc dữ liệu Excel
        List<Map<Integer, String>> excelData = EasyExcel.read(inputStream)
                .autoCloseStream(true)
                .headRowNumber(0)
                .sheet()
                .doReadSync();

        if (excelData.isEmpty()) {
            throw new IllegalArgumentException("File Excel rỗng");
        }

        // B3: Xử lý header
        Map<Integer, String> headerRow = excelData.get(0);
        List<String> columnNames = headerRow.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // đảm bảo đúng thứ tự cột
                .map(entry -> toSqlColumn(entry.getValue()))
                .collect(Collectors.toList());

        // B4: Kiểm tra tồn tại bảng → nếu có thì drop
        if (doesTableExist(tableName)) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
        }

        // B5: Tạo bảng mới
        String createTableSql = generateCreateTableSQL(tableName, columnNames);
        jdbcTemplate.execute(createTableSql);

        // B6: Chuẩn bị dữ liệu batch insert
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 1; i < excelData.size(); i++) {
            Map<Integer, String> rowMap = excelData.get(i);
            Object[] rowValues = columnNames.stream()
                    .map(col -> {
                        int index = columnNames.indexOf(col);
                        return rowMap.containsKey(index) ? rowMap.get(index) : null;
                    })
                    .toArray();
            batchArgs.add(rowValues);
        }

        // B7: Thực hiện batch insert
        String insertSql = generatePreparedInsertSQL(tableName, columnNames);
        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

    private String generatePreparedInsertSQL(String tableName, List<String> columns) {
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(","));
        return "INSERT INTO " + tableName + " (" + String.join(",", columns) + ") VALUES (" + placeholders + ")";
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
        List<Map<String, Object>> orders = fetchAllData("orders");

        // Tạo map để tra cứu nhanh (ma_don_hang + sku_combo) -> orders row
        Map<String, Map<String, Object>> orderLookup = new HashMap<>();
        for (Map<String, Object> o : orders) {
            String orderNumber = (o.get("order_number") + "").trim();
            String skuCombo = (o.get("sku_combo") + "").trim();
            String key = orderNumber + "_" + skuCombo;
            orderLookup.put(key, o);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : report) {
            String maDonHang = (row.get("ma_don_hang") + "").trim();
            String barcode = (row.get("barcode") + "").trim();
            String key = maDonHang + "_" + barcode;

            if (orderLookup.containsKey(key)) {
                Map<String, Object> matchedOrder = orderLookup.get(key);

                // Clone dòng hiện tại để cập nhật
                Map<String, Object> newRow = new LinkedHashMap<>(row);
                newRow.put("ten_san_pham", matchedOrder.get("product_name"));
                newRow.put("barcode", matchedOrder.get("barcode"));

                result.add(newRow);
            } else {
                // Không match thì giữ nguyên
                result.add(row);
            }
        }

        return result;
    }

    private boolean doesTableExist(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{tableName}, Integer.class);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> fetchAllData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

}
