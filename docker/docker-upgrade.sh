#/bin/bash
set -e

docker compose down
docker compose up -d --remove-orphans