FROM openjdk:11-jre-buster AS build
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && \
        DEBIAN_FRONTEND="noninteractive" && \
        apt-get install -y --no-install-recommends git maven && \
        cd /home && \
        git clone https://gitee.com/18010473990/maven.git && \
        cp maven/settings.xml /usr/share/maven/conf/ && \
        git clone https://gitee.com/xieyu1989/wvp-pro-assist.git &&\
        cd /home/wvp-pro-assist
        mvn clean package -Dmaven.test.skip=true && \
        cp /home/wvp-pro-assist/target/*.jar /opt/assist/
        cp /home/wvp-pro-assist/src/main/resources/application-docker.yml /opt/assist/conf/application.yml

FROM ubuntu:20.04
ARG DEBIAN_FRONTEND=noninteractive
EXPOSE 18081/tcp
RUN apt-get update && \
        DEBIAN_FRONTEND="noninteractive" && \
        apt-get install -y --no-install-recommends openjdk-11-jre-headless ca-certificates  ffmpeg && \
        apt-get autoremove -y && \
        apt-get clean -y && \
        rm -rf /var/lib/apt/lists/*dic
COPY --from=build /opt /opt
WORKDIR /opt/wvp
CMD ["java", "-jar","*.jar","--spring.config.location=/opt/assist/conf/application.yml"]
