<template>
    <div id="devicePosition" style="height: calc(100vh - 84px);width: 100%;">
      <div style="height: 100%; display: grid; grid-template-columns: 360px auto">
        <DeviceTree ref="deviceTree" @clickEvent="treeChannelClickEvent" :showPosition="true" :contextmenu="getContextmenu()"/>
        <MapComponent ref="mapComponent" @loaded="initChannelLayer" @coordinateSystemChange="initChannelLayer"></MapComponent>
      </div>
      <div class="map-tool-box-bottom-right">
        <div class="map-tool-btn-group">
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
          <div class="map-tool-btn">
            <i class="iconfont icon-plus1"></i>
          </div>
          <div class="map-tool-btn">
            <i class="iconfont icon-minus1"></i>
          </div>
        </div>
      </div>
      <div class="map-tool-box-top-left">
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" title="图层抽稀">
            <i class="iconfont icon-mti-sandian"></i> <span>图层抽稀</span>
          </div>
          <div class="map-tool-btn" title="位置编辑" @click="testArray">
            <i class="el-icon-edit"></i> <span>位置编辑</span>
          </div>
        </div>
      </div>
      <div class="map-tool-box-top-right">
        <div class="map-tool-btn-group">
          <div class="map-tool-btn" title="抽稀">
            <i class="iconfont icon-mti-sandian"></i>
          </div>
          <div class="map-tool-btn" title="聚合">
            <i class="iconfont icon-mti-jutai"></i>
          </div>
          <div class="map-tool-btn" title="默认">
            <i class="iconfont icon-mti-jutai"></i>
          </div>
        </div>
      </div>
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
import gcoord from 'gcoord'

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
      mapTileList: []
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
          if (this.$refs.mapComponent.coordinateInView([data.gbLongitude, data.gbLatitude])) {
            this.showChannelInfo(data)
          }else {
            this.$refs.mapComponent.panTo([data.gbLongitude, data.gbLatitude], 16, () => {
              this.showChannelInfo(data)
            })
          }
        })
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
      let position = [data.gbLongitude, data.gbLatitude]
      this.infoBoxId = this.$refs.mapComponent.openInfoBox(position, this.$refs.infobox, [0, -50])
    },

    initChannelLayer: function () {
      this.mapTileList = this.$refs.mapComponent.mapTileList
      // 获取所有有位置的通道
      this.closeInfoBox()
      this.$store.dispatch('commonChanel/getAllForMap', {}).then(data => {
        let array = []
        for (let i = 0; i < data.length; i++) {
          let item = data[i]
          if (item.gbLongitude && item.gbLatitude) {
            let position = [item.gbLongitude, item.gbLatitude]
            array.push({
              id: item.gbId,
              position: position,
              data: item,
              image: {
                anchor: [0.5, 1],
                src: this.getImageByChannel(item)
              }
            })
          }
        }
        this.updateChannelLayer(array)
      })
    },
    changeMapTile: function (index) {
      this.$refs.mapComponent.changeMapTile(index)
    },
    updateChannelLayer: function(array) {
      if (this.channelLayer) {
        this.channelLayer = this.$refs.mapComponent.updateLayer(this.channelLayer, array, true)
      }else {
        this.channelLayer = this.$refs.mapComponent.addLayer(array, data => {
          this.closeInfoBox()
          this.$nextTick(() => {
            if (data[0].edit) {
              this.showEditInfo(data[0])
            }else {
              this.showChannelInfo(data[0])
            }

          })
        })
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
