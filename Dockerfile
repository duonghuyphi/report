# ---------- Stage 1: Build the JAR ----------
FROM eclipse-temurin:17-jdk AS builder

# Cài các công cụ cần thiết
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Copy source code vào container
WORKDIR /app
COPY . .

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

# Cài Chrome
RUN curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# App runtime
WORKDIR /app
COPY --from=builder /app/target/report-0.0.1-SNAPSHOT.jar app.jar

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]