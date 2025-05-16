<template>
  <div id="DeviceRecord" class="app-container">
    <div :style="boxStyle">
      <div>
        <div class="record-list-box-box">
          <div v-if="showSidebar">
            <el-date-picker
              v-model="chooseDate"
              size="mini"
              :picker-options="pickerOptions"
              type="date"
              value-format="yyyy-MM-dd"
              placeholder="日期"
              style="width: 190px"
              @change="dateChange()"
            />
            <!--            <el-button :disabled="!mediaServerId" size="mini" type="primary" icon="fa fa-cloud-download" style="margin: auto; margin-left: 12px " title="裁剪合并" @click="drawerOpen"></el-button>-->
          </div>
          <div class="record-list-box" style="height: calc(100vh - 170px); overflow: auto">
            <ul v-if="detailFiles.length >0" class="infinite-list record-list">
              <li v-for="(item,index) in detailFiles" :key="index" class="infinite-list-item record-list-item">
                <el-tag
                  v-if="chooseFileIndex !== index"
                  style="background-color: #ecf5ff; color: #017690; "
                  @click="chooseFile(index)"
                >
                  <i class="el-icon-video-camera" />
                  {{ getFileShowName(item) }}
                </el-tag>
                <el-tag v-if="chooseFileIndex === index" type="danger">
                  <i class="el-icon-video-camera" />
                  {{ getFileShowName(item) }}
                </el-tag>
                <a
                  class="el-icon-download"
                  style="color: #409EFF;font-weight: 600;margin-left: 10px;"
                  target="_blank"
                  @click="downloadFile(item)"
                />
              </li>
            </ul>
            <div v-if="detailFiles.length === 0" class="record-list-no-val">暂无数据</div>
          </div>
        </div>
      </div>
      <div id="playerBox">
        <div class="playBox" style="height: calc(100% - 90px); width: 100%; background-color: #000000">
          <div
            v-if="playLoading"
            style="position: relative; left: calc(50% - 32px); top: 43%; z-index: 100;color: #fff;float: left; text-align: center;"
          >
            <div class="el-icon-loading" />
            <div style="width: 100%; line-height: 2rem">正在加载</div>
          </div>
          <h265web
            ref="recordVideoPlayer"
            :video-url="videoUrl"
            :height="'calc(100vh - 250px)'"
            :show-button="false"
            :has-audio="true"
            @playStatusChange="playingChange"
            @playTimeChange="showPlayTimeChange"
          />
        </div>
        <div class="player-option-box">
          <VideoTimeline
            ref="Timeline"
            :init-time="initTime"
            :time-segments="timeSegments"
            :init-zoom-index="4"
            @timeChange="playTimeChange"
            @mousedown="timelineMouseDown"
            @mouseup="mouseupTimeline"
          />
          <div v-if="showTime" class="time-line-show">{{ showTimeValue }}</div>
        </div>
        <div style="height: 40px; background-color: #383838; display: grid; grid-template-columns: 1fr 400px 1fr">
          <div style="text-align: left;">
            <div class="record-play-control" style="background-color: transparent; box-shadow: 0 0 10px transparent">
              <a
                target="_blank"
                class="record-play-control-item iconfont icon-list"
                title="列表"
                @click="sidebarControl()"
              />
              <a
                target="_blank"
                class="record-play-control-item iconfont icon-camera1196054easyiconnet"
                title="截图"
                @click="snap()"
              />
              <!--              <a target="_blank" class="record-play-control-item iconfont icon-xiazai011" title="下载" @click="gbPause()" />-->
            </div>
          </div>
          <div style="text-align: center;">
            <div class="record-play-control">
              <el-dropdown @command="scale">
                <a
                    target="_blank"
                    class="record-play-control-item record-play-control-speed"
                    title="倍速播放"
                >快退</a>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item :command="[4, 1]">1X</el-dropdown-item>
                  <el-dropdown-item :command="[4, 2]">2X</el-dropdown-item>
                  <el-dropdown-item :command="[4, 4]">4X</el-dropdown-item>
                  <el-dropdown-item :command="[4, 8]">8X</el-dropdown-item>
                  <el-dropdown-item :command="[4, 16]">16X</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
              <a
                v-if="chooseFileIndex > 0"
                target="_blank"
                class="record-play-control-item iconfont icon-diyigeshipin"
                title="上一个"
                @click="playLast()"
              />
              <a
                v-else
                style="color: #acacac; cursor: not-allowed"
                target="_blank"
                class="record-play-control-item iconfont icon-diyigeshipin"
                title="上一个"
              />
              <a
                target="_blank"
                class="record-play-control-item iconfont icon-stop1"
                style="font-size: 14px"
                title="停止"
                @click="stopPLay()"
              />
              <a
                v-if="playing"
                target="_blank"
                class="record-play-control-item iconfont icon-zanting"
                title="暂停"
                @click="pausePlay()"
              />
              <a
                v-if="!playing"
                target="_blank"
                class="record-play-control-item iconfont icon-kaishi"
                title="播放"
                @click="play()"
              />
              <a
                v-if="chooseFileIndex < detailFiles.length - 1"
                target="_blank"
                class="record-play-control-item iconfont icon-zuihouyigeshipin"
                title="下一个"
                @click="playNext()"
              />
              <a
                v-else
                style="color: #acacac; cursor: not-allowed"
                target="_blank"
                class="record-play-control-item iconfont icon-zuihouyigeshipin"
                title="下一个"
                @click="playNext()"
              />
              <el-dropdown @command="scale">
                <a
                  target="_blank"
                  class="record-play-control-item record-play-control-speed"
                  title="倍速播放"
                >快进</a>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item :command="[3, 1]">1X</el-dropdown-item>
                  <el-dropdown-item :command="[3, 2]">2X</el-dropdown-item>
                  <el-dropdown-item :command="[3, 4]">4X</el-dropdown-item>
                  <el-dropdown-item :command="[3, 8]">8X</el-dropdown-item>
                  <el-dropdown-item :command="[3, 16]">16X</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </div>
          <div style="text-align: right;">
            <div class="record-play-control" style="background-color: transparent; box-shadow: 0 0 10px transparent">
              <a
                v-if="!isFullScreen"
                target="_blank"
                class="record-play-control-item iconfont icon-fangdazhanshi"
                title="全屏"
                @click="fullScreen()"
              />
              <a
                v-else
                target="_blank"
                class="record-play-control-item iconfont icon-suoxiao1"
                title="全屏"
                @click="fullScreen()"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
    <record-download ref="recordDownload" />
    <chooseTimeRange ref="chooseTimeRange" />
  </div>
