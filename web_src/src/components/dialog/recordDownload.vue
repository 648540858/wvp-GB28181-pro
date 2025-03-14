<template>
<div id="recordDownload" >
  <el-dialog :title="title" v-if="showDialog"  width="45rem" :append-to-body="true" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()" center>
    <el-row>
      <el-col :span="18" style="padding-top: 7px;">
        <el-progress :percentage="percentage"></el-progress>
      </el-col>
      <el-col :span="6" >
        <el-button icon="el-icon-download" v-if="downloadFile" size="mini" title="点击下载" @click="downloadFileClientEvent()">下载</el-button>
      </el-col>
    </el-row>
  </el-dialog>
</div>
</template>


<script>

import moment from "moment";

export default {
    name: 'recordDownload',
    created() {
      window.addEventListener('beforeunload', this.stopDownloadRecord)

    },
    data() {
        return {
          title: "下载中...",
          deviceId: "",
          channelId: "",
          app: "",
          stream: "",
          mediaServerId: "",
          showDialog: false,
          scale: 1,
          percentage: 0.00,
          streamInfo: null,
          taskId: null,
          getProgressRun: false,
          timer: null,
          downloadFile: null,
        };
    },
    methods: {
        openDialog: function (deviceId, channelId, app, stream, mediaServerId) {
            this.deviceId = deviceId;
            this.channelId = channelId;
            this.app = app;
            this.stream = stream;
            this.mediaServerId = mediaServerId;
            this.showDialog = true;
            this.getProgressRun = true;
            this.percentage = 0.0;
            this.downloadFile = null;
            this.getProgressTimer()
        },
        getProgressTimer: function (){
          if (!this.getProgressRun) {
            return;
          }
          if (this.downloadFile) {
            return;
          }
          setTimeout( ()=>{
            if (!this.showDialog) return;
            this.getProgress(this.getProgressTimer)
          }, 5000)
        },
        getProgress: function (callback){
          this.$axios({
            method: 'get',
            url: `/api/gb_record/download/progress/${this.deviceId}/${this.channelId}/${this.stream}`
          }).then((res)=> {
              if (res.data.code === 0) {
                this.streamInfo = res.data.data;
                if (parseFloat(res.data.progress) === 1) {
                  this.percentage = 100;
                }else {
                  this.percentage = (parseFloat(res.data.data.progress)*100).toFixed(1);
                }
                if (this.streamInfo.downLoadFilePath) {
                  if (location.protocol === "https:") {
                    this.downloadFile = this.streamInfo.downLoadFilePath.httpsPath;
                  }else {
                    this.downloadFile = this.streamInfo.downLoadFilePath.httpPath;
                  }
                  this.percentage = 100
                  this.getProgressRun = false;
                  this.downloadFileClientEvent()
                }
                if (callback)callback();
              }else {
                this.$message({
                  showClose: true,
                  message: res.data.msg,
                  type: "error",
                });
                this.close();
              }

          }).catch((e) =>{
            console.log(e)
          });
        },
        close: function (){
          if (this.streamInfo.progress < 1) {
            this.stopDownloadRecord();
          }

          if (this.timer !== null) {
            window.clearTimeout(this.timer);
            this.timer = null;
          }
          this.showDialog=false;
          this.getProgressRun = false;
        },
        gbScale: function (scale){
          this.scale = scale;
        },

        stopDownloadRecord: function (callback) {
          if (this.deviceId && this.channelId && this.stream) {
            this.$axios({
              method: 'get',
              url: '/api/gb_record/download/stop/' + this.deviceId + "/" + this.channelId+ "/" + this.stream
            }).then((res)=> {
              if (callback) callback(res)
            });
          }
        },
      downloadFileClientEvent: function (){
        // window.open(this.downloadFile )

        let x = new XMLHttpRequest();
        x.open("GET", this.downloadFile, true);
        x.responseType = 'blob';
        x.onload=(e)=> {
          let url = window.URL.createObjectURL(x.response)
          let a = document.createElement('a');
          a.href = url
          a.download = this.deviceId + "-" + this.channelId + ".mp4";
          a.click()
        }
        x.send();
      }
    },
    destroyed() {
      window.removeEventListener('beforeunload', this.stopDownloadRecord)
    }
};
</script>

<style>

</style>
