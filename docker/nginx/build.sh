#/bin/bash
set -e

version=2.7.3

rm ./dist/static/js/config.js
cp ./config.js ./dist/static/js/

docker build -t polaris-nginx:${version} .
docker tag polaris-nginx:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-nginx:${version}
docker tag polaris-nginx:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-nginx:latest