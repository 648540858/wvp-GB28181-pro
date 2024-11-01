<template>
  <div id="log" style="width: 100%;height: 100%">
    <log-viewer :log="data" :loading="loading" :auto-scroll="false" :height="winHeight" />

  </div>
</template>

<script>
// import uiHeader from '../layout/UiHeader.vue'
import {AnsiUp} from 'ansi_up'


export default {
  name: 'log',
  components: {},
  data() {
    return {
      loading: false,
      winHeight: window.innerHeight - 120,
      data: '',
      ansiUp: new AnsiUp()
    };
  },

  created() {
    console.log('created');
    this.initData();
  },
  destroyed() {},
  methods: {
    initData: function () {
      console.log('initData');
      const websocket = new WebSocket("ws://localhost:18080/channel/log");
      websocket.onclose = e => {
        console.log(`conn closed: code=${e.code}, reason=${e.reason}, wasClean=${e.wasClean}`)
      }
      websocket.onmessage = e => {
        console.log(e.data);
        this.data += e.data + "\r\n"
      }
      websocket.onerror = e => {
        console.log(`conn err`)
        console.error(e)
      }
      websocket.onopen = e => {
        console.log(`conn open: ${e}`);
      }
    },
  }
};
</script>

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
  background-image: url("../assets/loading.png");
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
</style>
