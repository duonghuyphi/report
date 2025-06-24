# === Stage 1: Build WAR ===
FROM maven:3-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# === Stage 2: Runtime with Chrome ===
FROM openjdk:17-jdk-slim
WORKDIR /app

# Cài Chrome & ChromeDriver
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    unzip \
    curl \
    fontconfig \
    fonts-liberation \
    libappindicator3-1 \
    libasound2 \
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
    libgbm1 \
    libu2f-udev \
    libvulkan1 \
    && rm -rf /var/lib/apt/lists/*

# Cài Chrome stable
RUN wget -q -O /etc/apt/trusted.gpg.d/google.gpg https://dl.google.com/linux/linux_signing_key.pub && \
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && apt-get install -y google-chrome-stable

# Cài ChromeDriver cố định (hoặc tự động khớp)
ENV CHROMEDRIVER_VERSION=123.0.6312.105
RUN wget -O /tmp/chromedriver.zip https://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && chmod +x /usr/local/bin/chromedriver

# Env cho Chrome & Selenium
ENV CHROME_BIN=/usr/bin/google-chrome \
    CHROMEDRIVER_BIN=/usr/local/bin/chromedriver \
    DBUS_SESSION_BUS_ADDRESS=/dev/null \
    DISPLAY=:99

# Copy WAR từ stage build
COPY --from=build /app/target/report-0.0.1-SNAPSHOT.war report.war

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "report.war"]
