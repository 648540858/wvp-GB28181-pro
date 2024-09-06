<!-- 部署 -->

# 部署
**请仔细阅读以下内容**
1. WVP-PRO与ZLM支持分开部署;
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
4. 生产环境按需开放端口，但是建议修改默认端口，尤其是5060端口，易受到攻击;
5. zlm使用docker部署的情况，请使用host模式，或者端口映射一致，比如映射5060,应将外部端口也映射为5060端口;
6. zlm与wvp会保持高频率的通信，所以不要去将wvp与zlm分属在两个网络，比如wvp在内网，zlm却在公网的情况。
7. 启动服务，以linux为例
**启动WVP-PRO**
```shell
nohup java -jar wvp-pro-*.jar &
```
**war包：**  
下载Tomcat后将war包放入webapps中，启动Tomcat以解压war包，停止Tomcat后，删除ROOT目录以及war包，将解压后的war包目录重命名为ROOT，将配置文件中的Server.port配置为与Tomcat端口一致
然后启动Tomcat。  
**启动ZLM**
```shell
nohup ./MediaServer -d -m 3 &
```
### 前后端分离部署
前后端部署目前在最新的版本已经支持，请使用3月15日之后的版本部署
前端编译后的文件在`src/main/resources/static`中，将此目录下的文件部署。
WVP默认开启全部接口支持跨域。部署前端文件到WEB容器，并将访问的地址设置为WVP的地址即可。
**配置前端服务器**
1. 在`src/main/resources/static/static/js/config.js`下配置服务器的地址，也就是wvp服务的地址
```javascript
window.baseUrl = "http://xxx.com:18080"
```
`这里的地址是需要客户电脑能访问到的，因为请求是客户端电脑发起，与代理不同`  
[接入设备](./_content/ability/device.md)
### 默认账号和密码
部署完毕后，可以通过访问 ip加端口的方式访问 WVP ，WVP的默认登录账号和密码均为 admin。



