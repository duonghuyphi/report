package com.engagePlus.report.service;

import com.alibaba.excel.EasyExcelFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class ReportExpansionService {
    public List<Map<String, Object>> expandComboProducts(InputStream reportStream, InputStream giftStream) {
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
}
