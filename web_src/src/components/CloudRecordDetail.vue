<template>
	<div id="recordDetail">
		<el-container>
      <el-aside width="300px">
        <div class="record-list-box-box">
          <el-date-picker size="mini" v-model="chooseDate" :picker-options="pickerOptions" type="date" value-format="yyyy-MM-dd" placeholder="日期" @change="dateChange()"></el-date-picker>
          <div class="record-list-box" :style="recordListStyle">
            <ul v-if="detailFiles.length >0" class="infinite-list record-list" v-infinite-scroll="infiniteScroll" >
              <li v-for="item in detailFiles" class="infinite-list-item record-list-item" >
                <el-tag v-if="choosedFile != item" @click="chooseFile(item)">
                  <i class="el-icon-video-camera"  ></i>
                  {{ item.substring(0,17)}}
                </el-tag>
                <el-tag type="danger" v-if="choosedFile == item">
                  <i class="el-icon-video-camera"  ></i>
                  {{ item.substring(0,17)}}
                </el-tag>
<!--                <a class="el-icon-download" style="color: #409EFF;font-weight: 600;margin-left: 10px;" :href="`${basePath}/${mediaServerId}/record/${recordFile.app}/${recordFile.stream}/${chooseDate}/${item}`" download />-->
                <a class="el-icon-download" style="color: #409EFF;font-weight: 600;margin-left: 10px;" :href="`${basePath}/download.html?url=${recordFile.app}/${recordFile.stream}/${chooseDate}/${item}`" target="_blank" />
              </li>
            </ul>
          </div>
          <div v-if="detailFiles.length ==0" class="record-list-no-val" >暂无数据</div>
        </div>

        <div class="record-list-option">
          <el-button size="mini" type="primary" icon="fa fa-cloud-download" style="margin: auto; " title="裁剪合并" @click="drawerOpen"></el-button>
        </div>
      </el-aside>
			<el-main style="padding: 22px">
        <div class="playBox" :style="playerStyle">
          <player ref="recordVideoPlayer" :videoUrl="videoUrl"  fluent autoplay :height="true" ></player>
        </div>
        <div class="player-option-box" >
          <el-slider
            class="playtime-slider"
            v-model="playTime"
            id="playtimeSlider"
            :disabled="detailFiles.length === 0"
            :min="sliderMIn"
            :max="sliderMax"
            :format-tooltip="playTimeFormat"
            @change="playTimeChange"
            :marks="playTimeSliderMarks">
          </el-slider>
          <div class="slider-val-box">
            <div class="slider-val" v-for="item of detailFiles" :style="'width:'  +  getDataWidth(item) + '%; left:' + getDataLeft(item) + '%'"></div>
          </div>
        </div>

			</el-main>
		</el-container>
    <el-drawer
      title="录像下载"
      :visible.sync="drawer"
      :direction="direction"
      :before-close="drawerClose">
      <div class="drawer-box">
          <el-button icon="el-icon-plus" size="mini"  type="primary" @click="addTask"></el-button>
        <el-tabs type="border-card" style="height: 100%" v-model="tabVal" @tab-click="tabClick">
          <el-tab-pane name="running">
            <span slot="label"><i class="el-icon-scissors"></i>进行中</span>
            <ul class="task-list">
              <li class="task-list-item" v-for="item in taskListForRuning">
                <div class="task-list-item-box">
                  <span>{{ item.startTime.substr(10) }}-{{item.endTime.substr(10)}}</span>
                  <el-progress :percentage="(parseFloat(item.percentage)*100).toFixed(1)"></el-progress>
                </div>
              </li>

            </ul>
          </el-tab-pane>
          <el-tab-pane name="ended">
            <span slot="label"><i class="el-icon-finished"></i>已完成</span>
            <ul class="task-list">
              <li class="task-list-item" v-for="item in taskListEnded">
                <div class="task-list-item-box" style="height: 2rem;line-height: 2rem;">
                  <span>{{ item.startTime.substr(10) }}-{{item.endTime.substr(10)}}</span>
                  <a class="el-icon-download download-btn" :href="basePath  + '/download.html?url=../' + item.recordFile" target="_blank">
                  </a>
                </div>
              </li>

            </ul>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
    <el-dialog title="选择时间段" :visible.sync="showTaskBox">
      <el-date-picker
        type="datetimerange"
        v-model="taskTimeRange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        format="HH:mm:ss"
        placeholder="选择时间范围">
      </el-date-picker>
      <el-button size="mini"  type="primary" @click="addTaskToServer">确认</el-button>
    </el-dialog>
	</div>
