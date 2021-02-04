/**
 * 经纬度转换
 */
export default {
  PI: 3.1415926535897932384626,  
  //PI: 3.14159265358979324,
  x_pi: (3.1415926535897932384626 * 3000.0) / 180.0,
  delta: function (lat, lng) {
    // Krasovsky 1940
    //
    // a = 6378245.0, 1/f = 298.3
    // b = a * (1 - f)
    // ee = (a^2 - b^2) / a^2;
    var a = 6378245.0; //  a: 卫星椭球坐标投影到平面地图坐标系的投影因子。
    var ee = 0.00669342162296594323; //  ee: 椭球的偏心率。
    var dLat = this.transformLat(lng - 105.0, lat - 35.0);
    var dLng = this.transformLng(lng - 105.0, lat - 35.0);
    var radLat = (lat / 180.0) * this.PI;
    var magic = Math.sin(radLat);
    magic = 1 - ee * magic * magic;
    var sqrtMagic = Math.sqrt(magic);
    dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * this.PI);
    dLng = (dLng * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * this.PI);
    return {
      lat: dLat,
      lng: dLng
    };
  },
  /**
   * WGS-84 to GCJ-02 GPS坐标转中国坐标
   * @param  {number} wgsLat GPS纬度
   * @param  {number} wgsLng GPS经度
   * @return {object}        返回中国坐标经纬度对象
   */
  GPSToChina: function (wgsLat, wgsLng) {
    if (this.outOfChina(wgsLat, wgsLng)) return {
      lat: wgsLat,
      lng: wgsLng
    };
    var d = this.delta(wgsLat, wgsLng);
    return {
      lat: Number(wgsLat) + Number(d.lat),
      lng: Number(wgsLng) + Number(d.lng)
    };
  },
  /**
   * GCJ-02 to WGS-84 中国标准坐标转GPS坐标
   * @param  {number} gcjLat 中国标准坐标纬度
   * @param  {number} gcjLng 中国标准坐标经度
   * @return {object}        返回GPS经纬度对象
   */
  chinaToGPS: function (gcjLat, gcjLng) {
    if (this.outOfChina(gcjLat, gcjLng)) return {
      lat: gcjLat,
      lng: gcjLng
    };
    var d = this.delta(gcjLat, gcjLng);
    return {
      lat: Number(gcjLat) - Number(d.lat),
      lng: Number(gcjLng) - Number(d.lng)
    };
  },
  /**
   * GCJ-02 to WGS-84 exactly 中国标准坐标转GPS坐标(精确)
   * @param  {number} gcjLat  中国标准坐标纬度
   * @param  {number} gcjLng  中国标准坐标经度
   * @return {object}         返回GPS经纬度对象(精确)
   */
  chinaToGPSExact: function (gcjLat, gcjLng) {
    var initDelta = 0.01;
    var threshold = 0.000000001;
    var dLat = initDelta,
      dLng = initDelta;
    var mLat = gcjLat - dLat,
      mLng = gcjLng - dLng;
    var pLat = gcjLat + dLat,
      pLng = gcjLng + dLng;
    var wgsLat,
      wgsLng,
      i = 0;
    while (1) {
      wgsLat = (mLat + pLat) / 2;
      wgsLng = (mLng + pLng) / 2;
      var tmp = this.gcj_encrypt(wgsLat, wgsLng);
      dLat = tmp.lat - gcjLat;
      dLng = tmp.lng - gcjLng;
      if (Math.abs(dLat) < threshold && Math.abs(dLng) < threshold) break;

      if (dLat > 0) pLat = wgsLat;
      else mLat = wgsLat;
      if (dLng > 0) pLng = wgsLng;
      else mLng = wgsLng;

      if (++i > 10000) break;
    }
    //console.log(i);
    return {
      lat: wgsLat,
      lng: wgsLng
    };
  },
  /**
   * GCJ-02 to BD-09 中国标准坐标转百度坐标(精确)
   * @param  {number} gcjLat  中国标准坐标纬度
   * @param  {number} gcjLng  中国标准坐标经度
   * @return {object}         返回百度经纬度对象
   */
  chinaToBaidu: function (gcjLat, gcjLng) {
    var x = gcjLng,
      y = gcjLat;
    var z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * this.x_pi);
    var theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * this.x_pi);
    var bdLng = z * Math.cos(theta) + 0.0065;
    var bdLat = z * Math.sin(theta) + 0.006;
    return {
      lat: bdLat,
      lng: bdLng
    };
  },
  /**
   * BD-09 to GCJ-02 百度坐标转中国标准坐标
   * @param  {number} bdLat  百度坐标纬度
   * @param  {number} bdLng  百度坐标经度
   * @return {object}        返回中国标准经纬度对象
   */
  baiduToChina: function (bdLat, bdLng) {
    var x = bdLng - 0.0065,
      y = bdLat - 0.006;
    var z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * this.x_pi);
    var theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * this.x_pi);
    var gcjLng = z * Math.cos(theta);
    var gcjLat = z * Math.sin(theta);
    return {
      lat: gcjLat,
      lng: gcjLng
    };
  },
  /**
   * BD-09 to GCJ-02 百度坐标转gps坐标
   * @param  {number} bdLat  百度坐标纬度
   * @param  {number} bdLng  百度坐标经度
   * @return {object}        返回gps经纬度对象
   */
  baiduToGPS: function (bdLat, bdLng) {
    let china = this.baiduToChina(bdLat, bdLng);
    return this.chinaToGPS(china.lat, china.lng);
  },
  /**
   * WGS-84 to to BD-09 GPS坐标转Baidu坐标
   * @param  {number} gpsLat GPS纬度
   * @param  {number} gpsLng GPS经度
   * @return {object}        返回百度经纬度对象
   */
  GPSToBaidu: function (gpsLat, gpsLng) {
    var china = this.GPSToChina(gpsLat, gpsLng);
    return this.chinaToBaidu(china.lat, china.lng);
  },
  /**
   * WGS-84 to Web mercator GPS坐标转墨卡托坐标
   * @param  {number} wgsLat GPS纬度
   * @param  {number} wgsLng GPS经度
   * @return {object}        返回墨卡托经纬度对象
   */
  GPSToMercator: function (wgsLat, wgsLng) {
    var x = (wgsLng * 20037508.34) / 180;
    var y = Math.log(Math.tan(((90 + wgsLat) * this.PI) / 360)) / (this.PI / 180);
    y = (y * 20037508.34) / 180;
    return {
      lat: y,
      lng: x
    };
    /*
    if ((Math.abs(wgsLng) > 180 || Math.abs(wgsLat) > 90))
        return null;
    var x = 6378137.0 * wgsLng * 0.017453292519943295;
    var a = wgsLat * 0.017453292519943295;
    var y = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));
    return {'lat' : y, 'lng' : x};
    //*/
  },
  /**
   * Web mercator to WGS-84 墨卡托坐标转GPS坐标
   * @param  {number} mercatorLat 墨卡托纬度
   * @param  {number} mercatorLng 墨卡托经度
   * @return {object}             返回GPS经纬度对象
   */
  mercatorToGPS: function (mercatorLat, mercatorLng) {
    var x = (mercatorLng / 20037508.34) * 180;
    var y = (mercatorLat / 20037508.34) * 180;
    y = (180 / this.PI) * (2 * Math.atan(Math.exp((y * this.PI) / 180)) - this.PI / 2);
    return {
      lat: y,
      lng: x
    };
    /*
    if (Math.abs(mercatorLng) < 180 && Math.abs(mercatorLat) < 90)
        return null;
    if ((Math.abs(mercatorLng) > 20037508.3427892) || (Math.abs(mercatorLat) > 20037508.3427892))
        return null;
    var a = mercatorLng / 6378137.0 * 57.295779513082323;
    var x = a - (Math.floor(((a + 180.0) / 360.0)) * 360.0);
    var y = (1.5707963267948966 - (2.0 * Math.atan(Math.exp((-1.0 * mercatorLat) / 6378137.0)))) * 57.295779513082323;
    return {'lat' : y, 'lng' : x};
    //*/
  },
  /**
   * 两点之间的距离
   * @param  {number} latA 起点纬度
   * @param  {number} lngA 起点经度
   * @param  {number} latB 终点纬度
   * @param  {number} lngB 终点经度
   * @return {number}      返回距离(米)
   */
  distance: function (latA, lngA, latB, lngB) {
    var earthR = 6371000;
    var x = Math.cos((latA * this.PI) / 180) * Math.cos((latB * this.PI) / 180) * Math.cos(((lngA - lngB) * this.PI) / 180);
    var y = Math.sin((latA * this.PI) / 180) * Math.sin((latB * this.PI) / 180);
    var s = x + y;
    if (s > 1) s = 1;
    if (s < -1) s = -1;
    var alpha = Math.acos(s);
    var distance = alpha * earthR;
    return distance;
  },
  /**
   * 是否在中国之外
   * @param  {number} lat 纬度
   * @param  {number} lng 经度
   * @return {boolean]}     返回结果真或假
   */
  outOfChina: function (lat, lng) {
    if (lat < 72.004 || lat > 137.8347) return true;
    if (lng < 0.8293 || lng > 55.8271) return true;
    return false;
  },
  transformLat: function (x, y) {
    var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
    ret += ((20.0 * Math.sin(6.0 * x * this.PI) + 20.0 * Math.sin(2.0 * x * this.PI)) * 2.0) / 3.0;
    ret += ((20.0 * Math.sin(y * this.PI) + 40.0 * Math.sin((y / 3.0) * this.PI)) * 2.0) / 3.0;
    ret += ((160.0 * Math.sin((y / 12.0) * this.PI) + 320 * Math.sin((y * this.PI) / 30.0)) * 2.0) / 3.0;
    return ret;
  },
  transformLng: function (x, y) {
    var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
    ret += ((20.0 * Math.sin(6.0 * x * this.PI) + 20.0 * Math.sin(2.0 * x * this.PI)) * 2.0) / 3.0;
    ret += ((20.0 * Math.sin(x * this.PI) + 40.0 * Math.sin((x / 3.0) * this.PI)) * 2.0) / 3.0;
    ret += ((150.0 * Math.sin((x / 12.0) * this.PI) + 300.0 * Math.sin((x / 30.0) * this.PI)) * 2.0) / 3.0;
    return ret;
  }
};
