<template>
    <div id="devicePosition" style="height: calc(100vh - 84px);width: 100%;">
      <div style="height: 100%; display: grid; grid-template-columns: 360px auto">
        <DeviceTree ref="deviceTree" @clickEvent="treeChannelClickEvent" :showPosition="true" :contextmenu="getContextmenu()"/>
        <MapComponent ref="mapComponent" @loaded="initChannelLayer" @coordinateSystemChange="initChannelLayer" @zoomChange="zoomChange"></MapComponent>
      </div>
      <div class="map-tool-box-bottom-right">

        <div class="map-tool-btn-group" v-if="mapTileList.length > 0">
          <el-dropdown placement="top"  @command="changeLayerStyle">
            <div class="el-dropdown-link map-tool-btn">
              <i class="iconfont icon-mti-jutai"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item :command="0" >
                <span v-if="layerStyle !== 0">图层关闭</span>
                <span v-if="layerStyle === 0" style="color: rgb(64, 158, 255);">图层关闭</span>
              </el-dropdown-item>
              <el-dropdown-item :command="1" >
                <span v-if="layerStyle !== 1">直接展示</span>
                <span v-if="layerStyle === 1" style="color: rgb(64, 158, 255);">直接展示</span>
              </el-dropdown-item>
              <el-dropdown-item :command="2">
                <span v-if="layerStyle !== 2">碰撞检测</span>
                <span v-if="layerStyle === 2" style="color: rgb(64, 158, 255);">碰撞检测</span>
              </el-dropdown-item>
              <el-dropdown-item :command="3">
                <span v-if="layerStyle !== 3">抽稀图层</span>
                <span v-if="layerStyle === 3" style="color: rgb(64, 158, 255);">抽稀图层</span>
              </el-dropdown-item>
<!--              <el-dropdown-item :command="4">-->
<!--                <span v-if="layerStyle !== 4">聚合图层</span>-->
<!--                <span v-if="layerStyle === 4" style="color: rgb(64, 158, 255);">聚合图层</span>-->
<!--              </el-dropdown-item>-->
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div class="map-tool-btn-group" v-if="mapTileList.length > 0">
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
          <div class="map-tool-btn" @click="initChannelLayer">
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
            <el-slider v-model="diffPixels" show-input :min="10" :max="200" input-size="mini" ></el-slider>
            <div style="margin-left: 10px; line-height: 38px;">
              <el-button :loading="quicklyDrawThinLoading" @click="quicklyDrawThin" size="mini">快速抽稀</el-button>
              <el-button size="mini" @click="boxDrawThin" >局部抽稀</el-button>
              <el-button :loading="saveDrawThinLoading" type="primary" :disabled="!layerGroupSource" size="mini" @click="saveDrawThin()">保存</el-button>
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
              <el-button type="primary" size="small" title="编辑位置" icon="el-icon-edit" @click="edit(channel)">编辑</el-button>
<!--              <el-button type="primary" size="small" title="轨迹查询" icon="el-icon-map-location" @click="getTrace(channel)">轨迹</el-button>-->
            </div>
            <span class="infobox-close el-icon-close" @click="closeInfoBox"></span>
          </div>
        </transition>

      </div>

      <div ref="infoboxForEdit">
        <transition name="el-zoom-in-center">
          <div class="infobox-edit-content" v-if="dragChannel">
            <div style="width: 100%; height: 2rem; line-height: 1.5rem; font-size: 14px">{{dragChannel.gbName}}  ({{dragChannel.gbDeviceId}})</div>
            <span style="font-size: 14px">经度:</span> <el-input v-model="dragChannel.gbLongitude" placeholder="请输入经度" style="width: 7rem; margin-right: 10px"></el-input>
            <span style="font-size: 14px">纬度: </span> <el-input v-model="dragChannel.gbLatitude" placeholder="请输入纬度" style="width: 7rem; "></el-input>
            <el-button icon="el-icon-close" size="medium" type="text" @click="cancelEdit(dragChannel)" style="margin-left: 1rem; font-size: 18px; color: #2b2f3a"></el-button>
            <el-button icon="el-icon-check" size="medium" type="text" @click="submitEdit(dragChannel)" style="font-size: 18px; color: #0842e2"></el-button>
          </div>
        </transition>
      </div>
      <devicePlayer ref="devicePlayer" ></devicePlayer>
      <queryTrace ref="queryTrace" ></queryTrace>
    </div>
