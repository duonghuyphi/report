# Stage 1: Build Java WAR
FROM maven:3-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests


# Stage 2: Run Java app + Headless Chrome + Chromedriver
FROM debian:bullseye-slim

# Cài Chrome & phụ thuộc
RUN apt-get update && apt-get install -y wget curl unzip gnupg ca-certificates \
    fonts-liberation libappindicator3-1 libasound2 libatk-bridge2.0-0 \
    libatk1.0-0 libcups2 libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 \
    libx11-xcb1 libxcomposite1 libxdamage1 libxrandr2 libgbm1 libxshmfence1 \
    libxss1 libxtst6 chromium && rm -rf /var/lib/apt/lists/*

# Cài ChromeDriver
RUN CHROMEDRIVER_VERSION=`curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE` && \
    wget -O /tmp/chromedriver.zip "https://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip" && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && chmod +x /usr/local/bin/chromedriver && rm /tmp/chromedriver.zip

# Copy app
WORKDIR /app
COPY --from=build /app/target/report-0.0.1-SNAPSHOT.war report.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "report.war"]
