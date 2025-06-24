FROM maven:3-openjdk-17-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests


FROM openjdk:17-alpine

WORKDIR /app

# Cài các thư viện cần thiết
RUN apk add --no-cache \
    bash \
    curl \
    unzip \
    chromium \
    chromium-chromedriver \
    nss \
    freetype \
    ttf-freefont \
    fontconfig \
    harfbuzz \
    ca-certificates

# Thiết lập đường dẫn để Selenium tìm thấy Chrome và ChromeDriver
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROMEDRIVER=/usr/bin/chromedriver

# Copy WAR file từ stage build
COPY --from=build /app/target/report-0.0.1-SNAPSHOT.war report.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "report.war"]
