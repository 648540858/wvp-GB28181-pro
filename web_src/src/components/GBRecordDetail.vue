<template>
	<div style="width: 100%">
    <div class="page-header" >
      <div class="page-title">
        <el-page-header @back="goBack" content="国标录像"></el-page-header>
      </div>
    </div>
		<el-container>
      <el-aside width="300px">
        <div class="record-list-box-box">
          <el-date-picker size="mini" v-model="chooseDate" type="date" value-format="yyyy-MM-dd" placeholder="日期" @change="dateChange()"></el-date-picker>
          <div class="record-list-box" v-loading="recordsLoading" :style="recordListStyle">
            <ul v-if="detailFiles.length >0" class="infinite-list record-list" >
              <li v-for="item in detailFiles" class="infinite-list-item record-list-item" >

                <el-tag v-if="chooseFile != item" @click="checkedFile(item)">
                  <i class="el-icon-video-camera"  ></i>
                  {{ moment(item.startTime).format('HH:mm:ss')}}-{{ moment(item.endTime).format('HH:mm:ss')}}
                </el-tag>
                <el-tag v-if="chooseFile == item" type="danger" >
                  <i class="el-icon-video-camera"  ></i>
                  {{ moment(item.startTime).format('HH:mm:ss')}}-{{ moment(item.endTime).format('HH:mm:ss')}}
                </el-tag>
                <i style="color: #409EFF;margin-left: 5px;" class="el-icon-download" @click="downloadRecord(item)" ></i>
              </li>
            </ul>
          </div>
          <div size="mini" v-if="detailFiles.length ==0" class="record-list-no-val" >暂无数据</div>
        </div>

      </el-aside>
			<el-main style="padding-bottom: 10px;">
        <div class="playBox" :style="playerStyle">
          <player ref="recordVideoPlayer"
                  :videoUrl="videoUrl"
                  :error="videoError"
                  :message="videoError"
                  :hasAudio="hasAudio"
                  style="max-height: 100%"
                  fluent autoplay live ></player>
        </div>
        <div class="player-option-box">
          <div>
            <el-button-group >
              <el-time-picker
                size="mini"
                is-range
                align="left"
                v-model="timeRange"
                value-format="yyyy-MM-dd HH:mm:ss"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                @change="timePickerChange"
                placeholder="选择时间范围">
              </el-time-picker>
            </el-button-group>

            <el-button-group >
              <el-button size="mini" class="iconfont icon-zanting" title="开始" @click="gbPause()"></el-button>
              <el-button size="mini" class="iconfont icon-kaishi" title="暂停" @click="gbPlay()"></el-button>
              <el-dropdown size="mini" title="播放倍速"  @command="gbScale">
                <el-button size="mini">
                  倍速 <i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-dropdown-menu  slot="dropdown">
                  <el-dropdown-item command="0.25">0.25倍速</el-dropdown-item>
                  <el-dropdown-item command="0.5">0.5倍速</el-dropdown-item>
                  <el-dropdown-item command="1.0">1倍速</el-dropdown-item>
                  <el-dropdown-item command="2.0">2倍速</el-dropdown-item>
                  <el-dropdown-item command="4.0">4倍速</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
              <el-button size="mini" class="iconfont icon-xiazai1" title="下载选定录像" @click="downloadRecord()"></el-button>
              <el-button v-if="sliderMIn === 0 && sliderMax === 86400" size="mini" class="iconfont icon-slider" title="放大滑块" @click="setSliderFit()"></el-button>
              <el-button v-if="sliderMIn !== 0 || sliderMax !== 86400" size="mini" class="iconfont icon-slider-right" title="恢复滑块" @click="setSliderFit()"></el-button>
            </el-button-group>
          </div>
          <el-slider
            class="playtime-slider"
            v-model="playTime"
            id="playtimeSlider"
            :disabled="detailFiles.length === 0"
            :min="sliderMIn"
            :max="sliderMax"
            :range="true"
            :format-tooltip="playTimeFormat"
            @change="playTimeChange"
            :marks="playTimeSliderMarks">
          </el-slider>
          <div class="slider-val-box">
            <div class="slider-val" v-for="item of detailFiles" :style="'width:' + getDataWidth(item) + '%; left:' + getDataLeft(item) + '%'"></div>
          </div>
        </div>

			</el-main>
		</el-container>
    <recordDownload ref="recordDownload"></recordDownload>
	</div>