</template>


<script>
  // TODO 根据查询的时间列表设置滑轨的最大值与最小值，
	import uiHeader from './UiHeader.vue'
	import player from './dialog/easyPlayer.vue'
  import moment  from 'moment'
	export default {
		name: 'app',
		components: {
			uiHeader, player
		},
    props: ['recordFile', 'mediaServerId', 'dateFiles', 'mediaServerPath'],
		data() {
			return {
        basePath: `${this.mediaServerPath}/record`,
			  dateFilesObj: [],
			  detailFiles: [],
        chooseDate: null,
        videoUrl: null,
        choosedFile: null,
        queryDate: new Date(),
        currentPage: 1,
        count: 1000000, // TODO 分页导致滑轨视频有效值无法获取完全
        total: 0,
        direction: "ltr",
        drawer: false,
        showTaskBox: false,
        taskTimeRange: [],
        taskListEnded: [],
        taskListForRuning: [],
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
			    "margin": "auto",
			    "margin-bottom": "20px",
          "height": this.winHeight + "px",
        },
        winHeight: window.innerHeight - 240,
        playTime: 0,
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
        pickerOptions:{
          cellClassName:(date) =>{
            // 通过显示一个点标识这一天有录像
            let time = moment(date).format('YYYY-MM-DD')
            if (this.dateFilesObj[time]){
              return "data-picker-true"
            }else {
              return "data-picker-false"
            }
          }
        }
			};
		},
		computed: {

		},
		mounted() {
      this.recordListStyle.height = this.winHeight + "px";
      this.playerStyle["height"] = this.winHeight + "px";
      // 查询当年有视频的日期
      this.getDateInYear(()=>{
        if (Object.values(this.dateFilesObj).length > 0){
          this.chooseDate = Object.values(this.dateFilesObj)[Object.values(this.dateFilesObj).length -1];
          this.dateChange();
        }
      })
		},
		destroyed() {
			this.$destroy('recordVideoPlayer');
		},
		methods: {
      dateChange(){
        this.playTime = 0;
        this.detailFiles = [];
        this.currentPage = 1;
        this.sliderMIn= 0;
        this.sliderMax= 86400;
        let chooseFullDate = new Date(this.chooseDate + " " + "00:00:00");
        if (chooseFullDate.getFullYear() !== this.queryDate.getFullYear()
          || chooseFullDate.getMonth() !== this.queryDate.getMonth()){
          // this.getDateInYear()
        }
        this.queryRecordDetails(()=>{
          if (this.detailFiles.length > 0){
            let timeForFile = this.getTimeForFile(this.detailFiles[0]);
            let lastTimeForFile = this.getTimeForFile(this.detailFiles[this.detailFiles.length - 1]);
            let timeNum = timeForFile[0].getTime() - new Date(this.chooseDate + " " + "00:00:00").getTime()
            let lastTimeNum = lastTimeForFile[1].getTime() - new Date(this.chooseDate + " " + "00:00:00").getTime()

            this.playTime = parseInt(timeNum/1000)
            this.sliderMIn = parseInt(timeNum/1000 - timeNum/1000%(60*60))
            this.sliderMax = parseInt(lastTimeNum/1000 - lastTimeNum/1000%(60*60)) + 60*60
          }
        });
      },
      infiniteScroll(){
			  if (this.total > this.detailFiles.length) {
          this.currentPage ++;
          this.queryRecordDetails();
        }
      },
      queryRecordDetails: function (callback){
        let that = this;
        that.$axios({
          method: 'get',
          url:`/record_proxy/${that.mediaServerId}/api/record/file/list`,
          params: {
            app: that.recordFile.app,
            stream: that.recordFile.stream,
            startTime: that.chooseDate + " 00:00:00",
            endTime: that.chooseDate + " 23:59:59",
            page: that.currentPage,
            count: that.count
          }
        }).then(function (res) {
          that.total = res.data.data.total;
          that.detailFiles = that.detailFiles.concat(res.data.data.list);
          that.loading = false;
          if (callback) callback();
        }).catch(function (error) {
          console.log(error);
          that.loading = false;
        });
      },
      chooseFile(file){
        this.choosedFile = file;
			  if (file == null) {
          this.videoUrl = "";
        }else {
			    // TODO 控制列表滚动条
          this.videoUrl = `${this.basePath}/${this.recordFile.app}/${this.recordFile.stream}/${this.chooseDate}/${this.choosedFile}`
          console.log(this.videoUrl)
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
      playTimeChange(val){
        let minTime = this.getTimeForFile(this.detailFiles[0])[0]
        let maxTime = this.getTimeForFile(this.detailFiles[this.detailFiles.length - 1])[1];
        this.chooseFile(null);
        let timeMilli = new Date(this.chooseDate + " 00:00:00").getTime() + val*1000
        if (timeMilli >= minTime.getTime() && timeMilli <= maxTime.getTime()){
          for (let i = 0; i < this.detailFiles.length; i++) {
            let timeForFile = this.getTimeForFile(this.detailFiles[i]);
            if (timeMilli >= timeForFile[0].getTime() && timeMilli <= timeForFile[1].getTime()){
              // TODO 当前未按照实际时间偏移，仅仅是找到对应的文静播放
              this.chooseFile(this.detailFiles[i])
              return;
            }
          }
        }
      },
      getTimeForFile(file){
        let timeStr = file.substring(0,17);
        let starTime = new Date(this.chooseDate + " " + timeStr.split("-")[0]);
        let endTime = new Date(this.chooseDate + " " + timeStr.split("-")[1]);
        return [starTime, endTime, endTime.getTime() - starTime.getTime()];
      },
      playTimeFormat(val){
        let h = parseInt(val/3600);
        let m = parseInt((val - h*3600)/60);
        let s = parseInt(val - h*3600 - m*60);
        return h + ":" + m + ":" + s
      },
      deleteRecord(){
			  // TODO
        let that = this;
        this.$axios({
          method: 'delete',
          url:`/record_proxy/${that.mediaServerId}/api/record/delete`,
          params: {
            page: that.currentPage,
            count: that.count
          }
        }).then(function (res) {
          if (res.data.code == 0) {
            that.total = res.data.data.total;
            that.recordList = res.data.data.list;
          }
        }).catch(function (error) {
          console.log(error);
        });
      },
      getDateInYear(callback){
        let that = this;
        that.dateFilesObj = {};
        this.$axios({
          method: 'get',
          url:`/record_proxy/${that.mediaServerId}/api/record/date/list`,
          params: {
            app: that.recordFile.app,
            stream: that.recordFile.stream
          }
        }).then(function (res) {
          if (res.data.code === 0) {
            if (res.data.data.length > 0) {
              for (let i = 0; i < res.data.data.length; i++) {
                that.dateFilesObj[res.data.data[i]] = res.data.data[i]
              }

              console.log(that.dateFilesObj)
            }
          }
          if(callback)callback();
        }).catch(function (error) {
          console.log(error);
        });
      },
      tabClick(){
        this.getTaskList(this.tabVal === "ended")
      },
      drawerClose(){
        this.drawer = false;
        if (this.taskUpdate != null) {
          window.clearInterval(this.taskUpdate)
        }
      },
      drawerOpen(){
        this.drawer = true;
        if (this.taskUpdate != null) {
          window.clearInterval(this.taskUpdate)
        }
        this.taskUpdate = setInterval(()=>{
          this.getTaskList(this.tabVal === "ended")
        }, 1000)
      },
      addTask(){
        this.showTaskBox = true;
        let startTimeStr = this.chooseDate + " " + this.detailFiles[0].substring(0,8);
        let endTimeStr = this.chooseDate + " " + this.detailFiles[this.detailFiles.length - 1].substring(9,17);
        this.taskTimeRange[0] = new Date(startTimeStr)
        this.taskTimeRange[1] = new Date(endTimeStr)
      },
      addTaskToServer(){
        let that = this;
        this.$axios({
          method: 'get',
          url:`/record_proxy/${that.mediaServerId}/api/record/file/download/task/add`,
          params: {
            app: that.recordFile.app,
            stream: that.recordFile.stream,
            startTime: moment(this.taskTimeRange[0]).format('YYYY-MM-DD HH:mm:ss'),
            endTime: moment(this.taskTimeRange[1]).format('YYYY-MM-DD HH:mm:ss'),
          }
        }).then(function (res) {
          if (res.data.code === 0 && res.data.msg === "success") {
            that.showTaskBox = false
            that.getTaskList(false);
          }else {
            that.$message.error(res.data.msg);
          }
        }).catch(function (error) {
          console.log(error);
        });
      },
      handleTabClick() {
        this.getTaskList(this.tabVal === "ended")
      },
      getTaskList(isEnd){
        let that = this;
        this.$axios({
          method: 'get',
          url:`/record_proxy/${that.mediaServerId}/api/record/file/download/task/list`,
          params: {
            isEnd: isEnd,
          }
        }).then(function (res) {
          if (res.data.code == 0) {
            if (isEnd){
              that.taskListEnded = res.data.data;
            }else {
              that.taskListForRuning = res.data.data;
            }
          }
        }).catch(function (error) {
          console.log(error);
        });
      }
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
  .drawer-box{
    height: 100%;
  }

  .task-list{
    list-style: none;
    padding: 0;
    margin: 0;
    background-color: #FFF;
  }

  .task-list-item{
    padding: 0;
    margin: 0;
    margin: 1.5rem 0;
  }
  .task-list-item-box{
    text-align: left;
    font-size: 13px;
    color: #555;
  }
  .download-btn{
    display: inline-block;
    line-height: 1;
    white-space: nowrap;
    cursor: pointer;
    background: #FFF;
    background-color: rgb(255, 255, 255);
    border: 1px solid #DCDFE6;
    border-top-color: rgb(220, 223, 230);
    border-right-color: rgb(220, 223, 230);
    border-bottom-color: rgb(220, 223, 230);
    border-left-color: rgb(220, 223, 230);
    border-top-color: rgb(220, 223, 230);
    border-right-color: rgb(220, 223, 230);
    border-bottom-color: rgb(220, 223, 230);
    border-left-color: rgb(220, 223, 230);
    -webkit-appearance: none;
    text-align: center;
    -webkit-box-sizing: border-box;
    box-sizing: border-box;
    outline: 0;
    margin: 0;
    -webkit-transition: .1s;
    transition: .1s;
    font-weight: 500;
    padding: 7px 14px;
    font-size: 0.875rem;
    border-radius: 4px;
    font-size: 0.75rem;
    border-radius: 3px;
    color: #FFF;
    background-color: #409EFF;
    border-color: #409EFF;
    float: right;
  }
  .download-btn:hover{
    background: #66b1ff;
    border-color: #66b1ff;
    color: #FFF;
  }
  .time-box{
  }
</style>
