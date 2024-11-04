<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <div>历史日志</div>
      </div>

      <div class="page-header-btn">
        搜索:
        <el-input @input="getFileList" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                  prefix-icon="el-icon-search" v-model="search" clearable></el-input>
        开始时间:
        <el-date-picker
          size="mini"
          v-model="startTime"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          @change="getFileList"
          placeholder="选择日期时间">
        </el-date-picker>
        结束时间:
        <el-date-picker
          size="mini"
          v-model="endTime"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          @change="getFileList"
          placeholder="选择日期时间">
        </el-date-picker>
        <!--        <el-button size="mini" icon="el-icon-delete" type="danger" @click="deleteRecord()">批量删除</el-button>-->
        <el-button icon="el-icon-refresh-right" circle size="mini" :loading="loading"
                   @click="getFileList()"></el-button>
      </div>
    </div>
    <!--日志列表-->
    <el-table size="medium" :data="fileList" style="width: 100%" :height="winHeight">
      <el-table-column
        type="selection"
        width="55">
      </el-table-column>
      <el-table-column prop="fileName" label="文件名">
      </el-table-column>
      <el-table-column prop="fileSize" label="文件大小">
        <template slot-scope="scope">
          {{formatFileSize(scope.row.fileSize)}}
        </template>
      </el-table-column>
      <el-table-column label="开始时间">
        <template slot-scope="scope">
          {{formatTimeStamp(scope.row.startTime)}}
        </template>
      </el-table-column>
      <el-table-column label="结束时间">
        <template slot-scope="scope">
          {{formatTimeStamp(scope.row.endTime)}}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template slot-scope="scope">
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
    <el-dialog
      top="10"
      :title="playerTitle"
      :visible.sync="showLog"
      width="90%">
      <operationsFoShowLog ref="recordVideoPlayer" :fileUrl="fileUrl" :loadEnd="loadEnd"></operationsFoShowLog>
    </el-dialog>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import MediaServer from './service/MediaServer'
import operationsFoShowLog from './dialog/operationsFoShowLog.vue'
import moment from 'moment'

export default {
  name: 'app',
  components: {
    uiHeader, operationsFoShowLog
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
        "margin": "auto",
        "margin-bottom": "20px",
        "width": window.innerWidth / 2 + "px",
        "height": this.winHeight / 2 + "px",
      },
      mediaServerList: [], // 滅体节点列表
      mediaServerId: "", // 媒体服务
      mediaServerPath: null, // 媒体服务地址
      fileList: [], // 设备列表
      chooseRecord: null, // 媒体服务

      updateLooper: 0, //数据刷新轮训标志
      winHeight: window.innerHeight - 250,
      loading: false,
      mediaServerObj: new MediaServer(),

    };
  },
  computed: {},
  mounted() {
    this.initData();
  },
  destroyed() {
    this.$destroy('recordVideoPlayer');
  },
  methods: {
    initData: function () {
      this.getFileList();
    },
    getFileList: function () {
      this.$axios({
        method: 'get',
        url: `/api/log/list`,
        params: {
          query: this.search,
          startTime: this.startTime,
          endTime: this.endTime,
        }
      }).then((res) => {
        console.log(res)
        if (res.data.code === 0) {
          this.fileList = res.data.data;
        }
        this.loading = false;
      }).catch((error) => {
        console.log(error);
        this.loading = false;
      });
    },
    showLogView(file) {
      this.playerTitle = '正在加载日志...'
      this.fileUrl = `/api/log/file/${file.fileName}`
      this.showLog = true
      this.file = file

    },
    downloadFile(file) {
      const link = document.createElement('a');
      link.target = "_blank";
      link.download = file.fileName;
      if (process.env.NODE_ENV === 'development') {
        link.href = `/debug/api/log/file/${file.fileName}`
      }else {
        link.href = `/api/log/file/${file.fileName}`
      }

      link.click();
    },
    loadEnd() {
      this.playerTitle = this.file.fileName
    },
    deleteRecord() {
      // TODO
      let that = this;
      this.$axios({
        method: 'delete',
        url: `/record_proxy/api/record/delete`,
        params: {
          page: that.currentPage,
          count: that.count
        }
      }).then(function (res) {
        console.log(res)
        if (res.data.code === 0) {
          that.total = res.data.data.total;
          that.fileList = res.data.data.list;
        }
      }).catch(function (error) {
        console.log(error);
      });
    },
    formatTime(time) {
      const h = parseInt(time / 3600 / 1000)
      const minute = parseInt((time - h * 3600 * 1000) / 60 / 1000)
      let second = Math.ceil((time - h * 3600 * 1000 - minute * 60 * 1000) / 1000)
      if (second < 0) {
        second = 0;
      }
      return (h > 0 ? h + `小时` : '') + (minute > 0 ? minute + '分' : '') + (second > 0 ? second + '秒' : '')
    },
    formatTimeStamp(time) {
      return moment.unix(time / 1000).format('yyyy-MM-DD HH:mm:ss')
    },
    formatFileSize(fileSize) {
      if (fileSize < 1024) {
        return fileSize + 'B';
      } else if (fileSize < (1024*1024)) {
        let temp = fileSize / 1024;
        temp = temp.toFixed(2);
        return temp + 'KB';
      } else if (fileSize < (1024*1024*1024)) {
        let temp = fileSize / (1024*1024);
        temp = temp.toFixed(2);
        return temp + 'MB';
      } else {
        let temp = fileSize / (1024*1024*1024);
        temp = temp.toFixed(2);
        return temp + 'GB';
      }
    }


  }
};
</script>

<style>

</style>
