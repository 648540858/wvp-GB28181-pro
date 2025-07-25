<template>
  <div id="configInfo">
    <el-form :inline="true" size="mini" @submit.native.prevent>
      <el-form-item>
        <el-select
          v-model="type"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="请选择类型"
          default-first-option
        >
          <el-option label="图像" :value="0" />
          <el-option label="音频" :value="1" />
          <el-option label="视频" :value="2" />
        </el-select>
        <el-select
          v-model="event"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="请选择事件"
          default-first-option
        >
          <el-option label="平台下发指令" :value="0" />
          <el-option label="定时动作" :value="1" />
          <el-option label="抢劫报警触发" :value="2" />
          <el-option label="碰撞侧翻报警触发" :value="3" />
        </el-select>
        <el-select
          v-model="chanelId"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="请选择通道"
          default-first-option
        >
          <el-option label="所有通道" :value="0" />
          <el-option v-for="item in channelList" :key="item.channelId" :label="item.name" :value="item.channelId" />
        </el-select>
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          format="yyyy-MM-dd HH-mm-ss"
          value-format="yyyy-MM-dd HH:mm:ss"
          :picker-options="pickerOptions"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          align="right">
        </el-date-picker>

      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="queryLoading" icon="el-icon-search" @click="search()" >
          检索
        </el-button>
      </el-form-item>
    </el-form>
    <el-table :data="mediaDataInfoList" :height="500" stripe style="width: 100%" empty-text="暂无数据">
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="type" label="类型" >
        <template v-slot:default="scope">
          {{typeLabel(scope.row.type)}}
        </template>
      </el-table-column>
      <el-table-column label="事件" >
        <template v-slot:default="scope">
          {{eventCodeLabel(scope.row.eventCode)}}
        </template>
      </el-table-column>
      <el-table-column prop="channelId" label="通道ID" />
      <el-table-column label="操作" fixed="right">
        <template v-slot:default="scope">
          <el-button
            size="medium"
            type="text"
            icon="el-icon-location-information"
            :loading="scope.row.addRegionLoading"
            @click="showPositionInfo(scope.row)"
          >
            位置汇报
          </el-button>
          <el-button
            size="medium"
            type="text"
            icon="el-icon-download"
            :loading="scope.row.addRegionLoading"
            @click="download(scope.row)"
          >
            下载
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <position ref="position"></position>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import position from './position.vue'
import dayjs from 'dayjs'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  components: { position },
  props: ['phoneNumber', 'deviceId', 'channelList'],
  data() {
    return {
      queryLoading: false,
      type: 0,
      chanelId: 0,
      event: 0,
      mediaDataInfoList: [],
      timeRange: '',
      pickerOptions: {
        shortcuts: [
          {
            text: '最近24小时',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近一周',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近一个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近三个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
              picker.$emit('pick', [start, end])
          }
        }]
      }
    }
  },
  computed: {},
  created() {

  },
  methods: {
    close: function() {
      this.mediaDataInfoList = []
      this.channelList = []
      this.type = 0
      this.chanelId = 0
      this.event = 0
    },
    typeLabel: function(type) {
      switch (type){
        case 0:
          return '图像'
        case 1:
          return '音频'
        case 2:
          return '视频'
        default:
          return type
      }
    },
    eventCodeLabel: function(eventCode) {
      switch (eventCode){
        case 0:
          return '平台下发指令'
        case 1:
          return '定时动作'
        case 2:
          return '抢劫报警触发'
        case 3:
          return '碰撞侧翻报警触发'
        case 4:
          return '门开拍照'
        case 5:
          return '门关拍照'
        case 6:
          return '车门由开变关, 车速从小于20km到超过20km'
        case 7:
          return '定距拍照'
        default:
          return eventCode
      }
    },
    search: function() {
      this.mediaDataInfoList = []
      this.queryLoading = true
      this.$store.dispatch('jtDevice/queryMediaData', {
        phoneNumber: this.phoneNumber,
        queryMediaDataCommand: {
          type: this.type,
          channelId: this.chanelId,
          event: this.event,
          startTime: this.timeRange === '' ? null : this.timeRange[0],
          endTime: this.timeRange === '' ? null : this.timeRange[1]
        }
      })
        .then(data => {
          console.log(data)
          if (data.length === 0) {
            this.$message.info('未查询到相关记录')
          }
          this.mediaDataInfoList = data
        })
        .finally(() => {
          this.queryLoading = false
        })
    },
    showPositionInfo: function(row) {
      this.$refs.position.openDialog(row.positionBaseInfo)
    },
    download: function(row) {
      this.$message.success('下载请求已发送', { closed: true })
      // 文件下载地址
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      const fileUrl = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + `/api/jt1078/media/upload/one/upload?phoneNumber=${this.phoneNumber}&mediaId=${row.id}`
      let controller = new AbortController()
      let signal = controller.signal
      // 设置请求头
      const headers = new Headers()
      headers.append('access-token', this.$store.getters.token) // 设置授权头，替换YourAccessToken为实际的访问令牌
      // 发起  请求
      fetch(fileUrl, {
        method: 'GET',
        headers: headers,
        signal: signal
      })
        .then(response => response.blob())
        .then(blob => {
          console.log(blob)
          // 创建一个虚拟的链接元素，模拟点击下载
          const link = document.createElement('a')
          link.href = window.URL.createObjectURL(blob)
          let suffix = 'jpg'
          switch (row.type){
            case 0:
              suffix = 'jpg'
              break
            case 1:
              suffix = 'mp3'
              break
            case 2:
              suffix = 'mp4'
              break
          }
          link.download = `${row.id}.${suffix}` // 设置下载文件名，替换filename.ext为实际的文件名和扩展名
          document.body.appendChild(link)
          // 模拟点击
          link.click()
          // 移除虚拟链接元素
          document.body.removeChild(link)
        })
        .catch(error => console.error('下载失败：', error))

      setTimeout(() => {
        this.$message.error('下载超时', { closed: true })
        controller.abort('timeout')
      }, 15000)
    }
  }
}
</script>

<style scoped>
>>> .el-upload {
  width: 100% !important;
}
.el-slider__marks-text {
  margin-top: -36px;
  font-size: 12px;
  width: 2rem !important;
}
</style>
