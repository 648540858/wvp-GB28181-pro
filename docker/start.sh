#!/bin/sh
set -e

CONFIG_DIR=/opt/wvp/config
DEFAULT_DIR=/opt/wvp/default-config

# 首次启动：把内置默认配置复制到配置目录；已存在则不覆盖，保留用户修改
for f in application.yml application-standalone-docker.yml; do
  if [ ! -f "${CONFIG_DIR}/${f}" ]; then
    cp "${DEFAULT_DIR}/${f}" "${CONFIG_DIR}/${f}"
    echo "[start.sh] 首次启动，已生成默认配置: ${CONFIG_DIR}/${f}"
  fi
done

cd /opt/wvp
exec java ${JAVA_OPTS:--Xms512m -Xmx1024m} -jar wvp.jar \
  --spring.config.location="${CONFIG_DIR}/application.yml,${CONFIG_DIR}/application-standalone-docker.yml" "$@"
