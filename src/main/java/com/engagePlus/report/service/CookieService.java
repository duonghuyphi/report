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
                if (cookie.contains("sid.omnipower.sid")) {
                    String sid = cookie.split(";")[0].split("=")[1];
                    replaceCookieInProperties(sid);
                    System.out.println("✅ Lưu cookie thành công: " + sid);
                    return;
                }
            }

            System.out.println("❌ Không tìm thấy cookie sid.omnipower.sid");

        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi khi lấy cookie bằng HttpClient", e);
        }
    }

    private static HttpRequest.BodyPublisher buildFormData(Map<String, String> data) {
        String encoded = data.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return HttpRequest.BodyPublishers.ofString(encoded);
    }

    private void replaceCookieInProperties(String sidCk) {
        String filePath = "src/main/resources/application.properties";
        String key = "haravan.cookie";

        try {
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(key + "=")) {
                    content.append(key).append("=").append("sid.omnipower.sid=").append(sidCk)
                            .append(System.lineSeparator());
                } else {
                    content.append(line).append(System.lineSeparator());
                }
            }
            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

            System.out.println("✅ Cookie đã được lưu vào file application.properties");

        } catch (IOException e) {
            throw new RuntimeException("❌ Lỗi ghi file", e);
        }
    }

}
