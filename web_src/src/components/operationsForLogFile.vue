<template>
  <div id="operations" style="width: 100%;height: 100%">
    <el-container style="height: 82vh;">
      <el-aside width="200px" style="text-align: left">
        <el-menu :default-active="activeIndex" :height="winHeight">
          <el-menu-item index="systemInfo">
            <template slot="title"><i class="el-icon-message"></i>平台信息</template>
          </el-menu-item>
          <el-submenu index="log">
            <template slot="title"><i class="el-icon-message"></i>日志信息</template>
            <el-menu-item index="realTimeLog">日志文件</el-menu-item>
            <el-menu-item index="logFile">实时日志</el-menu-item>
          </el-submenu>
          <el-submenu index="senior">
            <template slot="title"><i class="el-icon-setting"></i>高级维护</template>
            <el-menu-item disabled="disabled" index="tcpdump">网络抓包</el-menu-item>
            <el-menu-item disabled="disabled" index="networkCard">网卡信息</el-menu-item>
          </el-submenu>
        </el-menu>
      </el-aside>
      <el-main style="padding: 5px;">
      </el-main>
    </el-container>

  </div>
</template>

<script>


export default {
  name: 'log',
  components: {},
  data() {
    return {
      loading: false,
      winHeight: (window.innerHeight - 160) + "px",
      activeIndex: 'systemInfo',
      data: [],
      filter: "",
    };
  },

  created() {
    console.log('created');
    this.initData();
  },
  destroyed() {
  },
  methods: {
    initData: function () {
      console.log('initData');
      const websocket = new WebSocket("ws://localhost:18080/channel/log");
      websocket.onclose = e => {
        console.log(`conn closed: code=${e.code}, reason=${e.reason}, wasClean=${e.wasClean}`)
      }
      websocket.onmessage = e => {
        console.log(e.data);
        // this.data += e.data + "\r\n"

        this.data.push(e.data);
      }
      websocket.onerror = e => {
        console.log(`conn err`)
        console.error(e)
      }
      websocket.onopen = e => {
        console.log(`conn open: ${e}`);
      }
    },
    getLogData: function () {
      if (this.data.length === 0) {
        return "";
      } else {
        let result = '';
        for (let i = 0; i < this.data.length; i++) {
          if (this.filter.length === 0) {
            result += this.data[i] + "\r\n"
          } else {
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
