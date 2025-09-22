<template>
  <div id="h265Player" ref="container" style="background-color: #000000; " @dblclick="fullscreenSwich">
    <div id="h265PlayerContainer" ref="playerBox" style="width: 100%; height: 100%; margin: 0 auto;" >
      <div v-if="playerLoading" class="play-loading">
        <i class="el-icon-loading" />
        视频加载中
      </div>
    </div>

    <div v-if="showButton" id="buttonsBox" class="buttons-box">
      <div class="buttons-box-left">
        <i v-if="!playing" class="iconfont icon-play h265web-btn" @click="unPause" />
        <i v-if="playing" class="iconfont icon-pause h265web-btn" @click="pause" />
        <i class="iconfont icon-stop h265web-btn" @click="destroy" />
        <i v-if="isNotMute" class="iconfont icon-audio-high h265web-btn" @click="mute()" />
        <i v-if="!isNotMute" class="iconfont icon-audio-mute h265web-btn" @click="cancelMute()" />
      </div>
      <div class="buttons-box-right">
        <!--          <i class="iconfont icon-file-record1 h265web-btn"></i>-->
        <!--          <i class="iconfont icon-xiangqing2 h265web-btn" ></i>-->
        <i
          class="iconfont icon-camera1196054easyiconnet h265web-btn"
          style="font-size: 1rem !important"
          @click="screenshot"
        />
        <i class="iconfont icon-shuaxin11 h265web-btn" @click="playBtnClick" />
        <i v-if="!fullscreen" class="iconfont icon-weibiaoti10 h265web-btn" @click="fullscreenSwich" />
        <i v-if="fullscreen" class="iconfont icon-weibiaoti11 h265web-btn" @click="fullscreenSwich" />
      </div>
    </div>
  </div>
</template>

<script>
const h265webPlayer = {}
/**
 * 从github上复制的
 * @see https://github.com/numberwolf/h265web.js/blob/master/example_normal/index.js
 */

// 需要加载的h265web的wasm的js文件(完整http地址)
const wasm_js_uri = window.location.origin + '/static/js/h265web2/h265web_wasm.js'
// 需要加载的h265web的wasm文件(完整http地址)
const wasm_wasm_uri = window.location.origin + '/static/js/h265web2/h265web_wasm.wasm'
// 需要加载的扩展wasm的js文件(完整http地址)
const ext_src_js_uri = window.location.origin + '/static/js/h265web2/extjs.js'
// 需要加载的扩展wasm的js文件(完整http地址)
const ext_wasm_js_uri = window.location.origin + '/static/js/h265web2/extwasm.js'

window.wasm_js_uri = wasm_js_uri
window.wasm_wasm_uri = wasm_wasm_uri
window.ext_src_js_uri = ext_src_js_uri
window.ext_wasm_js_uri = ext_wasm_js_uri

