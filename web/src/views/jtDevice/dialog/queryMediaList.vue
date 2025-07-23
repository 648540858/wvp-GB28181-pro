<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="存储多媒体数据检索"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
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
            v-model="chanelId"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="请选择通道"
            default-first-option
          >
            <el-option label="平台下发指令" :value="0" />
            <el-option label="定时动作" :value="1" />
            <el-option label="抢劫报警触发" :value="2" />
            <el-option label="碰撞侧翻报警触发" :value="3" />
          </el-select>
          <el-select
            v-model="event"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="请选择事件"
            default-first-option
          >
            <el-option label="所有通道" :value="0" />
            <el-option v-for="item in channelList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            :picker-options="pickerOptions"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            align="right">
          </el-date-picker>

        </el-form-item>
        <el-form-item>
          <el-button type="primary"  icon="el-icon-search" @click="removeRow(scope.$index)" >
            检索
          </el-button>
        </el-form-item>
      </el-form>
      <el-table :data="mediaDataInfoList" :height="500" stripe style="width: 100%" empty-text="暂无数据">
        <el-table-column prop="ID" label="ID" />
        <el-table-column prop="type" label="类型" />
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
              style="color: #f56c6c"
              icon="el-icon-delete"
              :loading="scope.row.addRegionLoading"
              @click="removeRow(scope.$index)"
            >
              位置汇报
            </el-button>
            <el-button
              size="medium"
              type="text"
              style="color: #f56c6c"
              icon="el-icon-delete"
              :loading="scope.row.addRegionLoading"
              @click="removeRow(scope.$index)"
            >
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>

    </el-dialog>
  </div>
</template>

<script>

import * as XLSX from 'xlsx'
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      deviceId: null,
      phoneNumber: null,
      showDialog: false,
      type: 0,
      chanelId: 0,
      event: 0,
      channelList: [],
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
  created() {},
  methods: {
    openDialog: function(phoneNumber, deviceId) {
      this.showDialog = true
      this.phoneNumber = phoneNumber
      this.deviceId = deviceId
      this.mediaDataInfoList = []
      this.$store.dispatch('jtDevice/queryChannels', {
        page: 1,
        count: 1000,
        deviceId: this.deviceId
      })
        .then(data => {
          this.channelList = data.list
        })
    },
    close: function() {
      this.showDialog = false
      this.mediaDataInfoList = []
      this.channelList = []
      this.type = 0
      this.chanelId = 0
      this.event = 0
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
    }
  }
}
</script>

<style scoped>
>>> .el-upload {
  width: 100% !important;
}
</style>