</template>

<script>

import h265web from '../../common/h265web.vue'
import VideoTimeline from '../../common/VideoTimeLine/index.vue'
import recordDownload from '../../dialog/recordDownload.vue'
import ChooseTimeRange from '../../dialog/chooseTimeRange.vue'
import moment from 'moment'
import screenfull from 'screenfull'

export default {
  name: 'DeviceRecord',
  components: {
    h265web, VideoTimeline, recordDownload, ChooseTimeRange
  },
  data() {
    return {
      showSidebar: false,
      phoneNumber: this.$route.params.phoneNumber,
      channelId: this.$route.params.channelId,
      mediaServerId: null,
      dateFilesObj: [],
      mediaServerList: [],
      detailFiles: [],
      videoUrl: null,
      streamInfo: null,
      loading: false,
      chooseDate: null,
      playTime: null,
      playerTime: 0,
      playSpeed: 1,
      chooseFileIndex: null,
      queryDate: new Date(),
      currentPage: 1,
      count: 1000000, // TODO 分页导致滑轨视频有效值无法获取完全
      total: 0,
      playLoading: false,
      showTime: true,
      isFullScreen: false,
      playSeekValue: 0,
      playing: false,
      taskTimeRange: [],
      timeFormat: '00:00:00',
      initTime: null,
      timelineControl: false,
      showOtherSpeed: true,
      timeSegments: [],
      pickerOptions: {
        cellClassName: (date) => {
          // 通过显示一个点标识这一天有录像
          const time = moment(date).format('YYYY-MM-DD')
          if (this.dateFilesObj[time]) {
            return 'data-picker-true'
          } else {
            return 'data-picker-false'
          }
        }
      },
    }
  },
  computed: {
    boxStyle() {
      if (this.showSidebar) {
        return {
          display: 'grid',
          gridTemplateColumns: '210px minmax(0, 1fr)'
        }
      } else {
        return {
          display: 'grid', gridTemplateColumns: '0 minmax(0, 1fr)'
        }
      }
    },
    showTimeValue() {
      return moment(this.playTime).format('YYYY-MM-DD HH:mm:ss')
    },
    startTime() {
      return this.chooseDate + ' 00:00:00'
    },
    endTime() {
      return this.chooseDate + ' 23:59:59'
    }
  },
  mounted() {
    // 查询当年有视频的日期
    this.chooseDate = moment().format('YYYY-MM-DD')
    this.dateChange()
    window.addEventListener('beforeunload', this.stopPlayRecord)
  },
  destroyed() {
    this.$destroy('recordVideoPlayer')
    window.removeEventListener('beforeunload', this.stopPlayRecord)
  },
  methods: {
    sidebarControl() {
      this.showSidebar = !this.showSidebar
    },
    snap() {
      this.$refs.recordVideoPlayer.screenshot()
    },
    playLast() {
      // 播放上一个
      if (this.chooseFileIndex === 0) {
        return
      }
      this.chooseFile(this.chooseFileIndex - 1)
    },
    playNext() {
      // 播放上一个
      if (this.chooseFileIndex === this.detailFiles.length - 1) {
        return
      }
      this.chooseFile(this.chooseFileIndex + 1)
    },
    scale(command) {
      this.control(command[0], command[1])
    },
    control(command, playbackSpeed, time) {
      // 倍速播放
      this.playSpeed = playbackSpeed
      this.$refs.recordVideoPlayer.setPlaybackRate(parseFloat(this.playSpeed))
      this.$store.dispatch('jtDevice/controlPlayback', {
        phoneNumber: this.phoneNumber,
        channelId: this.channelId,
        command: command,
        playbackSpeed: playbackSpeed,
        time: time
      })
    },
    stopPLay() {
      // 停止
      this.$refs.recordVideoPlayer.destroy()
      this.stopPlayRecord()
    },
    pausePlay() {
      // 暂停
      this.$refs.recordVideoPlayer.pause()
      this.control(1, 0)
    },
    play() {
      if (this.$refs.recordVideoPlayer.loaded) {
        this.$refs.recordVideoPlayer.unPause()
      } else {
        this.playRecord(this.showTimeValue, this.endTime)
      }
    },
    fullScreen() {
      // 全屏
      if (this.isFullScreen) {
        screenfull.exit()
        this.isFullScreen = false
        return
      }
      const playerWidth = this.$refs.recordVideoPlayer.playerWidth
      const playerHeight = this.$refs.recordVideoPlayer.playerHeight
      screenfull.request(document.getElementById('playerBox'))
      screenfull.on('change', (event) => {
        this.$refs.recordVideoPlayer.resize(playerWidth, playerHeight)
        this.isFullScreen = screenfull.isFullscreen
      })
      this.isFullScreen = true
    },
    dateChange() {
      this.detailFiles = []
      this.$store.dispatch('jtDevice/queryRecordList', {
        phoneNumber: this.phoneNumber,
        channelId: this.channelId,
        startTime: this.startTime,
        endTime: this.endTime
      })
        .then((data) => {
          // 处理时间信息
          if (data.length === 0) {
            return
          }
          this.detailFiles = data
          this.initTime = new Date(this.detailFiles[0].startTime).getTime()
          for (let i = 0; i < this.detailFiles.length; i++) {
            this.timeSegments.push({
              beginTime: new Date(this.detailFiles[i].startTime).getTime(),
              endTime: new Date(this.detailFiles[i].endTime).getTime(),
              color: '#017690',
              startRatio: 0.7,
              endRatio: 0.85,
              index: i
            })
          }
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.recordsLoading = false
        })
    },
    stopPlayRecord(callback) {
      console.log('停止录像回放')
      if (this.streamInfo !== null) {
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
      } else {
        if (callback) callback()
      }
    },
    chooseFile(index) {
      this.chooseFileIndex = index
      const chooseFile = this.detailFiles[this.chooseFileIndex]
      this.playTime = new Date(chooseFile.startTime).getTime()
      this.playRecord(chooseFile.startTime, this.endTime)
    },
    playRecord(startTime, endTime) {
      if (this.streamInfo !== null) {
        this.stopPlayRecord(() => {
          this.streamInfo = null
          this.playRecord(startTime, endTime)
        })
      } else {
        this.playerTime = 0
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
            this.videoUrl = this.getUrlByStreamInfo()
            this.hasAudio = this.streamInfo.tracks && this.streamInfo.tracks.length > 1
          })
      }
    },
    getUrlByStreamInfo() {
      if (location.protocol === 'https:') {
        this.videoUrl = this.streamInfo['wss_flv']
      } else {
        this.videoUrl = this.streamInfo['ws_flv']
      }
      return this.videoUrl
    },
    downloadFile(row) {
      if (!row) {
        const startTimeStr = moment(new Date(this.chooseDate + ' 00:00:00').getTime() + this.playTime[0] * 1000).format('YYYY-MM-DD HH:mm:ss')
        const endTimeStr = moment(new Date(this.chooseDate + ' 00:00:00').getTime() + this.playTime[1] * 1000).format('YYYY-MM-DD HH:mm:ss')
        row = {
          startTime: startTimeStr,
          endTime: endTimeStr
        }
      }
      if (this.streamInfo !== null) {
        this.stopPlayRecord(() => {
          this.downloadFile(row)
        })
      } else {
        this.downloadRecord(row)
      }
    },
    downloadRecord: function(row) {
      const loading = this.$loading({
        lock: true,
        text: '正在请求录像',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      const downloadFileUrl = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) +
        `/api/jt1078/playback/download?phoneNumber=${this.phoneNumber}&channelId=${this.channelId}&startTime=${row.startTime}&endTime=${row.endTime}` +
        `&alarmSign=${row.alarmSign}&mediaType=${row.mediaType}&streamType=${row.streamType}&storageType=${row.storageType}&access-token=${this.$store.getters.token}`
      const x = new XMLHttpRequest()
      x.open('GET', downloadFileUrl, true)
      x.responseType = 'blob'
      x.onload = (e) => {
        const url = window.URL.createObjectURL(x.response)
        const a = document.createElement('a')
        a.href = url
        a.download = this.phoneNumber + '-' + this.channelId + '.mp4'
        a.click()
        loading.close()
      }
      x.ontimeout = (e) => {
        loading.close()
        this.$message.error({
          showClose: true,
          message: '加载超时'
        })
      }
      x.onerror = (e) => {
        loading.close()
        this.$message.error({
          showClose: true,
          message: e.error
        })
      }
      x.send()
    },
    getFileShowName(item) {
      return moment(item.startTime).format('HH:mm:ss') + '-' + moment(item.endTime).format('HH:mm:ss')
    },

    showPlayTimeChange(val) {
      this.playTime += (val * 1000 - this.playerTime)
      this.playerTime = val * 1000
    },
    playingChange(val) {
      this.playing = val
    },
    playTimeChange(val) {
      if (val === this.playTime) {
        return
      }
      this.playTime = val
    },
    timelineMouseDown() {
      this.timelineControl = true
    },
    mouseupTimeline(event) {
      if (!this.timelineControl) {
        this.timelineControl = false
        return
      }
      this.timelineControl = false
      this.playRecord(this.showTimeValue, this.endTime)
    }
  }
}
</script>

