<template>
  <div id="log" style="width: 100%;height: 100%">
    <div style="width: 100%; height: 40px">
      <div style="width: 15vw; text-align: center; line-height: 40px">
        <span style="width: 5vw">过滤: </span>
        <el-input size="mini" v-model="filter" placeholder="请输入过滤关键字" style="width: 10vw"></el-input>
      </div>
      <div></div>
    </div>
    <log-viewer :log="getLogData()" :loading="loading" :auto-scroll="true" :height="winHeight" />
  </div>
</template>

<script>

import userService from "./service/UserService";

export default {
  name: 'log',
  components: {},
  data() {
    return {
      loading: false,
      winHeight: window.innerHeight - 180,
      data: [],
      filter: "",
      websocket: null,
    };
  },

  created() {
    console.log('created');
    this.initData();
  },
  destroyed() {
    console.log('destroyed');
    window.websocket.close();
  },
  methods: {
    initData: function () {
      console.log(window.location.host)
      let url = "ws://localhost:18080/channel/log";
      if (process.env.NODE_ENV !== 'development') {
        if (location.protocol === "https:") {
          url = `wss://${window.location.host}/channel/log`
        }else {
          url = `ws://${window.location.host}/channel/log`
        }
      }
      window.websocket = new WebSocket(url, userService.getToken());
      window.websocket.onclose = e => {
        console.log(`conn closed: code=${e.code}, reason=${e.reason}, wasClean=${e.wasClean}`)
      }
      window.websocket.onmessage = e => {
        console.log(e.data);
        // this.data += e.data + "\r\n"

        this.data.push(e.data);
      }
      window.websocket.onerror = e => {
        console.log(`conn err`)
        console.error(e)
      }
      window.websocket.onopen = e => {
        console.log(`conn open: ${e}`);
      }
    },
    getLogData: function () {
      if (this.data.length === 0) {
        return "";
      }else {
        let result = '';
        for (let i = 0; i < this.data.length; i++) {
          if (this.filter.length === 0) {
            result += this.data[i] + "\r\n"
          }else {
            if (this.data[i].indexOf(this.filter) > -1) {
              result += this.data[i] + "\r\n"
            }
          }
        }
        return result;
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
