<template>
  <div style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="部标录像" @back="goBack" />
      </div>
    </div>
    <el-container>
      <el-aside width="300px">
        <div class="record-list-box-box">
          <el-date-picker v-model="chooseDate" size="mini" type="date" value-format="yyyy-MM-dd" placeholder="日期" @change="dateChange()" />
          <div v-loading="recordsLoading" class="record-list-box" :style="recordListStyle">
            <ul v-if="detailFiles.length >0" class="infinite-list record-list">
              <li v-for="item in detailFiles" class="infinite-list-item record-list-item">

                <el-tag v-if="chooseFile !== item" @click="checkedFile(item)">
                  <i class="el-icon-video-camera" />
                  {{ moment(item.startTime).format('HH:mm:ss') }}-{{ moment(item.endTime).format('HH:mm:ss') }}
                </el-tag>
                <el-tag v-if="chooseFile === item" type="danger">
                  <i class="el-icon-video-camera" />
                  {{ moment(item.startTime).format('HH:mm:ss') }}-{{ moment(item.endTime).format('HH:mm:ss') }}
                </el-tag>
                <i style="color: #409EFF;margin-left: 5px;" class="el-icon-download" @click="downloadRecord(item)" />
              </li>
            </ul>
          </div>
          <div v-if="detailFiles.length ==0" size="mini" class="record-list-no-val">暂无数据</div>
        </div>

      </el-aside>
      <el-main style="padding-bottom: 10px;">
        <div class="playBox" :style="playerStyle">
          <player
            ref="recordVideoPlayer"
            :video-url="videoUrl"
            :error="videoError"
            :message="videoError"
            :has-audio="hasAudio"
            style="max-height: 100%"
            fluent
            autoplay
            live
          />
        </div>
        <div class="player-option-box">
          <div>
            <el-button-group>
              <el-time-picker
                v-model="timeRange"
                size="mini"
                is-range
                align="left"
                value-format="yyyy-MM-dd HH:mm:ss"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                placeholder="选择时间范围"
                @change="timePickerChange"
              />
            </el-button-group>

            <el-button-group>
              <el-button size="mini" class="iconfont icon-zanting" title="开始" @click="control(0, 0)" />
              <el-button size="mini" class="iconfont icon-kaishi" title="暂停" @click="control(1, 0)" />
              <el-button size="mini" class="iconfont icon-stop" title="结束" @click="control(2, 0)" />
              <el-dropdown size="mini" title="播放倍速" @command="scale">
                <el-button size="mini">
                  快进/快退 <i class="el-icon-arrow-down el-icon--right" />
                </el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item :command="[3, 1]">正常快进</el-dropdown-item>
                  <el-dropdown-item :command="[3, 2]">2倍速快进</el-dropdown-item>
                  <el-dropdown-item :command="[3, 4]">4倍速快进</el-dropdown-item>
                  <el-dropdown-item :command="[3, 8]">8倍速快进</el-dropdown-item>
                  <el-dropdown-item :command="[3, 16]">16倍速快进</el-dropdown-item>
                  <el-dropdown-item :command="[4, 1]">正常快退</el-dropdown-item>
                  <el-dropdown-item :command="[4, 2]">2倍速快退</el-dropdown-item>
                  <el-dropdown-item :command="[4, 4]">4倍速快退</el-dropdown-item>
                  <el-dropdown-item :command="[4, 8]">8倍速快退</el-dropdown-item>
                  <el-dropdown-item :command="[4, 16]">16倍速快退</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
              <el-button size="mini" class="iconfont icon-xiazai1" title="下载选定录像" @click="downloadRecord()" />
              <el-button v-if="sliderMIn === 0 && sliderMax === 86400" size="mini" class="iconfont icon-slider" title="放大滑块" @click="setSliderFit()" />
              <el-button v-if="sliderMIn !== 0 || sliderMax !== 86400" size="mini" class="iconfont icon-slider-right" title="恢复滑块" @click="setSliderFit()" />
            </el-button-group>
          </div>
          <el-slider
            id="playtimeSlider"
            v-model="playTime"
            class="playtime-slider"
            :disabled="detailFiles.length === 0"
            :min="sliderMIn"
            :max="sliderMax"
            :range="true"
            :format-tooltip="playTimeFormat"
            :marks="playTimeSliderMarks"
            @change="playTimeChange"
          />
          <div class="slider-val-box">
            <div v-for="item of detailFiles" class="slider-val" :style="'width:' + getDataWidth(item) + '%; left:' + getDataLeft(item) + '%'" />
          </div>
        </div>

      </el-main>
    </el-container>
  </div>
