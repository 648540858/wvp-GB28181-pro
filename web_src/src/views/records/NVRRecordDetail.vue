<template>
  <div>
    <a-row :gutter="[16, 0]">
      <a-col :span="8">
        <a-card :bordered="false" style="margin-bottom: 0.5rem">
          <a-space :size="20">
            <div>选择日期：
              <a-date-picker v-model="chooseDate" placeholder="请选择日期" :style="{'width':'10rem'}" @change="dateChange">
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
            <span class="table-page-search-submitButtons" :style="{ float: 'right', overflow: 'hidden'} ">
              <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
              <a-button style="margin-left: 8px" @click="handleGoBack">返回</a-button>
              <a-button icon="edit" style="margin-left: 8px" @click="drawerOpen">操作</a-button>
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

    <a-drawer
      title="录像下载"
      :visible.sync="drawer"
      :direction="direction"
      width = "600"
      :destroyOnClose ="true"
      @close="drawerClose">
      <div class="drawer-box">

        <a-button type="primary" icon="plus" @click="addTask" style="display:block;margin:0 auto">添加</a-button>
        <a-divider />
        <a-tabs style="height: 100%" v-model="tabVal" @change="tabClick">

          <a-tab-pane key="1">
            <span slot="tab">
               <a-icon type="scissor" />
                进行中
            </span>
            <ul class="task-list">
              <li class="task-list-item" v-for="item in taskListForRuning">
                <div class="task-list-item-box">
                  <span>{{ item.startTime }}   -  {{item.endTime}}</span>
                  <a-progress :percentage="(parseFloat(item.percentage)*100).toFixed(1)"></a-progress>
                </div>
              </li>
            </ul>
          </a-tab-pane>

          <a-tab-pane key="2">
            <span slot="tab">
               <a-icon type="check" />
                已完成
            </span>
            <ul class="task-list">
              <li class="task-list-item" v-for="item in taskListEnded" style="margin-top: 8px">
                <div class="task-list-item-box" style="height: 2rem;line-height: 2rem;">
                  <span>{{ item.startTime}} - {{item.endTime}}</span>
                  &nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp
                  <a @click="deleteTask(item)">删除</a>
                  <a-divider type="vertical" />
                  <a :href="downloadUrl" download @click="downTask(item.recordFile)">下载</a>

                </div>
              </li>
            </ul>
          </a-tab-pane>

        </a-tabs>
      </div>
    </a-drawer>
    <a-modal title="选择时间段" :visible.sync="showTaskBox"
             :destroyOnClose ="true"
             :footer="null"
             @cancel="closeMode"
    >
      <a-range-picker
        v-model="taskTimeRange"
        range-separator="至"
        format="YYYY-MM-DD HH:mm:ss"
        showTime
        style="width: 20rem"
      >
      </a-range-picker>
      <a-button style="margin-left: 10px" type="primary" icon="check-circle" :size="size" @click="addTaskToServer">确认</a-button>
    </a-modal>

  </div>
</template>

<script>
import {STable} from '@/components'
import {addDownLoadTask, deleteDownLoadTask, queryRecordDetails, recordDateList} from "@/api/recordList";
import moment from "moment";
import easyPlayer from "@/components/VideoPlayer/easyPlayer";

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

export default {
  name: "NVRRecordDetail",
  components: {
    STable,
    easyPlayer
  },
  props: {
    record: {
      type: [Object, String],
      default: ''
    }
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
      showTaskBox: false,
      startTime: null,
      endTime: null,
      direction: "ltr",
      drawer: false,
      taskTimeRange: [],
      taskListEnded: [],
      taskListForRuning: [],
      taskUpdate: null,
      selectedRowKeys: [],
      selectedRows: [],
      tabVal: '1',
      dateFilesObj: [],
      chooseDate: '',
      videoUrl: '',
      basePath: process.env.NODE_ENV === 'development' ? `${location.origin}/debug/zlm` : `${location.origin}/zlm`,
      playerStyle: {
        "margin": "auto",
        "margin-bottom": "20px",
        "height": window.innerHeight - 350 + "px",
      },
      downloadUrl: ''
    }
  },
  created() {
    console.log(this.record)
  },
  mounted() {
  },
  methods: {
    handleGoBack() {
      this.$emit('goBack')
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
    dateChange(date, dateStr) {
      this.chooseDate = dateStr
    },
    playVideo(record) {
      this.videoUrl = `${this.basePath}/${this.record.mediaServerId}/record/${this.record.app}/${this.record.stream}/${this.chooseDate}/${record.originalName}`
    },
    downloadVideo(record) {
      this.downloadUrl = `${this.basePath}/${this.record.mediaServerId}/record/${this.record.app}/${this.record.stream}/${this.chooseDate}/${record.originalName}`
    },
    drawerOpen(){
      this.drawer = true;
      if (this.taskUpdate != null) {
        window.clearInterval(this.taskUpdate)
      }
      this.taskUpdate = setInterval(()=>{
        this.getTaskList(this.tabVal === '2')
      }, 5000)
    },
    addTaskToServer(){
      let that = this;
      const requestParameter = Object.assign({})
      requestParameter.mediaServerId = this.record.mediaServerId
      requestParameter.app = this.record.app
      requestParameter.stream = this.record.stream
      requestParameter.startTime = moment(this.taskTimeRange[0]).format('YYYY-MM-DD HH:mm:ss')
      requestParameter.endTime = moment(this.taskTimeRange[1]).format('YYYY-MM-DD HH:mm:ss')
      return addDownLoadTask(requestParameter).then(res => {
        if (res.code === 0 && res.msg === "success") {
          that.showTaskBox = false
          that.getTaskList(false);
        }else {
          that.$message.error(res.data.msg);
        }
      }).catch(error =>{
        console.log(error);
      })
    },
    downTask(recordFile){
      this.downloadUrl = `${this.basePath}/${this.record.mediaServerId}/${recordFile}`
    },
    tabClick(){
      this.getTaskList(this.tabVal === '2')
    },
    addTask(){
      this.showTaskBox = true;
      let startTimeStr = this.chooseDate + " " + this.detailFiles[0].substr(0,8);
      let endTimeStr = this.chooseDate + " " + this.detailFiles[this.detailFiles.length - 1].substr(9,17);
      this.taskTimeRange[0] = new Date(startTimeStr)
      this.taskTimeRange[1] = new Date(endTimeStr)
    },
    drawerClose(){
      this.drawer = false;
      if (this.taskUpdate != null) {
        window.clearInterval(this.taskUpdate)
      }
    },
    closeMode(){
      let that = this;
      that.showTaskBox = false
    },
    deleteTask(row){
      let that = this;
      const requestParameter = Object.assign({})
      requestParameter.mediaServerId = this.record.mediaServerId
      requestParameter.recordFilePath = row.recordFile.replaceAll("\\","/")
      requestParameter.id = row.id
      requestParameter.stream = row.stream
      return deleteDownLoadTask(requestParameter).then(res => {
        if (res.code == 200 ) {

        }else {
          that.$message.error(res.msg);
        }
      }).catch(error =>{
        console.log(error);
      })


    }
  }
}
</script>

<style scoped>

</style>