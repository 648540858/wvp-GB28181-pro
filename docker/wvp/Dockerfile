FROM ubuntu:20.04 AS build

ARG gitUrl="https://gitee.com/pan648540858"
ARG zlmGitUrl="https://gitee.com/xia-chu/ZLMediaKit"

RUN export DEBIAN_FRONTEND=noninteractive &&\
        apt-get update && \
        apt-get install -y --no-install-recommends openjdk-11-jre git maven nodejs npm build-essential \
        cmake ca-certificates openssl ffmpeg

RUN cd /home && \
        git clone "https://gitee.com/pan648540858/maven.git" && \
        cp maven/settings.xml /usr/share/maven/conf/ && \
        git clone "${gitUrl}/wvp-GB28181-pro.git" && \
        git clone "${gitUrl}/wvp-pro-assist.git" && \
        git clone --depth=1 "${zlmGitUrl}" && \
        mkdir -p /opt/wvp/config /opt/assist/config /opt/media/www/record

RUN cd /home/wvp-GB28181/web_src && \
        npm install && \
        npm run build

RUN cd /home/wvp-GB28181 && \
        mvn clean package -Dmaven.test.skip=true && \
        cp /home/wvp-GB28181/target/*.jar /opt/wvp/ && \
        cp /home/wvp-GB28181/src/main/resources/application-docker.yml /opt/wvp/config/application.yml

RUN cd /home/wvp-pro-assist && \
        mvn clean package -Dmaven.test.skip=true && \
        cp /home/wvp-pro-assist/target/*.jar /opt/assist/ && \
        cp /home/wvp-pro-assist/src/main/resources/application-dev.yml /opt/assist/config/application.yml

RUN cd /home/ZLMediaKit && \
        git submodule update --init --recursive && \
        mkdir -p build release/linux/Release/ &&\
        cd build && \
        cmake -DCMAKE_BUILD_TYPE=Release .. && \
        make -j4 && \
        rm -rf ../release/linux/Release/config.ini && \
        cp -r ../release/linux/Release/* /opt/media

RUN cd /opt/wvp && \
        echo '#!/bin/bash' > run.sh && \
        echo 'echo ${WVP_IP}' >> run.sh && \
        echo 'echo ${WVP_CONFIG}' >> run.sh && \
        echo 'cd /opt/assist' >> run.sh && \
        echo 'nohup java -jar *.jar --spring.config.location=/opt/assist/config/application.yml --userSettings.record=/opt/media/www/record/  --media.record-assist-port=18081 ${ASSIST_CONFIG} &' >> run.sh && \
        echo 'nohup /opt/media/MediaServer -d -m 3 &' >> run.sh && \
        echo 'cd /opt/wvp' >> run.sh && \
        echo 'java -jar *.jar --spring.config.location=/opt/wvp/config/application.yml --media.record-assist-port=18081 ${WVP_CONFIG}' >> run.sh && \
        chmod +x run.sh

FROM ubuntu:20.04

EXPOSE 18080/tcp
EXPOSE 5060/tcp
EXPOSE 5060/udp
EXPOSE 6379/tcp
EXPOSE 18081/tcp
EXPOSE 80/tcp
EXPOSE 1935/tcp
EXPOSE 554/tcp
EXPOSE 554/udp
EXPOSE 30000-30500/tcp
EXPOSE 30000-30500/udp

ENV LC_ALL zh_CN.UTF-8

RUN export DEBIAN_FRONTEND=noninteractive &&\
        apt-get update && \
        apt-get install -y --no-install-recommends openjdk-11-jre ca-certificates ffmpeg language-pack-zh-hans && \
        apt-get autoremove -y && \
        apt-get clean -y && \
        rm -rf /var/lib/apt/lists/*dic

COPY --from=build /opt /opt
WORKDIR /opt/wvp
CMD ["sh", "run.sh"]
