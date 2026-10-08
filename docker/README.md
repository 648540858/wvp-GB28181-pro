当前目录下可直接使用 Docker Compose 运行整套环境。

- `docker compose up -d`：直接启动服务
- `docker compose up -d --build --force-recreate`：强制重建镜像并重建容器

`.env` 用来配置环境变量，修改后会自动作用到 `docker-compose.yml` 中引用的配置。

`build.sh` 会按当天日期为镜像打 tag，并在配置了 `DOCKER_REGISTRY` 时推送到指定仓库。

当前架构中，前端静态资源和 nginx 已合并进 `polaris-wvp` 容器，不再使用独立的 nginx 容器。
