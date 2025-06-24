package com.engagePlus.report.controller;

import com.engagePlus.report.service.CookieService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CookieController {

    private final CookieService seleniumService;

    public CookieController(CookieService seleniumService) {
        this.seleniumService = seleniumService;
    }

    @GetMapping("/getCookie")
    public ResponseEntity<?> getCookie() {
        String cookie = seleniumService.getSidCookie();
        return ResponseEntity.ok(Map.of("cookie", cookie));
    }
}
