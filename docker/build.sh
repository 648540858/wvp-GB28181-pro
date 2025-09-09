#!/bin/bash

# 获取当前日期作为标签（格式：YYYYMMDD）
date_tag=$(date +%Y%m%d)

# 切换到脚本所在目录的上一级目录作为工作目录
cd "$(dirname "$0")/.." || {
    echo "错误：无法切换到上级目录"
    exit 1
}
echo "已切换工作目录到：$(pwd)"

# 检查私有仓库环境变量
if [ -z "$DOCKER_REGISTRY" ]; then
    echo "未设置DOCKER_REGISTRY环境变量"
    read -p "请输入私有Docker注册库地址（如不推送请留空）: " input_registry
    docker_registry="$input_registry"
else
    docker_registry="$DOCKER_REGISTRY"
fi

# 定义要构建的镜像和对应的Dockerfile路径（相对当前工作目录）
images=(
    "wvp-service:docker/wvp/Dockerfile"
    "wvp-nginx:docker/nginx/Dockerfile"
)

# 构建镜像的函数
build_image() {
    local image_name="$1"
    local dockerfile_path="$2"
    
    # 检查Dockerfile是否存在
    if [ ! -f "$dockerfile_path" ]; then
        echo "错误：未找到Dockerfile - \"$dockerfile_path\"，跳过构建"
        return 1
    fi
    
    # 构建镜像
    local full_image_name="${image_name}:${date_tag}"
    echo
    echo "=============================================="
    echo "开始构建镜像：${full_image_name}"
    echo "Dockerfile路径：${dockerfile_path}"
    
    docker build -t "${full_image_name}" -f "${dockerfile_path}" .
    if [ $? -ne 0 ]; then
        echo "镜像${full_image_name}构建失败"
        return 1
    fi
    
    # 推送镜像（如果设置了仓库地址）
    if [ -n "$docker_registry" ]; then
        local registry_image="${docker_registry}/${full_image_name}"
        echo "给镜像打标签：${registry_image}"
        docker tag "${full_image_name}" "${registry_image}"
        
        echo "推送镜像到注册库"
        docker push "${registry_image}"
        if [ $? -eq 0 ]; then
            echo "镜像${registry_image}推送成功"
        else
            echo "镜像${registry_image}推送失败"
        fi
    else
        echo "未提供注册库地址，不执行推送"
    fi
    echo "=============================================="
    echo
}

# 循环构建所有镜像
for item in "${images[@]}"; do
    IFS=':' read -r image_name dockerfile_path <<< "$item"
    build_image "$image_name" "$dockerfile_path"
done

echo "所有镜像处理完成"
exit 0
