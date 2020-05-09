# wvp
WEB VIDEO PLATFORM是一个基于GB28181-2016标准实现的网络视频平台，目前只实现了核心信令部分，支持NAT穿透，支持海康、大华、宇视等品牌的IPC、NVR、DVR接入。
流媒体服务基于ZLMediaKit-https://github.com/xiongziliang/ZLMediaKit  
前端展示基于MediaServerUI-https://gitee.com/kkkkk5G/MediaServerUI

# 应用场景：
主要应用在IPC等设备没有固定IP地址，但需要在互联网中观看的场景。  
要求IPC设备可以访问互联网，有云服务器用于部署本服务。

# 支持特性：
1、视频预览  
2、云台控制（方向控制、缩放控制）  
3、视频设备信息同步  
4、离在线监控  
5、录像查询（基于NVR\DVR）  

# 路线图：
1、录像回放（基于NVR\DVR）  
2、设备认证（密码与数字证书）  
3、流媒体认证（ZLM推流、取流）  
4、语音对讲  
5、集群部署  
6、云端录像与回放  
7、时间同步  
8、上级级联  
9、其他国标能力  

业务集成数据请求流程参考wiki说明

项目部署文档参考wiki说明