<style>

.record-list-box-box {
  width: fit-content;
  float: left;
}

.record-list-box {
  width: 100%;
  overflow: auto;
  list-style: none;
  padding: 0;
  margin: 0;
  background-color: #FFF;
  margin-top: 10px;
}

.record-list {
  list-style: none;
  padding: 0;
  margin: 0;
  background-color: #FFF;

}

.record-list-no-val {
  width: fit-content;
  position: relative;
  color: #9f9f9f;
  top: 50%;
  left: calc(50% - 2rem);
}

.record-list-item {
  padding: 0;
  margin: 0;
  margin: 0.5rem 0;
  cursor: pointer;
}

.record-play-control {
  height: 32px;
  line-height: 32px;
  display: inline-block;
  width: fit-content;
  padding: 0 10px;
  -webkit-box-shadow: 0 0 10px #262626;
  box-shadow: 0 0 10px #262626;
  background-color: #262626;
  margin: 4px 0;
}

.record-play-control-item {
  display: inline-block;
  padding: 0 10px;
  color: #fff;
  margin-right: 2px;
}

.record-play-control-item:hover {
  color: #1f83e6;
}

.record-play-control-speed {
  font-weight: bold;
  color: #fff;
  user-select: none;
}

.player-option-box {
  height: 50px
}

.time-line-show {
  position: relative;
  color: rgba(250, 249, 249, 0.89);
  left: calc(50% - 85px);
  top: -72px;
  text-shadow: 1px 0 #5f6b7c, -1px 0 #5f6b7c, 0 1px #5f6b7c, 0 -1px #5f6b7c, 1.1px 1.1px #5f6b7c, 1.1px -1.1px #5f6b7c, -1.1px 1.1px #5f6b7c, -1.1px -1.1px #5f6b7c;
}
</style>
