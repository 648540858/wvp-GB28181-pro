#!/bin/bash
# ============================================================
# WVP 服务重启脚本
#
# 放在 jar 包同级目录(与 config/ 目录并列), 由后端接口调用, 也可手动执行。
#
# 用法:
#   ./restart.sh          重启服务
#   ./restart.sh start    启动服务
#   ./restart.sh stop     停止服务
#   ./restart.sh status   查看状态
#
# PID 文件: <脚本所在目录>/wvp.pid
# 启动后会把进程ID写入该文件, 重启时优先使用它来停止旧进程。
# ============================================================

# 脚本所在目录(与 jar、config 同级)
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_HOME" || exit 1

PID_FILE="$APP_HOME/wvp.pid"
LOG_DIR="$APP_HOME/logs"
OUT_LOG="$LOG_DIR/wvp.out"
PID_TIMEOUT=60

# JAVA_HOME 已设置则使用, 否则使用 PATH 中的 java
if [ -n "$JAVA_HOME" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
else
    JAVA_BIN="$(command -v java)"
fi

# JVM 参数, 可通过环境变量 JAVA_OPTS 覆盖
JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx2048m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -Duser.timezone=Asia/Shanghai}"

# 额外的启动参数, 可通过环境变量 SPRING_ARGS 覆盖(例如指定配置目录)
SPRING_ARGS="${SPRING_ARGS:-}"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 自动查找当前目录下的 jar 包(排除 sources/javadoc 等)
# 目录中存在多个 jar 时: 先按修改时间, 时间相同再按文件名排序, 取最后一个
# (打包产物名以构建时间戳结尾, 因此文件名序与构建先后一致)
find_jar() {
    local jar=""
    local f name mtime
    while IFS= read -r f; do
        [ -n "$f" ] && jar="$f"
    done <<EOF
$(for f in "$APP_HOME"/*.jar; do
    [ -e "$f" ] || continue
    name=$(basename "$f")
    case "$name" in
    *sources* | *javadoc* | *-plain* | *original*) continue ;;
    esac
    mtime=$(stat -c %Y "$f" 2>/dev/null || stat -f %m "$f" 2>/dev/null || echo 0)
    printf '%s\t%s\n' "$mtime" "$f"
done | sort -k1,1n -k2,2 | cut -f2-)
EOF
    echo "$jar"
}

# 读取 PID 文件中的进程ID
read_pid() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid="$(cat "$PID_FILE" 2>/dev/null | tr -d '[:space:]')"
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            echo "$pid"
            return 0
        fi
    fi
    return 1
}

# 通过命令行匹配查找进程( PID 文件失效时的兜底 )
find_pid_by_ps() {
    local jar_name="$1"
    ps -ef | grep "$jar_name" | grep -v grep | awk '{print $2}' | head -n 1
}

stop_service() {
    local jar_name="$1"
    local pid=""

    if pid="$(read_pid)"; then
        log "从 PID 文件读取到进程: $pid"
    else
        log "PID 文件不存在或进程已退出, 尝试通过进程名匹配"
        pid="$(find_pid_by_ps "$jar_name")"
        if [ -n "$pid" ]; then
            log "匹配到进程: $pid"
        fi
    fi

    if [ -z "$pid" ]; then
        log "服务未在运行"
        rm -f "$PID_FILE"
        return 0
    fi

    log "正在停止服务 (pid: $pid) ..."
    kill -TERM "$pid" 2>/dev/null

    local waited=0
    while [ "$waited" -lt "$PID_TIMEOUT" ]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            log "服务已停止"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
        waited=$((waited + 1))
    done

    log "服务在 ${PID_TIMEOUT}s 内未退出, 强制杀死"
    kill -9 "$pid" 2>/dev/null
    sleep 2
    rm -f "$PID_FILE"
    log "服务已强制停止"
    return 0
}

start_service() {
    local jar
    jar="$(find_jar)"
    if [ -z "$jar" ]; then
        log "错误: 当前目录未找到 jar 包: $APP_HOME"
        return 1
    fi
    if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
        log "错误: 未找到 java 可执行文件, 请设置 JAVA_HOME"
        return 1
    fi

    mkdir -p "$LOG_DIR"

    # 配置文件目录: 与 jar 同级时自动追加 --spring.config.location
    local config_arg=""
    if [ -d "$APP_HOME/config" ]; then
        config_arg="--spring.config.location=optional:classpath:/,optional:file:$APP_HOME/config/"
        log "使用配置目录: $APP_HOME/config"
    fi

    log "启动服务: $(basename "$jar")"
    # shellcheck disable=SC2086
    nohup "$JAVA_BIN" $JAVA_OPTS -jar "$jar" $config_arg $SPRING_ARGS >>"$OUT_LOG" 2>&1 &
    local new_pid=$!

    # 启动后将进程ID写入磁盘, 供后续重启使用
    echo "$new_pid" >"$PID_FILE"
    log "进程ID $new_pid 已写入 $PID_FILE"

    sleep 2
    if kill -0 "$new_pid" 2>/dev/null; then
        log "服务启动成功 (pid: $new_pid)"
        log "输出日志: $OUT_LOG"
        return 0
    else
        log "错误: 服务启动失败, 请检查 $OUT_LOG"
        rm -f "$PID_FILE"
        return 1
    fi
}

status_service() {
    local pid
    if pid="$(read_pid)"; then
        log "服务正在运行 (pid: $pid)"
        return 0
    else
        log "服务未在运行"
        return 1
    fi
}

case "$1" in
start)
    start_service
    ;;
stop)
    stop_service "$(basename "$(find_jar)")"
    ;;
restart)
    stop_service "$(basename "$(find_jar)")"
    sleep 2
    start_service
    ;;
status)
    status_service
    ;;
*)
    log "用法: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac
