package com.engagePlus.report.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class CookieService {

    public void fetchSidCookieWithHttpClient() {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        try {
            String loginUrl = "https://enablerplus.myharavan.com/admin";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(loginUrl))
                    .POST(buildFormData(Map.of(
                            "customer[email]", "duonghuyphi@gmail.com",
                            "customer[password]", "Enablerplus!@#2025"
                    )))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/114.0.0.0 Safari/537.36")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            List<String> cookies = response.headers().allValues("set-cookie");

            for (String cookie : cookies) {
                if (cookie.startsWith("sid.omnipower.sid=")) {
                    String sid = cookie.split(";")[0].split("=")[1];
                    replaceCookieInProperties(sid);
                    System.out.println("✅ Lấy và ghi cookie thành công.");
                    return;
                }
            }

            throw new RuntimeException("❌ Không tìm thấy cookie sid.omnipower.sid");

        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi khi đăng nhập và lấy cookie", e);
        }
    }

    private static HttpRequest.BodyPublisher buildFormData(Map<String, String> data) {
        String encoded = data.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return HttpRequest.BodyPublishers.ofString(encoded);
    }

    private void replaceCookieInProperties(String newSidValue) {
        String filePath = "src/main/resources/application.properties";
        String key = "haravan.cookie";

        try {
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;

            boolean replaced = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(key + "=")) {
                    String originalCookieString = line.substring((key + "=").length());

                    // Regex thay thế đúng phần sid.omnipower.sid=...
                    String updatedCookieString = originalCookieString.replaceAll(
                            "sid\\.omnipower\\.sid=[^;]*",
                            "sid.omnipower.sid=" + newSidValue
                    );

                    content.append(key).append("=").append(updatedCookieString).append(System.lineSeparator());
                    replaced = true;
                } else {
                    content.append(line).append(System.lineSeparator());
                }
            }
            reader.close();

            // Nếu không có dòng haravan.cookie thì thêm mới
            if (!replaced) {
                content.append(key)
                        .append("=sid.omnipower.sid=")
                        .append(newSidValue)
                        .append(System.lineSeparator());
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

            System.out.println("✅ Đã cập nhật sid.omnipower.sid trong haravan.cookie");

        } catch (IOException e) {
            throw new RuntimeException("❌ Không thể ghi file application.properties", e);
        }
    }
}
