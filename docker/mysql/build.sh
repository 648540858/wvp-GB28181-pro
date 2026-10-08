#/bin/bash
set -e

version=2.7.3

docker build -t polaris-mysql:${version} .
docker tag polaris-mysql:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-mysql:${version}
docker tag polaris-mysql:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-mysql:latest