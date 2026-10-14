#!/usr/bin/env bash
# ============================================================
# WVP 服务管理脚本
#
# 位置: 与 jar 包、application*.yml 同级(打包时由 pom 从 bin/ 复制到 target/)
#
# 用法:
#   ./restart.sh          重启服务(默认)
#   ./restart.sh start    启动服务
#   ./restart.sh stop     停止服务
#   ./restart.sh restart  重启服务
#   ./restart.sh status   查看状态
#   ./restart.sh help     显示帮助
#
# 说明:
#   - 后端接口 ApplicationRestartServiceImpl 会以 `bash restart.sh restart` 调用本脚本;
#   - 打包时 application.yml / application-*.yml 会被复制到 jar 同级目录(不进入 jar 内部),
#     因此脚本会探测真正包含 application.yml 的目录, 并通过 --spring.config.location 显式指定,
#     避免出现“配置目录指错导致 profile 未生效、占位符解析失败”的启动问题;
#   - 可通过环境变量覆盖: JAVA_OPTS / SPRING_ARGS / APP_HOME / PID_FILE / LOG_DIR / STOP_TIMEOUT。
# ============================================================

set -u -o pipefail

# 脚本所在目录(解析软链接), 与 jar、配置文件同级
APP_HOME="${APP_HOME:-$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd -P)}"
[ -n "$APP_HOME" ] || { echo "错误: 无法定位脚本所在目录"; exit 1; }
cd "$APP_HOME" || exit 1

PID_FILE="${PID_FILE:-$APP_HOME/wvp.pid}"
LOG_DIR="${LOG_DIR:-$APP_HOME/logs}"
OUT_LOG="${OUT_LOG:-$LOG_DIR/wvp.out}"
STOP_TIMEOUT="${STOP_TIMEOUT:-60}"
START_WAIT="${START_WAIT:-2}"

# 主配置文件名称, 与 SettingServiceImpl 保持一致
MAIN_CONFIG_NAMES=("application.yml" "application.yaml")

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# ------------------------------------------------------------
# Java 可执行文件
# ------------------------------------------------------------
resolve_java() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        echo "$JAVA_HOME/bin/java"
        return 0
    fi
    command -v java 2>/dev/null
}

JAVA_BIN="$(resolve_java || true)"

# JVM 参数, 可通过环境变量 JAVA_OPTS 覆盖
JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx2048m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -Duser.timezone=Asia/Shanghai}"

