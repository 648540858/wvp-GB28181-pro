<template>
  <div>
    <a-row type="flex" :gutter="[16,0]">
      <a-col :flex="1">
        <a-card size="small" :bordered="false" title="轨迹查询">
          <a-space>
            <a-range-picker
              :ranges="{
              '今天':[moment(), moment().endOf('day')],
              '昨天': [moment().subtract(1,'days'), moment()],
              '上周': [moment().subtract(1,'weeks'), moment()],
              '上月': [moment().subtract(1,'months'), moment()]}"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              @change="onRangePickerChange"/>
            <a-button type="primary" @click="showHistoryPath">历史轨迹</a-button>
            <a-button type="primary" @click="showLatestPosition">最新位置</a-button>
          </a-space>
        </a-card>
      </a-col>
      <a-col :flex="1">
        <a-card size="small" :bordered="false" title="位置订阅">
          <a-space>
            <div>过期时间 (秒)：
              <a-input-number v-model="expired"/>
            </div>
            <div>上报周期 (秒)：
              <a-input-number v-model="interval"/>
            </div>
            <a-button type="primary" @click="subscribeMobilePosition">位置订阅</a-button>
            <a-button type="primary" @click="unSubscribeMobilePosition">取消订阅</a-button>
            <a-button @click="handleBack()">返回</a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
    <a-card size="small" :bordered="false" style="margin-top: 0.5rem">
      <div id="mapContainer" class="baiduMap"></div>
    </a-card>
  </div>
</template>

<script>
//baidu map
import {loadBMap} from "@/core/loadMap";
import moment from "moment";
import {positionHistory, queryLatestPosition, subscribePosition} from "@/api/deviceList";
import geoTools from "@/utils/GeoConvertTools"

export default {
  name: "DevicePostion",
  props: {
    record: {
      type: [Object, String],
      default: ''
    }
  },
  data() {
    return {
      map: null,
      mobilePositionList: [],
      mapPointList: [],
      searchFrom: null,
      searchTo: null,
      expired: 600,
      interval: 5
    }
  },
  created() {
    window.initBaiduMapScript = () => {
      this.mapHandle();
    };
    loadBMap("initBaiduMapScript");
  },
  methods: {
    moment,
    handleBack() {
      this.$emit('goBack')
    },
    mapHandle() {
      this.map = new BMap.Map("mapContainer"); // 创建地图实例
      let point = new BMap.Point(116.231398, 39.567445); // 创建点坐标
      this.map.centerAndZoom(point, 5); // 初始化地图，设置中心点坐标和地图级别
      this.map.enableScrollWheelZoom(true); //开启鼠标滚轮缩放
      this.map.addControl(new BMap.NavigationControl());
      this.map.addControl(new BMap.ScaleControl());
      this.map.addControl(new BMap.OverviewMapControl());
      this.map.addControl(new BMap.MapTypeControl());
      //map.setMapStyle({ style: 'midnight' }) //地图风格
    },
    showHistoryPath() {
      this.map.clearOverlays()
      if (!this.searchFrom || !this.searchTo) {
        this.$message.error("请选择时间和结束时间")
        return
      }
      let gbStartTime = this.searchFrom ? this.searchFrom.replaceAll(" ", "T") : null
      let gbEndTime = this.searchTo ? this.searchTo.replace(" ", "T") : null
      positionHistory({deviceId: this.record.deviceId, start: gbStartTime, end: gbEndTime,}).then(res => {
        if (res.success && res.data && res.data.length > 0) {
          this.mobilePositionList = res.data
          this.$nextTick(() => { //等待DOM更新后再标点
            this.showMarkPoints();
          })
        } else {
          this.$message.warn("未找到符合条件的移动位置信息")
        }
      }).catch(err => {
        console.log(err)
      })
    },
    onRangePickerChange(dates, dateStrings) {
      this.searchFrom = dateStrings[0]
      this.searchTo = dateStrings[1]
    },
    showMarkPoints() {
      let point = null
      this.mobilePositionList.forEach(position => {
        if (position.geodeticSystem === 'BD-09') {
          point = new BMap.Point(position.cnLng, position.cnLat)
        } else {
          let bdPos = geoTools.GPSToBaidu(position.longitude, position.latitude);
          point = new BMap.Point(bdPos.lat, bdPos.lng)
        }
        this.mapPointList.push(point)
        let marker = new BMap.Marker(point); // 创建标注
        this.map.addOverlay(marker); // 将标注添加到地图中
        //提示信息可以解析HTML标签以及CSS
        let infoWindow = new BMap.InfoWindow(`<p style='text-align:left;font-weight:800'>设备: ${position.deviceId}</p>
                            <p style='text-align:left;font-weight:0'>时间: ${position.time}</p>`);
        // 鼠标移上标注点要发生的事
        marker.addEventListener("mouseover", function () {
          this.openInfoWindow(infoWindow);
        });
        // 鼠标移开标注点要发生的事
        marker.addEventListener("mouseout", function () {
          this.closeInfoWindow(infoWindow);
        });
        // 鼠标点击标注点要发生的事情
        marker.addEventListener("click", function () {
        });
      })
      let view = this.map.getViewport(eval(this.mapPointList));
      this.map.centerAndZoom(view.center, view.zoom);
    },
    showLatestPosition() {
      this.map.clearOverlays();
      this.mapPointList = [];
      this.mobilePositionList = [];
      queryLatestPosition({deviceId: this.record.deviceId}).then(res => {
        if (res.success && res.data && res.data.length() > 0) {
          this.mobilePositionList.push(res.data)
          this.$nextTick(() => {
            this.showMarkPoints()
          })
        } else {
          this.$message.warn("未找到符合条件的移动位置信息")
        }
      }).catch(err => {
        console.log(err)
      })
    },
    subscribeMobilePosition() {
      let params = {deviceId: this.record.deviceId, expires: this.expired, interval: this.interval}
      subscribePosition(params).then(res => {
        if (res.success) {
          this.$message.success(res.data)
        } else {
          this.$message.error(res.data)
        }
      }).catch(err => {
        console.log(err)
      })
    },
    unSubscribeMobilePosition() {
      let params = {deviceId: this.record.deviceId, expires: 0, interval: this.interval}
      subscribePosition(params).then(res => {
        if (res.success) {
          this.$message.success(res.data)
        } else {
          this.$message.error(res.data)
        }
      }).catch(err => {
        console.log(err)
      })
    }
  }
}
</script>

<style scoped>
.baiduMap {
  width: 100%;
  height: 39.5rem;
  border: none;
  margin: 0;
  padding: 0;
}
</style>