# wvp
WEB VIDEO PLATFORM是一个基于GB28181-2016标准实现的网络视频平台，负责实现核心信令与设备管理后台部分，支持NAT穿透，支持海康、大华、宇视等品牌的IPC、NVR、DVR接入。   
流媒体服务基于ZLMediaKit-https://github.com/xiongziliang/ZLMediaKit   
前端展示基于MediaServerUI-https://gitee.com/kkkkk5G/MediaServerUI/tree/gb28181/

### fork自  [swwheihei/wvp-GB28181](https://github.com/swwheihei/wvp-GB28181)

# 应用场景：
主要应用在IPC等设备没有固定IP地址，但需要在互联网中观看的场景。  
要求IPC设备可以访问互联网，有云服务器用于部署本服务。
预计7月可以达到商用级别的文档性

# 支持特性：
1、视频预览  
2、云台控制（方向、缩放控制）  
3、视频设备信息同步  
4、离在线监控  
5、录像查询与回放（基于NVR\DVR，暂不支持快进、seek操作）  
6、无人观看自动断流

# 2020路线图：
5月中旬-录像回放（基于NVR\DVR）、设备认证（基于密码）  
5月下旬-设备报警  
6月上旬-流媒体认证（ZLM推流、取流）  
6月下旬-语音对讲、Android Deme\iOS Demo  
7月下旬-设备认证（基于数字证书）、集群部署  
8月下旬-云端录像与回放  
9月下旬-Onvif协议支持  
10月下旬-GB28181-2011版设备适配  
12月底-上级级联、时间同步、其他国标能力  

# 项目部署
参考wiki说明

# 使用帮助
参考wiki说明

# 致谢
感谢作者[夏楚](https://github.com/xiongziliang) 提供这么棒的开源流媒体服务框架  
感谢作者[kkkkk5G](https://gitee.com/kkkkk5G) 提供这么棒的前端UI


[]: https://github.com/swwheihei/wvp-GB28181