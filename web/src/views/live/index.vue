<template>
  <div id="live" style="height: calc(100vh - 124px)">
    <div v-loading="loading" style="height: 100%; display: grid; grid-template-columns: 400px auto" element-loading-text="拼命加载中">
      <div style="background-color: #ffffff">
        <DeviceTree :click-event="clickEvent" :context-menu-event="contextMenuEvent" />
      </div>
      <div style="display: grid; grid-template-rows: 5vh auto">
        <div style="font-size: 17px;line-height:5vh; display: grid; grid-template-columns: 1fr 1fr">
          <div style="text-align: left">
            分屏:
            <i class="iconfont icon-a-mti-1fenpingshi btn" :class="{active:spiltIndex === 0}" @click="spiltIndex=0" />
            <i class="iconfont icon-a-mti-4fenpingshi btn" :class="{active: spiltIndex === 1}" @click="spiltIndex=1" />
            <i class="iconfont icon-a-mti-6fenpingshi btn" :class="{active: spiltIndex === 2}" @click="spiltIndex=2" />
            <i class="iconfont icon-a-mti-9fenpingshi btn" :class="{active: spiltIndex === 3}" @click="spiltIndex=3" />
          </div>
          <div style="text-align: right; margin-right: 10px;">
            <i class="el-icon-full-screen btn" @click="fullScreen()" />
          </div>
        </div>
        <div style="padding: 0; margin: 0 auto;">
          <div
            ref="playBox"
            :style="liveStyle"
          >
            <div
              v-for="i in layout[spiltIndex].spilt"
              :key="i"
              class="play-box"
              :class="getPlayerClass(spiltIndex, i)"
              @click="playerIdx = (i-1)"
            >
              <div v-if="!videoUrl[i-1]" style="color: #ffffff;font-size: 15px;font-weight: bold;">{{ videoTip[i-1]?videoTip[i-1]:"无信号" }}</div>
              <player
                v-else
                :ref="'player'[i-1]"
                :video-url="videoUrl[i-1]"
                fluent
                autoplay
                @screenshot="shot"
                @destroy="destroy"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>

import player from '../common/jessibuca.vue'
import DeviceTree from '../common/DeviceTree.vue'
import screenFull from 'screenfull'

