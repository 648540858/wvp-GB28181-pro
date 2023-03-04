FROM ubuntu:20.04   as   build

ARG gitUrl="https://gitee.com/pan648540858"
ARG gitBranch="wvp-28181-2.0"

RUN export DEBIAN_FRONTEND=noninteractive &&\
        apt-get update && \
        apt-get install -y --no-install-recommends openjdk-11-jre git maven nodejs npm &&\
        mkdir -p /opt/wvp/config

RUN git config --global http.sslverify "false"

RUN cd /home && \
    git clone "${gitUrl}/maven.git" && \
    cp maven/settings.xml /usr/share/maven/conf/

RUN cd /home && \
    git clone --depth 1 --branch ${gitBranch} "${gitUrl}/wvp-GB28181-pro.git"

RUN cd /home/wvp-GB28181-pro/web_src && \
        npm install && \
        npm run build

RUN cd /home/wvp-GB28181-pro && \
        mvn clean package -Dmaven.test.skip=true && \
        cp /home/wvp-GB28181-pro/target/*.jar /opt/wvp/

FROM ubuntu:20.04

EXPOSE 18080/tcp
EXPOSE 5060/tcp
EXPOSE 5060/udp

RUN export DEBIAN_FRONTEND=noninteractive &&\
        apt-get update && \
        apt-get install -y --no-install-recommends openjdk-11-jre locales language-pack-en tzdata && \
        apt-get autoremove -y && \
        apt-get clean -y && \
        rm -rf /var/lib/apt/lists/*dic

COPY --from=build /opt /opt

ADD config/application.yml /opt/wvp/config/application.yml
ADD config/application-prod.yml /opt/wvp/config/application-prod.yml
ADD config/logback-spring-local.xml /opt/wvp/config/logback-spring-local.xml

WORKDIR /opt/wvp
CMD java -jar *.jar