export default {
  name: 'H265web',
  props: ['videoUrl', 'error', 'hasAudio', 'height', 'showButton'],
  data() {
    return {
      playing: false,
      isNotMute: false,
      quieting: false,
      fullscreen: false,
      loaded: false, // mute
      speed: 0,
      kBps: 0,
      btnDom: null,
      videoInfo: null,
      volume: 1,
      rotate: 0,
      vod: true, // 点播
      forceNoOffscreen: false,
      playerWidth: 0,
      playerHeight: 0,
      inited: false,
      playerLoading: false,
      mediaInfo: null
    }
  },
  watch: {
    videoUrl(newData, oldData) {
      this.play(newData)
    },
    playing(newData, oldData) {
      this.$emit('playStatusChange', newData)
    },
    immediate: true
  },
  mounted() {
    const paramUrl = decodeURIComponent(this.$route.params.url)
    window.onresize = () => {
      this.updatePlayerDomSize()
    }
    this.btnDom = document.getElementById('buttonsBox')
    if (paramUrl) {
      console.log('初始化时的地址为: ' + paramUrl)

    }
    console.log('初始化时的地址为: ' + wasm_js_uri)
    if (paramUrl) {
      this.play(this.videoUrl)
    }
  },
  destroyed() {
    if (h265webPlayer[this._uid]) {
      h265webPlayer[this._uid].release()
    }
    this.playing = false
    this.loaded = false
    this.playerLoading = false
  },
  methods: {
    updatePlayerDomSize() {
      const dom = this.$refs.container
      if (!this.parentNodeResizeObserver) {
        this.parentNodeResizeObserver = new ResizeObserver(entries => {
          this.updatePlayerDomSize()
        })
        this.parentNodeResizeObserver.observe(dom.parentNode)
      }
      const boxWidth = dom.parentNode.clientWidth
      const boxHeight = dom.parentNode.clientHeight
      let width = boxWidth
      let height = (9 / 16) * width
      if (boxHeight > 0 && boxWidth > boxHeight / 9 * 16) {
        height = boxHeight
        width = boxHeight / 9 * 16
      }

      const clientHeight = Math.min(document.body.clientHeight, document.documentElement.clientHeight)
      if (height > clientHeight) {
        height = clientHeight
        width = (16 / 9) * height
      }

      this.$refs.playerBox.style.width = width + 'px'
      this.$refs.playerBox.style.height = height + 'px'
      this.playerWidth = width
      this.playerHeight = height
      if (this.playing) {
        h265webPlayer[this._uid].resize(this.playerWidth, this.playerHeight)
      }
    },
    resize(width, height) {
      this.playerWidth = width
      this.playerHeight = height
      this.$refs.playerBox.style.width = width + 'px'
      this.$refs.playerBox.style.height = height + 'px'
      if (this.playing) {
        h265webPlayer[this._uid].resize(this.playerWidth, this.playerHeight)
      }
    },
    create(url) {
      this.playerLoading = true
      h265webPlayer[this._uid] = H265webjsPlayer()
      const player_config ={
        player_id: 'h265PlayerContainer', // 播放器容器id
        wasm_js_uri: wasm_js_uri, // h265web的wasm的js文件的地址(完整http地址)
        wasm_wasm_uri: wasm_wasm_uri, // h265web的wasm文件的地址(完整http地址)
        ext_src_js_uri: ext_src_js_uri, //  需要加载的扩展wasm的js文件(完整http地址), 有hls的播放则必须填写
        ext_wasm_js_uri: ext_wasm_js_uri, //  需要加载的扩展wasm文件(完整http地址), 有hls的播放则必须填写
        width: this.playerWidth,
        height: this.playerHeight,
        color: 'black', // 背景颜色
        auto_play: true, // 是否自动播放, 默认否
        readframe_multi_times: -1,
        enable_play_button: true,
        // core: 'mse_hevc',
        // core: 'wasm_hevc',
        // core: 'webcodec_hevc', // 优先使用的内核，不填写则自动识别
        ignore_audio: false // 是否要忽略掉音频，默认否
      }
      h265webPlayer[this._uid].build(player_config)

      const h265web = h265webPlayer[this._uid]

      h265web.on_ready_show_done_callback = () => {
        // 准备好显示了，尝试自动播放
        this.playing = true
        this.playerLoading = false
      }
      h265web.video_probe_callback = () => {
        this.loaded = true
        // 可以获取mediaInfo
        // @see https://github.com/numberwolf/h265web.js/blob/8b26a31ffa419bd0a0f99fbd5111590e144e36a8/example_normal/index.js#L252C9-L263C11
        // this.mediaInfo = h265web.mediaInfo()
      }
      h265web.on_play_time = (videoPTS) => {
        if (h265web.videoPTS) {
          this.$emit('playTimeChange', videoPTS - h265web.videoPTS)
        }
        h265web.videoPTS = videoPTS
      }
      // h265web.do()

      h265web.load_media(url)
    },
    screenshot: function() {
      if (h265webPlayer[this._uid]) {
        const canvas = document.createElement('canvas')
        console.log(this.mediaInfo)
        canvas.width = this.mediaInfo.meta.size.width
        canvas.height = this.mediaInfo.meta.size.height
        h265webPlayer[this._uid].screenshot(canvas) // snapshot to canvas

        // 下载截图
        const link = document.createElement('a')
        link.download = 'screenshot.png'
        link.href = canvas.toDataURL('image/png').replace('image/png', 'image/octet-stream')
        link.click()
      }
    },
    playBtnClick: function(event) {
      this.play(this.videoUrl)
    },
    play: function(url) {
      if (h265webPlayer[this._uid]) {
        this.destroy()
      }
      if (!url) {
        return
      }
      if (this.playerWidth === 0 || this.playerHeight === 0) {
        this.updatePlayerDomSize()
        setTimeout(() => {
          this.play(url)
        }, 300)
        return
      }
      this.create(url)
    },
    unPause: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].play()
        this.playing = h265webPlayer[this._uid].isPlaying()
      }
      this.err = ''
    },
    pause: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].pause()
        this.playing = h265webPlayer[this._uid].isPlaying()
      }
      this.err = ''
    },
    mute: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].setVoice(0.0)
        this.isNotMute = false
      }
    },
    cancelMute: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].setVoice(1.0)
        this.isNotMute = true
      }
    },
    destroy: function() {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].release()
      }
      h265webPlayer[this._uid] = null
      this.playing = false
      this.err = ''
    },
    fullscreenSwich: function() {
      const isFull = this.isFullscreen()
      if (isFull) {
        h265webPlayer[this._uid].closeFullScreen()
      } else {
        h265webPlayer[this._uid].fullScreen()
      }
      this.fullscreen = !isFull
    },
    isFullscreen: function() {
      return document.fullscreenElement ||
        document.msFullscreenElement ||
        document.mozFullScreenElement ||
        document.webkitFullscreenElement || false
    },
    setPlaybackRate: function(speed) {
      h265webPlayer[this._uid].set_playback_rate(speed)
    }
  }
}
</script>

<style>
.play-loading {
  width: 100%;
  height: 100%;
  color: rgb(255, 255, 255);
  display: flex;
  align-items: center;
  margin: 0 auto;
  justify-content: center;
  font-size: 18px;
}
.buttons-box {
  width: 100%;
  height: 28px;
  background-color: rgba(43, 51, 63, 0.7);
  position: absolute;
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  left: 0;
  bottom: 0;
  user-select: none;
  z-index: 10;
}

.h265web-btn {
  width: 20px;
  color: rgb(255, 255, 255);
  line-height: 27px;
  margin: 0px 10px;
  padding: 0px 2px;
  cursor: pointer;
  text-align: center;
  font-size: 0.8rem !important;
}

.buttons-box-right {
  position: absolute;
  right: 0;
}
.player-loading {
  width: fit-content;
  height: 30px;
  position: absolute;
  left: calc(50% - 52px);
  top: calc(50% - 52px);
  color: #fff;
  font-size: 16px;
}
.player-loading i{
  font-size: 24px;
  line-height: 24px;
  text-align: center;
  display: block;
}
.player-loading span{
  display: inline-block;
  font-size: 16px;
  height: 24px;
  line-height: 24px;
}
</style>