export default {
  name: 'Live',
  components: {
    player, DeviceTree
  },

  data() {
    return {
      videoUrl: [''],
      videoTip: [''],
      spiltIndex: 2, // 分屏
      playerIdx: 0, // 激活播放器

      updateLooper: 0, // 数据刷新轮训标志
      count: 15,
      total: 0,

      // channel
      loading: false,
      layout: [
        {
          spilt: 1,
          columns: '1fr',
          rows: '1fr',
          style: function() {}
        },
        {
          spilt: 4,
          columns: '1fr 1fr',
          rows: '1fr 1fr',
          style: function() {}
        },
        {
          spilt: 6,
          columns: '1fr 1fr 1fr',
          rows: '1fr 1fr 1fr',
          style: function(index) {
            console.log(index)
            if (index === 0) {
              return {
                gridColumn: ' 1 / span 2',
                gridRow: ' 1 / span 2'
              }
            }
          }

        },
        {
          spilt: 9,
          columns: '1fr 1fr 1fr',
          rows: '1fr 1fr 1fr',
          style: function() {}
        }
      ]
    }
  },

  computed: {
    liveStyle() {
      if (!this.$store.getters.sidebar.opened) {
        return { width: '151vh', height: '85vh', display: 'grid', gridTemplateColumns: this.layout[this.spiltIndex].columns,
          gridTemplateRows: this.layout[this.spiltIndex].rows, gap: '4px', backgroundColor: '#a9a8a8' }
      } else {
        return { width: '140vh', height: '79vh', display: 'grid', gridTemplateColumns: this.layout[this.spiltIndex].columns,
          gridTemplateRows: this.layout[this.spiltIndex].rows, gap: '4px', backgroundColor: '#a9a8a8' }
      }

      // this.$nextTick(() => {
      //   for (let i = 0; i < this.spilt; i++) {
      //     const player = this.$refs.player
      //     player && player[i] && player[i].updatePlayerDomSize()
      //   }
      // })
      // return style
    }
  },
  watch: {
    spilt(newValue) {
      console.log('切换画幅;' + newValue)
      const that = this
      for (let i = 1; i <= newValue; i++) {
        if (!that.$refs['player' + i]) {
          continue
        }
        this.$nextTick(() => {
          if (that.$refs['player' + i] instanceof Array) {
            that.$refs['player' + i][0].resize()
          } else {
            that.$refs['player' + i].resize()
          }
        })
      }
      window.localStorage.setItem('split', newValue)
    },
    '$route.fullPath': 'checkPlayByParam'
  },
  mounted() {

  },
  created() {
    this.checkPlayByParam()
  },
  destroyed() {
    clearTimeout(this.updateLooper)
  },
  methods: {
    destroy(idx) {
      console.log(idx)
      this.clear(idx.substring(idx.length - 1))
    },
    clickEvent: function(channelId) {
      this.sendDevicePush(channelId)
    },
    getPlayerClass: function(splitIndex, i) {
      let classStr = 'play-box-' + splitIndex + '-' + i
      if (this.playerIdx === (i - 1)) {
        classStr += ' redborder'
      }
      return classStr
    },
    contextMenuEvent: function(device, event, data, isCatalog) {

    },
    // 通知设备上传媒体流
    sendDevicePush: function(channelId) {
      this.save(channelId)
      const idxTmp = this.playerIdx
      this.setPlayUrl('', idxTmp)
      this.$set(this.videoTip, idxTmp, '正在拉流...')
      this.$store.dispatch('commonChanel/playChannel', channelId)
        .then(data => {
          let videoUrl
          if (location.protocol === 'https:') {
            videoUrl = data.wss_flv
          } else {
            videoUrl = data.ws_flv
          }
          this.setPlayUrl(videoUrl, idxTmp)
        })
        .catch(err => {
          this.$set(this.videoTip, idxTmp, '播放失败: ' + err)
        })
        .finally(() => {
          this.loading = false
        })
    },
    setPlayUrl(url, idx) {
      this.$set(this.videoUrl, idx, url)
      const _this = this
      setTimeout(() => {
        window.localStorage.setItem('videoUrl', JSON.stringify(_this.videoUrl))
      }, 100)
    },
    checkPlayByParam() {
      const query = this.$route.query
      if (query.channelId) {
        this.sendDevicePush(query.channelId)
      }
    },
    shot(e) {
      // console.log(e)
      // send({code:'image',data:e})
      var base64ToBlob = function(code) {
        const parts = code.split(';base64,')
        const contentType = parts[0].split(':')[1]
        const raw = window.atob(parts[1])
        const rawLength = raw.length
        const uInt8Array = new Uint8Array(rawLength)
        for (let i = 0; i < rawLength; ++i) {
          uInt8Array[i] = raw.charCodeAt(i)
        }
        return new Blob([uInt8Array], {
          type: contentType
        })
      }
      const aLink = document.createElement('a')
      const blob = base64ToBlob(e) // new Blob([content]);
      const evt = document.createEvent('HTMLEvents')
      evt.initEvent('click', true, true) // initEvent 不加后两个参数在FF下会报错  事件类型，是否冒泡，是否阻止浏览器的默认行为
      aLink.download = '截图'
      aLink.href = URL.createObjectURL(blob)
      aLink.click()
    },
    save(item) {
      const dataStr = window.localStorage.getItem('playData') || '[]'
      const data = JSON.parse(dataStr)
      data[this.playerIdx] = item
      window.localStorage.setItem('playData', JSON.stringify(data))
    },
    clear(idx) {
      const dataStr = window.localStorage.getItem('playData') || '[]'
      const data = JSON.parse(dataStr)
      data[idx - 1] = null
      console.log(data)
      window.localStorage.setItem('playData', JSON.stringify(data))
    },
    fullScreen: function() {
      if (screenFull.isEnabled) {
        screenFull.toggle(this.$refs.playBox)
      }
    }
  }
}
</script>
<style>
.btn {
  margin: 0 10px;

}

.btn:hover {
  color: #409EFF;
}

.btn.active {
  color: #409EFF;

}

.redborder {
  border: 4px solid rgb(0, 198, 255) !important;
}

.play-box {
  background-color: #000000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.play-box-2-1 {
  grid-column: 1 / span 2;
  grid-row: 1 / span 2;
}
</style>
<style>
.videoList {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
}

.video-item {
  position: relative;
  width: 15rem;
  height: 10rem;
  margin-right: 1rem;
  background-color: #000000;
}

.video-item-img {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 100%;
  height: 100%;
}

.video-item-img:after {
  content: "";
  display: inline-block;
  position: absolute;
  z-index: 2;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 3rem;
  height: 3rem;
  background-image: url("../../assets/loading.png");
  background-size: cover;
  background-color: #000000;
}

.video-item-title {
  position: absolute;
  bottom: 0;
  color: #000000;
  background-color: #ffffff;
  line-height: 1.5rem;
  padding: 0.3rem;
  width: 14.4rem;
}

.baidumap {
  width: 100%;
  height: 100%;
  border: none;
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  margin: auto;
}

/* 去除百度地图版权那行字 和 百度logo */
.baidumap > .BMap_cpyCtrl {
  display: none !important;
}

.baidumap > .anchorBL {
  display: none !important;
}
</style>
