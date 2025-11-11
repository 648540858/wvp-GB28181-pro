<template>
    <div id="devicePosition" style="height: calc(100vh - 84px);width: 100%;">
      <div style="height: 100%; display: grid; grid-template-columns: 360px auto">
        <DeviceTree ref="deviceTree" @clickEvent="treeChannelClickEvent" :showPosition="true" :contextmenu="getContextmenu()"/>
        <MapComponent ref="mapComponent" @loaded="initChannelLayer" @coordinateSystemChange="initChannelLayer" @zoomChange="zoomChange"></MapComponent>
      </div>
      <div class="map-tool-box-bottom-right">
        <div class="map-tool-btn-group" v-if="mapTileList.length > 0">
          <el-dropdown placement="top"  @command="changeLayerType">
            <div class="el-dropdown-link map-tool-btn">
              <i class="iconfont icon-mti-jutai"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item :command="0" >
                <span v-if="layerType !== 0">图层关闭</span>
                <span v-if="layerType === 0" style="color: rgb(64, 158, 255);">图层关闭</span>
              </el-dropdown-item>
              <el-dropdown-item :command="1" >
                <span v-if="layerType !== 1">直接展示</span>
                <span v-if="layerType === 1" style="color: rgb(64, 158, 255);">直接展示</span>
              </el-dropdown-item>
              <el-dropdown-item :command="2">
                <span v-if="layerType !== 2">抽稀图层</span>
                <span v-if="layerType === 2" style="color: rgb(64, 158, 255);">抽稀图层</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div class="map-tool-btn-group" v-if="mapTileList && mapTileList.length > 1">
          <el-dropdown placement="top"  @command="changeMapTile">
            <div class="el-dropdown-link map-tool-btn">
              <i class="iconfont icon-tuceng"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item  v-for="(item,index) in mapTileList" :key="index" :command="index">{{item.name}}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" @click="refreshLayer">
            <i class="iconfont icon-shuaxin3"></i>
          </div>
        </div>
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" @click="zoomIn">
            <i class="iconfont icon-plus1"></i>
          </div>
          <div class="map-tool-btn" @click="zoomOut">
            <i class="iconfont icon-minus1"></i>
          </div>
        </div>
      </div>
      <div class="map-tool-box-top-left">
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" title="图层抽稀" @click="showDrawThinBox(true)">
            <i class="iconfont icon-mti-sandian"></i> <span>图层抽稀</span>
          </div>
        </div>
      </div>
      <transition name="el-zoom-in-top">
        <div v-show="showDrawThin"  class="map-tool-draw-thin">
          <div class="map-tool-draw-thin-density">
            <span style="line-height: 36px; font-size: 15px">间隔： </span>
            <el-slider v-model="diffPixels" show-input :min="1" :max="200" input-size="mini" ></el-slider>
            <div style="margin-left: 10px; line-height: 38px;">
              <el-button :loading="quicklyDrawThinLoading" @click="quicklyDrawThin" size="mini">快速抽稀</el-button>
              <el-button :loading="boxDrawThinLoading" size="mini" @click="boxDrawThin" >局部抽稀</el-button>
              <el-button size="mini" @click="resetDrawThinData()">数据还原</el-button>
              <el-button :loading="saveDrawThinLoading" type="primary" :disabled="drawThinId === null" size="mini" @click="saveDrawThin()">保存</el-button>
              <el-button type="warning" size="mini" @click="showDrawThinBox(false)">取消</el-button>
            </div>
          </div>
        </div>
      </transition>

<!--      <div class="map-tool-box-top-right">-->
<!--        <div class="map-tool-btn-group">-->
<!--          <div class="map-tool-btn" title="抽稀">-->
<!--            <i class="iconfont icon-mti-sandian"></i>-->
<!--          </div>-->
<!--          <div class="map-tool-btn" title="聚合">-->
<!--            <i class="iconfont icon-mti-jutai"></i>-->
<!--          </div>-->

