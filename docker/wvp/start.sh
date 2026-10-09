#!/bin/sh
set -e

CONFIG_DIR=${CONFIG_DIR:-/config}
APP_DIR=/opt/wvp
DEFAULT_CONFIG=${CONFIG_FILE:-application-default.yml}

mkdir -p "$CONFIG_DIR" "$APP_DIR/data" "$APP_DIR/www/record"

# 未挂载配置目录、或目录内没有配置文件时，把镜像内的默认配置复制过去使用
if [ ! -f "$CONFIG_DIR/$DEFAULT_CONFIG" ]; then
    echo "[start] $CONFIG_DIR 中没有 $DEFAULT_CONFIG ，复制镜像内的默认配置"
    cp "$APP_DIR/$DEFAULT_CONFIG" "$CONFIG_DIR/"
fi

if [ ! -f "$CONFIG_DIR/jwk.json" ]; then
    echo "[start] $CONFIG_DIR 中没有 jwk.json ，复制镜像内的默认密钥"
    cp "$APP_DIR/jwk.json" "$CONFIG_DIR/"
fi

echo "[start] 使用配置目录: $CONFIG_DIR"
ls -l "$CONFIG_DIR"

exec java $JAVA_OPTS -jar "$APP_DIR/wvp.jar" \
    --spring.config.location="optional:classpath:/,optional:file:${CONFIG_DIR}/"
