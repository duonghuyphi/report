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
            String loginUrl = "https://enablerplus.myharavan.com/account/login";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(loginUrl))
                    .POST(buildFormData(Map.of(
                            "customer[email]", "duonghuyphi@gmail.com",
                            "customer[password]", "Enablerplus!@#2025"
                    )))
                    .header("Content-Type", "application/x-www-form-urlencoded")
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
                    String oldValue = line.substring((key + "=").length());

                    // Tách các cookie thành mảng theo dấu ;
                    String[] cookies = oldValue.split(";");
                    for (int i = 0; i < cookies.length; i++) {
                        String cookie = cookies[i].trim();
                        if (cookie.startsWith("sid.omnipower.sid=")) {
                            cookies[i] = "sid.omnipower.sid=" + newSidValue;
                            break;
                        }
                    }

                    // Gộp lại thành chuỗi mới
                    String updatedValue = String.join("; ", cookies);
                    content.append(key).append("=").append(updatedValue).append(System.lineSeparator());
                    replaced = true;
                } else {
                    content.append(line).append(System.lineSeparator());
                }
            }
            reader.close();

            // Nếu chưa có key trong file thì thêm mới
            if (!replaced) {
                content.append(key)
                        .append("=")
                        .append("sid.omnipower.sid=").append(newSidValue)
                        .append(System.lineSeparator());
            }

            // Ghi lại vào file
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

            System.out.println("✅ Cookie sid.omnipower.sid đã được cập nhật trong application.properties");

        } catch (IOException e) {
            throw new RuntimeException("❌ Không ghi được file application.properties", e);
        }
    }
}
