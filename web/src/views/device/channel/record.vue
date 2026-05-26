<template>
  <div id="DeviceRecord" class="app-container">
    <div :style="boxStyle">
      <div>
        <div v-if="this.$route.query.mediaServerId" class="page-header-btn" style="padding-right: 1rem">
          <b>节点：</b> {{ mediaServerId }}
        </div>
        <div v-if="this.$route.params.mediaServerId">
          <span>流媒体：{{ this.$route.params.mediaServerId }}</span>
        </div>
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
      <div id="playerBox" style="width: 100%;">
        <div :class="{
          'play-box': true,
          'play-box-fullscreen': isFullScreen,
        }">
          <div
            v-if="playLoading"
            style="position: relative; left: calc(50% - 32px); top: 43%; z-index: 100;color: #fff;float: left; text-align: center;"
          >
            <div class="el-icon-loading" />
            <div style="width: 100%; line-height: 2rem">正在加载</div>
          </div>
          <jessibucaPlayer
            v-if="activePlayer === 'jessibuca'"
            ref="recordVideoPlayer"
            :has-audio="true"
            :height="'calc(100vh - 90px)'"
            :show-button="false"
            autoplay
            @playStatusChange="playingChange"
            @playTimeChange="showPlayTimeChange"
          />
          <rtcPlayer
            v-if="activePlayer === 'webRTC'"
            ref="recordVideoPlayer"
            :has-audio="true"
            :show-controls="false"
            style="height: calc(100vh - 220px)"
            autoplay
            @playStatusChange="playingChange"
            @playTimeChange="showPlayTimeChange"
          />
          <h265web
            v-if="activePlayer === 'h265web'"
            ref="recordVideoPlayer"
            :height="'calc(100vh - 220px)'"
            :show-button="false"
            :has-audio="true"
            @playStatusChange="playingChange"
            @playTimeChange="showPlayTimeChange"
          />
        </div>
        <div :class="{
          'player-option-box': true,
          'player-option-box-bottom': isFullScreen
        }">
          <div v-if="showTime" class="time-line-show">{{ showTimeValue }}</div>
          <VideoTimeline
            style="height: 55px"
            ref="Timeline"
            :init-time="initTime"
            :time-segments="timeSegments"
            :init-zoom-index="4"
            @timeChange="playTimeChange"
            @mousedown="timelineMouseDown"
            @mouseup="mouseupTimeline"
          />
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
                <a
                  target="_blank"
                  class="record-play-control-item iconfont icon-xiazai1"
                  title="下载录像"
                  @click="chooseTimeForRecord()"
                />
                <!--              <a target="_blank" class="record-play-control-item iconfont icon-xiazai011" title="下载" @click="gbPause()" />-->
              </div>
            </div>
            <div style="text-align: center;">
              <div class="record-play-control">
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
                  class="record-play-control-item iconfont icon-kuaijin"
                  title="快退五秒"
                  @click="seekBackward()"
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
                <a v-if="!playing" target="_blank" class="record-play-control-item iconfont icon-kaishi" title="播放" @click="play()" />
                <a
                  target="_blank"
                  class="record-play-control-item iconfont icon-houtui"
                  title="快进五秒"
                  @click="seekForward()"
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
                <el-dropdown @command="changePlaySpeed" placement="top">
                  <a
                    target="_blank"
                    class="record-play-control-item record-play-control-speed"
                    title="倍速播放"
                  >{{ playSpeed }}X</a>
                  <el-dropdown-menu slot="dropdown" :append-to-body="false">
                    <el-dropdown-item
                      v-for="item in playSpeedRange"
                      :key="item"
                      :command="item"
                    >{{ item }}X
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </div>
            </div>
            <div style="text-align: right;">
              <div class="record-play-control" style="background-color: transparent; box-shadow: 0 0 10px transparent">
                <el-dropdown @command="changePlayer" placement="top">
                  <a
                    target="_blank"
                    class="record-play-control-item record-play-control-speed"
                    title="切换播放器"
                  >{{ playerLabel }}</a>
                  <el-dropdown-menu slot="dropdown" :append-to-body="false">
                    <el-dropdown-item command="jessibuca">Jessibuca</el-dropdown-item>
                    <el-dropdown-item command="webRTC">WebRTC</el-dropdown-item>
                    <el-dropdown-item command="h265web">H265Web</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
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
    </div>
    <record-download ref="recordDownload" />
    <chooseTimeRange ref="chooseTimeRange" />
  </div>
