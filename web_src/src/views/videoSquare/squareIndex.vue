<template>
  <div>
    <a-drawer
      title="视频列表"
      placement="right"
      :width="300"
      :closable="false"
      :visible="visible"
      :after-visible-change="afterVisibleChange"
      @close="onClose"
    >
      <a-tree
        show-icon
        @select="onSelect"
        :tree-data="treeData">
        <font-awesome-icon slot="cameraVideo" :icon="['fas', 'video']" style="margin-right: 0.25rem; color: #1890FF"/>
      </a-tree>
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1,
        }"
      >
        <a-button type="danger" @click="onClose">
          关闭
        </a-button>
      </div>
    </a-drawer>
    <a-card size="small" :bordered="false">
      <a-row type="flex" :gutter="[16, 0]" style="float: left">
        <a-col :flex="1">
          <div>视频矩阵：
            <a-select default-value="1" @change="changeMatrix" style="width: 15rem">
              <a-select-option value="1">1 x 1</a-select-option>
              <a-select-option value="2">2 x 2</a-select-option>
              <a-select-option value="3">3 x 3</a-select-option>
              <a-select-option value="4">4 x 4</a-select-option>
            </a-select>
          </div>
        </a-col>
        <a-col>
          <a-space>
            <a-button type="primary">保存配置</a-button>
            <a-button type="primary" @click="setFullscreen">电视墙模式</a-button>
            <a-button type="primary" @click="showDrawer">视频列表</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>
    <component :is="currentComponet" :fullScreen = isFullScreen @setvideoPlayer="setVideoPlayer" @setFullScreenEle="setFullScreenEle"></component>
  </div>
</template>

<script>
import squareMatrixOne from "@/views/videoSquare/squareMatrixOne";
import squareMatrixTwo from "@/views/videoSquare/squareMatrixTwo";
import squareMatrixThree from "@/views/videoSquare/squareMatrixThree";
import squareMatrixFour from "@/views/videoSquare/squareMatrixFour";
import {getVideoTree} from "@/api/videoSquare";
import {Tree} from 'ant-design-vue'
import {noticePushStream} from "@/api/deviceList";
import fscreen from 'fscreen'

export default {
  name: "squareIndex",
  components: {
    squareMatrixOne,
    squareMatrixTwo,
    squareMatrixThree,
    squareMatrixFour,
    'a-tree': Tree
  },
  data() {
    return {
      currentComponet: 'squareMatrixOne',
      visible: false,
      treeData: [],
      player: null,
      fullscreenEle: null,
      playerContainers: [],
      playerList: [],
      isFullScreen: false
    }
  },
  created() {
    getVideoTree().then(res => {
      this.treeData = res.data
    })
  },
  mounted() {
    fscreen.onfullscreenchange = (e) => {
      this.isFullScreen = !this.isFullScreen
      console.log(this.isFullScreen)
      this.playerContainers.forEach(container => {
        container.style.height = (9 / 16) * container.clientWidth + 'px'
      })
      Object.keys(this.playerList).forEach(key => {
        let tempPlayer = this.playerList[key]
        tempPlayer.resize()
      })
    }
  },
  methods: {
    afterVisibleChange(val) {
    },
    showDrawer() {
      this.visible = true;
    },
    onClose() {
      this.visible = false;
    },
    changeMatrix(val) { //动态组件切换
      if ('1' === val) {
        this.currentComponet = 'squareMatrixOne'
      } else if ('2' === val) {
        this.currentComponet = 'squareMatrixTwo'
      } else if ('3' === val) {
        this.currentComponet = 'squareMatrixThree'
      }else if ('4' === val) {
        this.currentComponet = 'squareMatrixFour'
      }
    },
    setVideoPlayer(player) {
      this.player = player
    },
    onSelect(selectedKeys, info) {
      if (selectedKeys.length > 0) {
        if (!this.player) {
          this.$message.warn('请选择一个预览窗口')
          return;
        }
        if (this.player && this.player.playing) {
          this.$message.warn('当前窗口被占用，请选择其他窗口或者停止当前窗口的播放')
          return
        }
        let key = selectedKeys[0];
        let deviceId = key.split('_')[0]
        let channelId = key.split('_')[1]
        noticePushStream({deviceId: deviceId, channelId: channelId}).then(res => {
          if (res.code === 0) {
            let streamInfo = res.data;
            this.playeVideo(streamInfo)
          }
        })
      }
    },
    playeVideo(streamInfo) {
      if (location.protocol === "https:") {
        if (streamInfo.wss_flv === null) {
          this.$message.error('媒体服务器未配置ssl端口');
        } else {
          this.player.play(streamInfo.wss_flv);
        }
      } else {
        this.player.play(streamInfo.ws_flv);
      }
    },
    setFullScreenEle(el, playerContainers, playerList) {

      this.fullscreenEle = el
      this.playerContainers = playerContainers
      this.playerList = playerList
    },
    setFullscreen() {
      if (!fscreen.requestFullscreen) {
        this.$message.error('浏览器不支持全屏API')
        return
      }
      if (!fscreen.fullscreenEnabled) {
        this.$message.error("浏览器未开启全屏模式功能")
        return;
      }
      fscreen.requestFullscreen(this.fullscreenEle);
    }
  }
}
</script>

<style scoped>

</style>