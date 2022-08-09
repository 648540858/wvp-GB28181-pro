<!-- 部署 -->

# 部署
**请仔细阅读以下内容**
1. WVP-PRO与ZLM支持分开部署，但是wvp-pro-assist必须与zlm部署在同一台主机;
2. 需要开放的端口
| 服务  | 端口                       | 类型          | 必选    |
|-----|:-------------------------|-------------|-------|
| wvp | server.port              | tcp         | 是     |
| wvp | sip.port                 | udp and tcp | 是     |
| zlm | http.port                | tcp         | 是     |
| zlm | http.sslport             | tcp         | 否     |
| zlm | rtmp.port                | tcp         | 否     |
| zlm | rtmp.sslport             | tcp         | 否     |
| zlm | rtsp.port                | udp and tcp | 否     |
| zlm | rtsp.sslport             | udp and tcp | 否     |
| zlm | rtp_proxy.port           | udp and tcp | 单端口开放 |
| zlm | rtp.port-range(在wvp中配置)  | udp and tcp | 多端口开放 |

3. 测试环境部署建议所有服务部署在一台主机，关闭防火墙，减少因网络出现问题的可能;
4. WVP-PRO与ZLM支持分开部署，但是wvp-pro-assist必须与zlm部署在同一台主机;
5. 生产环境按需开放端口，但是建议修改默认端口，尤其是5060端口，易受到攻击;
6. zlm使用docker部署的情况，要求端口映射一致，比如映射5060,应将外部端口也映射为5060端口;
7. 启动服务，以linux为例
**启动WVP-PRO**
```shell
nohup java -jar java -jar wvp-pro-*.jar &
```

**启动ZLM**
```shell
nohup ./MediaServer -d -m 3 &
```

[接入设备](./_content/ability/device.md)

