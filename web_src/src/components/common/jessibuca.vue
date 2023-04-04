<template>
  <div ref="container" @dblclick="fullscreenSwich"
       style="width:100%;height:100%;background-color: #000000;margin:0 auto;">
    <div class="buttons-box" id="buttonsBox">
      <div class="buttons-box-left">
        <i v-if="!playing" class="iconfont icon-play jessibuca-btn" @click="playBtnClick"></i>
        <i v-if="playing" class="iconfont icon-pause jessibuca-btn" @click="pause"></i>
        <i class="iconfont icon-stop jessibuca-btn" @click="destroy"></i>
        <i v-if="isNotMute" class="iconfont icon-audio-high jessibuca-btn" @click="mute()"></i>
        <i v-if="!isNotMute" class="iconfont icon-audio-mute jessibuca-btn" @click="cancelMute()"></i>
      </div>
      <div class="buttons-box-right">
        <span class="jessibuca-btn">{{ kBps }} kb/s</span>
        <!--          <i class="iconfont icon-file-record1 jessibuca-btn"></i>-->
        <!--          <i class="iconfont icon-xiangqing2 jessibuca-btn" ></i>-->
        <i class="iconfont icon-camera1196054easyiconnet jessibuca-btn" @click="screenshot"
           style="font-size: 1rem !important"></i>
        <i class="iconfont icon-shuaxin11 jessibuca-btn" @click="playBtnClick"></i>
        <i v-if="!fullscreen" class="iconfont icon-weibiaoti10 jessibuca-btn" @click="fullscreenSwich"></i>
        <i v-if="fullscreen" class="iconfont icon-weibiaoti11 jessibuca-btn" @click="fullscreenSwich"></i>
      </div>
    </div>
  </div>
</template>

<script>
let jessibucaPlayer = {};
export default {
  name: 'jessibuca',
  data() {
    return {
      playing: false,
      isNotMute: false,
      quieting: false,
      fullscreen: false,
      loaded: false, // mute
      speed: 0,
      performance: "", // 工作情况
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

      jessibucaPlayer[this._uid] = new window.Jessibuca(Object.assign(
        {
          container: this.$refs.container,
          autoWasm: true,
          background: "",
          controlAutoHide: false,
          debug: false,
          decoder: "static/js/jessibuca/decoder.js",
          forceNoOffscreen: true,
          hasAudio: typeof (this.hasAudio) == "undefined" ? true : this.hasAudio,
          hasVideo: true,
          heartTimeout: 5,
          heartTimeoutReplay: true,
          heartTimeoutReplayTimes: 3,
          hiddenAutoPause: false,
          hotKey: false,
          isFlv: false,
          isFullResize: false,
          isNotMute: this.isNotMute,
          isResize: false,
          keepScreenOn: false,
          loadingText: "请稍等, 视频加载中......",
          loadingTimeout: 10,
          loadingTimeoutReplay: true,
          loadingTimeoutReplayTimes: 3,
          openWebglAlignment: false,
          operateBtns: {
            fullscreen: false,
            screenshot: false,
            play: false,
            audio: false,
            record: false
          },
          recordType: "webm",
          rotate: 0,
          showBandwidth: false,
          supportDblclickFullscreen: false,
          timeout: 10,
          useMSE: location.hostname !== "localhost" && location.protocol !== "https:",
          useOffscreen: false,
          useWCS: location.hostname === "localhost" || location.protocol === "https",
          useWebFullScreen: false,
          videoBuffer: 0,
          wasmDecodeAudioSyncVideo: true,
          wasmDecodeErrorReplay: true,
          wcsUseVideoRender: true
        },
        options
      ));
      let jessibuca = jessibucaPlayer[this._uid];
      let _this = this;
      jessibuca.on("load", function () {
        console.log("on load init");
      });

      jessibuca.on("log", function (msg) {
        console.log("on log", msg);
      });
      jessibuca.on("record", function (msg) {
        console.log("on record:", msg);
      });
      jessibuca.on("pause", function () {
        _this.playing = false;
      });
      jessibuca.on("play", function () {
        _this.playing = true;
      });
      jessibuca.on("fullscreen", function (msg) {
        console.log("on fullscreen", msg);
        _this.fullscreen = msg
      });

      jessibuca.on("mute", function (msg) {
        console.log("on mute", msg);
        _this.isNotMute = !msg;
      });
      jessibuca.on("audioInfo", function (msg) {
        console.log("audioInfo", msg);
      });

      jessibuca.on("bps", function (bps) {
        // console.log('bps', bps);

      });
      let _ts = 0;
      jessibuca.on("timeUpdate", function (ts) {
        // console.log('timeUpdate,old,new,timestamp', _ts, ts, ts - _ts);
        _ts = ts;
      });

      jessibuca.on("videoInfo", function (info) {
        console.log("videoInfo", info);
      });

      jessibuca.on("error", function (error) {
        console.log("error", error);
      });

      jessibuca.on("timeout", function () {
        console.log("timeout");
      });

      jessibuca.on('start', function () {
        console.log('start');
      })

      jessibuca.on("performance", function (performance) {
        let show = "卡顿";
        if (performance === 2) {
          show = "非常流畅";
        } else if (performance === 1) {
          show = "流畅";
        }
        _this.performance = show;
      });
      jessibuca.on('buffer', function (buffer) {
        // console.log('buffer', buffer);
      })

      jessibuca.on('stats', function (stats) {
        // console.log('stats', stats);
      })

      jessibuca.on('kBps', function (kBps) {
        _this.kBps = Math.round(kBps);
      });

      // 显示时间戳 PTS
      jessibuca.on('videoFrame', function () {

      })

      //
      jessibuca.on('metadata', function () {

      });
    },
    playBtnClick: function (event) {
      this.play(this.videoUrl)
    },
    play: function (url) {
      console.log(url)
      if (jessibucaPlayer[this._uid]) {
        this.destroy();
      }
      this.create();
      jessibucaPlayer[this._uid].on("play", () => {
        this.playing = true;
        this.loaded = true;
        this.quieting = jessibuca.quieting;
      });
      if (jessibucaPlayer[this._uid].hasLoaded()) {
        jessibucaPlayer[this._uid].play(url);
      } else {
        jessibucaPlayer[this._uid].on("load", () => {
          console.log("load 播放")
          jessibucaPlayer[this._uid].play(url);
        });
      }
    },
    pause: function () {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].pause();
      }
      this.playing = false;
      this.err = "";
      this.performance = "";
    },
    screenshot: function () {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].screenshot();
      }
    },
    mute: function () {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].mute();
      }
    },
    cancelMute: function () {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].cancelMute();
      }
    },
    destroy: function () {
      if (jessibucaPlayer[this._uid]) {
        jessibucaPlayer[this._uid].destroy();
      }
      if (document.getElementById("buttonsBox") == null) {
        this.$refs.container.appendChild(this.btnDom)
      }
      jessibucaPlayer[this._uid] = null;
      this.playing = false;
      this.err = "";
      this.performance = "";

    },
    eventcallbacK: function (type, message) {
      // console.log("player 事件回调")
      // console.log(type)
      // console.log(message)
    },
    fullscreenSwich: function () {
      let isFull = this.isFullscreen()
      jessibucaPlayer[this._uid].setFullscreen(!isFull)
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
    if (jessibucaPlayer[this._uid]) {
      jessibucaPlayer[this._uid].destroy();
    }
    this.playing = false;
    this.loaded = false;
    this.performance = "";
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
