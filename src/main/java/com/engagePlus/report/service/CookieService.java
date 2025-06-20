package com.engagePlus.report.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

@Service
public class CookieService {

    public void fetchSidCookie() {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();

        try {
            driver.get("https://enablerplus.myharavan.com/admin");

            Thread.sleep(2000);

            WebElement usernameInput = driver.findElement(By.id("Username"));
            WebElement passwordInput = driver.findElement(By.id("Password"));

            usernameInput.sendKeys("duonghuyphi@gmail.com");
            passwordInput.sendKeys("Enablerplus!@#2025");

            WebElement loginButton = driver.findElement(By.id("btn-submit-login"));
            loginButton.click();

            Thread.sleep(5000);

            Cookie sidCookie = driver.manage().getCookieNamed("sid.omnipower.sid");
            if (sidCookie != null) {
                replaceCookie(sidCookie.getValue());
            } else {
                System.out.println("❌ Không tìm thấy cookie sid.omnipower.sid.");
            }

        } catch (Exception e) {
            System.out.println("❌ Lỗi xảy ra: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private void replaceCookie(String sidCk) {
        String filePath = "src/main/resources/application.properties";
        String key = "haravan.cookie";

        try {
            // Đọc toàn bộ file thành từng dòng
            File file = new File(filePath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(key + "=")) {
                    // Replace sid.omnipower.sid in the line
                    String updatedLine = line.replaceAll("sid\\.omnipower\\.sid=([^;]*)", "sid.omnipower.sid=" + sidCk);
                    content.append(updatedLine).append(System.lineSeparator());
                } else {
                    content.append(line).append(System.lineSeparator());
                }
            }
            reader.close();

            // Ghi lại toàn bộ nội dung
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

            System.out.println("✅ Cookie đã được cập nhật thành công.");
        } catch (IOException e) {
            throw new RuntimeException("❌ Ghi file application.properties thất bại", e);
        }
    }
}
