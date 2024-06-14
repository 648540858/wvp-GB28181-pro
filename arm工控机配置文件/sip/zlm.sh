#!/bin/sh

function start()
{
   echo "Start ZLMediaKit ..."
   nohup /root/ZLMediaKit/release/linux/Debug/MediaServer -d &
   sleep 3
   exit

}

function stop()
{
	echo "Stop ZLMediaKit ..."
	killall -2 MediaServer
}

case $1 in
    start)
    start;;
    stop)
    stop;;
    *)

esac
