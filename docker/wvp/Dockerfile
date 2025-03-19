FROM m.daocloud.io/docker.io/ubuntu:20.04 AS build

ARG gitUrl="https://gitee.com/pan648540858"

ENV LC_ALL=zh_CN.UTF-8
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN export DEBIAN_FRONTEND=noninteractive
RUN sed -i 's@archive.ubuntu.com@mirrors.tuna.tsinghua.edu.cn@g' /etc/apt/sources.list
RUN sed -i 's@security.ubuntu.com@mirrors.tuna.tsinghua.edu.cn@g' /etc/apt/sources.list
RUN apt-get update -y
RUN apt-get install -y --no-install-recommends tzdata openjdk-11-jdk git maven nodejs npm
RUN mkdir -p /opt/wvp/config /opt/wvp/heapdump /opt/wvp/config /opt/assist/config /opt/assist/heapdump /opt/media/www/record

#WORKDIR /home
#RUN git clone "${gitUrl}/wvp-pro-assist.git"
#WORKDIR /home/wvp-pro-assist
#RUN git reset --hard 58f1a79136a55a7cd1593c95b56ddadcc2225b61
#RUN mvn clean package -Dmaven.test.skip=true
#RUN cp /home/wvp-pro-assist/target/*.jar /opt/assist/
#RUN cp /home/wvp-pro-assist/src/main/resources/application-dev.yml /opt/assist/config/application.yml

COPY ./docker/maven/settings.xml /usr/share/maven/conf/

# 先复制package.json
WORKDIR /home/wvp-GB28181-pro/web_src
COPY ./web_src/package.json ./package.json
RUN npm config set registry https://registry.npmmirror.com
RUN npm install

# 先复制pom.xml
WORKDIR /home/wvp-GB28181-pro
COPY ./pom.xml ./pom.xml
RUN mvn dependency:go-offline

# 复制全部文件
WORKDIR /home/wvp-GB28181-pro
COPY . .

# 编译前端项目
WORKDIR /home/wvp-GB28181-pro/web_src
RUN npm run build

# 编译后端jar包
WORKDIR /home/wvp-GB28181-pro
RUN mvn clean package -Dmaven.test.skip=true
RUN cp /home/wvp-GB28181-pro/target/*.jar /opt/wvp/
RUN cp /home/wvp-GB28181-pro/src/main/resources/application-docker.yml /opt/wvp/config/application.yml

WORKDIR /opt/wvp
RUN echo '#!/bin/bash' > run.sh && \
        echo 'echo ${WVP_IP}' >> run.sh && \
        echo 'echo ${WVP_CONFIG}' >> run.sh && \
        echo 'cd /opt/assist' >> run.sh && \
        echo 'nohup java ${ASSIST_JVM_CONFIG} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/assist/heapdump/ -jar *.jar --spring.config.location=/opt/assist/config/application.yml --userSettings.record=/opt/media/www/record/  --media.record-assist-port=18081 ${ASSIST_CONFIG} &' >> run.sh && \
        echo 'cd /opt/wvp' >> run.sh && \
        echo 'java ${WVP_JVM_CONFIG} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/wvp/heapdump/ -jar *.jar --spring.config.location=/opt/wvp/config/application.yml --media.record-assist-port=18081 ${WVP_CONFIG}' >> run.sh && \
        chmod +x run.sh

FROM m.daocloud.io/docker.io/ubuntu:20.04

EXPOSE 18080/tcp
EXPOSE 5060/tcp
EXPOSE 5060/udp
EXPOSE 6379/tcp
EXPOSE 18081/tcp

ENV LC_ALL=zh_CN.UTF-8

RUN export DEBIAN_FRONTEND=noninteractive &&\
        apt-get update && \
        apt-get install -y --no-install-recommends iputils-ping nano htop openjdk-11-jre ca-certificates ffmpeg language-pack-zh-hans && \
        apt-get autoremove -y && \
        apt-get clean -y && \
        rm -rf /var/lib/apt/lists/*dic

COPY --from=build /opt /opt
WORKDIR /opt/wvp
CMD ["sh", "run.sh"]
