FROM node:18-bullseye AS frontend-builder

WORKDIR /build/web
COPY ./web/package*.json ./
RUN npm --registry=https://registry.npmmirror.com install
COPY ./web ./
RUN npm run build:prod


FROM ringcentral/jdk:21.0.9 AS builder

EXPOSE 18978/tcp
EXPOSE 8116/tcp
EXPOSE 8116/udp
EXPOSE 8080/tcp

RUN java -version && javac -version
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

COPY . /build
COPY --from=frontend-builder /build/src/main/resources/static /build/src/main/resources/static
WORKDIR /build
RUN mvn clean package -Dmaven.test.skip=true
WORKDIR /build/target
RUN mv wvp-pro-*.jar wvp.jar


FROM ringcentral/jdk:21.0.9

EXPOSE 18978/tcp
EXPOSE 8116/tcp
EXPOSE 8116/udp
EXPOSE 8080/tcp

RUN apt-get update && \
    apt-get install -y nginx gettext-base && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/wvp /opt/dist /opt/ylcx/wvp /etc/nginx/templates /opt/dist/static/js
WORKDIR /opt/wvp
COPY --from=builder /build/target /opt/wvp
COPY --from=frontend-builder /build/src/main/resources/static /opt/dist
COPY ./docker/wvp/wvp /opt/ylcx/wvp
COPY ./docker/wvp/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY ./docker/wvp/entrypoint.sh /entrypoint.sh
COPY ./web/public/static/js/config.js /opt/dist/static/js/config.js
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]



#RUN mkdir -p /opt/wvp
#WORKDIR /opt/wvp
#COPY ./wvp /opt/wvp
#
#WORKDIR /home
#RUN cd /home && \
    #git clone https://gitee.com/pan648540858/wvp-GB28181-pro.git
#
#RUN cd /home/wvp-GB28181-pro && \
    #mvn clean package -Dmaven.test.skip=true && \
    #cp /home/wvp-GB28181-pro/target/*.jar /opt/wvp/wvp.jar
#
#WORKDIR /opt/wvp
#ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/opt/ylcx/", "-jar", "wvp.jar", "--spring.config.location=/opt/ylcx/wvp/application.yml"]
