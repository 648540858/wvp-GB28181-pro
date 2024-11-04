<template>
  <div id="log" style="width: 100%;height: 100%;">
    <div style="width: 100%; height: 40px; display: grid; grid-template-columns: 1fr 1fr">
      <div style="text-align: left; line-height: 40px;">
        <span style="width: 5vw">过滤: </span>
        <el-input size="mini" v-model="filter" placeholder="请输入过滤关键字" style="width: 20vw"></el-input>
      </div>
      <div style="text-align: right; line-height: 40px;">
        <el-button size="mini" icon="el-icon-download"  @click="downloadFile()">下载
        </el-button>
      </div>
    </div>
    <log-viewer :log="logData"  :auto-scroll="true" :height="winHeight"/>
  </div>
</template>

<script>

import userService from "./../service/UserService";
import moment from "moment/moment";
import stripAnsi from "strip-ansi";

export default {
  name: 'log',
  props: [ 'fileUrl', 'remoteUrl', 'loadEnd'],
  components: {},
  data() {
    return {
      loading: true,
      winHeight: window.innerHeight - 300,
      data: [],
      filter: "",
      logData: "",
      websocket: null,
    };
  },
  watch: {
    remoteUrl(newValue) {
      console.log(newValue);
      this.remoteUrl = newValue;
      this.initData();
    },
    fileUrl(newValue) {
      this.fileUrl = newValue;
      this.initData();
    },
    filter(newValue) {
      this.filter = newValue;
      this.logData = this.getLogData();
    },
    data(newValue) {
      this.data = newValue;
      this.logData = this.getLogData();
    }
  },
  created() {
    this.data = []
    if (this.fileUrl || this.remoteUrl) {
      this.initData();
    }
  },
  destroyed() {
    console.log('destroyed');
    window.websocket.close();
  },
  methods: {
    initData: function () {
      this.loading = true
      this.data = []
      console.log(this.loading)
      if (this.fileUrl) {
        this.$axios({
          method: 'get',
          url: this.fileUrl,
        }).then((res) => {
          let dataArray = res.data.split("\n");
          dataArray.forEach(item => {
            this.data.push(item);
          })
          this.loading = false
          if (this.loadEnd && typeof this.loadEnd === "function") {
            this.loadEnd()
          }
        }).catch((error) => {
          console.log(error);
        });
      }else if (this.remoteUrl) {
        console.log('remoteUrl' + this.remoteUrl);
        console.log(window.location.host)
        window.websocket = new WebSocket(this.remoteUrl, userService.getToken());
        window.websocket.onclose = e => {
          console.log(`conn closed: code=${e.code}, reason=${e.reason}, wasClean=${e.wasClean}`)
        }
        window.websocket.onmessage = e => {
          this.loading = false
          this.data.push(e.data);
        }
        window.websocket.onerror = e => {
          console.log(`conn err`)
          console.error(e)
        }
        window.websocket.onopen = e => {
          console.log(`conn open: ${e}`);
        }
      }
    },
    getLogData: function () {
      this.loading = true;
      if (this.data.length === 0) {
        this.loading = false;
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
        this.loading = false;
        return result;
      }
    },
    getLogDataWithOutAnsi: function () {
      if (this.data.length === 0) {
        return "";
      }else {
        let result = '';
        for (let i = 0; i < this.data.length; i++) {
          if (this.filter.length === 0) {
            result += stripAnsi(this.data[i]) + "\r\n"
          }else {
            if (this.data[i].indexOf(this.filter) > -1) {
              result += stripAnsi(this.data[i]) + "\r\n"
            }
          }
        }
        return result;
      }
    },
    downloadFile() {
      let blob = new Blob([this.getLogDataWithOutAnsi()], {
        type: "text/plain;charset=utf-8"
      });
      let reader = new FileReader();
      reader.readAsDataURL(blob);
      reader.onload = (e)=> {
        let a = document.createElement('a');
        a.download = `wvp-${this.filter}-${moment().format('yyyy-MM-DD')}.log`;
        a.href = e.target.result;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    },
  }
};
</script>

<style>
.log-loading{
  position: absolute;
  left: 50%;
  top: 50%;
  display: inline-block;
  text-align: center;
  background-color: transparent;
  font-size: 20px;
  color: rgb(255, 255, 255);
  z-index: 1000;
}
</style>
