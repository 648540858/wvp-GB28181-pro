<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row type="flex" :gutter="[16, 0]">
            <a-col :flex="1">
              <span style="font-size: medium;">NVR录像信息列表</span>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="录像日期" style="float: right">
                <a-date-picker placeholder="选择录像日期" v-model="date"/>
              </a-form-item>
            </a-col>
            <a-col>
              <span class="table-page-search-submitButtons">
                <a-button type="primary" @click="initData">查询</a-button>
                <a-button style="margin: 0 8px " @click="() => this.date = null">重置</a-button>
                <a-button @click="goBack">返回</a-button>
              </span>
            </a-col>
          </a-row>
        </a-form>
      </div>
    </a-card>

    <a-card :bordered="false">
      <a-table
        ref="table"
        size="default"
        :rowKey="(record) => record.filePath "
        :columns="columns"
        :data-source="videoRecords"
      >

        <span slot="status" slot-scope="text">
          <a-badge :status="text | statusTypeFilter" :text="text | statusFilter"/>
        </span>

        <span slot="action" slot-scope="text, record">
          <a-button-group>
            <a-button size="small" @click="playRecord(record)">
              <font-awesome-icon :icon="['fas','play']" style="margin-right: 0.25rem;font-size: 12px"/>
              播放
            </a-button>
            <a-button size="small" type="primary" @click="downloadRecord(record)">
              <font-awesome-icon :icon="['fas','hdd']" style="margin-right: 0.25rem;font-size: 12px"/>
              缓存录像
            </a-button>
          </a-button-group>
        </span>
      </a-table>
    </a-card>
    <record-player ref="recordPlayer"/>
  </div>
</template>

<script>
import {queryRecords} from "@/api/deviceList";
import moment from "moment";
import RecordPlayer from './RecordPlayer'
import {STable} from "@/components";

const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '文件',
    align: 'center',
    dataIndex: 'filePath'
  },
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
    title: '操作',
    dataIndex: 'action',
    align: 'center',
    scopedSlots: {customRender: 'action'}
  }]
export default {
  name: "GBRecords",
  components: {
    RecordPlayer,
    STable
  },
  props: {
    record: {
      type: [Object, String],
      default: ''
    }
  },
  data() {
    return {
      columns,
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      videoRecords: [],
      date: null,
      isDownloading: false
    }
  },
  created() {
  },
  mounted() {
    //节点挂载成功后去查询录像
    this.initData()
  },
  methods: {
    initData() {
      if (!this.date) {
        this.date = moment()
      }
      let paramsDate = this.date.format('YYYY-M-D')
      console.log('查询 ' + paramsDate + ' NVR上录像记录')
      let params = {
        deviceId: this.record.deviceId,
        channelId: this.record.channelId,
        startTime: paramsDate + " 00:00:00",
        endTime: paramsDate + " 23:59:59"
      }
      queryRecords(params).then(res => {
        this.videoRecords = res.recordList
      })
    },
    goBack() {
      this.$emit('goChannelPage', this.record)
    },

    playRecord(row) {
      this.$refs.recordPlayer.openDialog(row, this.record, 'play')
    },

    downloadRecord(row) {
      this.$refs.recordPlayer.openDialog(row, this.record, 'download')
    }
  }
}
</script>

<style scoped>

</style>