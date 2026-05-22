#!/bin/sh
set -e

envsubst '${Stream_IP}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

nginx

exec java -Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/ylcx/ -jar /opt/wvp/wvp.jar --spring.config.location=/opt/ylcx/wvp/application.yml
