![logo](https://gitee.com/pan648540858/wvp-GB28181-pro/raw/wvp-28181-2.0/web_src/static/logo.png)
# 开箱即用的的28181协议视频平台

[![Build Status](https://travis-ci.org/xia-chu/ZLMediaKit.svg?branch=master)](https://travis-ci.org/xia-chu/ZLMediaKit)
[![license](http://img.shields.io/badge/license-MIT-green.svg)](https://github.com/xia-chu/ZLMediaKit/blob/master/LICENSE)
[![JAVA](https://img.shields.io/badge/language-java-red.svg)](https://en.cppreference.com/)
[![platform](https://img.shields.io/badge/platform-linux%20|%20macos%20|%20windows-blue.svg)](https://github.com/xia-chu/ZLMediaKit)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-yellow.svg)](https://github.com/xia-chu/ZLMediaKit/pulls)


WEB VIDEO PLATFORM是一个基于GB28181-2016标准实现的开箱即用的网络视频平台，负责实现核心信令与设备管理后台部分，支持NAT穿透，支持海康、大华、宇视等品牌的IPC、NVR、DVR接入。支持国标级联，支持rtsp/rtmp等视频流转发到国标平台，支持rtsp/rtmp等推流转发到国标平台。   

流媒体服务基于ZLMediaKit-https://github.com/xiongziliang/ZLMediaKit

前端页面基于MediaServerUI进行修改.  

# 应用场景：
支持浏览器无插件播放摄像头视频。  
支持摄像机、平台、NVR等设备接入。 
支持国标级联。  
支持rtsp/rtmp等视频流转发到国标平台。  
支持rtsp/rtmp等推流转发到国标平台。  

# 项目目标
旨在打造一个易配置,易使用,便于维护的28181国标信令系统, 依托优秀的开源流媒体服务框架ZLMediaKit, 实现一个完整易用GB28181平台. 

# 部署文档
[https://github.com/648540858/wvp-GB28181-pro/wiki](https://github.com/648540858/wvp-GB28181-pro/wiki)

# gitee同步仓库
https://gitee.com/pan648540858/wvp-GB28181-pro.git

# 截图
![build_1.png](https://github.com/648540858/wiki/blob/master/images/Screenshot_1.png)
![build_1.png](https://github.com/648540858/wiki/blob/master/images/Screenshot_2.png)
![build_1.png](https://github.com/648540858/wiki/blob/master/images/Screenshot_20201012_151459.png)
![build_1.png](https://github.com/648540858/wiki/blob/master/images/Screenshot_20201012_152643.png)
![build_1.png](https://github.com/648540858/wiki/blob/master/images/Screenshot_20201012_151606.png)

# 1.0 基础特性  
1. 视频预览;  
2. 云台控制（方向、缩放控制）;  
3. 视频设备信息同步;   
4. 离在线监控;  
5. 录像查询与回放（基于NVR\DVR，暂不支持快进、seek操作）;  
6. 无人观看自动断流;    
7. 支持UDP和TCP两种国标信令传输模式; 
8. 集成web界面, 不需要单独部署前端服务, 直接利用wvp内置文件服务部署, 随wvp一起部署;   
9. 支持平台接入, 针对大平台大量设备的情况进行优化;  
10. 支持检索,通道筛选;  
11. 支持自动配置ZLM媒体服务, 减少因配置问题所出现的问题;  
12. 支持启用udp多端口模式, 提高udp模式下媒体传输性能;  
13. 支持通道是否含有音频的设置;  
14. 支持通道子目录查询;  
15. 支持udp/tcp国标流传输模式;  
16. 支持直接输出RTSP、RTMP、HTTP-FLV、Websocket-FLV、HLS多种协议流地址  
17. 支持国标网络校时  
18. 支持公网部署, 支持wvp与zlm分开部署   
19. 支持播放h265, g.711格式的流(需要将closeWaitRTPInfo设为false)
20. 报警信息处理，支持向前端推送报警信息

# 1.0 新支持特性  
1. 集成web界面, 不需要单独部署前端服务, 直接利用wvp内置文件服务部署, 随wvp一起部署;   
2. 支持平台接入, 针对大平台大量设备的情况进行优化;  
3. 支持检索,通道筛选;  
4. 支持自动配置ZLM媒体服务, 减少因配置问题所出现的问题;  
5. 支持启用udp多端口模式, 提高udp模式下媒体传输性能;  
6. 支持通道是否含有音频的设置;  
7. 支持通道子目录查询;  
8. 支持udp/tcp国标流传输模式;  
9. 支持直接输出RTSP、RTMP、HTTP-FLV、Websocket-FLV、HLS多种协议流地址  
10. 支持国标网络校时  
11. 支持公网部署, 支持wvp与zlm分开部署   
12. 支持播放h265, g.711格式的流   
13. 支持固定流地址和自动点播，同时支持未点播时直接播放流地址，代码自动发起点播.  ( [查看WIKI](https://github.com/648540858/wvp-GB28181-pro/wiki/%E5%A6%82%E4%BD%95%E4%BD%BF%E7%94%A8%E5%9B%BA%E5%AE%9A%E6%92%AD%E6%94%BE%E5%9C%B0%E5%9D%80%E4%B8%8E%E8%87%AA%E5%8A%A8%E7%82%B9%E6%92%AD)）
14. 报警信息处理，支持向前端推送报警信息
15. 支持订阅与通知方法
   -  [X] 移动位置订阅
   -  [X] 移动位置通知处理
   -  [X] 报警事件订阅
   -  [X] 报警事件通知处理
   -  [ ] 设备目录订阅
   -  [X] 设备目录通知处理
16. 移动位置查询和显示，可通过配置文件设置移动位置历史是否存储

# 2.0 支持特性
- [X] 国标通道向上级联
    - [X] WEB添加上级平台
    - [X] 注册
    - [X] 心跳保活
    - [X] 通道选择
    - [X] 通道推送
    - [X] 点播
    - [X] 云台控制
    - [X] 平台状态查询
    - [X] 平台信息查询
    - [X] 平台远程启动
- [X] 添加RTSP视频
- [X] 添加接口鉴权
- [ ] 添加ONVIF探测局域网内的设备
- [X] 添加RTMP视频
- [X] 云端录像（需要部署单独服务配合使用）
- [X] 多流媒体节点，自动选择负载最低的节点使用。
- [X] 支持使用mysql作为数据库，默认sqlite3,开箱即用。
- [ ] 添加系统配置
- [ ] 添加用户管理
- [X] WEB端支持播放H264与H265，音频支持G.711A/G.711U/AAC,覆盖国标常用编码格式。

# docker快速体验
```shell
docker pull 648540858/wvp_pro

docker run  --env WVP_IP="你的IP" -it -p 18080:18080 -p 30000-30500:30000-30500/udp -p 30000-30500:30000-30500/tcp -p 80:80 -p 5060:5060 -p 5060:5060/udp 648540858/wvp_pro
```
docker使用详情查看：[https://hub.docker.com/r/648540858/wvp_pro](https://hub.docker.com/r/648540858/wvp_pro)

# gitee同步仓库
https://gitee.com/pan648540858/wvp-GB28181-pro.git  

# 使用帮助
QQ群: 901799015, 690854210(ZLM大群)  
QQ私信一般不回, 精力有限.欢迎大家在群里讨论.觉得项目对你有帮助，欢迎star和提交pr。


# 致谢
感谢作者[夏楚](https://github.com/xia-chu) 提供这么棒的开源流媒体服务框架  

