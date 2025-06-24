# Base image có Java 17
FROM eclipse-temurin:17-jdk

# Cài các công cụ cần thiết
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libappindicator3-1 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdbus-1-3 \
    libgdk-pixbuf2.0-0 \
    libnspr4 \
    libnss3 \
    libx11-xcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    xdg-utils \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# Cài Google Chrome Stable
RUN curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" | \
    tee /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && apt-get install -y google-chrome-stable

# Tạo thư mục chứa ứng dụng
WORKDIR /app

# Copy file jar đã build từ local sang image
COPY ./target/report-0.0.1-SNAPSHOT.jar app.jar

# Mặc định chạy Chrome headless với các flag
ENV _JAVA_OPTIONS="-Dwebdriver.chrome.driver=/usr/bin/chromedriver"

# Port expose cho Spring Boot
EXPOSE 8080

# Command để chạy Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
