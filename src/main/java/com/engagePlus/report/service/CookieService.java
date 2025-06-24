package com.engagePlus.report.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Duration;

@Service
public class CookieService {
    @Value("${haravan.username}")
    private String Husername;

    @Value("${haravan.password}")
    private String Hpassword;

    public String getSidCookie() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/google-chrome"); // nếu dùng Docker
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("user-agent=Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        String sidValue = null;

        try {
            driver.get("https://accounts.haravan.com/account/login");

            WebElement emailInput = driver.findElement(By.id("Username"));
            WebElement passInput = driver.findElement(By.id("Password"));
            WebElement loginBtn = driver.findElement(By.id("btn-submit-login"));

            emailInput.sendKeys(Husername);
            passInput.sendKeys(Hpassword);
            loginBtn.click();

            // Chờ chuyển hướng sau khi login
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.urlToBe("https://enablerplus.myharavan.com/admin"));

            Thread.sleep(3000);
            Cookie sidCookie = driver.manage().getCookieNamed("sid.omnipower.sid");
            if (sidCookie != null) {
                sidValue = sidCookie.getValue();
                replaceCookieInProperties(sidValue);
            } else {
                System.err.println("❌ Không tìm thấy cookie sid.omnipower.sid – có thể login thất bại hoặc redirect chưa đúng");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return sidValue != null ? sidValue : "";
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
                    String cookieString = line.substring((key + "=").length());
                    String updatedCookieString = cookieString.replaceAll(
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

            if (!replaced) {
                content.append(key)
                        .append("=sid.omnipower.sid=")
                        .append(newSidValue)
                        .append(System.lineSeparator());
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

            System.out.println("✅ Đã cập nhật đúng phần sid.omnipower.sid");

        } catch (IOException e) {
            throw new RuntimeException("❌ Không thể ghi file application.properties", e);
        }
    }
}