</template>

<script>
import DeviceTree from '../common/DeviceTree.vue'
import queryTrace from './queryTrace.vue'
import MapComponent from '../common/MapComponent.vue'
import devicePlayer from '../common/channelPlayer/index.vue'

let cameraListForSource = []
let cameraList = []
let cameraListForLevelMap = new Map()
let cameraLayerExtent = []
export default {
  name: 'Map',
  components: {
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
      channelLayer: null,
      labelStyle: {
        width: '56px'
      },
      isLoging: false,
      longitudeStr: 'longitude',
      latitudeStr: 'latitude',
      mapTileList: [],
      diffPixels: 60,
      zoomValue: 10,
      showDrawThin: false,
      quicklyDrawThinLoading: false,
      saveDrawThinLoading: false,
      layerStyle: 0,
      drawThinLayer: null,
      layerGroupSource: null,
      infoBoxTempLayer: null
    }
  },
  created() {

  },
  destroyed() {

  },
  methods: {
    treeChannelClickEvent: function (id) {
      this.closeInfoBox()
      this.$store.dispatch('commonChanel/queryOne', id)
        .then(data => {
          if (!data.gbLongitude || data.gbLongitude < 0) {
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
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.play(data)
                })
            }
          },
          {
            label: '编辑位置',
            icon: 'el-icon-edit',
            type: 1,
            onClick: (event, data, node) => {
              this.$store.dispatch('commonChanel/queryOne', data.id)
                .then(data => {
                  this.edit(data)
                })
            }
          }
          // ,
          // {
          //   label: '轨迹查询',
          //   icon: 'el-icon-map-location',
          //   type: 1,
          //   onClick: (event, data, node) => {
          //
          //   }
          // }
        ]
    },
    showChannelInfo: function(data) {
      this.channel = data
      if (this.infoBoxTempLayer) {
        this.$refs.mapComponent.removeLayer(this.infoBoxTempLayer)
        this.infoBoxTempLayer = null
      }
      if (this.layerStyle === 0 || this.layerStyle === 2) {
        // 此时增加临时图标
        let position = [data.gbLongitude, data.gbLatitude]
        let cameraData = {
          id: data.gbId,
          position: position,
          data: data,
          image: {
            anchor: [0.5, 1],
            src: this.getImageByChannel(data)
          }
        }
        this.infoBoxTempLayer = this.$refs.mapComponent.addPointLayer([cameraData])
      }
      let position = [data.gbLongitude, data.gbLatitude]
      this.infoBoxId = this.$refs.mapComponent.openInfoBox(position, this.$refs.infobox, [0, -50])
    },
    zoomChange: function(zoom) {},

    initChannelLayer: function () {
      this.mapTileList = this.$refs.mapComponent.mapTileList
      // 获取所有有位置的通道
      this.closeInfoBox()
      this.$store.dispatch('commonChanel/getAllForMap', {}).then(data => {
        cameraListForSource = data
        console.log(data.length)
        let minLng = data[0].gbLongitude
        let maxLng = data[0].gbLongitude
        let minLat = data[0].gbLatitude
        let maxLat = data[0].gbLatitude
        for (let i = 1; i < data.length; i++) {
          let item = data[i]
          if (item.gbLongitude < minLng) {
            minLng = item.gbLongitude
          }
          if (item.gbLongitude > maxLng) {
            maxLng = item.gbLongitude
          }
          if (item.gbLatitude < minLat) {
            minLat = item.gbLatitude
          }
          if (item.gbLatitude > maxLat) {
            maxLat = item.gbLatitude
          }
          if (item.gbLongitude && item.gbLatitude) {
            let position = [item.gbLongitude, item.gbLatitude]
            let cameraData = {
              id: item.gbId,
              position: position,
              data: item,
              image: {
                anchor: [0.5, 1],
                src: this.getImageByChannel(item)
              }
            }
            cameraList.push(cameraData)
            if (item.mapLevel) { 
              if (cameraListForLevelMap.has(item.mapLevel)) {
                let list = cameraListForLevelMap.get(item.mapLevel)
                list.push(cameraData)
              }else {
                cameraListForLevelMap.set(item.mapLevel, [cameraData])
              }
            }else {
              cameraListForLevelMap.set(0, [cameraData])
            }
          }
        }
        cameraLayerExtent = [minLng, minLat, maxLng, maxLat]
        this.updateChannelLayer()
      })
    },
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
    changeLayerStyle: function (index) {
      if (this.layerStyle === index) {
        return
      }
      this.layerStyle = index
      this.$refs.mapComponent.removeLayer(this.channelLayer)
      this.channelLayer = null
      this.updateChannelLayer()
    },
    updateChannelLayer: function() {
      if (this.layerStyle === 0) {
        return
      }
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

      switch (this.layerStyle) {
        case 1:
          // 直接展示
          if (this.channelLayer) {
            this.channelLayer = this.$refs.mapComponent.updatePointLayer(this.channelLayer, cameraList, true)
          }else {
            console.log(cameraList.length)
            this.channelLayer = this.$refs.mapComponent.addPointLayer(cameraList, clientEvent, null)
          }
          break
        case 2:
          // 碰撞检测
          if (this.channelLayer) {
            this.channelLayer = this.$refs.mapComponent.updatePointLayer(this.channelLayer, cameraList, true)
          }else {
            this.channelLayer = this.$refs.mapComponent.addPointLayer(cameraList, clientEvent, {
              declutter: true
            })
          }
          break
        case 3:
          // 抽稀图层
          if (this.channelLayer) {
            this.channelLayer = this.$refs.mapComponent.updatePointLayerGroup(this.channelLayer, cameraListForLevelMap, true)
          }else {
            this.channelLayer = this.$refs.mapComponent.addPointLayerGroup(cameraListForLevelMap, clientEvent)
          }
          break
        // case 4:
        //   // 聚合图层
        //
        //   break
      }
    },
    getImageByChannel: function (channel) {
      if (channel.gbStatus === 'ON') {
        return 'static/images/gis/camera1.png'
      } else {
        return 'static/images/gis/camera1-offline.png'
      }
    },
    closeInfoBox: function () {
      if (this.infoBoxId !== null) {
        this.$refs.mapComponent.closeInfoBox(this.infoBoxId)
      }
      this.channel = null
      this.dragChannel = null
      if (this.infoBoxTempLayer) {
        this.$refs.mapComponent.removeLayer(this.infoBoxTempLayer)
        this.infoBoxTempLayer = null
      }
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
      this.$refs.mapComponent.setFeaturePositionById(this.channelLayer, channel.gbId, {
        id: channel.gbId,
        position: position,
        data: channel,
        image: {
          anchor: [0.5, 1],
          src: 'static/images/gis/camera1-red.png'
        }
      })
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
      this.$refs.mapComponent.setFeaturePositionById(this.channelLayer, channel.gbId, {
        id: channel.gbId,
        position: [channel.gbLongitude, channel.gbLatitude],
        data: channel,
        image: {
          anchor: [0.5, 1],
          src: this.getImageByChannel(channel)
        }
      })
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
          this.$refs.mapComponent.setFeaturePositionById(this.channelLayer, channel.gbId, {
            id: channel.gbId,
            position: position,
            data: channel,
            image: {
              anchor: [0.5, 1],
              src: this.getImageByChannel(channel)
            }
          })
          // 刷星树菜单
          this.$refs.deviceTree.refresh('channel' + channel.gbId)

        })
    },
    getTrace: function (data) {
      this.clean()
      this.$refs.queryTrace.openDialog(data, (channelPositions) => {
        if (channelPositions.length === 0) {
          this.$message.info({
            showClose: true,
            message: '未查询到轨迹信息'
          })
        } else {
          let positions = []
          for (let i = 0; i < channelPositions.length; i++) {
            if (channelPositions[i][this.longitudeStr] * channelPositions[i][this.latitudeStr] > 0) {
              positions.push([channelPositions[i][this.longitudeStr], channelPositions[i][this.latitudeStr]])
            }

          }
          if (positions.length === 0) {
            this.$message.info({
              showClose: true,
              message: '未查询到轨迹信息'
            })
            return
          }
        }
      })
    },
    clean: function (){
      if (this.infoBoxId !== null) {
        this.$refs.mapComponent.closeInfoBox(this.infoBoxId)
      }
    },
    testArray: function (){
      this.$store.dispatch('commonChanel/test')
    },
    showDrawThinBox: function(show){
      this.showDrawThin = show
      setTimeout(() => {
        if (!show) {
          // 关闭抽稀预览
          if (this.drawThinLayer !== null) {
            this.$refs.mapComponent.removeLayer(this.drawThinLayer)
            this.drawThinLayer = null
          }
          // 清空预览数据
          this.layerGroupSource = null
          this.updateChannelLayer()
        }else {
          //
        }
      }, 1)
    },
    quicklyDrawThin: function (){
      if (this.channelLayer) {
        this.$refs.mapComponent.removeLayer(this.channelLayer)
      }
      if (this.drawThinLayer !== null) {
        this.$refs.mapComponent.removeLayer(this.drawThinLayer)
        this.drawThinLayer = null
      }
      // 获取待抽稀数据
      let cameraList = cameraListForSource.slice()

      this.quicklyDrawThinLoading = true
      this.drawThin(cameraList).then((layerGroupSource) => {
        this.layerGroupSource = layerGroupSource
        this.drawThinLayer = this.$refs.mapComponent.addPointLayerGroup(layerGroupSource, data => {
          this.closeInfoBox()
          this.$nextTick(() => {
            if (data[0].edit) {
              this.showEditInfo(data[0])
            }else {
              this.showChannelInfo(data[0])
            }
          })
        })
        this.quicklyDrawThinLoading = false
        this.$message.success({
          showClose: true,
          message: '抽稀完成，请预览无误后保存抽稀结果'
        })
      })
    },
    boxDrawThin: function (){
      // 绘制框
      this.$refs.mapComponent.startDrawBox((extent) => {

        // 清理默认的摄像头图层
        if (this.channelLayer) {
          this.$refs.mapComponent.removeLayer(this.channelLayer)
        }

        let zoomExtent = this.$refs.mapComponent.getZoomExtent()
        let cameraListInExtent = []
        let cameraListOutExtent = []
        if (this.layerGroupSource !== null) {
          // 从当前预览的数据里，获取待抽稀的数据
          let sourceCameraList = this.layerGroupSource.get(0)
          console.log(sourceCameraList)
          if (!sourceCameraList) {
            this.$message.warning({
              showClose: true,
              message: '数据已经全部抽稀'
            })
            return
          }
          for (let i = 0; i < sourceCameraList.length; i++) {
            let value = sourceCameraList[i]
            if (!value.data.gbLongitude || !value.data.gbLatitude) {
              continue
            }
            if (value.data.gbLongitude >= extent[0] && value.data.gbLongitude <= extent[2]
              && value.data.gbLatitude >= extent[1] && value.data.gbLatitude <= extent[3]) {
              cameraListInExtent.push(value.data)
            }else {
              cameraListOutExtent.push(value.data)
            }
          }
        }else {
          for (let i = 0; i < cameraListForSource.length; i++) {
            let value = cameraListForSource[i]
            if (!value.gbLongitude || !value.gbLatitude) {
              continue
            }
            if (value.gbLongitude >= extent[0] && value.gbLongitude <= extent[2]
              && value.gbLatitude >= extent[1] && value.gbLatitude <= extent[3]) {
              cameraListInExtent.push(value)
            }else {
              cameraListOutExtent.push(value)
            }
          }
        }
        // 如果已经在预览，清理预览图层
        if (this.drawThinLayer !== null) {
          this.$refs.mapComponent.removeLayer(this.drawThinLayer)
          this.drawThinLayer = null
        }
        this.drawThin(cameraListInExtent).then((layerGroupSource) => {
          if (this.layerGroupSource !== null) {
            let zoom = zoomExtent[0]
            // 按照层级合并每次的抽稀结果
            while (zoom < zoomExtent[1]) {
              Array.prototype.push.apply(layerGroupSource.get(zoom), this.layerGroupSource.get(zoom))
              zoom += 1
            }
          }
          if (cameraListOutExtent.length > 0) {
            let layerSourceForOutExtent = this.createZoomLayerSource(cameraListOutExtent, zoomExtent[0])
            layerGroupSource.set(0, layerSourceForOutExtent)
          }
          this.layerGroupSource = layerGroupSource
          this.drawThinLayer = this.$refs.mapComponent.addPointLayerGroup(layerGroupSource, data => {
            this.closeInfoBox()
            this.$nextTick(() => {
              if (data[0].edit) {
                this.showEditInfo(data[0])
              }else {
                this.showChannelInfo(data[0])
              }
            })
          })
        })
      })
    },
    drawThin: function (cameraListInExtent){
      return new Promise((resolve, reject) => {
        try {
          let layerGroupSource = new Map()
          // 获取全部层级
          let zoomExtent = this.$refs.mapComponent.getZoomExtent()
          let zoom = zoomExtent[0]
          let zoomCameraMap = new Map()
          let useCameraMap = new Map()

          while (zoom < zoomExtent[1]) {
            // 计算经纬度差值
            let diff = this.$refs.mapComponent.computeDiff(this.diffPixels, zoom)
            let cameraMapForZoom = new Map()
            let useCameraMapForZoom = new Map()
            let useCameraList = Array.from(useCameraMap.values())
            for (let i = 0; i < useCameraList.length; i++) {
              let value = useCameraList[i]
              let lngGrid = Math.trunc(value.gbLongitude / diff)
              let latGrid = Math.trunc(value.gbLatitude / diff)
              let gridKey = latGrid + ':' + lngGrid
              useCameraMapForZoom.set(gridKey, value)
            }

            for (let i = 0; i < cameraListInExtent.length; i++) {
              let value = cameraListInExtent[i]
              if (useCameraMap.has(value.gbId) || !value.gbLongitude || !value.gbLatitude) {
                continue
              }
              let lngGrid = Math.trunc(value.gbLongitude / diff)
              let latGrid = Math.trunc(value.gbLatitude / diff)
              let gridKey = latGrid + ':' + lngGrid
              if (useCameraMapForZoom.has(gridKey)) {
                continue
              }
              if (cameraMapForZoom.has(gridKey)) {
                let oldValue = cameraMapForZoom.get(gridKey)
                if (value.gbLongitude % diff < oldValue.gbLongitude % diff) {
                  cameraMapForZoom.set(gridKey, value)
                  useCameraMap.set(value.gbId, value)
                  useCameraMap.delete(oldValue.gbId)
                }
              }else {
                cameraMapForZoom.set(gridKey, value)
                useCameraMap.set(value.gbId, value)
              }
            }

            let cameraArray = Array.from(cameraMapForZoom.values())
            zoomCameraMap.set(zoom, cameraArray)
            let layerSource = this.createZoomLayerSource(cameraArray)
            layerGroupSource.set(zoom - 1, layerSource)
            zoom += 1
          }
          let cameraArray = []
          for (let i = 0; i < cameraListInExtent.length; i++) {
            let value = cameraListInExtent[i]
            if (useCameraMap.has(value.gbId) || !value.gbLongitude || !value.gbLatitude) {
              continue
            }
            cameraArray.push(value)
          }
          let layerSource = this.createZoomLayerSource(cameraArray)
          layerGroupSource.set(zoomExtent[1] - 1, layerSource)

          resolve(layerGroupSource)
        }catch (error) {
          reject(error)
        }
      })
    },

    createZoomLayerSource(cameraArray) {
      let dataArray = []
      for (let i = 0; i < cameraArray.length; i++) {
        let item = cameraArray[i]
        let position = [item.gbLongitude, item.gbLatitude]
        dataArray.push({
          id: item.gbId,
          position: position,
          data: item,
          image: {
            anchor: [0.5, 1],
            src: this.getImageByChannel(item)
          }
        })
      }
     return dataArray
    },
    saveDrawThin: function(){
      if (!this.layerGroupSource) {
        return
      }
      this.saveDrawThinLoading = true
      let param = []
      let keys = Array.from(this.layerGroupSource.keys())
      for (let i = 0; i < keys.length; i++) {
        let zoom = keys[i]
        let values = this.layerGroupSource.get(zoom)
        for (let j = 0; j < values.length; j++) {
          let value = values[j]
          if (zoom === 0) {
            param.push({
              gbId: value.id
            })
          }else {
            param.push({
              gbId: value.id,
              mapLevel: zoom
            })
          }
        }
      }
      this.$store.dispatch('commonChanel/saveLevel', param)
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
        })
        .finally(() => {
          this.saveDrawThinLoading = false
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
