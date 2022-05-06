// map组件全局参数, 注释此内容可以关闭地图功能
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
