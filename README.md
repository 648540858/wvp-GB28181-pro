# wvp
WEB VIDEO PLATFORM是一个基于GB28181-2016标准实现的网络视频平台，负责实现核心信令与设备管理后台部分，支持NAT穿透，支持海康、大华、宇视等品牌的IPC、NVR、DVR接入。   
流媒体服务基于ZLMediaKit-https://github.com/xiongziliang/ZLMediaKit   
前端展示基于MediaServerUI-https://gitee.com/kkkkk5G/MediaServerUI/tree/gb28181/

### fork自  [swwheihei/wvp-GB28181](https://github.com/swwheihei/wvp-GB28181)

# 应用场景：
原项目比较侧重单个摄像机的接入，当前这个更侧重平台接入，当然，直接接摄像机也是没有问题的。

# 支持特性：
1、视频预览  
2、云台控制（方向、缩放控制）  
3、视频设备信息同步  
4、离在线监控    
5、无人观看自动断流

# 待实现：
录像查询与回放（基于NVR\DVR，暂不支持快进、seek操作） 
12月底-上级级联、时间同步、其他国标能力  

# 项目部署


# 使用帮助


# 致谢
感谢作者[夏楚](https://github.com/xiongziliang) 提供这么棒的开源流媒体服务框架  
感谢作者[kkkkk5G](https://gitee.com/kkkkk5G) 提供这么棒的前端UI