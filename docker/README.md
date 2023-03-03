### 一、系统环境：

​		操作系统为Ubuntu 18.04的宿主机，并且安装了docker和docker compose。

```
宿主机操作系统：Ubuntu 18.04 
docker版本：Docker version 23.0.1, build a5ee5b1
docker compose版本：Docker Compose version v2.16.0
```



### 二、使用方式：

##### 1、将docker整个文件夹拷贝至宿主机（例如：/opt/gb28181目录下）

- 将wvp项目的初始化sql拷贝到/opt/gb28181/docker/base_services/mysql目录下，用于数据库初始化数据时使用。
- 将自定义的redis.conf拷贝至/opt/gb28181/docker/base_services/redis目录下
- 根据项目实际情况修改/opt/gb28181/docker/wvp/config以及/opt/gb28181/docker/zlm/config中的文件内容



##### 2、进入到/opt/gb28181/docker/base_services目录下，启动mysql和redis服务

```sh
root@bigdata202:/opt/gb28181/docker/base_services/# docker compose up -d
[+] Running 2/2
 ⠿ Container mysql_wvp  Started     7.0s
 ⠿ Container reids_wvp  Started 
```



##### 3、验证redis和mysql服务已经启动成功并初始化了数据



##### 4、进入到/opt/gb28181/docker目录下，启动wvp和zlm服务

```sh
root@bigdata202:/opt/gb28181/docker# docker compose up -d
[+] Running 2/2
 ⠿ Container zlm  Started      7.0s
 ⠿ Container wvp  Started 
```

