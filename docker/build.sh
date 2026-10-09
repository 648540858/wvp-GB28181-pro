#!/bin/bash
set -e

# ============================================
# 本地构建流程：
#   1. 构建前端（输出到 src/main/resources/static，打进 jar）
#   2. mvn clean package 重新构建 jar（总是重建）
#   3. 归一化 jar 名字到 docker/wvp/wvp.jar
#   4. docker build 生成镜像（jar 直接引入镜像，不在容器内编译）
# ============================================

cd "$(dirname "$0")/.." || exit 1
echo "工作目录: $(pwd)"

date_tag=$(date +%Y%m%d)
image_name="wvp-service"
skip_web=false
skip_jar=false

for arg in "$@"; do
    case "$arg" in
        --skip-web) skip_web=true ;;
        --skip-jar) skip_jar=true ;;
    esac
done

# ---------- 1. 前端 ----------
if [ "$skip_web" = false ]; then
    echo "=============================================="
    echo "构建前端 (web -> src/main/resources/static)"
    echo "=============================================="
    if [ ! -d web/node_modules ]; then
        ( cd web && npm --registry=https://registry.npmmirror.com install )
    else
        echo "web/node_modules 已存在，跳过 npm install"
    fi
    ( cd web && npm run build:prod )
fi

# ---------- 2. jar ----------
if [ "$skip_jar" = false ]; then
    echo "=============================================="
    echo "构建 jar (mvn clean package)"
    echo "=============================================="
    mvn clean package -Dmaven.test.skip=true -B
fi

# ---------- 3. 归一化 jar ----------
jar_file=$(ls -t target/wvp-pro-*.jar 2>/dev/null | head -1)
if [ -z "$jar_file" ]; then
    echo "错误：target 下没有找到 jar"
    exit 1
fi
echo "使用 jar: $jar_file"
cp "$jar_file" docker/wvp/wvp.jar

# ---------- 4. 镜像 ----------
echo "=============================================="
echo "构建镜像：${image_name}:${date_tag}"
echo "=============================================="
docker build -t "${image_name}:${date_tag}" -f docker/wvp/Dockerfile .

full_name="${image_name}:latest"
docker tag "${image_name}:${date_tag}" "$full_name"
echo "已打标签: $full_name"

if [ -n "$DOCKER_REGISTRY" ]; then
    registry_image="${docker_registry}/${image_name}:${date_tag}"
    echo "打标签并推送: $registry_image"
    docker tag "${image_name}:${date_tag}" "$registry_image"
    docker push "$registry_image"
fi

echo "完成。镜像: ${image_name}:${date_tag} / ${image_name}:latest"
