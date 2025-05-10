<template>
  <div id="app" class="app-container">
    <div style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="搜索">
          <el-input
            v-model="search"
            style="margin-right: 1rem; width: auto;"
            size="mini"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="getFileList"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="startTime"
            size="mini"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择日期时间"
            @change="getFileList"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="endTime"
            size="mini"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择日期时间"
            @change="getFileList"
          />
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="getFileList()" />
        </el-form-item>
      </el-form>
      <!--日志列表-->
      <el-table size="medium" :data="fileList" style="width: 100%" :height="winHeight">
        <el-table-column
          type="selection"
          width="55"
        />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="fileSize" label="文件大小">
          <template v-slot:default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="开始时间">
          <template v-slot:default="scope">
            {{ formatTimeStamp(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间">
          <template v-slot:default="scope">
            {{ formatTimeStamp(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-document" type="text" @click="showLogView(scope.row)">查看
            </el-button>
            <el-button size="medium" icon="el-icon-download" type="text" @click="downloadFile(scope.row)">下载
            </el-button>
            <!--            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c"-->
            <!--                       @click="deleteRecord(scope.row)">删除-->
            <!--            </el-button>-->
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog
      top="10vh"
      :title="playerTitle"
      :visible.sync="showLog"
      width="90%"
    >
      <div style="height: 600px">
        <showLog ref="recordVideoPlayer" :file-url="fileUrl" :load-end="loadEnd" />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import showLog from './showLog.vue'
import moment from 'moment'
import { getToken } from '@/utils/auth'

export default {
  name: 'OperationsHistoryLog',
  components: {
    showLog
  },
  data() {
    return {
      search: '',
      startTime: '',
      endTime: '',
      showLog: false,
      playerTitle: '',
      fileUrl: '',
      playerStyle: {
        'margin': 'auto',
        'margin-bottom': '20px',
        'width': window.innerWidth / 2 + 'px',
        'height': this.winHeight / 2 + 'px'
      },
      mediaServerList: [], // 滅体节点列表
      mediaServerId: '', // 媒体服务
      mediaServerPath: null, // 媒体服务地址
      fileList: [], // 设备列表
      chooseRecord: null, // 媒体服务

      updateLooper: 0, // 数据刷新轮训标志
      winHeight: window.innerHeight - 180,
      loading: false

    }
  },
  computed: {},
  mounted() {
    this.initData()
  },
  destroyed() {
    this.$destroy('recordVideoPlayer')
  },
  methods: {
    initData: function() {
      this.getFileList()
    },
    getFileList: function() {
      this.$store.dispatch('log/queryList', {
        query: this.search,
        startTime: this.startTime,
        endTime: this.endTime
      })
        .then((data) => {
          this.fileList = data
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    showLogView(file) {
      this.playerTitle = '正在加载日志...'
      this.fileUrl = `/api/log/file/${file.fileName}`
      this.showLog = true
      this.file = file
    },
    downloadFile(file) {
      // const link = document.createElement('a');
      // link.target = "_blank";
      // link.download = file.fileName;
      // if (process.env.NODE_ENV === 'development') {
      //   link.href = `/debug/api/log/file/${file.fileName}`
      // }else {
      //   link.href = `/api/log/file/${file.fileName}`
      // }
      //
      // link.click();

      // 文件下载地址
      const fileUrl = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : window.baseUrl) + `/api/log/file/${file.fileName}`

      // 设置请求头
      const headers = new Headers()
      headers.append('access-token', getToken()) // 设置授权头，替换YourAccessToken为实际的访问令牌
      // 发起  请求
      fetch(fileUrl, {
        method: 'GET',
        headers: headers
      })
        .then(response => response.blob())
        .then(blob => {
          console.log(blob)
          // 创建一个虚拟的链接元素，模拟点击下载
          const link = document.createElement('a')
          link.target = '_blank'
          link.href = window.URL.createObjectURL(blob)
          link.download = file.fileName // 设置下载文件名，替换filename.ext为实际的文件名和扩展名
          document.body.appendChild(link)

          // 模拟点击
          link.click()

          // 移除虚拟链接元素
          document.body.removeChild(link)
          this.$message.success('已申请截图', { closed: true })
        })
        .catch(error => console.error('下载失败：', error))
    },
    loadEnd() {
      this.playerTitle = this.file.fileName
    },
    deleteRecord() {
      // TODO
      const that = this
      this.$axios({
        method: 'delete',
        url: `/record_proxy/api/record/delete`,
        params: {
          page: that.currentPage,
          count: that.count
        }
      }).then(function(res) {
        console.log(res)
        if (res.data.code === 0) {
          that.total = res.data.data.total
          that.fileList = res.data.data.list
        }
      }).catch(function(error) {
        console.log(error)
      })
    },
    formatTime(time) {
      const h = parseInt(time / 3600 / 1000)
      const minute = parseInt((time - h * 3600 * 1000) / 60 / 1000)
      let second = Math.ceil((time - h * 3600 * 1000 - minute * 60 * 1000) / 1000)
      if (second < 0) {
        second = 0
      }
      return (h > 0 ? h + `小时` : '') + (minute > 0 ? minute + '分' : '') + (second > 0 ? second + '秒' : '')
    },
    formatTimeStamp(time) {
      return moment.unix(time / 1000).format('yyyy-MM-DD HH:mm:ss')
    },
    formatFileSize(fileSize) {
      if (fileSize < 1024) {
        return fileSize + 'B'
      } else if (fileSize < (1024 * 1024)) {
        let temp = fileSize / 1024
        temp = temp.toFixed(2)
        return temp + 'KB'
      } else if (fileSize < (1024 * 1024 * 1024)) {
        let temp = fileSize / (1024 * 1024)
        temp = temp.toFixed(2)
        return temp + 'MB'
      } else {
        let temp = fileSize / (1024 * 1024 * 1024)
        temp = temp.toFixed(2)
        return temp + 'GB'
      }
    }

  }
}
</script>

<style>

</style>
