# ============================================
# WVP 单机镜像
# jar 在本地构建（docker/build.sh），本文件只负责把 jar 装进镜像
# 默认使用 H2 文件数据库 + 代码内内存版 Redis，无需 MySQL / Redis
# ============================================

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="wvp-GB28181-pro" \
      description="WVP standalone: H2 + in-memory redis, no MySQL/Redis"

RUN apk add --no-cache curl bash tzdata && rm -rf /var/cache/apk/*

ENV TZ=Asia/Shanghai
ENV CONFIG_DIR=/config
ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError"

WORKDIR /opt/wvp

# jar 由 docker/build.sh 从 target 复制为 docker/wvp/wvp.jar
COPY docker/wvp/wvp.jar /opt/wvp/wvp.jar
# 默认配置与密钥，放在 jar 旁边，启动时按需复制到 /config
COPY docker/wvp/application-default.yml docker/wvp/jwk.json /opt/wvp/
COPY docker/wvp/start.sh /opt/wvp/start.sh

RUN chmod +x /opt/wvp/start.sh && \
    mkdir -p /opt/wvp/data /opt/wvp/www/record /opt/wvp/logs

EXPOSE 18978/tcp
EXPOSE 8116/tcp
EXPOSE 8116/udp
EXPOSE 40001-40500/tcp
EXPOSE 40001-40500/udp

VOLUME ["/config", "/opt/wvp/data", "/opt/wvp/www/record", "/opt/wvp/logs"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:18978/ || exit 1

ENTRYPOINT ["/opt/wvp/start.sh"]