</template>

<script>
import player from '../../common/jessibuca.vue'
import moment from 'moment'
export default {
  name: 'App',
  components: {
    player
  },
  data() {
    return {
      phoneNumber: this.$route.params.phoneNumber,
      channelId: this.$route.params.channelId,
      recordsLoading: false,
      streamId: '',
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
      tabVal: 'running',
      recordListStyle: {
        height: this.winHeight + 'px',
        overflow: 'auto',
        margin: '10px auto 10px auto'
      },
      playerStyle: {
        'margin': '0 auto 20px auto',
        'height': this.winHeight + 'px'
      },
      winHeight: window.innerHeight - 240,
      playTime: null,
      timeRange: null,
      startTime: null,
      endTime: null,
      playTimeSliderMarks: {
        0: '00:00',
        3600: '01:00',
        7200: '02:00',
        10800: '03:00',
        14400: '04:00',
        18000: '05:00',
        21600: '06:00',
        25200: '07:00',
        28800: '08:00',
        32400: '09:00',
        36000: '10:00',
        39600: '11:00',
        43200: '12:00',
        46800: '13:00',
        50400: '14:00',
        54000: '15:00',
        57600: '16:00',
        61200: '17:00',
        64800: '18:00',
        68400: '19:00',
        72000: '20:00',
        75600: '21:00',
        79200: '22:00',
        82800: '23:00',
        86400: '24:00'
      }
    }
  },
  computed: {

  },
  mounted() {
    this.recordListStyle.height = this.winHeight + 'px'
    this.playerStyle['height'] = this.winHeight + 'px'
    this.chooseDate = moment().format('YYYY-MM-DD')
    this.dateChange()
    window.addEventListener('beforeunload', this.stopPlayRecord)
  },
  destroyed() {
    this.$destroy('recordVideoPlayer')
    window.removeEventListener('beforeunload', this.stopPlayRecord)
  },
  methods: {
    dateChange() {
      if (!this.chooseDate) {
        return
      }

      this.setTime(this.chooseDate + ' 00:00:00', this.chooseDate + ' 23:59:59')
      this.recordsLoading = true
      this.detailFiles = []
      this.$store.dispatch('jtDevice/queryRecordList', {
        phoneNumber: this.phoneNumber,
        channelId: this.channelId,
        startTime: this.startTime,
        endTime: this.endTime
      })
        .then((data) => {
          this.detailFiles = data
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.recordsLoading = false
        })
    },
    moment: function(v) {
      return moment(v)
    },
    setTime: function(startTime, endTime) {
      this.startTime = startTime
      this.endTime = endTime
      const start = (new Date(this.startTime).getTime() - new Date(this.chooseDate + ' 00:00:00').getTime()) / 1000
      const end = (new Date(this.endTime).getTime() - new Date(this.chooseDate + ' 00:00:00').getTime()) / 1000
      console.log(start)
      console.log(end)
      this.playTime = [start, end]
      this.timeRange = [startTime, endTime]
    },
    videoError: function(e) {
      console.log('播放器错误：' + JSON.stringify(e))
    },
    checkedFile(file) {
      this.chooseFile = file
      this.setTime(file.startTime, file.endTime)
      // 开始回放
      this.playRecord()
    },
    playRecord: function() {
      if (this.streamId !== '') {
        this.stopPlayRecord(() => {
          this.streamId = ''
          this.playRecord()
        })
      } else {
        this.$store.dispatch('jtDevice/startPlayback', {
          phoneNumber: this.phoneNumber,
          channelId: this.channelId,
          startTime: this.startTime,
          endTime: this.endTime,
          type: 0,
          rate: 0,
          playbackType: 0,
          playbackSpeed: 0
        })
          .then((data) => {
            this.streamInfo = data
            this.app = this.streamInfo.app
            this.streamId = this.streamInfo.stream
            this.mediaServerId = this.streamInfo.mediaServerId
            this.ssrc = this.streamInfo.ssrc
            this.videoUrl = this.getUrlByStreamInfo()
            this.hasAudio = this.streamInfo.tracks && this.streamInfo.tracks.length > 1
          })
      }
    },
    control(command, playbackSpeed, time) {
      this.$store.dispatch('jtDevice/controlPlayback', {
        phoneNumber: this.phoneNumber,
        channelId: this.channelId,
        command: command,
        playbackSpeed: playbackSpeed,
        time: time
      })
    },
    scale(command) {
      this.control(command[0], command[1])
    },
    downloadRecord: function(row) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      const downloadFile = ((process.env.NODE_ENV === 'development') ? process.env.BASE_API : baseUrl) +
        `/api/jt1078/playback/download?phoneNumber=${this.phoneNumber}&channelId=${this.channelId}&startTime=${row.startTime}&endTime=${row.endTime}` +
        `&alarmSign=${row.alarmSign}&mediaType=${row.mediaType}&streamType=${row.streamType}&storageType=${row.storageType}`
      console.log(downloadFile)
      const x = new XMLHttpRequest()
      x.open('GET', downloadFile, true)
      x.responseType = 'blob'
      x.onload = (e) => {
        const url = window.URL.createObjectURL(x.response)
        const a = document.createElement('a')
        a.href = url
        a.download = this.phoneNumber + '-' + this.channelId + '.mp4'
        a.click()
      }
      x.send()
    },
    stopPlayRecord: function(callback) {
      console.log('停止录像回放')
      if (this.streamId !== '') {
        this.$refs['recordVideoPlayer'].pause()
        this.videoUrl = ''
        this.$store.dispatch('jtDevice/stopPlayback', {
          phoneNumber: this.phoneNumber,
          channelId: this.channelId,
          streamId: this.streamId
        })
          .then(function(res) {
            if (callback) callback()
          })
      }
    },
    getDataWidth(item) {
      const timeForFile = this.getTimeForFile(item)
      const result = (timeForFile[2]) / ((this.sliderMax - this.sliderMIn) * 1000)
      return result * 100
    },
    getDataLeft(item) {
      const timeForFile = this.getTimeForFile(item)
      const differenceTime = timeForFile[0].getTime() - new Date(this.chooseDate + ' 00:00:00').getTime()
      return parseFloat((differenceTime - this.sliderMIn * 1000) / ((this.sliderMax - this.sliderMIn) * 1000)) * 100
    },
    getUrlByStreamInfo() {
      if (location.protocol === 'https:') {
        this.videoUrl = this.streamInfo['wss_flv']
      } else {
        this.videoUrl = this.streamInfo['ws_flv']
      }
      return this.videoUrl
    },
    timePickerChange: function(val) {
      this.setTime(val[0], val[1])
    },
    playTimeChange(val) {
      console.log(val)

      const startTimeStr = moment(new Date(this.chooseDate + ' 00:00:00').getTime() + val[0] * 1000).format('YYYY-MM-DD HH:mm:ss')
      const endTimeStr = moment(new Date(this.chooseDate + ' 00:00:00').getTime() + val[1] * 1000).format('YYYY-MM-DD HH:mm:ss')

      this.setTime(startTimeStr, endTimeStr)

      this.playRecord()
    },
    setSliderFit() {
      if (this.sliderMIn === 0 && this.sliderMax === 86400) {
        if (this.detailFiles.length > 0) {
          const timeForFile = this.getTimeForFile(this.detailFiles[0])
          const lastTimeForFile = this.getTimeForFile(this.detailFiles[this.detailFiles.length - 1])
          const timeNum = timeForFile[0].getTime() - new Date(this.chooseDate + ' ' + '00:00:00').getTime()
          const lastTimeNum = lastTimeForFile[1].getTime() - new Date(this.chooseDate + ' ' + '00:00:00').getTime()

          this.playTime = parseInt(timeNum / 1000)
          this.sliderMIn = parseInt(timeNum / 1000 - timeNum / 1000 % (60 * 60))
          this.sliderMax = parseInt(lastTimeNum / 1000 - lastTimeNum / 1000 % (60 * 60)) + 60 * 60

          this.playTime = [this.sliderMIn, this.sliderMax]
        }
      } else {
        this.sliderMIn = 0
        this.sliderMax = 86400
      }
    },
    getTimeForFile(file) {
      const startTime = new Date(file.startTime)
      const endTime = new Date(file.endTime)
      return [startTime, endTime, endTime.getTime() - startTime.getTime()]
    },
    playTimeFormat(val) {
      const h = parseInt(val / 3600)
      const m = parseInt((val - h * 3600) / 60)
      const s = parseInt(val - h * 3600 - m * 60)

      let hStr = h
      let mStr = m
      let sStr = s
      if (h < 10) {
        hStr = '0' + hStr
      }
      if (m < 10) {
        mStr = '0' + mStr; s
      }
      if (s < 10) {
        sStr = '0' + sStr
      }
      return hStr + ':' + mStr + ':' + sStr
    },
    goBack() {
      // 如果正在进行录像回放则，发送停止
      if (this.streamId !== '') {
        this.stopPlayRecord(() => {
          this.streamId = ''
        })
      }
      window.history.go(-1)
    }
  }
}
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
