#/bin/bash
set -e

version=2.7.3

git clone https://gitee.com/pan648540858/wvp-GB28181-pro.git
cd wvp-GB28181-pro/web_src && \
    npm install && \
    npm run build 
    
cd ../../
mkdir -p ./nginx/dist
cp -r wvp-GB28181-pro/src/main/resources/static/* ./nginx/dist

echo "构建ZLM容器"
cd ./media/
chmod +x ./build.sh
./build.sh
cd ../

echo "构建数据库容器"
cd ./mysql/
chmod +x ./build.sh
./build.sh
cd ../

echo "构建Redis容器"
cd ./redis/
chmod +x ./build.sh
./build.sh
cd ../

echo "构建WVP容器"
cd ./wvp/
chmod +x ./build.sh
./build.sh
cd ../

echo "构建Nginx容器"
cd ./nginx/
chmod +x ./build.sh
./build.sh
cd ../

./push.sh
