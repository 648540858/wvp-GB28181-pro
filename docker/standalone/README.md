# WVP-GB28181-Pro Standalone Docker Image

> **Single container** deployment with WVP + ZLMediaKit + Nginx.  
> No external MySQL or Redis required — uses H2 embedded database and Caffeine cache.

## Quick Start

```bash
# 1. Clone and enter the project
git clone https://gitee.com/64468625/wvp-GB28181-pro.git
cd wvp-GB28181-pro/docker/standalone

# 2. Build the image (first time, may take 10-20 minutes)
docker compose build

# 3. Start the services
docker compose up -d

# 4. Check status
docker compose ps
docker compose logs -f

# 5. Access
#    Web Frontend: http://localhost:8080
#    WVP API:      http://localhost:18978
#    SIP Port:     8116 (TCP/UDP)
```

## Port Mapping

| Port    | Protocol | Service                    |
|---------|----------|----------------------------|
| 18978   | TCP      | WVP HTTP API               |
| 8080    | TCP      | Nginx Web Frontend         |
| 8116    | TCP/UDP  | SIP (GB28181 signaling)    |
| 10001   | TCP/UDP  | RTMP                       |
| 10002   | TCP/UDP  | RTSP                       |
| 10003   | TCP/UDP  | RTP Proxy                  |
| 30000-30500 | TCP/UDP | RTP Port Range (multi-port mode) |

Ports are configurable via environment variables (see `.env`).

## Directory Structure

```
docker/standalone/
├── Dockerfile              # Multi-stage build definition
├── docker-compose.yml      # Docker Compose configuration
├── start.sh                # Container entrypoint script
├── config.ini              # ZLMediaKit configuration (localhost hooks)
├── nginx.conf              # Main Nginx configuration
├── conf.d/
│   ├── default.conf        # Nginx server block (reverse proxy)
│   └── config.js           # Frontend base URL config
├── .env                    # Environment variables / port mappings
└── README.md               # This file
```

## Persistence

Data directories are mounted as volumes for persistence:

```bash
docker compose up -d

# H2 database (persists across restarts)
./data/wvp/

# ZLM recordings
./data/zlm/www/record/

# Application logs
./logs/wvp/
./logs/zlm/
```

## Configuration

### Environment Variables

Create a `.env` file in the `docker/standalone/` directory:

```bash
# SIP Configuration
SIP_PORT=8116
SIP_DOMAIN=3502000000
SIP_ID=35020000002000000001
SIP_PASSWORD=wvp_sip_password
SIP_SHOW_IP=127.0.0.1

# Web & API
WVP_PORT=18978
WEB_PORT=8080

# Stream IP (used in SDP and media URLs)
STREAM_IP=127.0.0.1
SDP_IP=127.0.0.1

# Media Ports
RTMP_PORT=10001
RTSP_PORT=10002
RTP_PORT=10003
RTP_RANGE_START=30000
RTP_RANGE_END=30500

# Java Heap
JAVA_OPTS=-Xms128m -Xmx512m
```

### ZLMediaKit Configuration

The default `config.ini` has all hooks pointing to `localhost:18978` since everything runs in the same container. To customize:

```bash
# Copy default config to volume
cp config.ini data/zlm/config.ini

# Edit and restart
vim data/zlm/config.ini
docker compose restart
```

## Building from Source

The build process includes:
1. **Stage 1**: Maven build of WVP Java application (JDK 21)
2. **Stage 2**: npm build of web frontend (Node.js 20)
3. **Stage 3**: Final image with WVP JAR + ZLM binary + Nginx

```bash
# Build with default settings
docker compose build

# Build with specific Dockerfile args
docker build --build-arg BUILDKIT_INLINE_CACHE=1 -t wvp-standalone:dev .
```

> **Note**: First build downloads Maven dependencies, Node modules, and builds ZLM from source. Subsequent builds use Docker cache.

## Troubleshooting

### Container won't start

```bash
# Check logs
docker compose logs polaris-standalone

# Check which service failed
docker inspect --format='{{.State.Status}}' polaris-standalone
```

### ZLM binary not found

The Dockerfile attempts to download pre-built ZLM from GitHub releases. If download fails:
- Check internet connectivity during build
- Manually place ZLM binary at `docker/standalone/zlm-binary`
- Or build ZLM locally and copy: `cp path/to/MediaServer docker/standalone/zlm-binary`

### H2 database issues

```bash
# Clear H2 data and re-initialize (WARNING: deletes all data!)
rm -rf data/wvp/*
docker compose down
docker compose up -d
```

### Port conflicts

Edit `.env` to change port mappings:

```bash
WVP_PORT=19978
WEB_PORT=9080
SIP_PORT=9116
```

### Memory issues

Increase Java heap size in `.env`:

```bash
JAVA_OPTS=-Xms256m -Xmx1024m
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│               polaris-standalone                     │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │    Nginx     │  │ ZLMediaKit   │  │  Spring    │ │
│  │  (Port 80)   │  │  (Port 80)   │  │  Boot      │ │
│  │              │  │              │  │  (Port     │ │
│  │  Frontend /  │  │  WebRTC /    │  │   18978)   │ │
│  │  Proxy /     │◄─┤  HLS / RTMP  │──┤            │ │
│  │  Static      │  │  RTSP / RTP  │  │  SIP /     │ │
│  │  Files       │  │  Recording   │  │  REST API  │ │
│  └──────────────┘  └──────────────┘  └────────────┘ │
│                                                      │
│  ┌──────────────┐                                     │
│  │    H2 DB     │                                     │
│  │  (File-based│                                     │
│  │   ./data/)   │                                     │
│  └──────────────┘                                     │
│                                                      │
│  ┌──────────────┐                                     │
│  │ Caffeine     │                                     │
│  │   Cache      │                                     │
│  └──────────────┘                                     │
└─────────────────────────────────────────────────────┘
```

## Comparison with Multi-Service Deployment

| Feature        | Standalone         | Multi-Service       |
|----------------|--------------------|---------------------|
| Containers     | 1                  | 4 (WVP, ZLM, Nginx, Redis, MySQL) |
| Database       | H2 (embedded)      | MySQL 8             |
| Cache          | Caffeine           | Redis               |
| Disk Usage     | ~1.5 GB            | ~4 GB               |
| Startup Time   | ~60 seconds        | ~90 seconds         |
| Scaling        | Horizontal only    | Independent scaling |
| Use Case       | Dev/Test/SMB       | Production/Enterprise |

## License

Same as the main WVP-GB28181-Pro project.
