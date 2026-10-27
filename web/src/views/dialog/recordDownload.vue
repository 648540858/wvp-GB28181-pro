<template>
  <div id="recordDownload">
    <el-dialog v-if="showDialog" v-el-drag-dialog :title="title" width="45rem" :append-to-body="true" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" center @close="close()">
      <div style="display: grid; grid-template-columns: auto 70px">
        <div>
          <el-progress :percentage="percentage" style="height: 28px; line-height: 25px;"/>
        </div>
        <div>
          <el-button v-if="downloadFile" icon="el-icon-download" size="mini" title="点击下载" @click="downloadFileClientEvent()">下载</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'RecordDownload',
  directives: { elDragDialog },
  data() {
    return {
      title: '下载中...',
      deviceId: '',
      channelId: '',
      app: '',
      stream: '',
      mediaServerId: '',
      showDialog: false,
      scale: 1,
      percentage: 0.00,
      streamInfo: null,
      taskId: null,
      getProgressRun: false,
      timer: null,
      downloadFile: null,
      progressFailCount: 0
    }
  },
  created() {
    window.addEventListener('beforeunload', this.stopDownloadRecord)
  },
  destroyed() {
    window.removeEventListener('beforeunload', this.stopDownloadRecord)
  },
  methods: {
    openDialog: function(deviceId, channelId, app, stream, mediaServerId) {
      this.deviceId = deviceId
      this.channelId = channelId
      this.app = app
      this.stream = stream
      this.mediaServerId = mediaServerId
      this.showDialog = true
      this.getProgressRun = true
      this.percentage = 0.0
      this.downloadFile = null
      this.progressFailCount = 0
      this.getProgressTimer()
    },
    getProgressTimer: function() {
      if (!this.getProgressRun) {
        return
      }
      if (this.downloadFile) {
        return
      }
      setTimeout(() => {
        if (!this.showDialog) return
        this.getProgress(this.getProgressTimer)
      }, 5000)
    },
    getProgress: function(callback) {
      this.$store.dispatch('gbRecord/queryDownloadProgress', [
        this.deviceId, this.channelId, this.stream
      ])
        .then(streamInfo => {
          // 请求失败时错误拦截器不会 reject，而是给出一次提示并返回空值；
          // 这里不能抛异常，否则轮询会永远停止，下载地址再也拿不到
          if (!streamInfo) {
            this.progressFailCount++
            return
          }
          this.progressFailCount = 0
          this.streamInfo = streamInfo
          if (parseFloat(streamInfo.progress) === 1) {
            this.percentage = 100
          } else {
            this.percentage = parseFloat((parseFloat(streamInfo.progress) * 100).toFixed(1))
          }
          if (streamInfo.downLoadFilePath) {
            if (location.protocol === 'https:') {
              this.downloadFile = streamInfo.downLoadFilePath.httpsPath
            } else {
              this.downloadFile = streamInfo.downLoadFilePath.httpPath
            }
            this.percentage = 100
            this.getProgressRun = false
            this.downloadFileClientEvent()
          }
        })
        .catch(() => {
          this.progressFailCount++
        })
        .finally(() => {
          // 连续失败多次说明下载信息已不可用（例如保留窗口到期、录像被清理），停止轮询避免持续提示
          if (this.progressFailCount >= 3) {
            this.getProgressRun = false
            this.$message.warning({ showClose: true, message: '未获取到录像下载信息，请稍后在云端录像中查看' })
            return
          }
          if (this.downloadFile) {
            return
          }
          if (callback)callback()
        })
      //
      // this.$axios({
      //   method: 'get',
      //   url: `/api/gb_record/download/progress/${this.deviceId}/${this.channelId}/${this.stream}`
      // }).then((res) => {
      //   if (res.data.code === 0) {
      //     this.streamInfo = res.data.data
      //     if (parseFloat(res.data.progress) === 1) {
      //       this.percentage = 100
      //     } else {
      //       this.percentage = (parseFloat(res.data.data.progress) * 100).toFixed(1)
      //     }
      //     if (this.streamInfo.downLoadFilePath) {
      //       if (location.protocol === 'https:') {
      //         this.downloadFile = this.streamInfo.downLoadFilePath.httpsPath
      //       } else {
      //         this.downloadFile = this.streamInfo.downLoadFilePath.httpPath
      //       }
      //       this.percentage = 100
      //       this.getProgressRun = false
      //       this.downloadFileClientEvent()
      //     }
      //     if (callback)callback()
      //   } else {
      //     this.$message({
      //       showClose: true,
      //       message: res.data.msg,
      //       type: 'error'
      //     })
      //     this.close()
      //   }
      // }).catch((e) => {
      //   console.log(e)
      // })
    },
    close: function() {
      if (this.streamInfo && this.streamInfo.progress < 1) {
        this.stopDownloadRecord()
      }

      if (this.timer !== null) {
        window.clearTimeout(this.timer)
        this.timer = null
      }
      this.showDialog = false
      this.getProgressRun = false
    },
    gbScale: function(scale) {
      this.scale = scale
    },

    stopDownloadRecord: function(callback) {
      if (this.deviceId && this.channelId && this.stream) {
        this.$axios({
          method: 'get',
          url: '/api/gb_record/download/stop/' + this.deviceId + '/' + this.channelId + '/' + this.stream
        }).then((res) => {
          if (callback) callback(res)
        })
      }
    },
    downloadFileClientEvent: function() {
      // window.open(this.downloadFile )

      const x = new XMLHttpRequest()
      x.open('GET', this.downloadFile, true)
      x.responseType = 'blob'
      x.onload = (e) => {
        const url = window.URL.createObjectURL(x.response)
        const a = document.createElement('a')
        a.href = url
        a.download = this.deviceId + '-' + this.channelId + '.mp4'
        a.click()
      }
      x.send()
    }
  }
}
</script>

<style>

</style>