<!--        </div>-->
<!--      </div>-->
      <div ref="infobox">
        <transition name="el-zoom-in-center">
          <div class="infobox-content" v-if="channel">
            <el-descriptions class="margin-top" :title="channel.gbName" :column="1" :colon="true" size="mini" :labelStyle="labelStyle" >
              <el-descriptions-item label="编号" >{{channel.gbDeviceId}}</el-descriptions-item>
              <el-descriptions-item label="生产厂商">{{channel.gbManufacture}}</el-descriptions-item>
              <el-descriptions-item label="安装地址" >{{channel.gbAddress == null?'未知': channel.gbAddress}}</el-descriptions-item>
            </el-descriptions>
            <div style="padding-top: 10px; margin: 0 auto; width: fit-content;">
              <el-button v-bind:disabled="channel.gbStatus !== 'ON'" type="primary" size="small" title="播放" icon="el-icon-video-play" @click="play(channel)">播放</el-button>
              <el-button type="primary" size="small" title="编辑" icon="el-icon-edit" @click="edit(channel)">编辑</el-button>
              <el-button type="primary" size="small" title="位置" icon="el-icon-coordinate" @click="editPosition(channel)">位置</el-button>
<!--              <el-button type="primary" size="small" title="轨迹查询" icon="el-icon-map-location" @click="getTrace(channel)">轨迹</el-button>-->
            </div>
            <span class="infobox-close el-icon-close" @click="closeInfoBox"></span>
          </div>
        </transition>

      </div>

      <div ref="infoboxForEdit">
        <transition name="el-zoom-in-center">
          <div class="infobox-edit-content" v-if="dragChannel">
            <div style="width: 100%; line-height: 1.5rem; font-size: 14px">{{dragChannel.gbName}}  ({{dragChannel.gbDeviceId}})</div>
            <span style="font-size: 14px">经度:</span> <el-input v-model="dragChannel.gbLongitude" placeholder="请输入经度" style="width: 7rem; margin-right: 10px"></el-input>
            <span style="font-size: 14px">纬度: </span> <el-input v-model="dragChannel.gbLatitude" placeholder="请输入纬度" style="width: 7rem; "></el-input>
            <el-button icon="el-icon-close" size="medium" type="text" @click="cancelEdit(dragChannel)" style="margin-left: 1rem; font-size: 18px; color: #2b2f3a"></el-button>
            <el-button icon="el-icon-check" size="medium" type="text" @click="submitEdit(dragChannel)" style="font-size: 18px; color: #0842e2"></el-button>
          </div>
        </transition>
      </div>
      <devicePlayer ref="devicePlayer" ></devicePlayer>
      <queryTrace ref="queryTrace" ></queryTrace>
      <CommonChannelEditDialog ref="commonChannelEditDialog" ></CommonChannelEditDialog>
      <DrawThinProgress ref="drawThinProgress" ></DrawThinProgress>
    </div>
</template>

<script>
import DeviceTree from '../common/DeviceTree.vue'
import queryTrace from './queryTrace.vue'
import MapComponent from '../common/MapComponent.vue'
import devicePlayer from '../common/channelPlayer/index.vue'
import CommonChannelEditDialog from '../dialog/commonChannelEditDialog.vue'
import DrawThinProgress from './dialog/drawThinProgress.vue'

