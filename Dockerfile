FROM eclipse-temurin:17-jdk AS builder

# Cài Maven wrapper & build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw package -DskipTests

FROM eclipse-temurin:17-jdk

# Cài Chrome mới
RUN apt-get update && apt-get install -y wget curl gnupg unzip \
    fonts-liberation libappindicator3-1 libatk-bridge2.0-0 libatk1.0-0 libcups2 \
    libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 libx11-xcb1 libxcomposite1 \
    libxdamage1 libxrandr2 xdg-utils --no-install-recommends && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && apt-get install -y google-chrome-stable && rm -rf /var/lib/apt/lists/*

# Copy jar
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENV CHROME_BIN=/usr/bin/google-chrome \
    CHROME_FLAGS="--no-sandbox --headless --disable-gpu --disable-dev-shm-usage"

ENTRYPOINT ["java", "-jar", "app.jar"]
