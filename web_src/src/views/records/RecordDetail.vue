<template>
  <div>
    <a-row :gutter="[16, 0]">
      <a-col :span="8">
        <a-card :bordered="false" style="margin-bottom: 0.5rem">
          <a-space :size="20">
            <div>选择日期：
              <a-date-picker v-model="chooseDate" placeholder="请选择日期" :style="{'width':'15rem'}" @change="dateChange">
                <template slot="dateRender" slot-scope="current, today">
                  <div class="ant-calendar-date" :style="getCurrentStyle(current, today)">
                    {{ current.date() }}
                  </div>
                </template>
                <template slot="renderExtraFooter">
                  <strong style="font-size: 12px;color: #f5222d">注意：蓝色圆圈代表当天有录像记录</strong>
                </template>
              </a-date-picker>
            </div>
            <span class="table-page-search-submitButtons" :style="{ float: 'right', overflow: 'hidden' } ">
              <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
              <a-button style="margin-left: 8px" @click="handleGoBack">返回</a-button>
            </span>
          </a-space>
        </a-card>
        <a-card :bordered="false">
          <s-table
            ref="table"
            size="default"
            rowKey="key"
            :columns="columns"
            :data="loadData"
            showPagination="auto"
          >
            <span slot="action" slot-scope="text, record">
              <a @click="playVideo(record)">播放</a>
              <a-divider type="vertical"/>
              <a :href="downloadUrl" download @click="downloadVideo(record)">下载</a>
            </span>
          </s-table>
        </a-card>
      </a-col>
      <a-col :span="16">
        <a-card :bordered="false">
          <div :style="playerStyle">
            <easy-player ref="recordVideoPlayer" :videoUrl="videoUrl" fluent autoplay :height="true"></easy-player>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import {Ellipsis, STable} from '@/components'
import {queryRecordDetails, recordDateList} from "@/api/recordList";
import easyPlayer from "@/components/VideoPlayer/easyPlayer";
import moment from "moment";

const columns = [
  {
    title: '开始时间',
    dataIndex: 'startTime',
    align: 'center'
  },
  {
    title: '结束时间',
    dataIndex: 'endTime',
    align: 'center'
  },
  {
    title: '时长 (分)',
    dataIndex: 'duration',
    align: 'center'
  },
  {
    title: '操作',
    dataIndex: 'action',
    align: 'center',
    scopedSlots: {customRender: 'action'}
  }
]

const statusMap = {
  0: {
    status: 'default',
    text: '离线'
  },
  1: {
    status: 'success',
    text: '在线'
  }
}

export default {
  name: 'TableList',
  components: {
    STable,
    Ellipsis,
    easyPlayer
  },
  props: {
    record: {}
  },
  data() {
    this.columns = columns
    return {
      // 查询参数
      queryParam: {},
      // 加载数据方法 必须为 Promise 对象
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        requestParameters.mediaServerId = this.record.mediaServerId
        requestParameters.app = this.record.app
        requestParameters.stream = this.record.stream
        return recordDateList(requestParameters).then(res => {
          this.dateFilesObj = res.data
          if (!this.chooseDate && this.dateFilesObj.length > 0) {
            this.chooseDate = this.dateFilesObj[this.dateFilesObj.length - 1];
          }
          requestParameters.startTime = this.chooseDate + " 00:00:00"
          requestParameters.endTime = this.chooseDate + " 23:59:59"
          return requestParameters
        }).then((params) => {
          return queryRecordDetails(params)
        }).then(res => {
          return res.data
        })
      },
      selectedRowKeys: [],
      selectedRows: [],
      dateFilesObj: [],
      chooseDate: '',
      detailFiles: [],
      videoUrl: '',
      downloadUrl: '',
      basePath: process.env.NODE_ENV === 'development' ? `${location.origin}/debug/zlm` : `${location.origin}/zlm`,
      playerStyle: {
        "margin": "auto",
        "margin-bottom": "20px",
        "height": window.innerHeight - 350 + "px",
      }
    }
  },
  methods: {
    handleGoBack() {
      this.$emit('goBack')
    },
    playVideo(record) {
      this.videoUrl = `${this.basePath}/${this.record.mediaServerId}/record/${this.record.app}/${this.record.stream}/${this.chooseDate}/${record.originalName}`
    },
    getCurrentStyle(current, today) {
      const style = {};
      // 通过显示一个圈识这一天有录像
      let date = moment(current).format('YYYY-MM-DD')
      if (this.dateFilesObj.includes(date)) {
        style.border = '1px solid #1890ff';
        style.borderRadius = '50%';
      }
      return style;
    },
    dateChange(date,dateStr){
      this.chooseDate = dateStr
    },
    downloadVideo(record) {
      this.downloadUrl = `${this.basePath}/${this.record.mediaServerId}/record/${this.record.app}/${this.record.stream}/${this.chooseDate}/${record.originalName}`
    }
  }
}
</script>
