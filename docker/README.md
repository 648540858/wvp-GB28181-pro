# WVP-GB28181-Pro Docker 部署

`docker compose` 一键部署：**WVP**（本项目镜像：H2 文件数据库 + 代码内置内存模拟 Redis，无需外部 MySQL/Redis）+ **ZLMediaKit 官方镜像**。

## 目录结构

```
docker/
├── .env                  # 部署配置(端口/SIP/流媒体IP等)
├── docker-compose.yml    # 一键部署(wvp + zlm)
├── Dockerfile            # WVP 镜像(JRE21 + jar, 容器内不编译)
├── build.sh              # 构建镜像(本地无 jar 时自动 mvn 编译)
├── push.sh               # 推送镜像到 Docker Hub
├── start.sh              # 容器启动脚本(首次启动自动生成默认配置)
├── default-config/       # 内置默认配置(打进镜像)
├── media/config.ini      # ZLM 配置(hook 指向 wvp 服务)
├── config/               # [自动生成] WVP 配置(可修改, 重启生效)
├── data/                 # [自动生成] H2 数据库 / ZLM 录像
└── logs/                 # [自动生成] 日志
```

## 方式一：直接使用已推送的镜像

1. `.env` 中填写你的 Docker Hub 用户名：

   ```
   DOCKER_HUB_USER=你的dockerhub用户名
   ```

2. **必须修改** `.env` 中 `STREAM_IP` / `SDP_IP`：客户端访问流媒体用的 IP（局域网部署填宿主机内网 IP，公网部署填公网 IP）。

3. 启动并查看日志：

   ```bash
   docker compose up -d
   docker compose logs -f wvp
   ```

4. 浏览器访问 `http://<宿主机IP>:18978`，默认账号 `admin` / `admin`。

## 方式二：本地构建镜像（并可推送到 Docker Hub）

本地需要 **JDK 21 + Maven**。

```bash
cd docker
./build.sh              # 构建镜像; target/ 下没有 jar 时自动 mvn 编译, 加 -r 强制重编
docker login            # 登录 Docker Hub(推送时需要)
./push.sh               # 推送; DOCKER_HUB_USER 未设置时交互输入用户名
```

推送完成后在 `.env` 中设置 `DOCKER_HUB_USER`，然后 `docker compose up -d`。

> 镜像为单架构，在部署目标机器上构建即可（x86_64 机器构建出 x86_64 镜像，arm 机器构建出 arm 镜像），无需交叉编译。

## 方式三：WVP 用本项目镜像，ZLM 单独用官方镜像

ZLM 已部署或希望单独部署时，先启动官方镜像（与代码 master 分支保持最新）：

```bash
docker run -d --name zlm --restart unless-stopped \
  -p 8080:80 -p 8443:443 \
  -p 1935:1935 -p 8554:554 \
  -p 10000:10000 -p 10000:10000/udp \
  -p 8000:8000/udp -p 9000:9000/udp \
  -p 30000-30500:30000-30500 -p 30000-30500:30000-30500/udp \
  zlmediakit/zlmediakit:master
```

再启动 WVP，通过环境变量关联 ZLM：

```bash
docker run -d --name wvp --restart unless-stopped \
  -p 18978:18978 -p 8116:8116/udp -p 8116:8116/tcp \
  -e ZLM_HOST=<ZLM所在IP> \
  -e ZLM_HOOK_HOST=<ZLM能访问到本机的IP> \
  -e STREAM_IP=<宿主机内网/公网IP> \
  -e SDP_IP=<一般与STREAM_IP相同> \
  -e ZLM_SECRET=<与ZLM配置中api.secret一致> \
  -v $PWD/config:/opt/wvp/config \
  -v $PWD/data/wvp:/opt/wvp/data \
  -v $PWD/logs/wvp:/opt/wvp/logs \
  <DOCKER_HUB_USER>/wvp-gb28181-pro:2.7.4
```

## 配置管理

- 首次启动时，容器自动把内置默认配置（`/opt/wvp/default-config/`）复制到配置目录（compose 部署即 `./config/`），**已存在的文件不会覆盖**。
- **修改配置**：直接编辑 `./config/` 下的文件，然后 `docker compose restart wvp` 生效。
- **恢复默认**：删除 `./config/` 下对应文件，下次启动重新生成。
- 常用修改项：

| 配置项 | 位置 | 说明 |
|---|---|---|
| Web 端口 / SIP 端口 / 国标编码 | `.env` | `WVP_HTTP_PORT`、`SIP_Port`、`SIP_Domain`、`SIP_Id`、`SIP_Password` |
| 流媒体 IP | `.env` | `STREAM_IP`、`SDP_IP`（必须按实际环境修改） |
| ZLM 地址 / 回调地址 | `./config/application-standalone-docker.yml` | `media.ip`、`media.hook-ip` |
| ZLM 端口 / hook | `media/config.ini` | 需与 compose 中 zlm 端口映射对应 |
| 功能开关 / 日志级别 | `./config/application-standalone-docker.yml` | `user-settings`、`logging` |

## 数据持久化

| 宿主目录 | 内容 |
|---|---|
| `docker/data/wvp/` | H2 数据库（设备、通道、用户等） |
| `docker/data/zlm/record/` | ZLM 录像 |
| `docker/logs/wvp/`、`docker/logs/zlm/` | 日志 |
| `docker/config/` | WVP 配置 |

## FAQ

- **内存模拟 Redis 是什么？** 未配置 `spring.data.redis.host` 时，项目内部用内存结构模拟 Redis（缓存、信令转发等），无需部署 Redis 服务；代价是重启后内存中的流状态清空，设备会自动重新注册恢复。
- **端口冲突**：修改 `.env` 中宿主机端口（`WVP_HTTP_PORT`、`SIP_Port`、`ZLM_*`、`RTP_RANGE_*`），并在防火墙/云安全组放行对应端口（RTP 多端口需放行整个范围）。
- **升级版本**：`./build.sh` + `./push.sh` 重新构建推送，`.env` 中 `VERSION` 改为新版本后 `docker compose pull && docker compose up -d`。
