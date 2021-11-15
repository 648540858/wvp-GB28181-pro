<template>
  <div id="jessibuca" style="width: auto; height: auto">
    <div id="container" ref="container" style="width: 100%; height: 10rem; background-color: #000"
         @dblclick="fullscreenSwich">
      <div class="buttons-box" id="buttonsBox">
        <div class="buttons-box-left">
          <a-button @click="playBtnClick" v-show="!playing" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas', 'play']"/>
          </a-button>
          <a-button @click="pause" v-show="playing" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas', 'pause']"/>
          </a-button>
          <a-button @click="destroy" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas', 'stop']"/>
          </a-button>
          <a-button @click="jessibuca.mute()" v-show="isNotMute" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas', 'volume-mute']"/>
          </a-button>
          <a-button @click="jessibuca.cancelMute()" v-show="!isNotMute" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas', 'volume-up']"/>
          </a-button>
        </div>
        <div class="buttons-box-right">
          <span style="color: #f0f2f5;margin-right: 5px">{{ kBps }} kb/s</span>
          <a-button @click="jessibuca.screenshot('截图','png',0.5)" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas', 'camera']"/>
          </a-button>
          <a-button @click="playBtnClick" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas','redo']"/>
          </a-button>
          <a-button @click="fullscreenSwich" class="jessibuca-btn" v-show="!fullscreen">
            <font-awesome-icon :icon="['fas','expand']"/>
          </a-button>
          <a-button @click="fullscreenSwich" v-show="fullscreen" class="jessibuca-btn">
            <font-awesome-icon :icon="['fas','compress']"/>
          </a-button>
        </div>
      </div>

    </div>
  </div>
</template>

<script>

import Jessibuca from '@/core/jessibuca/renderer'

export default {
  name: 'jessibuca',
  data() {
    return {
      jessibuca: null,
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
      ffUrl: ''
    };
  },
  props: ['videoUrl', 'error', 'hasAudio', 'height'],
  mounted() {

    window.onerror = (msg) => {
      // console.error(msg)
    };
    let paramUrl = decodeURIComponent(this.$route.params.url)
    this.$nextTick(() => {
      let dom = document.getElementById("container");
      dom.style.height = (9 / 16) * dom.clientWidth + "px"
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
    create() {
      let options = {};
      console.log("音频开关：" + this.hasAudio)
      this.jessibuca = new Jessibuca(Object.assign(
        {
          container: this.$refs.container,
          videoBuffer: 0.5, // 最大缓冲时长，单位秒
          isResize: true,
          decoder: "/jessibuca/ff.js",
          // text: "WVP-PRO",
          // background: "bg.jpg",
          loadingText: "加载中",
          hasAudio: typeof (this.hasAudio) == "undefined" ? true : this.hasAudio,
          debug: false,
          supportDblclickFullscreen: false, // 是否支持屏幕的双击事件，触发全屏，取消全屏事件。
          operateBtns: {
            fullscreen: false,
            screenshot: false,
            play: false,
            audio: false,
          },
          record: "record",
          vod: this.vod,
          forceNoOffscreen: this.forceNoOffscreen,
          isNotMute: this.isNotMute
        },
        options
      ));

      let _this = this;
      this.jessibuca.on("load", function () {
        console.log("on load init");
      });

      this.jessibuca.on("log", function (msg) {
        console.log("on log", msg);
      });
      this.jessibuca.on("record", function (msg) {
        console.log("on record:", msg);
      });
      this.jessibuca.on("pause", function () {
        _this.playing = false;
      });
      this.jessibuca.on("play", function () {
        _this.$emit('updateData')
        _this.playing = true;
      });
      this.jessibuca.on("fullscreen", function (msg) {
        console.log("on fullscreen", msg);
        _this.fullscreen = msg
      });

      this.jessibuca.on("mute", function (msg) {
        console.log("on mute", msg);
        _this.isNotMute = !msg;
      });
      this.jessibuca.on("audioInfo", function (msg) {
        // console.log("audioInfo", msg);
      });

      this.jessibuca.on("videoInfo", function (msg) {
        this.videoInfo = msg;
        // console.log("videoInfo", msg);

      });

      this.jessibuca.on("bps", function (bps) {
        // console.log('bps', bps);
        _this.kBps = Math.round(bps / 1024 /8)
      });
      let _ts = 0;
      this.jessibuca.on("timeUpdate", function (ts) {
        // console.log('timeUpdate,old,new,timestamp', _ts, ts, ts - _ts);
        _ts = ts;
      });

      this.jessibuca.on("videoInfo", function (info) {
        console.log("videoInfo", info);
      });

      this.jessibuca.on("error", function (error) {
        console.log("error", error);
      });

      this.jessibuca.on("timeout", function () {
        console.log("timeout");
      });

      this.jessibuca.on('start', function () {
        console.log('start');
      })

      this.jessibuca.on("performance", function (performance) {
        let show = "卡顿";
        if (performance === 2) {
          show = "非常流畅";
        } else if (performance === 1) {
          show = "流畅";
        }
        _this.performance = show;
      });
      this.jessibuca.on('buffer', function (buffer) {
        // console.log('buffer', buffer);
      })

      this.jessibuca.on('stats', function (stats) {
        // console.log('stats', stats);
      })

      this.jessibuca.on('kBps', function (kBps) {
        //_this.kBps = kBps;
      });

      // 显示时间戳 PTS
      this.jessibuca.on('videoFrame', function () {

      })

      //
      this.jessibuca.on('metadata', function () {

      });
    },
    playBtnClick: function (event) {
      this.play(this.videoUrl)
    },
    play: function (url) {
      console.log(url)
      if (this.jessibuca) {
        this.destroy();
      }
      this.create();
      this.jessibuca.on("play", () => {
        this.playing = true;
        this.loaded = true;
        this.quieting = this.jessibuca.quieting;
      });
      if (this.jessibuca.hasLoaded()) {
        this.jessibuca.play(url);
      } else {
        this.jessibuca.on("load", () => {
          console.log("load 播放")
          this.jessibuca.play(url);
        });
      }
    },
    pause: function () {
      if (this.jessibuca) {
        this.jessibuca.pause();
      }
      this.playing = false;
      this.err = "";
      this.performance = "";
    },
    destroy: function () {
      if (this.jessibuca) {
        this.jessibuca.destroy();
      }
      if (document.getElementById("buttonsBox") == null) {
        document.getElementById("container").appendChild(this.btnDom)
      }
      this.jessibuca = null;
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
      this.jessibuca.setFullscreen(!isFull)
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
    if (this.jessibuca) {
      this.jessibuca.destroy();
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
  padding-bottom: 0.1rem;
  width: 2.5rem;
  border: none;
  background: transparent;
  color: #e8e8e8;
  cursor: pointer;
  text-align: left;
}

.jessibuca-btn:hover {
  background: transparent;
}

.buttons-box-right {
  position: absolute;
  right: 0;
}

.buttons-box-left {
  position: absolute;
  left: 0;
}
</style>
