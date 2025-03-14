<template>
  <div ref="container" @dblclick="fullscreenSwich" style="width:100%;height:100%;background-color: #000000;margin:0 auto;">
    <div id="glplayer" style="width: 100%; height: 100%; display: flex"></div>
    <div class="buttons-box" id="buttonsBox">
      <div class="buttons-box-left">
        <i v-if="!playing" class="iconfont icon-play h265web-btn" @click="unPause"></i>
        <i v-if="playing" class="iconfont icon-pause h265web-btn" @click="pause"></i>
        <i class="iconfont icon-stop h265web-btn" @click="destroy"></i>
        <i v-if="isNotMute" class="iconfont icon-audio-high h265web-btn" @click="mute()"></i>
        <i v-if="!isNotMute" class="iconfont icon-audio-mute h265web-btn" @click="cancelMute()"></i>
      </div>
      <div class="buttons-box-right">
        <!--          <i class="iconfont icon-file-record1 h265web-btn"></i>-->
        <!--          <i class="iconfont icon-xiangqing2 h265web-btn" ></i>-->
        <i class="iconfont icon-camera1196054easyiconnet h265web-btn" @click="screenshot"
           style="font-size: 1rem !important"></i>
        <i class="iconfont icon-shuaxin11 h265web-btn" @click="playBtnClick"></i>
        <i v-if="!fullscreen" class="iconfont icon-weibiaoti10 h265web-btn" @click="fullscreenSwich"></i>
        <i v-if="fullscreen" class="iconfont icon-weibiaoti11 h265web-btn" @click="fullscreenSwich"></i>
      </div>
    </div>
  </div>
</template>

<script>
let h265webPlayer = {};
/**
 * 从github上复制的
 * @see https://github.com/numberwolf/h265web.js/blob/master/example_normal/index.js
 */
const token = "base64:QXV0aG9yOmNoYW5neWFubG9uZ3xudW1iZXJ3b2xmLEdpdGh1YjpodHRwczovL2dpdGh1Yi5jb20vbnVtYmVyd29sZixFbWFpbDpwb3JzY2hlZ3QyM0Bmb3htYWlsLmNvbSxRUTo1MzEzNjU4NzIsSG9tZVBhZ2U6aHR0cDovL3h2aWRlby52aWRlbyxEaXNjb3JkOm51bWJlcndvbGYjODY5NCx3ZWNoYXI6bnVtYmVyd29sZjExLEJlaWppbmcsV29ya0luOkJhaWR1";
export default {
  name: 'h265web',
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
    };
  },
  props: ['videoUrl', 'error', 'hasAudio', 'height'],
  mounted() {
    window.onerror = (msg) => {
      // console.error(msg)
    };
    console.log(this._uid)
    let paramUrl = decodeURIComponent(this.$route.params.url)
    this.$nextTick(() => {
      this.updatePlayerDomSize()
      window.onresize = () => {
        this.updatePlayerDomSize()
      }
      if (typeof (this.videoUrl) == "undefined") {
        this.videoUrl = paramUrl;
      }
      this.btnDom = document.getElementById("buttonsBox");
      console.log("初始化时的地址为: " + this.videoUrl)
      this.play(this.videoUrl)
    })
  },
  watch: {
    videoUrl(newData, oldData) {
      this.play(newData)
    },
    immediate: true
  },
  methods: {
    updatePlayerDomSize() {
      let dom = this.$refs.container;
      let width = dom.parentNode.clientWidth
      let height = (9 / 16) * width

      const clientHeight = Math.min(document.body.clientHeight, document.documentElement.clientHeight)
      if (height > clientHeight) {
        height = clientHeight
        width = (16 / 9) * height
      }

      dom.style.width = width + 'px';
      dom.style.height = height + "px";
    },
    create() {
      let options = {};
      console.log("hasAudio  " + this.hasAudio)
      h265webPlayer[this._uid] = new window.new265webjs(this.videoUrl, Object.assign(
        {
          player: "glplayer", // 播放器容器id
          width: 960,
          height: 540,
          token : token,
          extInfo : {
            coreProbePart : 0.4,
            probeSize : 8192,
            ignoreAudio : 0
          }
        },
        options
      ));
      let h265web = h265webPlayer[this._uid];
      h265web.onOpenFullScreen = () => {
        this.fullscreen = true
      }
      h265web.onCloseFullScreen = () => {
        this.fullscreen = false
      }
      h265web.onReadyShowDone = () => {
        // 准备好显示了，尝试自动播放
        const result = h265web.play()
        this.playing = result;
      }
      h265web.onLoadFinish = () => {
        this.loaded = true;
        // 可以获取mediaInfo
        // @see https://github.com/numberwolf/h265web.js/blob/8b26a31ffa419bd0a0f99fbd5111590e144e36a8/example_normal/index.js#L252C9-L263C11
        // mediaInfo = playerObj.mediaInfo();
      }
      h265web.onPlayTime = (...args) => {
        console.log(args)
      }
      h265web.do()
    },
    screenshot: function () {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].screenshot();
      }
    },
    playBtnClick: function (event) {
      this.play(this.videoUrl)
    },
    play: function (url) {
      console.log(url)
      if (h265webPlayer[this._uid]) {
        this.destroy();
      }
      this.create();
    },
    unPause: function () {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].play();
      }
      this.playing = h265webPlayer[this._uid].isPlaying();
      this.err = "";
    },
    pause: function () {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].pause();
      }
      this.playing = h265webPlayer[this._uid].isPlaying();
      this.err = "";
    },
    mute: function () {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].setVoice(0.0);
        this.isNotMute = false;
      }
    },
    cancelMute: function () {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].setVoice(1.0);
        this.isNotMute = true;
      }
    },
    destroy: function () {
      if (h265webPlayer[this._uid]) {
        h265webPlayer[this._uid].release();
      }
      if (document.getElementById("buttonsBox") == null) {
        this.$refs.container.appendChild(this.btnDom)
      }
      h265webPlayer[this._uid] = null;
      this.playing = false;
      this.err = "";

    },
    eventcallbacK: function (type, message) {
      // console.log("player 事件回调")
      // console.log(type)
      // console.log(message)
    },
    fullscreenSwich: function () {
      let isFull = this.isFullscreen()
      if (isFull) {
        h265webPlayer[this._uid].closeFullScreen()
      } else {
        h265webPlayer[this._uid].fullScreen()
      }
      this.fullscreen = !isFull;
    },
    isFullscreen: function () {
      return document.fullscreenElement ||
        document.msFullscreenElement ||
        document.mozFullScreenElement ||
        document.webkitFullscreenElement || false;
    }
  },
  destroyed() {
    if (h265webPlayer[this._uid]) {
      h265webPlayer[this._uid].destroy();
    }
    this.playing = false;
    this.loaded = false;
  },
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
</style>
