<template>
  <div id="recordDetail" class="app-container">
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
            <ul v-if="detailFiles.length >0" v-infinite-scroll="infiniteScroll" class="infinite-list record-list">
              <li v-for="(item,index) in detailFiles" :key="index" class="infinite-list-item record-list-item">
                <el-tag v-if="chooseFileIndex !== index" @click="chooseFile(index)">
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
        <div class="playBox" style="height: calc(100% - 24px); width: 100%; background-color: #000000">
          <div v-if="playLoading" style="position: relative; left: calc(50% - 32px); top: 43%; z-index: 100;color: #fff;float: left; text-align: center;">
            <div class="el-icon-loading" />
            <div style="width: 100%; line-height: 2rem">正在加载</div>
          </div>
          <h265web ref="recordVideoPlayer" :video-url="videoUrl" :height="'calc(100vh - 250px)'" :show-button="false" @playTimeChange="showPlayTimeChange" @playStatusChange="playingChange"/>
        </div>
        <div class="player-option-box">
          <div class="cloud-record-show-time">
            {{showPlayTimeValue}}
          </div>
          <div class="cloud-record-time-process" ref="timeProcess" @click="timeProcessMousedown($event)"
               @mouseenter="timeProcessMouseEnter($event)" @mousemove="timeProcessMouseMove($event)"
                @mouseleave="timeProcessMouseLeave($event)">
            <div v-if="streamInfo">
              <div class="cloud-record-time-process-value" :style="playTimeValue"></div>
              <div class="cloud-record-time-process-pointer" :style="playTimeTotal" @mousedown="timeProcessMousedown($event)"></div>
              <transition name="el-fade-in-linear">
                <div v-show="showTimeLeft" class="cloud-record-time-process-title" :style="playTimeTitleStyle" >{{showPlayTimeTitle}}</div>
              </transition>
            </div>
          </div>
          <div class="cloud-record-show-time">
            {{showPlayTimeTotal}}
          </div>
        </div>
        <div style="height: 40px; background-color: #383838; display: grid; grid-template-columns: 1fr 600px 1fr">
          <div style="text-align: left;">
            <div class="record-play-control" style="background-color: transparent; box-shadow: 0 0 10px transparent">
              <a target="_blank" class="record-play-control-item iconfont icon-list" title="列表" @click="sidebarControl()" />
              <a target="_blank" class="record-play-control-item iconfont icon-camera1196054easyiconnet" title="截图" @click="snap()" />
              <!--              <a target="_blank" class="record-play-control-item iconfont icon-xiazai011" title="下载" @click="gbPause()" />-->
            </div>
          </div>
          <div style="text-align: center;">
            <div class="record-play-control">
              <a v-if="chooseFileIndex > 0" target="_blank" class="record-play-control-item iconfont icon-diyigeshipin" title="上一个" @click="playLast()" />
              <a v-else style="color: #acacac; cursor: not-allowed" target="_blank" class="record-play-control-item iconfont icon-diyigeshipin" title="上一个" />
              <a target="_blank" class="record-play-control-item iconfont icon-kuaijin" title="快退五秒" @click="seekBackward()" />
              <a target="_blank" class="record-play-control-item iconfont icon-stop1" style="font-size: 14px" title="停止" @click="stopPLay()" />
              <a v-if="playing" target="_blank" class="record-play-control-item iconfont icon-zanting" title="暂停" @click="pausePlay()" />
              <a v-if="!playing" target="_blank" class="record-play-control-item iconfont icon-kaishi" title="播放" @click="play()" />
              <a target="_blank" class="record-play-control-item iconfont icon-houtui" title="快进五秒" @click="seekForward()" />
              <a v-if="chooseFileIndex < detailFiles.length - 1" target="_blank" class="record-play-control-item iconfont icon-zuihouyigeshipin" title="下一个" @click="playNext()" />
              <a v-else style="color: #acacac; cursor: not-allowed" target="_blank" class="record-play-control-item iconfont icon-zuihouyigeshipin" title="下一个" @click="playNext()" />
              <el-dropdown @command="changePlaySpeed">
                <a target="_blank" class="record-play-control-item record-play-control-speed" title="倍速播放">{{ playSpeed }}X</a>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item
                    v-for="item in playSpeedRange"
                    :key="item"
                    :command="item"
                  >{{ item }}X</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </div>
          <div style="text-align: right;">
            <div class="record-play-control" style="background-color: transparent; box-shadow: 0 0 10px transparent">
              <a v-if="!isFullScreen" target="_blank" class="record-play-control-item iconfont icon-fangdazhanshi" title="全屏" @click="fullScreen()" />
              <a v-else target="_blank" class="record-play-control-item iconfont icon-suoxiao1" title="全屏" @click="fullScreen()" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import h265web from '../common/h265web.vue'
