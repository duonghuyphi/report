package com.engagePlus.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class TableController {

    @Autowired
    private DataSource dataSource;
    @GetMapping("/tables")
    public List<String> getAllTableNames() {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tableNames;
    }

    @GetMapping("/table-data")
    public List<Map<String, Object>> getTableData(@RequestParam String tableName) {
        List<Map<String, Object>> data = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " LIMIT 100"; // limit để tránh bảng quá lớn

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                data.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

}
