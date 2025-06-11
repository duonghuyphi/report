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

        // B4: Tạo bảng nếu chưa có
        String createTableSql = generateCreateTableSQL(tableName, columnNames);
        jdbcTemplate.execute(createTableSql);

        // B5: Chuẩn bị dữ liệu batch insert
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

        // B6: Thực hiện batch insert
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

    public static List<Map<String, Object>> expandComboProducts(InputStream reportStream, InputStream giftStream, InputStream prodStream) throws IOException {
        // Đọc dữ liệu từ các stream
        List<Map<String, Object>> reportData = EasyExcelFactory.read(reportStream).sheet().doReadSync();
        List<Map<String, Object>> giftData = EasyExcelFactory.read(giftStream).sheet().doReadSync();
        List<Map<String, Object>> prodData = EasyExcelFactory.read(prodStream).sheet().doReadSync();

        List<Map<String, Object>> result = new ArrayList<>();

        // Duyệt qua từng dòng trong báo cáo
        for (Map<String, Object> row : reportData) {
            String barcode = String.valueOf(row.get("Barcode")).trim();

            // Tìm kiếm quà tặng khớp với sản phẩm trong báo cáo
            Optional<Map<String, Object>> matchingGift = giftData.stream()
                    .filter(g -> barcode.equals(String.valueOf(g.get("SKU Sản phẩm")).trim()))
                    .findFirst();

            if (matchingGift.isPresent()) {
                Map<String, Object> giftRow = matchingGift.get();

                // Duyệt qua các sản phẩm quà tặng (từ 1 đến 4)
                for (int i = 1; i <= 4; i++) {
                    String giftBarcode = String.valueOf(giftRow.get("Tên sản phẩm " + i));
                    String giftQtyStr = String.valueOf(giftRow.get("Số lượng " + i));

                    // Nếu mã vạch quà tặng hợp lệ, xử lý thêm dòng mới
                    if (giftBarcode != null && !giftBarcode.isBlank()) {
                        Map<String, Object> newRow = new HashMap<>(row);
                        newRow.put("Barcode", giftBarcode);

                        // Thử chuyển đổi số lượng quà tặng từ String sang Integer
                        try {
                            newRow.put("Số sản phẩm", Integer.parseInt(giftQtyStr));
                        } catch (NumberFormatException ignored) {}

                        // Tìm kiếm sản phẩm trong prodData dựa trên barcode của quà tặng
                        Optional<Map<String, Object>> matchingProduct = prodData.stream()
                                .filter(p -> giftBarcode.equals(String.valueOf(p.get("barcode")).trim()))
                                .findFirst();

                        // Nếu tìm thấy sản phẩm khớp với barcode quà tặng, thay thế tên sản phẩm
                        if (matchingProduct.isPresent()) {
                            Map<String, Object> productRow = matchingProduct.get();
                            String productName = String.valueOf(productRow.get("ten"));  // Lấy tên sản phẩm từ prodData
                            newRow.put("Sản phẩm", productName);  // Thay thế tên sản phẩm trong newRow
                        }

                        // Thêm dòng mới vào kết quả
                        result.add(newRow);
                    }
                }
            } else {
                // Nếu không có quà tặng tương ứng, thêm dòng báo cáo vào kết quả
                result.add(row);
            }
        }

        // Trả về kết quả cuối cùng
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
        // Lấy dữ liệu báo cáo và quà tặng
        List<Map<String, Object>> report = fetchAllData("report");
        List<Map<String, Object>> gift = fetchAllData("gift");
        List<Map<String, Object>> prod = fetchAllData("products");  // Dữ liệu sản phẩm

        // Map để tra cứu nhanh theo SKU sản phẩm trong quà tặng
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

        // Tạo map tra cứu sản phẩm từ barcode sang tên sản phẩm
        Map<String, String> prodBarcodeMap = new HashMap<>();
        for (Map<String, Object> p : prod) {
            String barcode = (p.get("barcode") + "").trim();
            String productName = (p.get("ten") + "").trim();  // Lấy tên sản phẩm
            prodBarcodeMap.put(barcode, productName);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        // Duyệt qua từng dòng trong báo cáo
        for (Map<String, Object> row : report) {
            String barcode = (row.get("barcode") + "").trim();

            // Kiểm tra xem sản phẩm có trong quà tặng hay không
            if (giftBarcodeMap.containsKey(barcode)) {
                List<String> children = giftBarcodeMap.get(barcode);
                for (String childBarcode : children) {
                    Map<String, Object> newRow = new LinkedHashMap<>(row);
                    newRow.put("barcode", childBarcode); // Chỉ thay đổi cột "Barcode"

                    // Tìm tên sản phẩm từ prodBarcodeMap và thêm vào "Sản phẩm"
                    String productName = prodBarcodeMap.get(childBarcode);
                    if (productName != null) {
                        newRow.put("san_pham", productName); // Cập nhật tên sản phẩm
                    }

                    result.add(newRow);
                }
            } else {
                result.add(row); // Nếu không phải combo, giữ nguyên dòng
            }
        }

        return result;
    }



    public List<Map<String, Object>> fetchAllData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

}
