<template>
  <div ref="container" @dblclick="fullscreenSwich"
       style="width:100%; height: 100%; background-color: #000000;margin:0 auto;position: relative;">
    <div style="width:100%; padding-top: 56.25%; position: relative;"></div>
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
  created() {
    let paramUrl = decodeURIComponent(this.$route.params.url)
    this.$nextTick(() => {
      console.log(2222)
      this.updatePlayerDomSize()
      window.onresize = this.updatePlayerDomSize
      if (typeof (this.videoUrl) == "undefined") {
        this.videoUrl = paramUrl;
      }
      this.btnDom = document.getElementById("buttonsBox");
    })
  },
  // mounted() {
  //   const ro = new ResizeObserver(entries => {
  //     entries.forEach(entry => {
  //       this.updatePlayerDomSize()
  //     });
  //   });
  //   ro.observe(this.$refs.container);
  // },
  mounted(){
    this.updatePlayerDomSize();
  },
  watch: {
    videoUrl: {
      handler(val, _) {
        this.$nextTick(() => {
          this.play(val);
        })
      },
      immediate: true
    }
  },
  methods: {
    updatePlayerDomSize() {
      let dom = this.$refs.container;
      let width = dom.parentNode.clientWidth
      let height = (9 / 16) * width
      console.log(height)

      console.log(dom.clientHeight)
      if (height > dom.clientHeight) {
        height = dom.clientHeight
        width = (16 / 9) * height
      }
      if (width > 0 && height > 0) {
        dom.style.width = width + 'px';
        dom.style.height = height + "px";
        dom.style.paddingTop = 0;
        console.log(width)
        console.log(height)
      }
    },
    create() {
      let options = {
        container: this.$refs.container,
        autoWasm: true,
        background: "",
        controlAutoHide: false,
        debug: false,
        decoder: "static/js/jessibuca/decoder.js",
        forceNoOffscreen: false,
        hasAudio: typeof (this.hasAudio) == "undefined" ? true : this.hasAudio,
        heartTimeout: 5,
        heartTimeoutReplay: true,
        heartTimeoutReplayTimes: 3,
        hiddenAutoPause: false,
        hotKey: true,
        isFlv: false,
        isFullResize: false,
        isNotMute: this.isNotMute,
        isResize: false,
        keepScreenOn: true,
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
        recordType: "mp4",
        rotate: 0,
        showBandwidth: false,
        supportDblclickFullscreen: false,
        timeout: 10,
        useMSE: true,
        useWCS: location.hostname === "localhost" || location.protocol === "https:",
        useWebFullScreen: true,
        videoBuffer: 0.1,
        wasmDecodeErrorReplay: true,
        wcsUseVideoRender: true
      };
      console.log("Jessibuca -> options: ", options);
      jessibucaPlayer[this._uid] = new window.Jessibuca({...options});

      let jessibuca = jessibucaPlayer[this._uid];
      let _this = this;
      jessibuca.on("pause", function () {
        _this.playing = false;
      });
      jessibuca.on("play", function () {
        _this.playing = true;
      });
      jessibuca.on("fullscreen", function (msg) {
        _this.fullscreen = msg
      });
      jessibuca.on("mute", function (msg) {
        _this.isNotMute = !msg;
      });
      jessibuca.on("performance", function (performance) {
        let show = "卡顿";
        if (performance === 2) {
          show = "非常流畅";
        } else if (performance === 1) {
          show = "流畅";
        }
        _this.performance = show;
      });
      jessibuca.on('kBps', function (kBps) {
        _this.kBps = Math.round(kBps);
      });
      jessibuca.on("videoInfo", function (msg) {
        console.log("Jessibuca -> videoInfo: ", msg);
      });
      jessibuca.on("audioInfo", function (msg) {
        console.log("Jessibuca -> audioInfo: ", msg);
      });
      jessibuca.on("error", function (msg) {
        console.log("Jessibuca -> error: ", msg);
      });
      jessibuca.on("timeout", function (msg) {
        console.log("Jessibuca -> timeout: ", msg);
      });
      jessibuca.on("loadingTimeout", function (msg) {
        console.log("Jessibuca -> timeout: ", msg);
      });
      jessibuca.on("delayTimeout", function (msg) {
        console.log("Jessibuca -> timeout: ", msg);
      });
      jessibuca.on("playToRenderTimes", function (msg) {
        console.log("Jessibuca -> playToRenderTimes: ", msg);
      });
    },
    playBtnClick: function (event) {
      this.play(this.videoUrl)
    },
    play: function (url) {
      console.log("Jessibuca -> url: ", url);
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
