#!/bin/bash
# ============================================
# 构建 WVP Docker 镜像
# 流程: 本地 mvn 编译出 jar -> 暂存目录最小化构建上下文 -> docker build(容器内不编译)
#
# 用法:
#   ./build.sh        使用 target/ 下已有 jar, 没有则自动编译
#   ./build.sh -r     强制重新编译
#
# 镜像名: ${DOCKER_HUB_USER}/wvp-gb28181-pro:${VERSION}
# (DOCKER_HUB_USER/VERSION 默认读取本目录 .env)
# ============================================
set -e
cd "$(dirname "$0")"
REPO_ROOT="$(cd .. && pwd)"

get_env() { grep -E "^$1=" .env 2>/dev/null | head -1 | cut -d= -f2-; }

VERSION="${VERSION:-$(get_env VERSION)}"
VERSION="${VERSION:-2.7.4}"
DOCKER_HUB_USER="${DOCKER_HUB_USER:-$(get_env DOCKER_HUB_USER)}"
DOCKER_HUB_USER="${DOCKER_HUB_USER:-wvp}"

JAR=$(ls "${REPO_ROOT}"/target/wvp-pro-*.jar 2>/dev/null | head -1 || true)
if [ -z "$JAR" ] || [ "$1" = "-r" ]; then
  echo "==> 未找到 jar(或指定 -r), 开始本地编译, 首次约几分钟..."
  (cd "$REPO_ROOT" && mvn clean package -Dmaven.test.skip=true)
  JAR=$(ls "${REPO_ROOT}"/target/wvp-pro-*.jar | head -1)
fi
echo "==> 使用 jar: ${JAR}"

# 最小化构建上下文(避免把大目录/权限受限目录带入)
STAGE=$(mktemp -d)
trap 'rm -rf "${STAGE}"' EXIT
mkdir -p "${STAGE}/target" "${STAGE}/docker/default-config" "${STAGE}/src/main/resources"
cp "${JAR}" "${STAGE}/target/"
cp Dockerfile start.sh "${STAGE}/docker/"
cp default-config/application.yml "${STAGE}/docker/default-config/"
cp "${REPO_ROOT}/src/main/resources/application-standalone-docker.yml" "${STAGE}/src/main/resources/"

IMAGE="wvp-gb28181-pro:${VERSION}"
echo "==> 构建镜像 ${IMAGE} ..."
# --network=host: 某些环境容器DNS不可用, 用宿主网络构建
docker build --network=host -f "${STAGE}/docker/Dockerfile" -t "${IMAGE}" "${STAGE}"
docker tag "${IMAGE}" "${DOCKER_HUB_USER}/${IMAGE}"

echo "==> 完成: ${IMAGE} (别名 ${DOCKER_HUB_USER}/${IMAGE})"
echo "    推送: ./push.sh    部署: docker compose up -d"
