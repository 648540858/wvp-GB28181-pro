#!/bin/bash

# 获取当前日期并格式化为 YYYY-MM-DD 的形式
current_date=$(date +"%Y-%m-%d")

mkdir -p "$current_date"/数据库

cp -r  ./数据库/2.7.3 "$current_date"/数据库

cp src/main/resources/配置详情.yml "$current_date"
cp src/main/resources/application-dev.yml "$current_date"/application.yml

cp ./target/wvp-pro-*.jar "$current_date"

zip -r "$current_date".zip "$current_date"

rm -rf "$current_date"

exit 0