</template>


<script>
	import uiHeader from '../layout/UiHeader.vue'
  import player from './common/jessibuca.vue'
  import moment  from 'moment'
  import recordDownload from './dialog/recordDownload.vue'
	export default {
		name: 'app',
		components: {
			uiHeader, player,recordDownload
		},
		data() {
			return {
        deviceId: this.$route.params.deviceId,
        channelId: this.$route.params.channelId,
        recordsLoading: false,
        streamId: "",
        hasAudio: false,
			  detailFiles: [],
        chooseDate: null,
        videoUrl: null,
        chooseFile: null,
        streamInfo: null,
        app: null,
        mediaServerId: null,
        ssrc: null,

        sliderMIn: 0,
        sliderMax: 86400,
        autoPlay: true,
        taskUpdate: null,
        tabVal: "running",
        recordListStyle: {
			    height: this.winHeight + "px",
          overflow: "auto",
          margin: "10px auto 10px auto"
        },
        playerStyle: {
			    "margin": "0 auto 20px auto",
          "height": this.winHeight + "px",
        },
        winHeight: window.innerHeight - 240,
        playTime: null,
        timeRange: null,
        startTime: null,
        endTime: null,
        playTimeSliderMarks: {
			    0: "00:00",
			    3600: "01:00",
			    7200: "02:00",
			    10800: "03:00",
			    14400: "04:00",
			    18000: "05:00",
			    21600: "06:00",
			    25200: "07:00",
			    28800: "08:00",
			    32400: "09:00",
			    36000: "10:00",
          39600: "11:00",
			    43200: "12:00",
			    46800: "13:00",
			    50400: "14:00",
			    54000: "15:00",
			    57600: "16:00",
			    61200: "17:00",
			    64800: "18:00",
			    68400: "19:00",
          72000: "20:00",
			    75600: "21:00",
			    79200: "22:00",
			    82800: "23:00",
          86400: "24:00",
        },
			};
		},
		computed: {

		},
		mounted() {
      this.recordListStyle.height = this.winHeight + "px";
      this.playerStyle["height"] = this.winHeight + "px";
      this.chooseDate = moment().format('YYYY-MM-DD')
      this.dateChange();
      window.addEventListener('beforeunload', this.stopPlayRecord)
		},
		destroyed() {
			this.$destroy('recordVideoPlayer');
      window.removeEventListener('beforeunload', this.stopPlayRecord)
		},
		methods: {
      dateChange(){
        if (!this.chooseDate) {
          return;
        }

        this.setTime(this.chooseDate + " 00:00:00", this.chooseDate + " 23:59:59");
        this.recordsLoading = true;
        this.detailFiles = [];
        this.$axios({
          method: 'get',
          url: '/api/gb_record/query/' + this.deviceId + '/' + this.channelId + '?startTime=' + this.startTime + '&endTime=' + this.endTime
        }).then((res)=>{
          this.recordsLoading = false;
          if(res.data.code === 0) {
            // 处理时间信息
            this.detailFiles = res.data.data.recordList;

          }else {
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: "error",
            });
          }

        }).catch((e)=> {
          this.recordsLoading = false;
          // that.videoHistory.searchHistoryResult = falsificationData.recordData;
        });
      },
      moment: function (v) {
        return moment(v)
      },
      setTime: function (startTime, endTime){
        this.startTime = startTime;
        this.endTime = endTime;
        let start = (new Date(this.startTime).getTime() - new Date(this.chooseDate + " 00:00:00").getTime())/1000;
        let end = (new Date(this.endTime).getTime() - new Date(this.chooseDate + " 00:00:00").getTime())/1000;
        console.log(start)
        console.log(end)
        this.playTime = [start, end];
        this.timeRange = [startTime, endTime];
      },
      videoError: function (e) {
        console.log("播放器错误：" + JSON.stringify(e));
      },
      checkedFile(file){
        this.chooseFile = file;
        this.setTime(file.startTime, file.endTime);
			  // 开始回放
        this.playRecord()
      },
      playRecord: function () {

        if (this.streamId !== "") {
          this.stopPlayRecord(()=> {
            this.streamId = "";
            this.playRecord();
          })
        } else {
          this.$axios({
            method: 'get',
            url: '/api/playback/start/' + this.deviceId + '/' + this.channelId + '?startTime=' + this.startTime + '&endTime=' +
              this.endTime
          }).then((res)=> {
            if (res.data.code === 0) {
              this.streamInfo = res.data.data;
              this.app = this.streamInfo.app;
              this.streamId = this.streamInfo.stream;
              this.mediaServerId = this.streamInfo.mediaServerId;
              this.ssrc = this.streamInfo.ssrc;
              this.videoUrl = this.getUrlByStreamInfo();
              this.hasAudio = this.streamInfo.tracks && this.streamInfo.tracks.length > 1
            }else {
              this.$message({
                showClose: true,
                message: res.data.msg,
                type: "error",
              });
            }
          });
        }
      },
      gbPlay(){
        console.log('前端控制：播放');
        this.$axios({
          method: 'get',
          url: '/api/playback/resume/' + this.streamId
        }).then((res)=> {
          this.$refs["recordVideoPlayer"].play(this.videoUrl)
        });
      },
      gbPause(){
        console.log('前端控制：暂停');
        this.$axios({
          method: 'get',
          url: '/api/playback/pause/' + this.streamId
        }).then(function (res) {});
      },
      gbScale(command){
        console.log('前端控制：倍速 ' + command);
        this.$axios({
          method: 'get',
          url: `/api/playback/speed/${this.streamId }/${command}`
        }).then(function (res) {});
      },
      downloadRecord: function (row) {
        if (!row) {
          let startTimeStr = moment(new Date(this.chooseDate + " 00:00:00").getTime() + this.playTime[0]*1000).format("YYYY-MM-DD HH:mm:ss");
          let endTimeStr = moment(new Date(this.chooseDate + " 00:00:00").getTime() + this.playTime[1]*1000).format("YYYY-MM-DD HH:mm:ss");
          console.log(startTimeStr);
          console.log(endTimeStr);
          row = {
            startTime: startTimeStr,
            endTime: endTimeStr
          }
        }
        if (this.streamId !== "") {
          this.stopPlayRecord(()=> {
            this.streamId = "";
            this.downloadRecord(row);
          })
        }else {
          this.$axios({
            method: 'get',
            url: '/api/gb_record/download/start/' + this.deviceId + '/' + this.channelId + '?startTime=' + row.startTime + '&endTime=' +
              row.endTime + '&downloadSpeed=4'
          }).then( (res)=> {
            if (res.data.code === 0) {
              let streamInfo = res.data.data;
              this.$refs.recordDownload.openDialog(this.deviceId, this.channelId, streamInfo.app, streamInfo.stream, streamInfo.mediaServerId);
            }else {
              this.$message({
                showClose: true,
                message: res.data.msg,
                type: "error",
              });
            }
          });
        }
      },
      stopDownloadRecord: function (callback) {
        this.$refs["recordVideoPlayer"].pause();
        this.videoUrl = '';
        this.$axios({
          method: 'get',
          url: '/api/gb_record/download/stop/' + this.deviceId + "/" + this.channelId+ "/" + this.streamId
        }).then((res)=> {
          if (callback) callback(res)
        });
      },
      stopPlayRecord: function (callback) {
        console.log("停止录像回放")
        if (this.streamId !== "") {
          this.$refs["recordVideoPlayer"].pause();
          this.videoUrl = '';
          this.$axios({
            method: 'get',
            url: '/api/playback/stop/' + this.deviceId + "/" + this.channelId + "/" + this.streamId
          }).then(function (res) {
            if (callback) callback()
          });
        }

      },
      getDataWidth(item){
        let timeForFile = this.getTimeForFile(item);
        let result = (timeForFile[2])/((this.sliderMax - this.sliderMIn)*1000)
        return result*100
      },
      getDataLeft(item){
        let timeForFile = this.getTimeForFile(item);
        let differenceTime = timeForFile[0].getTime() - new Date(this.chooseDate + " 00:00:00").getTime()
        return parseFloat((differenceTime - this.sliderMIn * 1000)/((this.sliderMax - this.sliderMIn)*1000))*100   ;
      },
      getUrlByStreamInfo(){
        if (location.protocol === "https:") {
          this.videoUrl = this.streamInfo["wss_flv"]
        }else {
          this.videoUrl = this.streamInfo["ws_flv"]
        }
        return this.videoUrl;

      },
      timePickerChange: function (val){
        this.setTime(val[0], val[1])
      },
      playTimeChange(val){
        console.log(val)

        let startTimeStr = moment(new Date(this.chooseDate + " 00:00:00").getTime() + val[0]*1000).format("YYYY-MM-DD HH:mm:ss");
        let endTimeStr = moment(new Date(this.chooseDate + " 00:00:00").getTime() + val[1]*1000).format("YYYY-MM-DD HH:mm:ss");

        this.setTime(startTimeStr, endTimeStr)

        this.playRecord();
      },
      setSliderFit() {
        if (this.sliderMIn === 0 && this.sliderMax === 86400) {
          if (this.detailFiles.length > 0){
            let timeForFile = this.getTimeForFile(this.detailFiles[0]);
            let lastTimeForFile = this.getTimeForFile(this.detailFiles[this.detailFiles.length - 1]);
            let timeNum = timeForFile[0].getTime() - new Date(this.chooseDate + " " + "00:00:00").getTime()
            let lastTimeNum = lastTimeForFile[1].getTime() - new Date(this.chooseDate + " " + "00:00:00").getTime()

            this.playTime = parseInt(timeNum/1000)
            this.sliderMIn = parseInt(timeNum/1000 - timeNum/1000%(60*60))
            this.sliderMax = parseInt(lastTimeNum/1000 - lastTimeNum/1000%(60*60)) + 60*60

            this.playTime = [this.sliderMIn, this.sliderMax];
          }
        }else {
          this.sliderMIn = 0;
          this.sliderMax = 86400;
        }
      },
      getTimeForFile(file){
        let startTime = new Date(file.startTime);
        let endTime = new Date(file.endTime);
        return [startTime, endTime, endTime.getTime() - startTime.getTime()];
      },
      playTimeFormat(val){
        let h = parseInt(val/3600);
        let m = parseInt((val - h*3600)/60);
        let s = parseInt(val - h*3600 - m*60);

        let hStr = h;
        let mStr = m;
        let sStr = s;
        if (h < 10) {
          hStr = "0" + hStr;
        }
        if (m < 10) {
          mStr = "0" + mStr;s
        }
        if (s < 10) {
          sStr = "0" + sStr;
        }
        return hStr + ":" + mStr + ":" + sStr
      },
      goBack(){
        // 如果正在进行录像回放则，发送停止
        if (this.streamId !== "") {
          this.stopPlayRecord(()=> {
            this.streamId = "";
          })
        }
        window.history.go(-1);
      },
		}
	};
