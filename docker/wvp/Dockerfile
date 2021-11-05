FROM ubuntu:20.04 AS build
RUN apt-get update && \
    DEBIAN_FRONTEND="noninteractive" && \
    apt-get install -y --no-install-recommends openjdk-11-jre-headless git maven nodejs npm &&\
    cd /home &&\
    git clone https://gitee.com/18010473990/maven.git && \
    cp maven/settings.xml /usr/share/maven/conf/ && \
    git clone https://gitee.com/xieyu1989/wvp-GB28181.git && \
    cd /home/wvp-GB28181/web_src && \
    npm install && \
    npm run build && \
    mkdir -p /opt/wvp/config && \
    cd /home/wvp-GB28181 && \
    mvn clean package -Dmaven.test.skip=true && \
    cp src/main/resources/application-docker.yml /opt/wvp/conf/application.yml && \
    cp target/wvp*.jar /opt/wvp/

FROM openjdk:11-jre-buster
EXPOSE 18080/tcp
EXPOSE 5060:5060/tcp
EXPOSE 5060:5060/udp
COPY --from=build /opt /opt
WORKDIR /opt/wvp
CMD ["java", "-jar","*.jar","--spring.config.location=/opt/wvp/conf/application.yml"]
