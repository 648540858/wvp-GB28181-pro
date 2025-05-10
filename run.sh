#!/bin/bash
# JDK路径
export JAVA_HOME=/usr/local/java/jdk1.8.0_202
export JRE_HOME=${JAVA_HOME}/jre
export CLASSPATH=.:${JAVA_HOME}/lib:${JRE_HOME}/lib
export PATH=${JAVA_HOME}/bin:$PATH

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

function log() {
	message="[Polaris Log]: $1 "
	case "$1" in
	*"失败"* | *"错误"* | *"请使用 root 或 sudo 权限运行此脚本"*)
		echo -e "${RED}${message}${NC}" 2>&1 | tee -a
		;;
	*"成功"*)
		echo -e "${GREEN}${message}${NC}" 2>&1 | tee -a
		;;
	*"忽略"* | *"跳过"*)
		echo -e "${YELLOW}${message}${NC}" 2>&1 | tee -a
		;;
	*)
		echo -e "${BLUE}${message}${NC}" 2>&1 | tee -a
		;;
	esac
}

echo
cat <<EOF
██████╗  ██████╗ ██╗      █████╗ ██████╗ ██╗███████╗
██╔══██╗██╔═══██╗██║     ██╔══██╗██╔══██╗██║██╔════╝
██████╔╝██║   ██║██║     ███████║██████╔╝██║███████╗
██╔═══╝ ██║   ██║██║     ██╔══██║██╔══██╗██║╚════██║
██║     ╚██████╔╝███████╗██║  ██║██║  ██║██║███████║
╚═╝      ╚═════╝ ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚══════╝

EOF

# WVP-pro defines
AppName=wvp-pro-2.7.18-11211115.jar
AppHome="/root/polaris/wvp/"
# JVM参数
JVM_OPTS="-Dname=$AppName  -Duser.timezone=Asia/Shanghai -Xms512m -Xmx2048m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps  -XX:+PrintGCDetails -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"

function start() {
	log "======================= 开启流媒体服务 ======================="
	log "AppHome: $AppHome"
	cd $AppHome
	log "AppName: $AppName"
	PID=`ps -ef |grep java|grep $AppName|grep -v grep|awk '{print $2}'`
	if [x"$PID" != x""]; then
		log "$AppName is running..."
	else
		nohup java $JVM_OPTS -jar $AppName > /dev/null 2>&1 &
	fi
	log "流媒体服务开启成功"
}

function stop() {
	log "======================= 停止流媒体服务 ======================="

	PID=""
	query() {
		PID=$(ps -ef | grep java | grep $AppName | grep -v grep | awk '{print $2}')
	}
	query
	if [ x"$PID" != x"" ]; then
		log "进程PID: $PID"
		kill -TERM $PID
		log "$AppName (pid:$PID) exiting..."
		while [ x"$PID" != x"" ]; do
			sleep 1
			query
		done
		log "成功:$AppName exited."
	else
		log "忽略:进程不存在"
	fi
}

function status() {
	log "======================= 运行状态 ======================="
	log ""

	PID=`ps -ef |grep java|grep $AppName|grep -v grep|awk '{print $2}'`
	if [ $PID != 0 ]; then
		log "进程PID: $PID"
		log "$AppName is running..."
	else
		log "$AppName is not running..."
	fi
	log ""
	log "========================================================"
}

function restart() {
	stop
	sleep 3
	start
}

case $1 in
start)
	start
	;;
stop)
	stop
	;;
restart)
	restart
	;;
status)
	status
	;;
*) ;;

esac

