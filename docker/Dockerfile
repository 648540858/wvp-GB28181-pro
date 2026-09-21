FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tzdata ca-certificates \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m"

WORKDIR /opt/wvp

# WVP 主程序（本地 mvn clean package -Dmaven.test.skip=true 产物）
COPY target/wvp-pro-*.jar ./wvp.jar

# 内置默认配置（首次启动由 start.sh 复制到 /opt/wvp/config/）
COPY docker/default-config/application.yml /opt/wvp/default-config/
COPY src/main/resources/application-standalone-docker.yml /opt/wvp/default-config/
COPY docker/start.sh /opt/wvp/start.sh
RUN chmod +x /opt/wvp/start.sh

# 18978: HTTP API / Web UI    8116: SIP 信令
EXPOSE 18978 8116/tcp 8116/udp

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fs http://127.0.0.1:18978/ || exit 1

ENTRYPOINT ["/opt/wvp/start.sh"]