let cameraListForSource = []
let cameraList = []
let cameraListForLevelMap = new Map()
let cameraLayerExtent = []
let channelLayer, channelTileLayer = null
export default {
  name: 'Map',
  components: {
    DrawThinProgress,
    CommonChannelEditDialog,
    DeviceTree,
    devicePlayer,
    queryTrace,
    MapComponent
  },
  data() {
    return {
      layer: null,
      channel: null,
      dragChannel: {},
      feature: null,
      device: null,
      infoBoxId: null,
      labelStyle: {
        width: '56px'
      },
      isLoging: false,
      longitudeStr: 'longitude',
      latitudeStr: 'latitude',
      mapTileList: [],
      diffPixels: 30,
      zoomValue: 10,
      showDrawThin: false,
      quicklyDrawThinLoading: false,
      boxDrawThinLoading: false,
      drawThinId: null,
      drawThinLayer: null,
      saveDrawThinLoading: false,
      layerType: 0
    }
  },
  created() {

  },
  destroyed() {

  },
  methods: {
    initChannelLayer: function () {
      this.mapTileList = this.$refs.mapComponent.mapTileList
      // 获取所有有位置的通道
      this.closeInfoBox()

      let clientEvent = data => {
        this.closeInfoBox()
        this.$nextTick(() => {
          if (data[0].edit) {
            this.showEditInfo(data[0])
          }else {
            this.showChannelInfo(data[0])
          }
        })
      }

      channelLayer = this.$refs.mapComponent.addPointLayer([], clientEvent, null)
    },
    refreshLayer(){
      this.closeInfoBox()
      // 刷新瓦片图层
      if (channelLayer) {
        this.$refs.mapComponent.refreshLayer(channelLayer)
      }
      if (channelTileLayer) {
        this.$refs.mapComponent.refreshLayer(channelTileLayer)
      }
    },
    treeChannelClickEvent: function (id) {
      this.closeInfoBox()
      this.$store.dispatch('commonChanel/queryOne', id)
        .then(data => {
          if (!data.gbLongitude || data.gbLongitude < 0 || !data.gbLatitude || data.gbLatitude < 0) {
            this.$message.warning({
              showClose: true,
              message: '无位置信息'
            })
            return
          }
          let zoomExtent = this.$refs.mapComponent.getZoomExtent()
          this.$refs.mapComponent.panTo([data.gbLongitude, data.gbLatitude], zoomExtent[1], () => {
            this.showChannelInfo(data)
          })
        })
    },
    zoomIn: function() {
      this.$refs.mapComponent.zoomIn()
    },
    zoomOut: function() {
      this.$refs.mapComponent.zoomOut()
    },
    getContextmenu: function (event) {
        return [
          {
            label: '播放通道',
            icon: 'el-icon-video-play',
            type: 1,
            onClick: (event, data, node) => {
              console.log(data)
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.play(data)
                })
            }
          },
          {
            label: '修改位置',
            icon: 'el-icon-coordinate',
            type: 1,
            onClick: (event, data, node) => {
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.editPosition(data)
                })
            }
          },
          {
            label: '编辑通道',
            icon: 'el-icon-edit',
            type: 1,
            onClick: (event, data, node) => {
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.edit(data)
                })
            }
          }
        ]
    },
    showChannelInfo: function(data) {
      this.channel = data
      // 此时增加临时图标
      let position = [data.gbLongitude, data.gbLatitude]
      let cameraData = {
        id: data.gbId,
        position: position,
        data: data,
        status: data.gbStatus
      }
      this.$refs.mapComponent.addFeature(channelLayer, cameraData)

      this.infoBoxId = this.$refs.mapComponent.openInfoBox(position, this.$refs.infobox, [0, -50])
    },
    zoomChange: function(zoom) {},

    changeMapTile: function (index) {
      if (this.showDrawThin) {
        this.$message.warning({
          showClose: true,
          message: '抽稀操作进行中，禁止切换图层'
        })
        return
      }
      this.$refs.mapComponent.changeMapTile(index)
    },
    clientEvent(data){
      this.closeInfoBox()
      this.$nextTick(() => {
        if (data[0].edit) {
          this.showEditInfo(data[0])
        }else {
          this.showChannelInfo(data[0])
        }
      })
    },
    changeLayerType: function (index) {
      this.layerType = index
      if (index === 0) {
        this.$refs.mapComponent.removeLayer(channelTileLayer)
        return
      }

      let geoCoordSys = this.$refs.mapComponent.getCoordSys()
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      let baseApi = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl)
      let tileUrl = null
      if (index === 1) {
        tileUrl = baseApi + `/api/common/channel/map/tile/{z}/{x}/{y}?geoCoordSys=${geoCoordSys}&accessToken=${this.$store.getters.token}`
      }else if (index === 2) {
        tileUrl = baseApi + `/api/common/channel/map/thin/tile/{z}/{x}/{y}?geoCoordSys=${geoCoordSys}&accessToken=${this.$store.getters.token}`
      }
      channelTileLayer = this.$refs.mapComponent.addVectorTileLayer(tileUrl, this.clientEvent)
    },
    closeInfoBox: function () {
      if (this.infoBoxId !== null) {
        this.$refs.mapComponent.closeInfoBox(this.infoBoxId)
      }
      this.channel = null
      this.dragChannel = null
    },
    play: function (channel) {
      const loading = this.$loading({
        lock: true,
        text: '正在请求视频',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/playChannel', channel.gbId)
        .then((data) => {
          this.$refs.devicePlayer.openDialog('media', channel.gbId, {
            streamInfo: data,
            hasAudio: channel.hasAudio
          })
        }).finally(() => {
          loading.close()
        })
    },
    edit: function (channel) {
      this.$refs.commonChannelEditDialog.openDialog(channel.gbId)
    },
    editPosition: function (channel) {
      this.closeInfoBox()
      // 开启图标可拖动
      this.$refs.mapComponent.dragInteraction.addFeatureId(channel.gbId,
        {
          startEvent: event => {
            this.closeInfoBox()
          },
          endEvent: event => {
            channel.gbLongitude = event.lonLat[0]
            channel.gbLatitude = event.lonLat[1]
            this.showEditInfo(channel)
          }
        }
      )

      let position = null
      if (!!channel.gbLongitude && !!channel.gbLatitude && channel.gbLongitude > 0 && channel.gbLatitude > 0) {
        position = [channel.gbLongitude, channel.gbLatitude]
        channel['oldLongitude'] = channel.gbLongitude
        channel['oldLatitude'] = channel.gbLatitude
      }else {
        position = this.$refs.mapComponent.getCenter()
        channel['oldLongitude'] = channel.gbLongitude
        channel['oldLatitude'] = channel.gbLatitude
        channel.gbLongitude = position[0]
        channel.gbLatitude = position[1]
      }

      channel['edit'] = true
      if (!this.$refs.mapComponent.coordinateInView(position)) {
        this.$refs.mapComponent.panTo(position, 16, () => {
          this.showEditInfo(channel)
        })
      }else {
        this.showEditInfo(channel)
      }

      // 标记可编辑图标为红色
      this.$refs.mapComponent.setFeaturePositionById(channelLayer, channel.gbId, {
        id: channel.gbId,
        position: position,
        data: channel,
        status: 'checked'
      })
      // 如果开启了瓦片图层，此时应该让瓦片图层不再显示这个feature
      if (channelTileLayer) {
        this.$refs.mapComponent.hideFeature(channelTileLayer, channel.gbId)
      }
    },
    showEditInfo: function(data) {
      this.dragChannel = data
      this.infoBoxId = this.$refs.mapComponent.openInfoBox([data.gbLongitude, data.gbLatitude], this.$refs.infoboxForEdit, [0, -50])
    },
    cancelEdit: function(channel) {
      this.closeInfoBox()
      this.$refs.mapComponent.dragInteraction.removeFeatureId(channel.gbId)
      channel.gbLongitude = channel.oldLongitude
      channel.gbLatitude = channel.oldLatitude
      channel['edit'] = false
      this.$refs.mapComponent.setFeaturePositionById(channelLayer, channel.gbId, {
        id: channel.gbId,
        position: [channel.gbLongitude, channel.gbLatitude],
        data: channel,
        status: channel.gbStatus
      })
      if (channelTileLayer) {
        this.$refs.mapComponent.cancelHideFeature(channelTileLayer, channel.gbId)
      }
    },
    submitEdit: function(channel) {
      let position = [channel.gbLongitude, channel.gbLatitude]
      this.$store.dispatch('commonChanel/update', channel)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
          this.closeInfoBox()
          channel['edit'] = false
          this.$refs.mapComponent.dragInteraction.removeFeatureId(channel.gbId)

          this.$refs.mapComponent.setFeaturePositionById(channelLayer, channel.gbId, {
            id: channel.gbId,
            position: position,
            data: channel,
            status: channel.gbStatus
          })
          // 刷星树菜单
          this.$refs.deviceTree.refresh('channel' + channel.gbId)

        })
    },
    showDrawThinBox: function(show){
      this.showDrawThin = show
      if (!show) {
        setTimeout(() => {
          // 关闭抽稀预览
          if (this.drawThinId !== null) {
            // 发送消息 清空抽稀结果
            this.$store.dispatch('commonChanel/clearThin', this.drawThinId)
            this.drawThinId = null
          }
          if (this.drawThinLayer !== null) {
            this.$refs.mapComponent.removeLayer(this.drawThinLayer)
            this.drawThinLayer = null
          }
          // 展示图层
          if (this.layerType > 0) {
            this.changeLayerType(this.layerType)
          }
        }, 1)
      }

    },
    quicklyDrawThin: function (){
      if (channelLayer) {
        this.$refs.mapComponent.removeLayer(channelLayer)
      }
      if (channelTileLayer) {
        this.$refs.mapComponent.removeLayer(channelTileLayer)
      }
      if (this.drawThinLayer !== null) {
        this.$refs.mapComponent.removeLayer(this.drawThinLayer)
        this.drawThinLayer = null
      }
      this.quicklyDrawThinLoading = true
      // 获取每一个图层的抽稀参数
      this.$store.dispatch('commonChanel/drawThin', {
        zoomParam: this.getDrawThinParam()
      })
        .then(drawThinId => {
          // 显示抽稀进度
          this.drawThinId = drawThinId
          this.$refs.drawThinProgress.openDialog(drawThinId, () => {
            this.closeInfoBox()
            this.$message.success({
              showClose: true,
              message: '抽稀完成，请预览无误后保存抽稀结果'
            })
            // 展示抽稀结果
            this.showDrawThinLayer(drawThinId)
          })
        })
        .finally(() => {
          this.quicklyDrawThinLoading = false
        })
    },
    showDrawThinLayer(thinId) {
      if (this.drawThinLayer) {
        this.$refs.mapComponent.removeLayer(this.drawThinLayer)
        this.drawThinLayer = null
      }
      // 展示抽稀结果
      let geoCoordSys = this.$refs.mapComponent.getCoordSys()
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      let baseApi = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl)
      let tileUrl = baseApi + `/api/common/channel/map/thin/tile/{z}/{x}/{y}?geoCoordSys=${geoCoordSys}&thinId=${thinId}&accessToken=${this.$store.getters.token}`
      this.drawThinLayer = this.$refs.mapComponent.addVectorTileLayer(tileUrl, this.clientEvent)
    },
    boxDrawThin: function (){
      this.$message.warning({
        showClose: true,
        message: '点击地图进行框选'
      })
      // 绘制框
      this.$refs.mapComponent.startDrawBox((extent) => {

        // 清理默认的摄像头图层
        if (channelLayer) {
          this.$refs.mapComponent.removeLayer(channelLayer)
        }
        if (channelTileLayer) {
          this.$refs.mapComponent.removeLayer(channelTileLayer)
        }
        if (this.drawThinLayer !== null) {
          this.$refs.mapComponent.removeLayer(this.drawThinLayer)
          this.drawThinLayer = null
        }
        this.boxDrawThinLoading = true
        // 获取每一个图层的抽稀参数
        this.$store.dispatch('commonChanel/drawThin', {
          zoomParam: this.getDrawThinParam(),
          extent: {
            minLng: extent[0],
            minLat: extent[1],
            maxLng: extent[2],
            maxLat: extent[3]
          },
          geoCoordSys: 'GCJ02'
        })
          .then(drawThinId => {
            // 显示抽稀进度
            this.drawThinId = drawThinId
            this.$refs.drawThinProgress.openDialog(drawThinId, () => {
              this.closeInfoBox()
              this.$message.success({
                showClose: true,
                message: '抽稀完成，请预览无误后保存抽稀结果'
              })
              // 展示抽稀结果
              this.showDrawThinLayer(drawThinId)
            })
          })
          .finally(() => {
            this.boxDrawThinLoading = false
          })
      })
    },
    getDrawThinParam() {
      // 获取全部层级
      let zoomExtent = this.$refs.mapComponent.getZoomExtent()
      let zoomMap = {}
      let zoom = zoomExtent[0]
      while (zoom <= zoomExtent[1]) {
        // 计算经纬度差值
        let diff = this.$refs.mapComponent.computeDiff(this.diffPixels, zoom)
        if (diff && diff > 0) {
          zoomMap[zoom] = diff
        }
        zoom += 1
      }
      return zoomMap
    },

    saveDrawThin: function(){
      if (!this.drawThinId) {
        return
      }
      this.saveDrawThinLoading = true
      this.$store.dispatch('commonChanel/saveThin', this.drawThinId)
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
          this.showDrawThinBox(false)
        })
        .finally(() => {
          this.saveDrawThinLoading = false
        })
    },
    resetDrawThinData(){
      this.$confirm('确定移除抽稀结果?', '操作提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('commonChanel/resetLevel')
          .then(() => {
            this.$message.success({
              showClose: true,
              message: '数据还原成功'
            })
          })
      })
    }
  }

}
</script>