</script>

<style>
  .el-slider__runway {
    background-color:rgba(206, 206, 206, 0.47) !important;
  }
  .el-slider__bar {
    background-color: rgba(153, 153, 153, 0) !important;
  }
  .playtime-slider {
    position: relative;
    z-index: 100;
  }
  .data-picker-true{

  }
  .data-picker-true:after{
    content: "";
    position: absolute;
    width: 4px;
    height: 4px;
    background-color: #606060;
    border-radius: 4px;
    left: 45%;
    top: 74%;

  }
  .data-picker-false{

  }
  .slider-val-box{
    height: 6px;
    position: relative;
    top: -22px;
  }
  .slider-val{
    height: 6px;
    background-color: #007CFF;
    position: absolute;
  }
  .record-list-box-box{
    width: 250px;
    float: left;
  }
  .record-list-box{
    overflow: auto;
    width: 220px;
    list-style: none;
    padding: 0;
    margin: 0;
    margin-top: 0px;
    padding: 1rem 0;
    background-color: #FFF;
    margin-top: 10px;
  }
  .record-list{
    list-style: none;
    padding: 0;
    margin: 0;
    background-color: #FFF;

  }
  .record-list-no-val {
    position: absolute;
    color: #9f9f9f;
    top: 50%;
    left: 110px;
  }
  .record-list-item{
    padding: 0;
    margin: 0;
    margin: 0.5rem 0;
    cursor: pointer;
  }
  .record-list-option {
    width: 10px;
    float: left;
    margin-top: 39px;

  }
  .player-option-box{
    padding: 0 20px;
  }
</style>
