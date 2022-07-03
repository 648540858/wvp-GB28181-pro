<template>
    <div id="devicePosition" style="width: 100vw; height: 91vh;">
      <el-container v-if="onOff" style="height: 91vh;" v-loading="isLoging">
        <el-aside width="auto" style="background-color: #ffffff">
          <DeviceTree ref="deviceTree" :clickEvent="clickEvent" :contextMenuEvent="contextmenuEventHandler" ></DeviceTree>
        </el-aside>
        <el-main style="height: 91vh; padding: 0">
          <MapComponent ref="map"></MapComponent>
        </el-main>
      </el-container>
      <div v-if="!onOff" style="width: 100%; height:100%; text-align: center; line-height: 5rem">
        <p>地图功能已关闭</p>
      </div>
      <div ref="infobox" v-if="channel != null " >
        <div v-if="channel != null" class="infobox-content">
          <el-descriptions class="margin-top" :title="channel.name" :column="1" :colon="true" size="mini" :labelStyle="labelStyle" >
            <el-descriptions-item label="编号" >{{channel.channelId}}</el-descriptions-item>
            <el-descriptions-item label="型号">{{channel.model}}</el-descriptions-item>
            <el-descriptions-item label="经度" >{{channel[longitudeStr]}}</el-descriptions-item>
            <el-descriptions-item label="纬度" >{{channel[latitudeStr]}}</el-descriptions-item>
            <el-descriptions-item label="生产厂商">{{channel.manufacture}}</el-descriptions-item>
            <el-descriptions-item label="行政区域" >{{channel.civilCode}}</el-descriptions-item>
            <el-descriptions-item label="设备归属" >{{channel.owner}}</el-descriptions-item>
            <el-descriptions-item label="安装地址" >{{channel.address == null?'未知': channel.address}}</el-descriptions-item>
            <el-descriptions-item label="云台类型" >{{channel.ptztypeText}}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" v-if="channel.status === 1">在线</el-tag>
              <el-tag size="small" type="info" v-if="channel.status === 0">离线</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div style="padding-top: 10px">
            <el-button v-bind:disabled="device == null || device.online === 0" type="primary" size="small" title="播放" icon="el-icon-video-play" @click="play(channel)"></el-button>
            <el-button type="primary" size="small" title="编辑位置" icon="el-icon-edit" @click="edit(channel)"></el-button>
            <el-button type="primary" size="small" title="轨迹查询" icon="el-icon-map-location" @click="getTrace(channel)"></el-button>
          </div>
          <span class="infobox-close el-icon-close" @click="closeInfoBox()"></span>
        </div>
      </div>
      <devicePlayer ref="devicePlayer" ></devicePlayer>
      <queryTrace ref="queryTrace" ></queryTrace>
    </div>
</template>

<script>
import MapComponent from "./common/MapComponent.vue";
import DeviceService from "./service/DeviceService";
import DeviceTree from "./common/DeviceTree";
import channelMapInfobox from "./dialog/channelMapInfobox";
import devicePlayer from './dialog/devicePlayer.vue'
import queryTrace from './dialog/queryTrace.vue'

