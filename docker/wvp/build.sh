#/bin/bash
set -e

version=2.7.3

docker build -t polaris-wvp:${version} .
docker tag polaris-wvp:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-wvp:${version}
docker tag polaris-wvp:${version} polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-wvp:latest