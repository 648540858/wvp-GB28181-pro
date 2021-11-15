/**
 * 按需加载地图API
 */

export function loadBMap(funcName) {
  const script = document.createElement("script");
  script.src = "//api.map.baidu.com/api?v=2.0&ak=rk73w8dv1rkE4UdZsataG68VarhYQzrx&s=1&callback=" + funcName;
  document.body.appendChild(script);
}