import moment from 'moment'
import screenfull from 'screenfull'

export default {
  name: 'CloudRecordDetail',
  components: {
    h265web
  },
  data() {
    return {
      showSidebar: false,
      app: this.$route.params.app,
      stream: this.$route.params.stream,
      mediaServerId: null,
      dateFilesObj: [],
      mediaServerList: [],
      detailFiles: [],
      videoUrl: null,
      streamInfo: null,
      showTimeLeft: null,
      isMousedown: false,
      loading: false,
      chooseDate: null,
      playTime: null,
      playerTime: null,
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
      playSpeedRange: [1, 2, 4, 6, 8, 16, 20]
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

    showPlayTimeValue() {
      return this.streamInfo !== null ? moment(this.playerTime).format('mm:ss') : '--:--'
    },
    playTimeValue() {
      return { width: this.playerTime/this.streamInfo.duration * 100 + '%' }
    },
    showPlayTimeTotal() {
      return this.streamInfo !== null ? moment(this.streamInfo.duration).format('mm:ss') : '--:--'
    },
    playTimeTotal() {
      return { left: `calc(${this.playerTime/this.streamInfo.duration * 100}% - 6px)` }
    },
    playTimeTitleStyle() {
      return { left: (this.showTimeLeft - 16) + 'px' }
    },
    showPlayTimeTitle() {
      if (this.showTimeLeft) {
        let time = this.showTimeLeft / this.$refs.timeProcess.clientWidth * this.streamInfo.duration
        return moment(time).format('mm:ss')
      }else {
        return ''
      }
    }
  },
  created() {
    document.addEventListener('mousemove', this.timeProcessMousemove)
    document.addEventListener('mouseup', this.timeProcessMouseup)
  },
  mounted() {
    // 查询当年有视频的日期
    this.getDateInYear(() => {
      if (Object.values(this.dateFilesObj).length > 0) {
        this.chooseDate = Object.values(this.dateFilesObj)[Object.values(this.dateFilesObj).length - 1]
        this.dateChange()
      }
    })
  },
  destroyed() {
    this.$destroy('recordVideoPlayer')
  },
  methods: {
    timeProcessMouseup(event) {
      this.isMousedown = false
    },
    timeProcessMousemove(event) {

    },
    timeProcessClick(event) {
      let x = event.offsetX
      let clientWidth = event.target.clientWidth
      this.seekRecord(x / clientWidth * this.streamInfo.duration)
    },
    timeProcessMousedown(event) {
      this.isMousedown = true
    },
    timeProcessMouseEnter(event) {
      this.showTimeLeft = event.offsetX
    },
    timeProcessMouseMove(event) {
      this.showTimeLeft = event.offsetX
    },
    timeProcessMouseLeave(event) {
      this.showTimeLeft = null
    },
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
    changePlaySpeed(speed) {
      // 倍速播放
      this.playSpeed = speed
      this.$store.dispatch('cloudRecord/speed', {
        mediaServerId: this.streamInfo.mediaServerId,
        app: this.streamInfo.app,
        stream: this.streamInfo.stream,
        key: this.streamInfo.key,
        speed: this.playSpeed,
        schema: 'ts'
      })
      this.$refs.recordVideoPlayer.setPlaybackRate(this.playSpeed)
    },
    seekBackward() {
      // 快退五秒
      this.playSeekValue -= 5 * 1000
      this.playRecord()
    },
    seekForward() {
      // 快进五秒
      this.playSeekValue += 5 * 1000
      this.playRecord()
    },
    stopPLay() {
      // 停止
      this.$refs.recordVideoPlayer.destroy()
      this.streamInfo = null
    },
    pausePlay() {
      // 暂停
      this.$refs.recordVideoPlayer.pause()
      // TODO
    },
    play() {
      if (this.$refs.recordVideoPlayer.loaded) {
        this.$refs.recordVideoPlayer.unPause()
      } else {
        this.playRecord()
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
      this.$refs.recordVideoPlayer.destroy()
      this.streamInfo = null
      this.chooseFileIndex = null
      this.detailFiles = []
      this.currentPage = 1
      const chooseFullDate = new Date(this.chooseDate + ' ' + this.timeFormat)
      if (chooseFullDate.getFullYear() !== this.queryDate.getFullYear() ||
        chooseFullDate.getMonth() !== this.queryDate.getMonth()) {
        this.queryDate = chooseFullDate
        this.getDateInYear()
      }
      this.queryRecordDetails()
    },
    infiniteScroll() {
      if (this.total > this.detailFiles.length) {
        this.currentPage++
        this.queryRecordDetails()
      }
    },
    queryRecordDetails: function(callback) {
      this.timeSegments = []
      this.$store.dispatch('cloudRecord/queryList', {
        app: this.app,
        stream: this.stream,
        startTime: this.chooseDate + ' 00:00:00',
        endTime: this.chooseDate + ' 23:59:59',
        page: this.currentPage,
        count: this.count,
        mediaServerId: this.mediaServerId,
        ascOrder: true
      })
        .then(data => {
          this.total = data.total
          this.detailFiles = this.detailFiles.concat(data.list)
          const temp = new Set()
          this.initTime = Number.parseInt(this.detailFiles[0].startTime)
          for (let i = 0; i < this.detailFiles.length; i++) {
            temp.add(this.detailFiles[i].mediaServerId)
            this.timeSegments.push({
              beginTime: Number.parseInt(this.detailFiles[i].startTime),
              endTime: Number.parseInt(this.detailFiles[i].endTime),
              color: '#01901d',
              startRatio: 0.7,
              endRatio: 0.85,
              index: i
            })
          }
          this.mediaServerList = Array.from(temp)
          if (this.mediaServerList.length === 1) {
            this.mediaServerId = this.mediaServerList[0]
          }
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.loading = false
          if (callback) callback()
        })
    },
    chooseFile(index) {
      this.chooseFileIndex = index
      this.playRecord()
    },
    playRecord() {
      if (this.$refs.recordVideoPlayer.playing) {
        this.$refs.recordVideoPlayer.destroy()
      }
      this.$store.dispatch('cloudRecord/loadRecord', {
        app: this.app,
        stream: this.stream,
        cloudRecordId: this.detailFiles[this.chooseFileIndex].id
      })
        .then(data => {
          this.playerTime = 0
          this.streamInfo = data
          if (location.protocol === 'https:') {
            this.videoUrl = data['wss_flv']
          } else {
            this.videoUrl = data['ws_flv']
          }
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.playLoading = false
        })

    },
    seekRecord(playSeekValue) {
      this.$store.dispatch('cloudRecord/seek', {
        mediaServerId: this.streamInfo.mediaServerId,
        app: this.streamInfo.app,
        stream: this.streamInfo.stream,
        seek: playSeekValue,
        schema: 'fmp4'
      })
        .then((data) => {
          this.playerTime = playSeekValue
        })
        .catch((error) => {
          console.log(error)
        })
    },
    downloadFile(file) {
      this.$store.dispatch('cloudRecord/getPlayPath', file.id)
        .then(data => {
          const link = document.createElement('a')
          link.target = '_blank'
          if (location.protocol === 'https:') {
            link.href = data.httpsPath + '&save_name=' + file.fileName
          } else {
            link.href = data.httpPath + '&save_name=' + file.fileName
          }
          link.click()
        })
        .catch((error) => {
          console.log(error)
        })
    },
    backToList() {
      this.$router.back()
    },
    getFileShowName(item) {
      return moment(item.startTime).format('HH:mm:ss') + '-' + moment(item.endTime).format('HH:mm:ss')
    },

    showPlayTimeChange(val) {
      this.playerTime += val * 1000
    },
    playingChange(val) {
      this.playing = val
    },
    getDateInYear(callback) {
      this.dateFilesObj = {}
      this.$store.dispatch('cloudRecord/queryListByData', {
        app: this.app,
        stream: this.stream,
        year: this.queryDate.getFullYear(),
        month: this.queryDate.getMonth() + 1,
        mediaServerId: this.mediaServerId
      })
        .then((data) => {
          if (data.length > 0) {
            for (let i = 0; i < data.length; i++) {
              this.dateFilesObj[data[i]] = data[i]
            }
            console.log(this.dateFilesObj)
          }
          if (callback) callback()
        })
        .catch((error) => {
          console.log(error)
        })
    },
    goBack() {
      this.$router.push('/cloudRecord')
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
  height: 20px;
  width: 100%;
  display: grid;
  grid-template-columns: 50px auto 50px;
  background-color: rgb(0, 0, 0);
}
.cloud-record-time-process {
  width: 100%;
  height: 8px;
  margin: 6px 0 ;
  border-radius: 4px;
  border: 1px solid #505050;
  background-color: rgb(56, 56, 56);
  cursor: pointer;
}
.cloud-record-show-time {
  color: #FFFFFF;
  text-align: center;
  font-size: 14px;
  line-height: 20px
}
.cloud-record-time-process-value {
  width: 100%;
  height: 6px;
  background-color: rgb(162, 162, 162);
}
.cloud-record-time-process-pointer {
  width: 12px;
  height: 12px;
  background-color: rgb(192 190 190);
  border-radius: 5px;
  position: relative;
  top: -9px;
}
.cloud-record-time-process-title {
  width: 40px;
  text-align: center;
  position: relative;
  top: -45px;
  color: rgb(192 190 190);
  font-size: 14px;
}
</style>
