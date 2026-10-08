#/bin/bash
set -e

version=2.7.3

docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-media:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-mysql:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-redis:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-wvp:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-media:${version}
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-mysql:${version}
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-redis:${version}
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/polaris-wvp:${version}
