<template>
  <div id="log" class="log-panel">
    <el-form :inline="true" size="mini">
      <el-form-item label="过滤">
        <el-input v-model="filter" size="mini" placeholder="请输入过滤关键字" style="width: 20vw" />
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button size="mini" icon="el-icon-download" @click="downloadFile()">下载</el-button>
      </el-form-item>
    </el-form>
    <div class="log-content" v-loading="loading" element-loading-text="日志加载中">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="log-status"
      />
      <el-empty
        v-else-if="!loading && !logData"
        :description="emptyDescription"
        :image-size="64"
      />
      <log-viewer v-else :log="logData" :auto-scroll="true" height="100%" />
    </div>
  </div>
</template>

<script>

import moment from 'moment/moment'
import { markRaw } from 'vue'
import logViewer from '@/components/LogViewer'
import stripAnsi from 'strip-ansi'
import request from '@/utils/request'

export default {
  name: 'Log',
  components: { logViewer },
  props: {
    fileUrl: { type: String, default: '' },
    remoteUrl: { type: String, default: '' },
    loadEnd: { type: Function, default: null }
  },
  data() {
    return {
      loading: true,
      data: [],
      filter: '',
      websocket: null,
      connectionState: 'idle',
      errorMessage: ''
    }
  },
  computed: {
    logData() {
      const keyword = this.filter.trim()
      const lines = keyword
        ? this.data.filter(item => String(item).includes(keyword))
        : this.data
      return lines.length > 0 ? `${lines.join('\r\n')}\r\n` : ''
    },
    emptyDescription() {
      if (this.filter.trim() && this.data.length > 0) return '没有匹配的日志内容'
      if (this.remoteUrl && this.connectionState === 'connected') return '实时日志已连接，等待新的日志内容'
      if (this.remoteUrl && this.connectionState === 'closed') return '实时日志连接已关闭，请刷新页面重试'
      return '暂无日志内容'
    }
  },
  watch: {
    remoteUrl(newValue, oldValue) {
      if (newValue !== oldValue) this.initData()
    },
    fileUrl(newValue, oldValue) {
      if (newValue !== oldValue) this.initData()
    }
  },
  created() {
    if (this.fileUrl || this.remoteUrl) {
      this.initData()
    } else {
      this.loading = false
    }
  },
  unmounted() {
    this.closeWebsocket()
  },
  methods: {
    initData: function() {
      this.closeWebsocket()
      this.loading = true
      this.errorMessage = ''
      this.connectionState = 'idle'
      this.data = []
      if (this.fileUrl) {
        request({
          method: 'get',
          url: this.fileUrl,
          responseType: 'text'
        }).then((res) => {
          const content = this.normalizeLogContent(res)
          this.data = content ? content.split(/\r?\n/) : []
          if (this.loadEnd) this.loadEnd()
        }).catch((error) => {
          this.errorMessage = this.resolveErrorMessage(error, '日志文件加载失败，请稍后重试')
        }).finally(() => {
          this.loading = false
        })
      } else if (this.remoteUrl) {
        this.connectionState = 'connecting'
        const token = this.$store.getters.token
        let websocket
        try {
          websocket = token
            ? new WebSocket(this.remoteUrl, token)
            : new WebSocket(this.remoteUrl)
        } catch (error) {
          this.loading = false
          this.connectionState = 'error'
          this.errorMessage = this.resolveErrorMessage(error, '实时日志连接失败，请刷新页面重试')
          return
        }
        this.websocket = markRaw(websocket)
        websocket.onclose = () => {
          if (this.websocket !== websocket) return
          this.websocket = null
          this.loading = false
          this.connectionState = 'closed'
        }
        websocket.onmessage = e => {
          if (this.websocket !== websocket) return
          this.loading = false
          this.connectionState = 'connected'
          this.data.push(String(e.data ?? ''))
        }
        websocket.onerror = () => {
          if (this.websocket !== websocket) return
          this.loading = false
          this.connectionState = 'error'
          this.errorMessage = '实时日志连接失败，请检查服务连接后刷新页面重试'
        }
        websocket.onopen = () => {
          if (this.websocket !== websocket) return
          this.loading = false
          this.connectionState = 'connected'
        }
      } else {
        this.loading = false
      }
    },
    closeWebsocket() {
      if (this.websocket) {
        const websocket = this.websocket
        this.websocket = null
        websocket.close()
      }
    },
    normalizeLogContent(response) {
      const content = response && typeof response === 'object' && 'data' in response
        ? response.data
        : response
      if (Array.isArray(content)) return content.map(item => String(item)).join('\n')
      return content == null ? '' : String(content)
    },
    resolveErrorMessage(error, fallback) {
      if (typeof error === 'string') return error
      const responseMessage = error && error.response && error.response.data
        ? error.response.data.msg
        : ''
      return responseMessage || fallback
    },
    getLogDataWithOutAnsi: function() {
      return stripAnsi(this.logData)
    },
    downloadFile() {
      const content = this.getLogDataWithOutAnsi()
      if (!content) {
        this.$message.warning('暂无可下载的日志内容')
        return
      }
      const blob = new Blob([content], {
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
.log-panel {
  height: 100%;
}

.log-content {
  position: relative;
  height: calc(100% - 60px);
  min-height: 240px;
}

.log-content .el-empty {
  height: 100%;
  justify-content: center;
}

.log-status {
  margin-bottom: 12px;
}
</style>
