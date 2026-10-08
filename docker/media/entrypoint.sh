#!/bin/sh
set -e

: "${MediaHttp:=80}"
: "${MediaHttps:=443}"
: "${MediaRtc:=8000}"
: "${MediaRtmp:=10001}"
: "${MediaRtp:=10003}"
: "${MediaRtsp:=10002}"
: "${SDP_IP:=}"

sed \
  -e "s/^port=80$/port=${MediaHttp}/" \
  -e "s/^sslport=443$/sslport=${MediaHttps}/" \
  -e "s/^externIP=$/externIP=${SDP_IP}/" \
  -e "/^\[rtc\]/,/^\[/ s/^port=8000$/port=${MediaRtc}/" \
  -e "/^\[rtc\]/,/^\[/ s/^tcpPort=8000$/tcpPort=${MediaRtc}/" \
  -e "/^\[rtmp\]/,/^\[/ s/^port=10001$/port=${MediaRtmp}/" \
  -e "/^\[rtp_proxy\]/,/^\[/ s/^port=10003$/port=${MediaRtp}/" \
  -e "/^\[rtsp\]/,/^\[/ s/^port=10002$/port=${MediaRtsp}/" \
  /conf/config.ini > /tmp/config.ini

exec MediaServer -c /tmp/config.ini -l 0
