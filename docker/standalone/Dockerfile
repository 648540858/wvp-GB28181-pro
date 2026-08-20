# ============================================
# WVP Standalone Dockerfile
# Combines WVP + ZLM + Nginx in a single container
# Dependencies eliminated: MySQL, Redis
# Built-in: H2 Embedded DB, Caffeine Cache
# ============================================

# ---------- Stage 1: Build WVP JAR ----------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -Dmaven.test.skip=true -B

# ---------- Stage 2: Prepare nginx static files ----------
FROM node:20-alpine AS web-builder

# Install build dependencies for frontend
RUN apk add --no-cache git

# Copy web frontend source
COPY web/ /build/web
WORKDIR /build/web

# Install and build frontend
RUN npm --registry=https://registry.npmmirror.com install 2>/dev/null || npm install
RUN npm run build:prod 2>/dev/null || (echo "build:prod failed, trying vite build directly" && npx vite build)

# ---------- Stage 3: Final image ----------
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="wvp-GB28181-pro" \
      description="WVP Standalone: WVP + ZLMediaKit + Nginx (No MySQL, No Redis)" \
      version="2.7.4"

# Install runtime dependencies
RUN apk add --no-cache \
    nginx \
    curl \
    bash \
    tzdata \
    ffmpeg \
    ca-certificates \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# ---- WVP ----
WORKDIR /opt/wvp
COPY --from=builder /build/target/*.jar wvp.jar
# Copy Spring config (application.yml with profile=standalone)
COPY docker/wvp/wvp/application.yml /opt/wvp/config/application.yml
# Copy H2 schema and data (for reference; Spring will load them from classpath)
COPY src/main/resources/db/h2-schema.sql /opt/wvp/config/h2-schema.sql
COPY src/main/resources/db/h2-data.sql /opt/wvp/config/h2-data.sql

# ---- ZLMediaKit ----
WORKDIR /opt/media
ARG ZLM_VERSION=release
# Download pre-built ZLM binary from GitHub/Gitee release
# Fallback: build from source if download fails
RUN mkdir -p bin www conf data && \
    if command -v wget >/dev/null 2>&1; then \
        DLTOOL="wget --no-check-certificate -O"; \
    else \
        DLTOOL="curl -fsL -o"; \
    fi; \
    # Try to download ZLM release (x86_64 Linux)
    $DLTOOL /tmp/zlm.tar.gz https://github.com/ZLMediaKit/ZLMediaKit/releases/latest/download/MediaServer-linux-x64.tar.gz 2>/dev/null && \
    tar xzf /tmp/zlm.tar.gz -C /opt/media/bin/ 2>/dev/null && \
    rm /tmp/zlm.tar.gz && \
    echo "ZLM downloaded successfully" || \
    echo "WARNING: ZLM download failed, will copy from build context"

COPY docker/standalone/zlm-binary /opt/media/bin/ 2>/dev/null || true

# ---- Nginx (serving frontend static files) ----
COPY --from=web-builder /build/web/dist /opt/dist
COPY docker/standalone/nginx.conf /etc/nginx/nginx.conf
COPY docker/standalone/conf.d/ /etc/nginx/conf.d/

# ---- Startup script ----
COPY docker/standalone/start.sh /opt/wvp/start.sh
RUN chmod +x /opt/wvp/start.sh

# Create data directories for persistence
RUN mkdir -p /opt/wvp/data /opt/media/data /opt/wvp/logs && \
    chown -R nginx:nginx /opt/wvp/data /opt/media/data /opt/wvp/logs

EXPOSE 18978/tcp    # WVP HTTP API
EXPOSE 8116/tcp     # SIP TCP
EXPOSE 8116/udp     # SIP UDP
EXPOSE 8080/tcp     # Nginx (Web Frontend)
EXPOSE 10001/tcp    # RTMP
EXPOSE 10001/udp    # RTMP
EXPOSE 10002/tcp    # RTSP
EXPOSE 10002/udp    # RTSP
EXPOSE 10003/tcp    # RTP Proxy
EXPOSE 10003/udp    # RTP Proxy
EXPOSE 30000-30500/tcp   # RTP Port Range (multi-port mode)
EXPOSE 30000-30500/udp   # RTP Port Range (multi-port mode)

VOLUME ["/opt/wvp/data", "/opt/media/data", "/opt/wvp/logs"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:18978/actuator/health || exit 1

ENTRYPOINT ["/opt/wvp/start.sh"]
