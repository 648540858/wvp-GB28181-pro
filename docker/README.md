可以在当前目录下：
使用`docker compose up -d`直接运行。
使用`docker compose up -d -build -force-recreate`强制重新构建所有服务的镜像并删除旧容器重新运行

`.env`用来配置环境变量，在这里配好之后，其它的配置会自动联动的。

`build.sh`用来以日期为tag构建镜像，推送到指定的容器注册表内（Windows下可以使用`Git Bash`运行）


其它的文件的作用暂不明确