<style>
.map-tool-box-bottom-right {
  position: absolute;
  right: 20px;
  bottom: 20px;
}
.map-tool-box-top-right {
  position: absolute;
  right: 20px;
  top: 20px;
}
.map-tool-box-top-left {
  position: absolute;
  left: 380px;
  top: 20px;
}
.map-tool-btn-group {
  background-color: #FFFFFF;
  border-radius: 3px;
  user-select: none;
  box-shadow: 0 2px 2px rgba(0, 0, 0, .15);
  margin-bottom: 10px;
}
.map-tool-box-top-left .map-tool-btn-group {
  display: flex;
}
.map-tool-box-top-right .map-tool-btn-group {
  display: flex;
}
.map-tool-box-top-left .map-tool-btn {
  padding: 0 10px;
}
.map-tool-box-top-right .map-tool-btn {
  padding: 0 10px;
}
.map-tool-btn {
  border-bottom: 1px #dfdfdf solid;
  border-right: 1px #dfdfdf solid;
  width: fit-content;
  min-width: 33px;
  height: 36px;
  cursor: pointer;
  text-align: center;
  line-height: 36px;
  font-size: 14px;
}
.map-tool-btn i {
  font-size: 14px;
}
.map-tool-btn-group:last-child {
  border-bottom: none;
  border-right: none;
}
.map-tool-draw-thin {
  position: absolute;
  top: 63px;
  left: 380px;
  border: 1px solid #dfdfdf;
  background-color: #fff;
  border-radius: 4px;
  padding: 0 10px;
}
.map-tool-draw-thin-density {
  display: grid;
  grid-template-columns: 50px 400px auto;
  padding: 0;
  margin: 0;
}

.infobox-content{
  width: 270px;
  background-color: #FFFFFF;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #868686;
}

.infobox-content::after {
  position: absolute;
  bottom: -11px;
  left: calc(50% - 8px);
  display: block;
  content: "";
  width: 16px;
  height: 16px;
  background: url('/static/images/arrow.png') no-repeat center;
}

.infobox-edit-content{
  width: 400px;
  background-color: #FFFFFF;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #868686;
}

.infobox-edit-content::after {
  position: absolute;
  bottom: -11px;
  left: calc(50% - 8px);
  display: block;
  content: "";
  width: 16px;
  height: 16px;
  background: url('/static/images/arrow.png') no-repeat center;
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
