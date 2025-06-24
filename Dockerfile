# Stage 1: Build với Maven (Alpine)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests


# Stage 2: Runtime với Alpine + Chrome + ChromeDriver
FROM eclipse-temurin:17-alpine
WORKDIR /app

# Cài Chrome & ChromeDriver từ Alpine repo
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

# ENV để Selenium tìm được trình duyệt
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROMEDRIVER=/usr/bin/chromedriver

COPY --from=build /app/target/report-0.0.1-SNAPSHOT.war report.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "report.war"]