<template>
  <div
    ref="container"
    style="width:100%; height: 100%; background-color: #000000;margin:0 auto;position: relative;"
    @dblclick="fullscreenSwich"
  >
    <div style="width:100%; padding-top: 56.25%; position: relative;" />
    <div id="buttonsBox" class="buttons-box" >
      <div class="buttons-box-left">
        <i v-if="!playing" class="iconfont icon-play jessibuca-btn" @click="playBtnClick" />
        <i v-if="playing" class="iconfont icon-pause jessibuca-btn" @click="pause" />
        <i class="iconfont icon-stop jessibuca-btn" @click="stop" />
        <i v-if="isNotMute" class="iconfont icon-audio-high jessibuca-btn" @click="mute()" />
        <i v-if="!isNotMute" class="iconfont icon-audio-mute jessibuca-btn" @click="cancelMute()" />
      </div>
      <div class="buttons-box-right">
        <span class="jessibuca-btn">{{ kBps }} kb/s</span>
        <!--          <i class="iconfont icon-file-record1 jessibuca-btn"></i>-->
        <!--          <i class="iconfont icon-xiangqing2 jessibuca-btn" ></i>-->
        <i
          class="iconfont icon-camera1196054easyiconnet jessibuca-btn"
          style="font-size: 1rem !important"
          @click="screenshot"
        />
        <i class="iconfont icon-shuaxin11 jessibuca-btn" @click="playBtnClick" />
        <i v-if="!fullscreen" class="iconfont icon-weibiaoti10 jessibuca-btn" @click="fullscreenSwich" />
        <i v-if="fullscreen" class="iconfont icon-weibiaoti11 jessibuca-btn" @click="fullscreenSwich" />
      </div>
    </div>
  </div>
</template>

