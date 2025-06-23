package com.engagePlus.report.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");  // Headless Chrome mới
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://enablerplus.myharavan.com/admin");

            Thread.sleep(2000);
            WebElement usernameInput = driver.findElement(By.id("Username"));
            WebElement passwordInput = driver.findElement(By.id("Password"));

            usernameInput.sendKeys("duonghuyphi@gmail.com");
            passwordInput.sendKeys("Enablerplus!@#2025");
            driver.findElement(By.id("btn-submit-login")).click();

            Thread.sleep(5000); // chờ chuyển trang

            Cookie sidCookie = driver.manage().getCookieNamed("sid.omnipower.sid");
            if (sidCookie != null) {
                replaceCookieInProperties(sidCookie.getValue());
                System.out.println("✅ Lấy cookie thành công: " + sidCookie.getValue());
            } else {
                throw new RuntimeException("❌ Không tìm thấy cookie sid.omnipower.sid");
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi khi login bằng Selenium", e);
        } finally {
            driver.quit();
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
