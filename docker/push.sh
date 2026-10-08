#/bin/bash
set -e

version=2.7.3

docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-media:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-mysql:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-redis:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-wvp:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-nginx:latest
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-media:${version}
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-mysql:${version}
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-redis:${version}
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-wvp:${version}
docker push polaris-tian-docker.pkg.coding.net/qt/polaris/ylcx-nginx:${version}