<script>
const jessibucaPlayer = {}
export default {
  name: 'Jessibuca',
  props: ['videoUrl', 'error', 'hasAudio', 'height', 'showButton'],
  data() {
    return {
      playing: false,
      isNotMute: true,
      quieting: false,
      fullscreen: false,
      loaded: false, // mute
      speed: 0,
      performance: '', // 工作情况
      kBps: 0,
      btnDom: null,
      videoInfo: null,
      volume: 1,
      playerTime: 0,
      rotate: 0,
      vod: true, // 点播
      forceNoOffscreen: false
    }
  },
  created() {
    const paramUrl = decodeURIComponent(this.$route.params.url)
    console.log(paramUrl)
    if (!this.videoUrl && paramUrl) {
      this.videoUrl = paramUrl
    }
    this.btnDom = document.getElementById('buttonsBox')
  },
  mounted() {},
  destroyed() {
    if (jessibucaPlayer[this._uid]) {
      jessibucaPlayer[this._uid].videoPTS = 0
      jessibucaPlayer[this._uid].destroy()
    }
    this.playing = false
    this.loaded = false
    this.performance = ''
    this.playerTime = 0
  },
  methods: {
    create() {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].destroy()
      }
      if (this.$refs.container.dataset['jessibuca']) {
        this.$refs.container.dataset['jessibuca'] = undefined
      }

      if (this.$refs.container.getAttribute('data-jessibuca')) {
        this.$refs.container.removeAttribute('data-jessibuca')
      }
      const options = {
        container: this.$refs.container,
        videoBuffer: 0,
        isResize: true,
        useMSE: true,
        useWCS: false,
        text: '',
        // background: '',
        controlAutoHide: false,
        debug: false,
        hotKey: true,
        decoder: '/static/js/jessibuca/decoder.js',
        sNotMute: true,
        timeout: 10,
        recordType: 'mp4',
        isFlv: false,
        forceNoOffscreen: true,
        hasAudio: typeof (this.hasAudio) === 'undefined' ? true : this.hasAudio,
        heartTimeout: 5,
        heartTimeoutReplay: true,
        heartTimeoutReplayTimes: 3,
        hiddenAutoPause: false,
        isFullResize: false,

        isNotMute: this.isNotMute,
        keepScreenOn: true,
        loadingText: '请稍等, 视频加载中......',
        loadingTimeout: 10,
        loadingTimeoutReplay: true,
        loadingTimeoutReplayTimes: 3,
        openWebglAlignment: false,
        operateBtns: {
          fullscreen: false,
          screenshot: false,
          play: false,
          audio: false,
          recorder: false
        },
        // rotate: 0,
        showBandwidth: false,
        supportDblclickFullscreen: false,

        useWebFullSreen: true,

        wasmDecodeErrorReplay: true,
        wcsUseVideoRendcer: true
      }
      console.log('Jessibuca -> options: ', options)
      jessibucaPlayer[this._uid] = new window.Jessibuca(options)

      const jessibuca = jessibucaPlayer[this._uid]
      jessibuca.on('pause', () => {
        this.playing = false
        this.$emit('playStatusChange', false)
      })
      jessibuca.on('play', () => {
        this.playing = true
        this.$emit('playStatusChange', true)
      })
      jessibuca.on('fullscreen', (msg) => {
        this.fullscreen = msg
      })
      jessibuca.on('mute', (msg) => {
        this.isNotMute = !msg
      })
      jessibuca.on('performance', (performance) => {
        let show = '卡顿'
        if (performance === 2) {
          show = '非常流畅'
        } else if (performance === 1) {
          show = '流畅'
        }
        this.performance = show
      })
      jessibuca.on('kBps', (kBps) => {
        this.kBps = Math.round(kBps)
      })
      jessibuca.on('videoInfo', (msg) => {
        console.log('Jessibuca -> videoInfo: ', msg)
      })
      jessibuca.on('audioInfo', (msg) => {
        console.log('Jessibuca -> audioInfo: ', msg)
      })
      jessibuca.on('error', (msg) => {
        console.log('Jessibuca -> error: ', msg)
      })
      jessibuca.on('timeout', (msg) => {
        console.log('Jessibuca -> timeout: ', msg)
      })
      jessibuca.on('loadingTimeout', (msg) => {
        console.log('Jessibuca -> timeout: ', msg)
      })
      jessibuca.on('delayTimeout', (msg) => {
        console.log('Jessibuca -> timeout: ', msg)
      })
      jessibuca.on('playToRenderTimes', (msg) => {
        console.log('Jessibuca -> playToRenderTimes: ', msg)
      })
      jessibuca.on('timeUpdate', (videoPTS) => {
        if (jessibuca.videoPTS) {
          this.playerTime += (videoPTS - jessibuca.videoPTS)
          this.$emit('playTimeChange', this.playerTime)
        }
        jessibuca.videoPTS = videoPTS
      })
      jessibuca.on('play', () => {
        this.playing = true
        this.loaded = true
        this.quieting = jessibuca.quieting
      })
    },
    playBtnClick: function() {
      this.play(this.videoUrl)
    },
    play: function(url) {
      this.videoUrl = url
      console.log('Jessibuca -> url: ', url)
      if (!jessibucaPlayer[this._uid]) {
        this.create()
      }
      jessibucaPlayer[this._uid].play(url)

      if (jessibucaPlayer[this._uid].hasLoaded()) {
        // jessibucaPlayer[this._uid].play(url)
      } else {
        jessibucaPlayer[this._uid].on('load', () => {
          // jessibucaPlayer[this._uid].play(url)
        })
      }

    },
    pause: function() {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].pause()
      }
      this.playing = false
      this.err = ''
      this.performance = ''
    },
    stop: function() {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].pause()
        jessibucaPlayer[this._uid].clearView()
      }
      this.playing = false
      this.err = ''
      this.performance = ''
    },
    screenshot: function() {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].screenshot()
      }
    },
    mute: function() {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].mute()
      }
    },
    cancelMute: function() {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].cancelMute()
      }
    },
    destroy: function() {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].destroy()
      }
      // if (document.getElementById('buttonsBox') === null && (typeof this.showButton === 'undefined' || this.showButton)) {
      //   this.$refs.container.appendChild(this.btnDom)
      // }
      jessibucaPlayer[this._uid] = null
      this.playing = false
      this.err = ''
      this.performance = ''
    },
    fullscreenSwich: function() {
      const isFull = this.isFullscreen()
      jessibucaPlayer[this._uid].setFullscreen(!isFull)
      this.fullscreen = !isFull
    },
    isFullscreen: function() {
      return document.fullscreenElement ||
        document.msFullscreenElement ||
        document.mozFullScreenElement ||
        document.webkitFullscreenElement || false
    },
    setPlaybackRate: function() {

    }
  }
}
</script>

<style>
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

.jessibuca-btn {
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
</style>
