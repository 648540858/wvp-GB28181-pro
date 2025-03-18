<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <div >云端录像</div>
      </div>

      <div class="page-header-btn">
        搜索:
        <el-input @input="initData" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                  prefix-icon="el-icon-search" v-model="search"  clearable></el-input>
        开始时间:
        <el-date-picker
            v-model="startTime"
            type="datetime"
            size="mini"
            value-format="yyyy-MM-dd HH:mm:ss"
            @change="initData"
            placeholder="选择日期时间">
        </el-date-picker>
        结束时间:
        <el-date-picker
            v-model="endTime"
            type="datetime"
            size="mini"
            value-format="yyyy-MM-dd HH:mm:ss"
            @change="initData"
            placeholder="选择日期时间">
        </el-date-picker>
        节点选择:
        <el-select size="mini" @change="initData" style="width: 16rem; margin-right: 1rem;"
                   v-model="mediaServerId" placeholder="请选择" >
          <el-option label="全部" value=""></el-option>
          <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id">
          </el-option>
        </el-select>
<!--        <el-button size="mini" icon="el-icon-delete" type="danger" @click="deleteRecord()">批量删除</el-button>-->
        <el-button icon="el-icon-refresh-right" circle size="mini" :loading="loading"
                   @click="initData()"></el-button>
      </div>
    </div>
    <!--设备列表-->
    <el-table size="medium"  :data="recordList" style="width: 100%" :height="$tableHeght">
      <el-table-column
        type="selection"
        width="55">
      </el-table-column>
      <el-table-column prop="app" label="应用名">
      </el-table-column>
      <el-table-column prop="stream" label="流ID" width="380">
      </el-table-column>
      <el-table-column label="开始时间">
        <template v-slot:default="scope">
          {{formatTimeStamp(scope.row.startTime)}}
        </template>
      </el-table-column>
      <el-table-column label="结束时间">
        <template v-slot:default="scope">
          {{formatTimeStamp(scope.row.endTime)}}
        </template>
      </el-table-column>
      <el-table-column  label="时长">
        <template v-slot:default="scope">
          <el-tag v-if="Vue.prototype.$myServerId !== scope.row.serverId" style="border-color: #ecf1af">{{formatTime(scope.row.timeLen)}}</el-tag>
          <el-tag v-if="Vue.prototype.$myServerId === scope.row.serverId">{{formatTime(scope.row.timeLen)}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名称">
      </el-table-column>
      <el-table-column prop="mediaServerId" label="流媒体">
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template v-slot:default="scope">
          <el-button size="medium" icon="el-icon-video-play" type="text" @click="play(scope.row)">播放
          </el-button>
          <el-button size="medium" icon="el-icon-download" type="text" @click="downloadFile(scope.row)">下载
          </el-button>
          <!--            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c"-->
          <!--                       @click="deleteRecord(scope.row)">删除-->
          <!--            </el-button>-->
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="text-align: right"
      @size-change="handleSizeChange"
      @current-change="currentChange"
      :current-page="currentPage"
      :page-size="count"
      :page-sizes="[15, 25, 35, 50]"
      layout="total, sizes, prev, pager, next"
      :total="total">
    </el-pagination>
    <el-dialog
      :title="playerTitle"
      :visible.sync="showPlayer"
      width="50%">
      <easyPlayer ref="recordVideoPlayer" :videoUrl="videoUrl" :height="false"  ></easyPlayer>
    </el-dialog>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import MediaServer from './service/MediaServer'
import easyPlayer from './common/easyPlayer.vue'
import moment  from 'moment'
import Vue from "vue";

export default {
  name: 'app',
  components: {
    uiHeader,easyPlayer
  },
  data() {
    return {
      search: '',
      startTime: '',
      endTime: '',
      showPlayer: false,
      playerTitle: '',
      videoUrl: '',
      playerStyle: {
          "margin": "auto",
          "margin-bottom": "20px",
          "width": window.innerWidth/2 + "px",
          "height": this.winHeight/2 + "px",
      },
      mediaServerList: [], // 滅体节点列表
      mediaServerId: "", // 媒体服务
      mediaServerPath: null, // 媒体服务地址
      recordList: [], // 设备列表
      chooseRecord: null, // 媒体服务
      updateLooper: 0, //数据刷新轮训标志
      winHeight: window.innerHeight - 250,
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      mediaServerObj: new MediaServer(),

    };
  },
  computed: {
    Vue() {
      return Vue
    },
  },
  mounted() {
    this.initData();
    this.getMediaServerList();
  },
  destroyed() {
      this.$destroy('recordVideoPlayer');
  },
  methods: {
    initData: function () {
      this.getRecordList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.getRecordList();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getRecordList();
    },
    getMediaServerList: function () {
      let that = this;
      that.mediaServerObj.getOnlineMediaServerList((data) => {
        that.mediaServerList = data.data;
      })
    },
    setMediaServerPath: function (serverId) {
      let that = this;
      let i;
      for (i = 0; i < that.mediaServerList.length; i++) {
        if (serverId === that.mediaServerList[i].id) {
          break;
        }
      }
      let port = that.mediaServerList[i].httpPort;
      if (location.protocol === "https:" && that.mediaServerList[i].httpSSlPort) {
        port = that.mediaServerList[i].httpSSlPort
      }
      that.mediaServerPath = location.protocol + "//" + that.mediaServerList[i].streamIp + ":" + port
      console.log(that.mediaServerPath)
    },
    getRecordList: function () {
      this.$axios({
        method: 'get',
        url: `/api/cloud/record/list`,
        params: {
          app: '',
          stream: '',
          query: this.search,
          startTime: this.startTime,
          endTime: this.endTime,
          mediaServerId: this.mediaServerId,
          page: this.currentPage,
          count: this.count
        }
      }).then((res) => {
        console.log(res)
        if (res.data.code === 0) {
          this.total = res.data.data.total;
          this.recordList = res.data.data.list;
        }
        this.loading = false;
      }).catch((error) => {
        console.log(error);
        this.loading = false;
      });
    },
    play(row) {
      console.log(row)
      this.chooseRecord = row;
      this.showPlayer = true;
      this.$axios({
        method: 'get',
        url: `/api/cloud/record/play/path`,
        params: {
          recordId: row.id,
        }
      }).then((res) => {
        console.log(res)
        if (res.data.code === 0) {
          if (location.protocol === "https:") {
            this.videoUrl = res.data.data.httpsPath;
          }else {
            this.videoUrl = res.data.data.httpPath;
          }
          console.log(222 )
          console.log(this.videoUrl )
        }
      }).catch((error) => {
        console.log(error);
      });
    },
    downloadFile(file){
      console.log(file)
      this.$axios({
        method: 'get',
        url: `/api/cloud/record/play/path`,
        params: {
          recordId: file.id,
        }
      }).then((res) => {
        console.log(res)
        const link = document.createElement('a');
        link.target = "_blank";
        if (res.data.code === 0) {
          if (location.protocol === "https:") {
            link.href = res.data.data.httpsPath + "&save_name=" + file.fileName;
          }else {
            link.href = res.data.data.httpPath + "&save_name=" + file.fileName;
          }
          link.click();
        }
      }).catch((error) => {
        console.log(error);
      });
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
          that.recordList = res.data.data.list;
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
      return moment.unix(time/1000).format('yyyy-MM-DD HH:mm:ss')
    }

  }
};
</script>

<style>

</style>
