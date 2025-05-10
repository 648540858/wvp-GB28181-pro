#/bin/bash
set -e

version=2.7.3

docker build -t polaris-redis:${version} .
docker tag polaris-redis:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-redis:${version}
docker tag polaris-redis:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-redis:latest