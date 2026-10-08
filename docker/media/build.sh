#/bin/bash
set -e

version=2.7.3

docker build -t polaris-media:${version} .
docker tag polaris-media:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-media:${version}
docker tag polaris-media:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-media:latest