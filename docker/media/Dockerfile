FROM ubuntu:20.04 AS build

#shell,rtmp,rtsp,rtsps,http,rtp
EXPOSE 10935/tcp
EXPOSE 5540/tcp
EXPOSE 6080/tcp
EXPOSE 10000/udp
EXPOSE 10000/tcp
EXPOSE 8000/udp
EXPOSE 8000/tcp
EXPOSE 9000/udp

# ADD sources.list /etc/apt/sources.list

RUN apt-get update && \
         DEBIAN_FRONTEND="noninteractive" \
         apt-get install -y --no-install-recommends \
         build-essential \
         cmake \
         git \
         curl \
         vim \
         wget \
         ca-certificates \
         tzdata \
         libssl-dev \
         gcc \
         g++ \
         gdb && \
         apt-get autoremove -y && \
         apt-get clean -y && \
         rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/media
WORKDIR /opt/media
RUN git clone --depth 1 https://gitee.com/xia-chu/ZLMediaKit && \
    cd ZLMediaKit && git submodule update --init

# 3rdpart init
WORKDIR /opt/media/ZLMediaKit/3rdpart
RUN wget https://polaris-tian-generic.pkg.coding.net/qt/dependencies/openssl-1.1.1k.tar.gz?version=latest -O openssl-1.1.1k.tar.gz && \
    tar -xvzf openssl-1.1.1k.tar.gz && \
    cd openssl-1.1.1k && ./config shared --openssldir=/usr/local/openssl --prefix=/usr/local/openssl && \
    make && make install && \
    echo "/usr/local/lib64/" >> /etc/ld.so.conf && \
    echo "/usr/local/openssl/lib" >> /etc/ld.so.conf && \
    ldconfig && \
    ln -s /usr/local/openssl/bin/openssl  /usr/local/bin/openssl

WORKDIR /opt/media/ZLMediaKit/3rdpart
RUN wget https://github.com/cisco/libsrtp/archive/v2.3.0.tar.gz -O libsrtp-2.3.0.tar.gz && \
    tar xfv libsrtp-2.3.0.tar.gz && \
    mv libsrtp-2.3.0 libsrtp && \
    cd libsrtp && ./configure --enable-openssl --with-openssl-dir=/usr/local/openssl && make -j $(nproc) && make install


WORKDIR /opt/media/ZLMediaKit/build
RUN cmake .. -DENABLE_WEBRTC=true  -DOPENSSL_ROOT_DIR=/usr/local/openssl  -DOPENSSL_LIBRARIES=/usr/local/openssl/lib && \
    cmake --build . --target MediaServer
COPY config.ini /opt/media/ZLMediaKit/release/linux/Debug/

FROM ubuntu:20.04

RUN apt-get update && \
         DEBIAN_FRONTEND="noninteractive" \
         apt-get install -y --no-install-recommends \
         vim \
         wget \
         ca-certificates \
         tzdata \
         curl \
         libssl-dev \
         ffmpeg \
         gcc \
         g++ \
         gdb && \
         apt-get autoremove -y && \
         apt-get clean -y && \
    rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
        && echo $TZ > /etc/timezone && \
        mkdir -p /opt/media/bin/www

WORKDIR /opt/media/bin/
COPY --from=build /opt/media/ZLMediaKit/release/linux/Debug/MediaServer /opt/media/ZLMediaKit/default.pem /opt/media/bin/
COPY --from=build /opt/media/ZLMediaKit/release/linux/Debug/config.ini /opt/media/conf/
COPY --from=build /opt/media/ZLMediaKit/www/ /opt/media/bin/www/
ENV PATH /opt/media/bin:$PATH
CMD ["./MediaServer","-s", "default.pem", "-c", "../conf/config.ini", "-l","0"]