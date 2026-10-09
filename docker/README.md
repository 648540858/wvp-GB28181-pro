# 单机部署（WVP + ZLMediaKit）

不依赖 MySQL 和 Redis：WVP 使用 **H2 文件数据库** 和 **代码内的内存版 Redis 门面**（未配置 `spring.data.redis.host` 时启用）。

## 构建

jar 在本地构建，Dockerfile 只负责把 jar 引入镜像（容器内不编译）。

```bash
cd docker
bash build.sh
```

`build.sh` 依次执行：

1. `npm install` + `npm run build:prod`（前端输出到 `src/main/resources/static`，打进 jar）
2. `mvn clean package -Dmaven.test.skip=true`（`clean`，总是重新构建 jar）
3. 归一化 jar 名字为 `docker/wvp/wvp.jar`
4. `docker build -t wvp-service:<日期> -f docker/wvp/Dockerfile .`

可选项：

- `--skip-web` 跳过前端构建（复用已有的 `src/main/resources/static`）
- `--skip-jar` 跳过 maven 构建（复用已有的 jar）
- 设置 `DOCKER_REGISTRY` 环境变量则构建后推送到私有仓库

## 运行

```bash
cd docker
docker compose up -d
```

访问 `http://127.0.0.1:18978`，默认账号 `admin` / `admin123`（首次登录后修改）。

## 配置

- 镜像内 `/opt/wvp/application-default.yml` 是默认配置，放在 jar 旁边。
- 启动脚本 `start.sh` 检查 `/config`：目录内没有 `application-default.yml` 时，把镜像内的默认配置和 `jwk.json` 复制过去，然后以 `--spring.config.location=optional:file:/config/` 启动。
- 因此自定义配置的做法：把 `application-default.yml` 放到挂载的配置目录（`docker/wvp/config`）里改，容器启动即使用它。
- 配置项全部支持环境变量覆盖（见 `docker/.env`），`ZLM_SECRET`、`RTP_RANGE_*` 必须与 `docker/media/config.ini` 保持一致。

## 数据

| 挂载 | 容器内 | 内容 |
| --- | --- | --- |
| `./wvp/config` | `/config` | 配置文件与 `jwk.json` |
| `./wvp/data` | `/opt/wvp/data` | H2 数据库文件 `wvp.mv.db` |
| `./volumes/video` | `/opt/wvp/www/record` 与 `/opt/media/bin/www/record` | 录像 |
| `./logs/wvp` | `/opt/wvp/logs` | WVP 日志 |
| `./logs/media` | `/opt/media/log` | ZLM 日志 |

## 端口

| 端口 | 用途 |
| --- | --- |
| `18978` | WVP HTTP（页面 + 接口） |
| `8116` tcp/udp | SIP 信令 |
| `10001`/`10004`/`10003` | ZLM 收流 RTMP/RTSP/RTP（单端口模式，`10003` 即 RTP 收流端口） |
| `40001-40500` tcp/udp | 国标级链发送端口范围，需与 `config.ini` 的 `[rtp_proxy] port_range` 一致 |

外部设备接入时，把 `.env` 里的 `SIP_ShowIP`、`Stream_IP`、`SDP_IP` 改成本机对外 IP。

## 目录内文件

```
docker/
├── build.sh                # 本地构建：前端 + jar + 镜像
├── docker-compose.yml      # wvp + zlm 部署
├── .env                    # 环境变量联动配置
├── media/config.ini        # ZLM 配置（官方镜像挂载此文件）
└── wvp/
    ├── Dockerfile            # 镜像定义，jar 直接引入
    ├── wvp.jar               # build.sh 生成
    ├── application-default.yml # 默认配置，放在 jar 旁边
    ├── jwk.json              # 默认 JWT 密钥
    ├── start.sh              # 启动脚本，把默认配置到 /config 时按需
    ├── config/               # 挂载的 /config（运行时）
    └── data/                 # 挂载的 /opt/wvp/data（运行时，H2 数据库）
```