# ------------------------------------------------------------
# 查找 jar 包
# 目录中存在多个 jar 时: 先按修改时间, 时间相同再按文件名排序, 取最后一个
# (打包产物名以构建时间戳结尾, 因此文件名序与构建先后一致)
# ------------------------------------------------------------
find_jar() {
    local best="" best_time=-1 f name mtime
    for f in "$APP_HOME"/*.jar; do
        [ -e "$f" ] || continue
        name=$(basename "$f")
        case "$name" in
        *sources* | *javadoc* | *-plain* | *original*) continue ;;
        esac
        mtime=$(stat -c %Y "$f" 2>/dev/null || stat -f %m "$f" 2>/dev/null || echo 0)
        if [ "$mtime" -gt "$best_time" ] || { [ "$mtime" -eq "$best_time" ] && [ "$f" \> "$best" ]; }; then
            best="$f"
            best_time="$mtime"
        fi
    done
    echo "$best"
}

# ------------------------------------------------------------
# 配置目录自适应
#
# 只指定“确实包含 application.yml”的目录, 且只指定一个,
# 保证 Spring 加载的配置文件与 SettingServiceImpl 读写的配置文件一致。
# 优先 ./config, 其次 jar 同级目录。
# 找不到时返回空, 交给 Spring 默认搜索路径(cwd 为 jar 同级目录)。
# ------------------------------------------------------------
resolve_config_dir() {
    local name
    for name in "${MAIN_CONFIG_NAMES[@]}"; do
        if [ -f "$APP_HOME/config/$name" ]; then
            echo "$APP_HOME/config"
            return 0
        fi
    done
    for name in "${MAIN_CONFIG_NAMES[@]}"; do
        if [ -f "$APP_HOME/$name" ]; then
            echo "$APP_HOME"
            return 0
        fi
    done
    echo ""
}

build_config_arg() {
    local dir
    dir="$(resolve_config_dir)"
    if [ -n "$dir" ]; then
        echo "--spring.config.location=optional:classpath:/,optional:file:$dir/"
    else
        echo ""
    fi
}

# ------------------------------------------------------------
# 进程查找
# ------------------------------------------------------------
read_pid() {
    [ -f "$PID_FILE" ] || return 1
    local pid
    pid="$(tr -dc '0-9' <"$PID_FILE" 2>/dev/null)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        echo "$pid"
        return 0
    fi
    return 1
}

# 通过完整 jar 路径匹配进程, 避免误伤其它 java 进程
find_pid_by_jar() {
    local jar="$1"
    [ -n "$jar" ] || return 1
    ps -eo pid=,args= 2>/dev/null | grep -F -- "-jar $jar" | grep -v grep | awk '{print $1}' | head -n 1
}

# ------------------------------------------------------------
# 启动 / 停止 / 状态
# ------------------------------------------------------------
stop_service() {
    local jar="$1"
    local pid=""

    if pid="$(read_pid)"; then
        log "从 PID 文件读取到进程: $pid"
    else
        rm -f "$PID_FILE"
        pid="$(find_pid_by_jar "$jar")"
        if [ -n "$pid" ]; then
            log "通过进程匹配找到: $pid"
        fi
    fi

    if [ -z "$pid" ]; then
        log "服务未在运行"
        return 0
    fi

    log "正在停止服务 (pid: $pid) ..."
    kill -TERM "$pid" 2>/dev/null

    local waited=0
    while [ "$waited" -lt "$STOP_TIMEOUT" ]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            log "服务已停止"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
        waited=$((waited + 1))
    done

    log "服务在 ${STOP_TIMEOUT}s 内未退出, 强制杀死"
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

    # 已在运行则不再重复启动
    local running_pid=""
    if running_pid="$(read_pid)"; then
        log "服务已在运行 (pid: $running_pid), 跳过启动"
        return 0
    fi
    running_pid="$(find_pid_by_jar "$jar")"
    if [ -n "$running_pid" ]; then
        log "服务已在运行 (pid: $running_pid), 跳过启动"
        echo "$running_pid" >"$PID_FILE"
        return 0
    fi

    mkdir -p "$LOG_DIR"

    local config_arg
    config_arg="$(build_config_arg)"
    if [ -n "$config_arg" ]; then
        log "使用配置目录: $(resolve_config_dir)"
    else
        log "未找到 application.yml, 使用 Spring 默认配置搜索路径"
    fi

    log "启动服务: $(basename "$jar")"
    # shellcheck disable=SC2086
    nohup "$JAVA_BIN" $JAVA_OPTS -jar "$jar" $config_arg ${SPRING_ARGS:-} >>"$OUT_LOG" 2>&1 &
    local new_pid=$!

    # 启动后立即记录进程ID, 供后续重启/停止使用
    echo "$new_pid" >"$PID_FILE"
    log "进程ID $new_pid 已写入 $PID_FILE"

    sleep "$START_WAIT"
    if kill -0 "$new_pid" 2>/dev/null; then
        log "服务启动成功 (pid: $new_pid)"
        log "输出日志: $OUT_LOG"
        return 0
    fi

    log "错误: 服务启动失败, 请检查 $OUT_LOG"
    rm -f "$PID_FILE"
    return 1
}

status_service() {
    local jar config_dir pid
    jar="$(find_jar)"
    config_dir="$(resolve_config_dir)"

    log "运行目录 : $APP_HOME"
    log "JAR 包   : ${jar:-未找到}"
    log "配置目录 : ${config_dir:-未找到 application.yml(使用默认搜索路径)}"

    if pid="$(read_pid)"; then
        log "状态     : 运行中 (pid: $pid)"
        return 0
    fi
    pid="$(find_pid_by_jar "$jar")"
    if [ -n "$pid" ]; then
        log "状态     : 运行中 (pid: $pid, 未记录到 PID 文件)"
        return 0
    fi
    log "状态     : 未运行"
    return 1
}

usage() {
    cat <<EOF
用法: $0 [start|stop|restart|status|help]

  start    启动服务
  stop     停止服务
  restart  重启服务(默认)
  status   查看状态
  help     显示本帮助
EOF
}

# ------------------------------------------------------------
# 入口
# ------------------------------------------------------------
case "${1:-restart}" in
start)
    start_service
    ;;
stop)
    stop_service "$(find_jar)"
    ;;
restart)
    stop_service "$(find_jar)"
    sleep 2
    start_service
    ;;
status)
    status_service
    ;;
help | -h | --help)
    usage
    ;;
*)
    log "未知命令: $1"
    usage
    exit 2
    ;;
esac