export default {
  name: "map",
  components: {
    MapComponent,
    DeviceTree,
    channelMapInfobox,
    devicePlayer,
    queryTrace,
  },
  data() {
    return {
      onOff: typeof window.mapParam !== "undefined" && window.mapParam.enable,
      deviceService: new DeviceService(),
      layer: null,
      lineLayer: null,
      channel: null,
      device: null,
      infoBoxId: null,
      labelStyle: {
        width: "56px"
      },
      isLoging: false,
      longitudeStr: "longitude",
      latitudeStr: "latitude",
    };
  },
  created() {
    if (this.$route.query.deviceId) {
      console.log(this.$route.query.deviceId)
      // this.$refs.deviceTree.openByDeivceId(this.$route.query.deivceId)
      setTimeout(()=>{ // 延迟以等待地图加载完成 TODO 后续修改为通过是实际这；状态加回调完成
        this.deviceService.getAllChannel(false, false, this.$route.query.deviceId, this.channelsHandler)
      }, 1000)
    }
    if (window.mapParam.coordinateSystem == "GCJ-02") {
      this.longitudeStr = "longitudeGcj02";
      this.latitudeStr = "latitudeGcj02";
    }else if (window.mapParam.coordinateSystem == "WGS84") {
      this.longitudeStr = "longitudeWgs84";
      this.latitudeStr = "latitudeWgs84";
    }else {
      this.longitudeStr = "longitude";
      this.latitudeStr = "latitude";
    }
  },
  destroyed() {

  },
  methods: {
    clickEvent: function (device, data, isCatalog) {
      this.device = device;
      if (data.channelId && !isCatalog) {
        // 点击通道
        if (data[this.longitudeStr] * data[this.latitudeStr] === 0) {
          this.$message.error('未获取到位置信息');
        } else {
          if (this.layer != null) {
            this.$refs.map.removeLayer(this.layer);
          }
          this.closeInfoBox()
          this.layer = this.$refs.map.addLayer([{
            position: [data[this.longitudeStr], data[this.latitudeStr]],
            image: {
              src: this.getImageByChannel(data),
              anchor: [0.5, 1]
            },
            data: data
          }], this.featureClickEvent)
          this.$refs.map.panTo([data[this.longitudeStr], data[this.latitudeStr]], mapParam.maxZoom)
        }
      }
    },
    contextmenuEventHandler: function (device, event, data, isCatalog) {
      console.log(device)
      console.log(device.online)
      this.device = device;
      if (data.channelId && !isCatalog) {
        // 点击通道
        this.$contextmenu({
          items: [
            {
              label: "播放",
              icon: "el-icon-video-play",
              disabled: device.online === 0,
              onClick: () => {
                this.play(data);
              }
            },
            {
              label: "编辑位置",
              icon: "el-icon-edit",
              disabled: false,
              onClick: () => {
                this.edit(data)
              }
            },
            {
              label: "轨迹查询",
              icon: "el-icon-map-location",
              disabled: false,
              onClick: () => {
                this.getTrace(data)
              }
            }
          ],
          event, // 鼠标事件信息
          customClass: "custom-class", // 自定义菜单 class
          zIndex: 3000, // 菜单样式 z-index
        });
      } else {
        if (typeof data.channelId === "undefined") {
          this.deviceOrSubChannelMenu(event, data)
        }else {
          // TODO 子目录暂时不支持查询他下面所有设备, 支持支持查询直属于这个目录的设备
          this.deviceOrSubChannelMenu(event, data)
        }

      }

    },
    deviceOrSubChannelMenu: function (event, data) {
      // 点击设备
      this.$contextmenu({
        items: [
          {
            label: "定位",
            icon: "el-icon-s-promotion",
            disabled: false,
            onClick: () => {
              if (!data.channelId) {
                this.deviceService.getAllChannel(false, false, data.deviceId, this.channelsHandler)
              }
              if (data.channelId && data.subCount > 0) {
                // 点击子目录
                this.deviceService.getAllSubChannel(false, data.deviceId, data.channelId, this.channelsHandler)
              }
            }
          },
          {
            label: "查询轨迹",
            icon: "el-icon-map-location",
            disabled: false,
            onClick: () => {
              this.getTrace(data)
            }
          }
        ],
        event, // 鼠标事件信息
        customClass: "custom-class", // 自定义菜单 class
        zIndex: 3000, // 菜单样式 z-index
      });

    },
    channelsHandler: function (channels) {
      console.log(2)
      if (channels.length > 0) {
        this.clean()
        this.closeInfoBox()
        let params = [];


        for (let i = 0; i < channels.length; i++) {
          let longitude = channels[i][this.longitudeStr];
          let latitude = channels[i][this.latitudeStr];
          if (longitude * latitude === 0) {
            continue;
          }
          let item = {
            position: [longitude, latitude],
            image: {
              src: this.getImageByChannel(channels[i]),
              anchor: [0.5, 1]
            },
            data: channels[i]
          }
          params.push(item);
        }
        console.log(3)

        this.layer = this.$refs.map.addLayer(params, this.featureClickEvent)
        console.log(4)
        if (params.length === 1) {
          this.$refs.map.panTo([channels[0][this.longitudeStr], channels[0][this.latitudeStr]], mapParam.maxZoom)
        } else if (params.length > 1) {
          this.$refs.map.fit(this.layer)
        } else {
          this.$message.error('未获取到位置信息');
        }
      } else {
        this.$message.error('未获取到位置信息');
      }
    },
    getImageByChannel: function (channel) {
      let src = "static/images/gis/camera.png"
      switch (channel.ptztype) {
        case 1:
          if (channel.status === 1) {
            src = "static/images/gis/camera1.png"
          } else {
            src = "static/images/gis/camera1-offline.png"
          }
          break;
        case 2:
          if (channel.status === 1) {
            src = "static/images/gis/camera2.png"
          } else {
            src = "static/images/gis/camera2-offline.png"
          }
          break;
        case 3:
          if (channel.status === 1) {
            src = "static/images/gis/camera3.png"
          } else {
            src = "static/images/gis/camera3-offline.png"
          }
          break;
        default:
          if (channel.status === 1) {
            src = "static/images/gis/camera.png"
          } else {
            src = "static/images/gis/camera-offline.png"
          }
      }
      return src;
    },
    featureClickEvent: function (channels) {
      this.closeInfoBox()
      if (channels.length > 0) {
        this.channel = channels[0]
      }
      this.$nextTick(() => {
        let position = [this.channel[this.longitudeStr], this.channel[this.latitudeStr]];
        this.infoBoxId = this.$refs.map.openInfoBox(position, this.$refs.infobox, [0, -50])
      })
    },
    closeInfoBox: function () {
      if (this.infoBoxId != null) {
        this.$refs.map.closeInfoBox(this.infoBoxId)
      }
    },
    play: function (channel) {

      let deviceId = channel.deviceId;
      this.isLoging = true;
      let channelId = channel.channelId;
      console.log("通知设备推流1：" + deviceId + " : " + channelId);
      let that = this;
      this.$axios({
        method: 'get',
        url: '/api/play/start/' + deviceId + '/' + channelId
      }).then(function (res) {
        that.isLoging = false;
        if (res.data.code === 0) {

          that.$refs.devicePlayer.openDialog("media", deviceId, channelId, {
            streamInfo: res.data.data,
            hasAudio: channel.hasAudio
          });

        } else {
          that.$message.error(res.data.msg);
        }
      }).catch(function (e) {
      });
    },
    edit: function (data) {
      this.$message.warning('暂不支持');
    },
    getTrace: function (data) {
      // this.$message.warning('暂不支持');
      this.clean()
      this.$refs.queryTrace.openDialog(data, (channelPositions) => {
        console.log("getTrace")
        console.log(channelPositions)
        if (channelPositions.length === 0) {
          this.$message.success('未查询到轨迹信息');
        } else {
          let positions = [];
          for (let i = 0; i < channelPositions.length; i++) {
            if (channelPositions[i][this.longitudeStr] * channelPositions[i][this.latitudeStr] > 0) {
              positions.push([channelPositions[i][this.longitudeStr], channelPositions[i][this.latitudeStr]])
            }

          }
          if (positions.length === 0) {
            this.$message.success('未查询到轨迹信息');
            return;
          }
          this.lineLayer = this.$refs.map.addLineLayer(positions)
          this.$refs.map.fit(this.lineLayer)
        }
      })
    },
    clean: function (){
      if (this.lineLayer != null) {
        this.$refs.map.removeLayer(this.lineLayer)
      }
      if (this.infoBoxId != null) {
        this.$refs.map.closeInfoBox(this.infoBoxId)
      }
      if (this.layer != null) {
        this.$refs.map.removeLayer(this.layer)
      }
    }
  },

};
</script>

<style>
.infobox-content{
  width: 260px;
  background-color: #FFFFFF;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #e2e2e2;
}

.infobox-content::after {
  position: absolute;
  bottom: -11px;
  left: 130px;
  display: block;
  content: "";
  width: 16px;
  height: 16px;
  background: url('~@static/images/arrow.png') no-repeat center;
}
.infobox-close {
  position: absolute;
  right: 1rem;
  top: 1rem;
  color: #000000;
  cursor:pointer
}
.el-descriptions__title {
  font-size: 1rem;
  font-weight: 700;
  padding: 20px 20px 0px 23px;
  text-align: center;
  width: 100%;
}
</style>
