<template>
  <div id="log" style="height: 100%">
    <el-form :inline="true" size="mini">
      <el-form-item label="过滤">
        <el-input v-model="filter" size="mini" placeholder="请输入过滤关键字" style="width: 20vw" />
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button size="mini" icon="el-icon-download" @click="downloadFile()">下载</el-button>
      </el-form-item>
    </el-form>
    <log-viewer :log="logData" :auto-scroll="true" :height="winHeight" style="height: calc(100% - 60px);" />
  </div>
</template>

<script>

import moment from 'moment/moment'
import logViewer from '@femessage/log-viewer'
import stripAnsi from 'strip-ansi'
import request from '@/utils/request'

export default {
  name: 'Log',
  components: { logViewer },
  props: ['fileUrl', 'remoteUrl', 'loadEnd'],
  data() {
    return {
      loading: true,
      winHeight: window.innerHeight - 100,
      data: [],
      filter: '',
      logData: '',
      websocket: null,
      destroyedCallback: null
    }
  },
  watch: {
    remoteUrl(newValue) {
      console.log(newValue)
      this.remoteUrl = newValue
      this.initData()
    },
    fileUrl(newValue) {
      this.fileUrl = newValue
      this.initData()
    },
    filter(newValue) {
      this.filter = newValue
      this.logData = this.getLogData()
    },
    data(newValue) {
      this.data = newValue
      this.logData = this.getLogData()
    }
  },
  created() {
    this.data = []
    if (this.fileUrl || this.remoteUrl) {
      this.initData()
    }
  },
  destroyed() {
    console.log('destroyed')
    if (this.destroyedCallback) {
      this.destroyedCallback()
    }
  },
  methods: {
    initData: function() {
      this.loading = true
      this.data = []
      console.log(this.loading)
      if (this.fileUrl) {
        request({
          method: 'get',
          url: this.fileUrl
        }).then((res) => {
          const dataArray = res.split('\n')
          dataArray.forEach(item => {
            this.data.push(item)
          })
          this.loading = false
          if (this.loadEnd && typeof this.loadEnd === 'function') {
            this.loadEnd()
          }
        }).catch((error) => {
          console.log(error)
        })
      } else if (this.remoteUrl) {
        console.log('remoteUrl' + this.remoteUrl)
        console.log(window.location.host)
        window.websocket = new WebSocket(this.remoteUrl, this.$store.getters.token)
        window.websocket.onclose = e => {
          console.log(`conn closed: code=${e.code}, reason=${e.reason}, wasClean=${e.wasClean}`)
        }
        window.websocket.onmessage = e => {
          this.loading = false
          this.data.push(e.data)
        }
        window.websocket.onerror = e => {
          console.log(`conn err`)
          console.error(e)
        }
        window.websocket.onopen = e => {
          console.log(`conn open: ${e}`)
          this.destroyedCallback = () => {
            window.websocket.close()
          }
        }
      }
    },
    getLogData: function() {
      this.loading = true
      if (this.data.length === 0) {
        this.loading = false
        return ''
      } else {
        let result = ''
        for (let i = 0; i < this.data.length; i++) {
          if (this.filter.length === 0) {
            result += this.data[i] + '\r\n'
          } else {
            if (this.data[i].indexOf(this.filter) > -1) {
              result += this.data[i] + '\r\n'
            }
          }
        }
        this.loading = false
        return result
      }
    },
    getLogDataWithOutAnsi: function() {
      if (this.data.length === 0) {
        return ''
      } else {
        let result = ''
        for (let i = 0; i < this.data.length; i++) {
          if (this.filter.length === 0) {
            result += stripAnsi(this.data[i]) + '\r\n'
          } else {
            if (this.data[i].indexOf(this.filter) > -1) {
              result += stripAnsi(this.data[i]) + '\r\n'
            }
          }
        }
        return result
      }
    },
    downloadFile() {
      const blob = new Blob([this.getLogDataWithOutAnsi()], {
        type: 'text/plain;charset=utf-8'
      })
      const reader = new FileReader()
      reader.readAsDataURL(blob)
      reader.onload = (e) => {
        const a = document.createElement('a')
        a.download = `wvp-${this.filter}-${moment().format('yyyy-MM-DD')}.log`
        a.href = e.target.result
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
      }
    }
  }
}
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
