#!/bin/bash
# ============================================
# 推送 WVP 镜像到 Docker Hub
#
# 用法:
#   ./push.sh
# DOCKER_HUB_USER: 优先取环境变量, 其次本目录 .env, 都为空则交互输入
# 请先 docker login
# ============================================
set -e
cd "$(dirname "$0")"

get_env() { grep -E "^$1=" .env 2>/dev/null | head -1 | cut -d= -f2-; }

VERSION="${VERSION:-$(get_env VERSION)}"
VERSION="${VERSION:-2.7.4}"
DOCKER_HUB_USER="${DOCKER_HUB_USER:-$(get_env DOCKER_HUB_USER)}"

if [ -z "$DOCKER_HUB_USER" ]; then
  read -p "请输入 Docker Hub 用户名: " DOCKER_HUB_USER
fi
if [ -z "$DOCKER_HUB_USER" ]; then
  echo "Docker Hub 用户名不能为空"
  exit 1
fi

IMAGE="wvp-gb28181-pro:${VERSION}"
PUSH_IMAGE="${DOCKER_HUB_USER}/${IMAGE}"
LATEST="${DOCKER_HUB_USER}/wvp-gb28181-pro:latest"

# build.sh 可能已打过带前缀的 tag, 优先使用
if docker image inspect "${PUSH_IMAGE}" >/dev/null 2>&1; then
  SRC="${PUSH_IMAGE}"
elif docker image inspect "${IMAGE}" >/dev/null 2>&1; then
  SRC="${IMAGE}"
else
  echo "错误: 未找到本地镜像 ${IMAGE}, 请先运行 ./build.sh"
  exit 1
fi

echo "==> 推送 ${PUSH_IMAGE} 和 ${LATEST} (请确保已 docker login)"
docker tag "${SRC}" "${PUSH_IMAGE}"
docker tag "${SRC}" "${LATEST}"
docker push "${PUSH_IMAGE}"
docker push "${LATEST}"

echo "==> 推送完成"
echo "    部署时在 docker/.env 中设置 DOCKER_HUB_USER=${DOCKER_HUB_USER}, 再执行 docker compose up -d"