</template>

<script>

import h265web from '../../common/h265web.vue'
import jessibucaPlayer from '../../common/jessibuca.vue'
import rtcPlayer from '../../common/rtcPlayer.vue'
import VideoTimeline from '../../common/VideoTimeLine/index.vue'
import recordDownload from '../../dialog/recordDownload.vue'
import ChooseTimeRange from '../../dialog/chooseTimeRange.vue'
import moment from 'moment'
import screenfull from 'screenfull'

export default {
  name: 'DeviceRecord',
  components: {
    h265web, jessibucaPlayer, rtcPlayer, VideoTimeline, recordDownload, ChooseTimeRange
  },
  data() {
    return {
      showSidebar: false,
      deviceId: this.$route.params.deviceId,
      channelId: this.$route.params.channelDeviceId,
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
      activePlayer: 'jessibuca',
      playerUrls: {
        jessibuca: ['ws_flv', 'wss_flv'],
        webRTC: ['rtc', 'rtcs'],
        h265web: ['ws_flv', 'wss_flv']
      },
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
      playSpeedRange: [0.25, 0.5, 1, 2, 4]
    }
  },
  computed: {
    playerLabel() {
      const labels = { jessibuca: 'Jessibuca', webRTC: 'WebRTC', h265web: 'H265Web' }
      return labels[this.activePlayer] || 'Jessibuca'
    },
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
    this.getDownloadSpeedArray()
    window.addEventListener('beforeunload', this.stopPlayRecord)
  },
  destroyed() {
    this.$destroy('recordVideoPlayer')
    window.removeEventListener('beforeunload', this.stopPlayRecord)
  },
  methods: {
    changePlayer(player) {
      if (this.activePlayer === player) return
      this.activePlayer = player
      if (this.streamInfo) {
        this.videoUrl = this.getUrlByStreamInfo()
        this.$nextTick(() => {
          if (this.$refs.recordVideoPlayer) {
            this.$refs.recordVideoPlayer.play(this.videoUrl)
          }
        })
      }
    },
    sidebarControl() {
      this.showSidebar = !this.showSidebar
    },
    snap() {
      this.$refs.recordVideoPlayer.screenshot()
    },
    chooseTimeForRecord() {
      let startTime = this.startTime
      let endTime = this.endTime
      if (this.detailFiles.length > 0) {
        startTime = this.detailFiles[0].startTime
        endTime = this.detailFiles[this.detailFiles.length - 1].endTime
      }
      console.log(startTime)
      console.log(endTime)
      this.$refs.chooseTimeRange.openDialog([new Date(startTime), new Date(endTime)], (time) => {
        console.log(time)
        const startTime = moment(time[0]).format('YYYY-MM-DD HH:mm:ss')
        const endTime = moment(time[1]).format('YYYY-MM-DD HH:mm:ss')
        this.downloadFile({
          startTime: startTime,
          endTime: endTime
        })
      })
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
    changePlaySpeed(speed) {
      console.log(this.streamInfo)
      console.log(speed)
      // 倍速播放
      this.playSpeed = speed
      this.$store.dispatch('playback/setSpeed', [this.streamInfo.stream, speed])
        .then(data => {
          this.$refs.recordVideoPlayer.setPlaybackRate(this.playSpeed)
        })
        .catch((err) => {
          console.log(err)
        })
    },
    seekBackward() {
      // 快退五秒
      this.playSeekValue -= 5 * 1000
      this.play()
    },
    seekForward() {
      // 快进五秒
      this.playSeekValue += 5 * 1000
      this.play()
    },
    stopPLay() {
      // 停止
      this.$refs.recordVideoPlayer.destroy()
    },
    pausePlay() {
      // 暂停
      this.$refs.recordVideoPlayer.pause()
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
        this.isFullScreen = screenfull.isFullscreen
        if (this.$refs.recordVideoPlayer && this.$refs.recordVideoPlayer.resize) {
          this.$refs.recordVideoPlayer.resize(playerWidth, playerHeight)
        }
      })
      this.isFullScreen = true
    },
    dateChange() {
      this.detailFiles = []
      this.$store.dispatch('gbRecord/query', [this.deviceId, this.channelId, this.startTime, this.endTime])
        .then(data => {
          // 处理时间信息
          if (data.recordList.length === 0) {
            return
          }
          this.detailFiles = data.recordList
          this.initTime = new Date(this.detailFiles[0].startTime).getTime()
          console.log(this.initTime)
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
        .finally(() => {
          this.recordsLoading = false
        })
    },
    getDownloadSpeedArray() {
      this.$store.dispatch('device/queryChannelOne', {
        deviceId: this.deviceId,
        channelDeviceId: this.channelId
      })
        .then(data => {
          if (data.downloadSpeed) {
            const speedArray = data.downloadSpeed.split('/')

            speedArray.forEach(item => {
              if (parseInt(item) > 4) {
                this.playSpeedRange.push(parseInt(item))
              }
            })
          }
        })
    },
    stopPlayRecord(callback) {
      console.log('停止录像回放')
      if (this.streamInfo !== null) {
        this.$refs['recordVideoPlayer'].pause()
        this.videoUrl = ''
        this.$store.dispatch('playback/stop', [this.deviceId, this.channelId, this.streamInfo.stream])
          .then((data) => {
            this.streamInfo = null
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
          this.playRecord(startTime, endTime)
        })
      } else {
        this.playerTime = 0
        this.$store.dispatch('playback/play', [this.deviceId, this.channelId, startTime, endTime])
          .then(data => {
            this.streamInfo = data
            this.videoUrl = this.getUrlByStreamInfo()
            this.hasAudio = this.streamInfo.tracks && this.streamInfo.tracks.length > 1
            this.$nextTick(() => {
              if (this.$refs.recordVideoPlayer) {
                this.$refs.recordVideoPlayer.play(this.videoUrl)
              }
            })
          })
      }
    },
    getUrlByStreamInfo() {
      const keys = this.playerUrls[this.activePlayer]
      if (location.protocol === 'https:') {
        this.videoUrl = this.streamInfo[keys[1]]
      } else {
        this.videoUrl = this.streamInfo[keys[0]]
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
        const loading = this.$loading({
          lock: true,
          text: '正在请求录像',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        })
        this.$store.dispatch('gbRecord/startDownLoad', [
          this.deviceId, this.channelId, row.startTime, row.endTime, this.playSpeedRange[this.playSpeedRange.length - 1]
        ])
          .then(streamInfo => {
            this.$refs.recordDownload.openDialog(this.deviceId, this.channelId, streamInfo.app, streamInfo.stream, streamInfo.mediaServerId)
          })
          .finally(() => {
            loading.close()
          })
      }
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
  height: 95px;
}

.player-option-box-bottom {
  width: 100%;
  position: absolute;
  bottom: 0;
}

.time-line-show {
  position: relative;
  color: rgba(250, 249, 249, 0.89);
  top: -24px;
  height: 0;
  width: stretch;
  float: left;
  text-align: center;
  text-shadow: 1px 0 #5f6b7c, -1px 0 #5f6b7c, 0 1px #5f6b7c, 0 -1px #5f6b7c, 1.1px 1.1px #5f6b7c, 1.1px -1.1px #5f6b7c, -1.1px 1.1px #5f6b7c, -1.1px -1.1px #5f6b7c;
}
.play-box {
  width: 100%; aspect-ratio: 16 / 9;
  background-color: #000000;
  height: calc(100vh - 220px);
}
.play-box-fullscreen {
  height: 100vh !important;
}
</style>
