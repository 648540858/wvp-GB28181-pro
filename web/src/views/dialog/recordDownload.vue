<template>
  <div id="recordDownload">
    <el-dialog v-if="showDialog" v-el-drag-dialog :title="title" width="45rem" :append-to-body="true" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" center @close="close()">
      <div style="display: grid; grid-template-columns: auto max-content; gap: 12px">
        <div>
          <el-progress :percentage="percentage" :status="downloadStatus === 'interrupted' ? 'exception' : undefined" style="height: 28px; line-height: 25px;"/>
        </div>
        <div>
          <el-button v-if="downloadFile" icon="el-icon-download" size="mini" @click="downloadFileClientEvent()">{{ downloadStatus === 'interrupted' ? '下载不完整文件' : '下载' }}</el-button>
        </div>
      </div>
      <el-alert v-if="downloadStatus === 'interrupted'" title="录像下载中断，文件不完整" :description="statusDescription" type="error" :closable="false" show-icon role="alert"/>
      <div v-else role="status" aria-live="polite">{{ statusDescription }}</div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

// 展示一次设备录像下载的真实结束状态，区分完整成品与中断后留下的部分文件。
export default {
  name: 'RecordDownload',
  directives: { elDragDialog },
  // 以服务端状态为事实来源，轮询身份只用于隔离关闭或重开前的旧响应。
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
      getProgressRun: false,
      timer: null,
      downloadFile: null,
      progressFailCount: 0,
      progressRequestId: 0,
      stopRequested: false,
      downloadStatus: 'downloading',
      downloadedDuration: null,
      statusDescription: ''
    }
  },
  // 离开页面时释放仍在进行的设备下载，已结束的部分文件继续保留。
  created() {
    window.addEventListener('beforeunload', this.stopDownloadRecord)
  },
  // 卸载时同时清理监听和轮询，避免迟到响应继续触发文件下载。
  destroyed() {
    window.removeEventListener('beforeunload', this.stopDownloadRecord)
    this.close()
  },
  methods: {
    // 开始展示新一轮任务，重置结果并使上一轮的异步读取失效。
    openDialog: function(deviceId, channelId, app, stream, mediaServerId) {
      this.clearProgressTimer()
      this.progressRequestId++
      this.deviceId = deviceId
      this.channelId = channelId
      this.app = app
      this.stream = stream
      this.mediaServerId = mediaServerId
      this.showDialog = true
      this.getProgressRun = true
      this.percentage = 0.0
      this.streamInfo = null
      this.downloadFile = null
      this.progressFailCount = 0
      this.stopRequested = false
      this.downloadStatus = 'downloading'
      this.downloadedDuration = null
      this.statusDescription = ''
      this.title = '下载中...'
      this.getProgressTimer()
    },
    // 保留唯一轮询定时器，结束和关闭都能明确取消下一次读取。
    clearProgressTimer: function() {
      if (this.timer !== null) {
        window.clearTimeout(this.timer)
        this.timer = null
      }
    },
    // 只为当前仍在展示的任务安排下一次进度读取。
    getProgressTimer: function() {
      if (!this.getProgressRun || !this.showDialog) return
      this.clearProgressTimer()
      const requestId = this.progressRequestId
      this.timer = setTimeout(() => {
        this.timer = null
        if (!this.showDialog || !this.getProgressRun || requestId !== this.progressRequestId) return
        this.getProgress(this.getProgressTimer)
      }, 5000)
    },
    // 读取进度并按明确终态更新页面，已有文件地址本身不能证明请求区间完整。
    getProgress: function(callback) {
      if (!this.getProgressRun || !this.showDialog) return Promise.resolve()
      const requestId = this.progressRequestId
      return this.$store.dispatch('gbRecord/queryDownloadProgress', [
        this.deviceId, this.channelId, this.stream
      ])
        .then(streamInfo => {
          if (requestId !== this.progressRequestId || !this.showDialog || !this.getProgressRun) return
          // 错误拦截器也可能以空值结束请求，不能让一次读取失败永久终止轮询。
          if (!streamInfo) {
            this.progressFailCount++
            return
          }
          this.progressFailCount = 0
          this.streamInfo = streamInfo
          const progress = Number.parseFloat(streamInfo.progress)
          this.percentage = Number.isFinite(progress) ? Math.max(0, Math.min(99.9, Number((progress * 100).toFixed(1)))) : 0
          const duration = Number(streamInfo.downloadedDuration)
          this.downloadedDuration = streamInfo.downloadedDuration != null && Number.isFinite(duration) && duration >= 0 ? duration : null
          const path = streamInfo.downLoadFilePath
          const file = path && (location.protocol === 'https:' ? path.httpsPath : path.httpPath)
          // 未带状态的旧版本继续按旧成品契约展示；新版本必须明确声明 completed。
          const completed = streamInfo.downloadStatus === 'completed' || (!streamInfo.downloadStatus && file)
          if (streamInfo.downloadStatus === 'interrupted') {
            this.downloadStatus = 'interrupted'
            this.title = '录像下载中断'
            const durationText = this.downloadedDuration === null ? '' : '已收到 ' + Math.floor(this.downloadedDuration / 60) + '分' + Math.floor(this.downloadedDuration % 60) + '秒录像。'
            // 用户提示使用固定文案，避免接口诊断信息中的路径或内部异常出现在页面。
            this.statusDescription = durationText + '录像未覆盖申请时长，可能是媒体中断或源录像缺失。请检查录像区间后重试；已有部分文件可单独保存。'
            this.downloadFile = file || null
            this.getProgressRun = false
            this.clearProgressTimer()
          } else if (completed && file) {
            this.downloadStatus = 'completed'
            this.title = '录像下载完成'
            this.statusDescription = '录像文件已生成。'
            this.downloadFile = file
            this.percentage = 100
            this.getProgressRun = false
            this.clearProgressTimer()
            this.downloadFileClientEvent()
          } else {
            this.downloadStatus = streamInfo.downloadStatus === 'finalizing' || completed ? 'finalizing' : 'downloading'
            this.title = this.downloadStatus === 'finalizing' ? '正在生成录像文件...' : '下载中...'
            this.statusDescription = this.downloadStatus === 'finalizing' ? '正在封装录像文件，请稍候。' : ''
          }
        })
        .catch(() => {
          if (requestId === this.progressRequestId && this.showDialog && this.getProgressRun) this.progressFailCount++
        })
        .finally(() => {
          if (requestId !== this.progressRequestId || !this.showDialog || !this.getProgressRun) return
          if (this.progressFailCount >= 3) {
            this.getProgressRun = false
            this.title = '暂时无法获取下载状态'
            this.statusDescription = '未获取到录像下载信息，请稍后在云端录像中查看。'
            this.$message.warning({ showClose: true, message: this.statusDescription })
            return
          }
          if (typeof callback === 'function') callback()
        })
    },
    // 取消未结束的下载并使在途读取失效，已完成或中断的成品不再被停止接口清除。
    close: function() {
      // Element UI 关闭动画触发事件前可能已将 visible 置为 false，不能用可见性判断是否需要停止。
      const shouldStop = this.downloadStatus !== 'completed' && this.downloadStatus !== 'interrupted'
      this.getProgressRun = false
      this.progressRequestId++
      this.clearProgressTimer()
      this.showDialog = false
      if (shouldStop) this.stopDownloadRecord()
    },
    // 保存录像下载使用的倍率，保持调用方现有接口。
    gbScale: function(scale) {
      this.scale = scale
    },
    // 只终止当前未结束的设备会话，并防止关闭与卸载重复提交停止命令。
    stopDownloadRecord: function(callback) {
      if (this.stopRequested || this.downloadStatus === 'completed' || this.downloadStatus === 'interrupted') return
      if (this.deviceId && this.channelId && this.stream) {
        this.stopRequested = true
        return this.$axios({
          method: 'get',
          url: '/api/gb_record/download/stop/' + this.deviceId + '/' + this.channelId + '/' + this.stream
        }).then((res) => {
          if (typeof callback === 'function') callback(res)
        }).catch(() => {
          // 停止响应失败不能推断设备已释放；保持结果未知，避免自动重放写请求。
        })
      }
    },
    // 保存已就绪文件，部分成品使用明确文件名，只有完整终态会自动调用此方法。
    downloadFileClientEvent: function() {
      if (!this.downloadFile) return
      const filename = this.deviceId + '-' + this.channelId + (this.downloadStatus === 'interrupted' ? '-partial' : '') + '.mp4'
      const x = new XMLHttpRequest()
      x.open('GET', this.downloadFile, true)
      x.responseType = 'blob'
      x.onload = () => {
        if (x.status < 200 || x.status >= 300) {
          this.$message.warning({ showClose: true, message: '录像文件下载失败，请重试。' })
          return
        }
        const url = window.URL.createObjectURL(x.response)
        const a = document.createElement('a')
        a.href = url
        a.download = filename
        a.click()
        window.URL.revokeObjectURL(url)
      }
      x.send()
    }
  }
}
</script>

<style>

</style>
