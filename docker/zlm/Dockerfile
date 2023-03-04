FROM ubuntu:18.04 AS build

ARG MODEL=Release
ARG gitUrl="https://gitee.com/xia-chu/ZLMediaKit.git"
ARG gitBranch="master"

#shell,rtmp,rtsp,rtsps,http,https,rtp
EXPOSE 1935/tcp
EXPOSE 554/tcp
EXPOSE 80/tcp
EXPOSE 443/tcp
EXPOSE 10000/udp
EXPOSE 10000/tcp
EXPOSE 30000-30500/tcp
EXPOSE 30000-30500/udp

ADD sources.list /etc/apt/sources.list

RUN apt-get update && \
         DEBIAN_FRONTEND="noninteractive" \
         apt-get install -y --no-install-recommends \
         build-essential \
         cmake \
         git \
         vim \
         wget \
         ca-certificates \
         tzdata \
         libssl-dev \
         libmysqlclient-dev \
         libx264-dev \
         libfaac-dev \
         gcc \
         g++ \
         libavcodec-dev libavutil-dev libswscale-dev libresample-dev \
         gdb && \
         apt-get autoremove -y && \
         apt-get clean -y && \
         wget https://github.com/cisco/libsrtp/archive/v2.2.0.tar.gz -O libsrtp-2.2.0.tar.gz && tar xfv libsrtp-2.2.0.tar.gz && \
         cd libsrtp-2.2.0 && ./configure --enable-openssl && make -j $(nproc) && make install && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/media
WORKDIR /opt/media/

RUN git config --global http.postBuffer 2000000000 && \
    git config --global http.sslverify "false" && \
    git clone --depth 1 --branch ${gitBranch} ${gitUrl} && \
    cd ZLMediaKit && \
    git submodule update --init --recursive

RUN mkdir -p build release/linux/

WORKDIR /opt/media/ZLMediaKit/build
RUN cmake -DCMAKE_BUILD_TYPE=${MODEL} -DENABLE_WEBRTC=true -DENABLE_FFMPEG=true -DENABLE_TESTS=false -DENABLE_API=false .. && \
    make -j $(nproc)

FROM ubuntu:18.04
ARG MODEL

ADD sources.list /etc/apt/sources.list

RUN apt-get update && \
         DEBIAN_FRONTEND="noninteractive" \
         apt-get install -y --no-install-recommends \
         vim \
         wget \
         ca-certificates \
         tzdata \
         libssl-dev \
         libx264-dev \
         libfaac-dev \
         ffmpeg \
         gcc \
         g++ \
         libavcodec-dev libavutil-dev libswscale-dev libresample-dev \
         gdb && \
         apt-get autoremove -y && \
         apt-get clean -y && \
         rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
        && echo $TZ > /etc/timezone && \
        mkdir -p /opt/media/bin/www

WORKDIR /opt/media/bin/
COPY --from=build /opt/media/ZLMediaKit/release/linux/${MODEL}/MediaServer /opt/media/ZLMediaKit/default.pem /opt/media/bin/
COPY --from=build /opt/media/ZLMediaKit/www/ /opt/media/bin/www/

ADD config/config.ini /opt/media/conf/
ADD config/default.pem /opt/media/bin/

ENV PATH /opt/media/bin:$PATH
CMD ["sh","-c","./MediaServer -s default.pem -c ../conf/config.ini"]
