![logo](doc/_media/logo.png)
# 开箱即用的28181协议视频平台

[![Build Status](https://travis-ci.org/xia-chu/ZLMediaKit.svg?branch=master)](https://travis-ci.org/xia-chu/ZLMediaKit)
[![license](http://img.shields.io/badge/license-MIT-green.svg)](https://github.com/xia-chu/ZLMediaKit/blob/master/LICENSE)
[![JAVA](https://img.shields.io/badge/language-java-red.svg)](https://en.cppreference.com/)
[![platform](https://img.shields.io/badge/platform-linux%20|%20macos%20|%20windows-blue.svg)](https://github.com/xia-chu/ZLMediaKit)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-yellow.svg)](https://github.com/xia-chu/ZLMediaKit/pulls)


WEB VIDEO PLATFORM是一个基于GB28181-2016标准实现的开箱即用的网络视频平台，负责实现核心信令与设备管理后台部分，支持NAT穿透，支持海康、大华、宇视等品牌的IPC、NVR接入。支持国标级联，支持将不带国标功能的摄像机/直播流/直播推流转发到其他国标平台。   

流媒体服务基于@夏楚 ZLMediaKit [https://github.com/ZLMediaKit/ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit)   
播放器使用@dexter jessibuca [https://github.com/langhuihui/jessibuca/tree/v3](https://github.com/langhuihui/jessibuca/tree/v3)  
前端页面基于@Kyle MediaServerUI [https://gitee.com/kkkkk5G/MediaServerUI](https://gitee.com/kkkkk5G/MediaServerUI) 进行修改.  

# 应用场景：
支持浏览器无插件播放摄像头视频。  
支持摄像机、平台、NVR等设备接入。 
支持国标级联。多平台级联。跨网视频预览。
支持rtsp/rtmp等视频流转发到国标平台。  
支持rtsp/rtmp等推流转发到国标平台。  

# 项目目标
旨在打造一个易配置,易使用,便于维护的28181国标信令系统, 依托优秀的开源流媒体服务框架ZLMediaKit, 实现一个完整易用GB28181平台. 

# 部署文档
[doc.wvp-pro.cn](https://doc.wvp-pro.cn)

# gitee同步仓库
https://gitee.com/pan648540858/wvp-GB28181-pro.git

# 截图
![index](doc/_media/index.png "index.png")
![2](doc/_media/2.png "2.png")
![3](doc/_media/3.png "3.png")
![3-1](doc/_media/3-1.png "3-1.png")
![3-2](doc/_media/3-2.png "3-2.png")
![3-3](doc/_media/3-3.png "3-3.png")
![build_1](https://images.gitee.com/uploads/images/2022/0304/101919_ee5b8c79_1018729.png "2022-03-04_10-13.png")

# 功能特性 
-  [X] 集成web界面
-  [X] 兼容性良好
-  [X] 支持电子地图，支持接入WGS84和GCJ02两种坐标系，并且自动转化为合适的坐标系进行展示和分发
-  [X] 接入设备
  -  [X] 视频预览
  -  [X] 无限制接入路数，能接入多少设备只取决于你的服务器性能
  -  [X] 云台控制，控制设备转向，拉近，拉远
  -  [X] 预置位查询，使用与设置
  -  [X] 查询NVR/IPC上的录像与播放，支持指定时间播放与下载
  -  [X] 无人观看自动断流，节省流量
  -  [X] 视频设备信息同步
  -  [X] 离在线监控
  -  [X] 支持直接输出RTSP、RTMP、HTTP-FLV、Websocket-FLV、HLS多种协议流地址
  -  [X] 支持通过一个流地址直接观看摄像头，无需登录以及调用任何接口
  -  [X] 支持UDP和TCP两种国标信令传输模式
  -  [X] 支持UDP和TCP两种国标流传输模式
  -  [X] 支持检索,通道筛选
  -  [X] 支持通道子目录查询
  -  [X] 支持过滤音频，防止杂音影响观看
  -  [X] 支持国标网络校时
  -  [X] 支持播放H264和H265
  -  [X] 报警信息处理，支持向前端推送报警信息
  -  [X] 支持订阅与通知方法
    -  [X] 移动位置订阅
    -  [X] 移动位置通知处理
    -  [X] 报警事件订阅
    -  [X] 报警事件通知处理
    -  [X] 设备目录订阅
    -  [X] 设备目录通知处理
  -  [X] 移动位置查询和显示
  - [X] 支持手动添加设备和给设备设置单独的密码
-  [X] 支持平台对接接入
-  [X] 支持国标级联
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
    - [X] 每个级联平台可自定义的虚拟目录
    - [X] 目录订阅与通知
    - [X] 录像查看与播放
    - [X] GPS订阅与通知（直播推流）
-  [X] 支持自动配置ZLM媒体服务, 减少因配置问题所出现的问题;  
-  [X] 多流媒体节点，自动选择负载最低的节点使用。
-  [X] 支持启用udp多端口模式, 提高udp模式下媒体传输性能;
-  [X] 支持公网部署； 
-  [X] 支持wvp与zlm分开部署，提升平台并发能力
-  [X] 支持拉流RTSP/RTMP，分发为各种流格式，或者推送到其他国标平台
-  [X] 支持推流RTSP/RTMP，分发为各种流格式，或者推送到其他国标平台
-  [X] 支持推流鉴权
-  [X] 支持接口鉴权
-  [X] 云端录像，推流/代理/国标视频绝可以录制在云端服务器，支持预览和下载


# 遇到问题如何解决
国标最麻烦的地方在于设备的兼容性，所以需要大量的设备来测试，目前作者手里的设备有限，再加上作者水平有限，所以遇到问题在所难免；
1. 查看wiki，仔细的阅读可以帮你避免几乎所有的问题
2. 搜索issues，这里有大部分的答案
3. 加QQ群（901799015），这里有大量热心的小伙伴，但是前提新希望你已经仔细阅读了wiki和搜索了issues。
4. 你可以请作者为你解答，但是我不是免费的。
5. 你可以把遇到问题的设备寄给我，可以更容易的复现问题。

# 使用帮助
QQ群: 901799015, ZLM使用文档[https://github.com/ZLMediaKit/ZLMediaKit](https://github.com/ZLMediaKit/ZLMediaKit)  
QQ私信一般不回, 精力有限.欢迎大家在群里讨论.觉得项目对你有帮助，欢迎star和提交pr。

# 授权协议
本项目自有代码使用宽松的MIT协议，在保留版权信息的情况下可以自由应用于各自商用、非商业的项目。 但是本项目也零碎的使用了一些其他的开源代码，在商用的情况下请自行替代或剔除； 由于使用本项目而产生的商业纠纷或侵权行为一概与本项目及开发者无关，请自行承担法律风险。 在使用本项目代码时，也应该在授权协议中同时表明本项目依赖的第三方库的协议

# 致谢
感谢作者[夏楚](https://github.com/xia-chu) 提供这么棒的开源流媒体服务框架,并在开发过程中给予支持与帮助。     
感谢作者[dexter langhuihui](https://github.com/langhuihui) 开源这么好用的WEB播放器。     
感谢作者[Kyle](https://gitee.com/kkkkk5G) 开源了好用的前端页面     
感谢各位大佬的赞助以及对项目的指正与帮助。包括但不限于代码贡献、问题反馈、资金捐赠等各种方式的支持！以下排名不分先后：  
[lawrencehj](https://github.com/lawrencehj) [Smallwhitepig](https://github.com/Smallwhitepig) [swwhaha](https://github.com/swwheihei) 
[hotcoffie](https://github.com/hotcoffie) [xiaomu](https://github.com/nikmu) [TristingChen](https://github.com/TristingChen)
[chenparty](https://github.com/chenparty) [Hotleave](https://github.com/hotleave) [ydwxb](https://github.com/ydwxb)
[ydpd](https://github.com/ydpd) [szy833](https://github.com/szy833) [ydwxb](https://github.com/ydwxb) [Albertzhu666](https://github.com/Albertzhu666)
[mk1990](https://github.com/mk1990) [SaltFish001](https://github.com/SaltFish001)

ps: 刚增加了这个名单，肯定遗漏了一些大佬，欢迎大佬联系我添加。

