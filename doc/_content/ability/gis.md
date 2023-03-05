<!-- 电子地图 -->
# 电子地图
WVP提供了简单的电子地图用于设备的定位以及移动设备的轨迹信息，电子地图基于开源的地图引擎openlayers开发。
### 查看设备定位
1. 可以在设备列表点击“定位”按钮,自动跳转到电子地图页面； 
2. 在电子地图页面在设备上右键点击“定位”获取设备/平台下的所有通道位置。
3. 单击通道信息可以定位到具体的通道 


### 查询设备轨迹
查询轨迹需要提前配置save-position-history选项开启轨迹信息的保存，目前WVP此处未支持分库分表，对于大数据量的轨迹信息无法胜任，有需求请自行二次开发或者定制开发。
在电子地图页面在设备上右键点击“查询轨迹”获取设备轨迹信息。

PS： 目前的底图仅用用作演示和学习，商用情况请自行购买授权使用。

### 更换底图以及底图配置
目前WVP支持使用了更换底图，配置文件在web_src/static/js/config.js，请修改后重新编译前端文件。
```javascript
window.mapParam = {
  // 开启/关闭地图功能
  enable: true,
  // 坐标系 GCJ-02 WGS-84,
  coordinateSystem: "GCJ-02",
  // 地图瓦片地址
  tilesUrl: "http://webrd0{1-4}.is.autonavi.com/appmaptile?x={x}&y={y}&z={z}&lang=zh_cn&size=1&scale=1&style=8",
  // 瓦片大小
  tileSize: 256,
  // 默认层级
  zoom:10,
  // 默认地图中心点
  center:[116.41020, 39.915119],
  // 地图最大层级
  maxZoom:18,
  // 地图最小层级
  minZoom: 3
}
```
