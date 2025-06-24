# ---------- Stage 1: Build the JAR ----------
FROM eclipse-temurin:17-jdk AS builder

# Cài công cụ cần thiết
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Copy source code vào container
WORKDIR /app
COPY . .

# ✅ Cấp quyền thực thi cho mvnw (sửa lỗi 126 trên Render)
RUN chmod +x mvnw

# Build project (bỏ qua test nếu cần)
RUN ./mvnw package -DskipTests

# ---------- Stage 2: Runtime with Chrome for Selenium ----------
FROM eclipse-temurin:17-jdk

# Cài Chrome + thư viện phụ thuộc
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
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Cài Google Chrome
RUN wget -q https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_115.0.5790.102-1_amd64.deb && \
    apt-get update && \
    apt-get install -y ./google-chrome-stable_115.0.5790.102-1_amd64.deb && \
    rm google-chrome-stable_115.0.5790.102-1_amd64.deb && \
    rm -rf /var/lib/apt/lists/*

# Tạo thư mục chạy ứng dụng
WORKDIR /app

# Copy file JAR đã build từ stage trước
COPY --from=builder /app/target/report-0.0.1-SNAPSHOT.jar app.jar

# Mở cổng ứng dụng Spring Boot
EXPOSE 8080

# Nếu dùng Chrome headless thì cần set biến môi trường
ENV CHROME_BIN=/usr/bin/google-chrome \
    CHROME_FLAGS="--no-sandbox --headless --disable-gpu --disable-dev-shm-usage"

# Khởi chạy app
ENTRYPOINT ["java", "-jar", "app.jar"]