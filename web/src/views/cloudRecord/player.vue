<template>
  <div id="cloudRecordPlayer" style="height: 100%">
    <div class="cloud-record-playBox" :style="playBoxStyle">
      <playerTabs
        ref="recordVideoPlayer"
        :show-button="false"
        :showTab="false"
        @playTimeChange="showPlayTimeChange"
        @playStatusChange="playingChange"
        @player-changed="onPlayerChanged"
      />
    </div>
    <div class="cloud-record-player-option-box">
      <div class="cloud-record-show-time">
        {{showPlayTimeValue}}
      </div>
      <div class="cloud-record-time-process" ref="timeProcess" @click="timeProcessClick($event)"
           @mouseenter="timeProcessMouseEnter($event)" @mousemove="timeProcessMouseMove($event)"
           @mouseleave="timeProcessMouseLeave($event)">
        <div v-if="streamInfo">
          <div class="cloud-record-time-process-value" :style="playTimeValue"></div>
          <transition name="el-fade-in-linear">
            <div v-show="showTimeLeft" class="cloud-record-time-process-title" :style="playTimeTitleStyle" >{{showPlayTimeTitle}}</div>
          </transition>
        </div>
      </div>
      <div class="cloud-record-show-time">
        {{showPlayTimeTotal}}
      </div>
    </div>
    <div style="height: 40px; background-color: #383838; display: grid; grid-template-columns: 1fr auto 1fr">
      <div style="text-align: left;">
        <div class="cloud-record-record-play-control" style="background-color: transparent; box-shadow: 0 0 10px transparent">
          <a v-if="showListCallback" target="_blank" class="cloud-record-record-play-control-item iconfont icon-list" title="列表" @click="sidebarControl()" />
          <a target="_blank" class="cloud-record-record-play-control-item iconfont icon-camera1196054easyiconnet" title="截图" @click="snap()" />
        </div>
      </div>
      <div style="text-align: center;">
        <div class="cloud-record-record-play-control">
          <a v-if="!lastDiable" target="_blank" class="cloud-record-record-play-control-item iconfont icon-diyigeshipin" title="上一个" @click="playLast()" />
          <a v-else style="color: #acacac; cursor: not-allowed" target="_blank" class="cloud-record-record-play-control-item iconfont icon-diyigeshipin" title="上一个" />
          <a target="_blank" class="cloud-record-record-play-control-item iconfont icon-kuaijin" title="快退五秒" @click="seekBackward()" />
          <a target="_blank" class="cloud-record-record-play-control-item iconfont icon-stop1" style="font-size: 14px" title="停止" @click="stopPLay()" />
          <a v-if="playing" target="_blank" class="cloud-record-record-play-control-item iconfont icon-zanting" title="暂停" @click="pausePlay()" />
          <a v-if="!playing" target="_blank" class="cloud-record-record-play-control-item iconfont icon-kaishi" title="播放" @click="play()" />
          <a target="_blank" class="cloud-record-record-play-control-item iconfont icon-houtui" title="快进五秒" @click="seekForward()" />
          <a v-if="!nextDiable" target="_blank" class="cloud-record-record-play-control-item iconfont icon-zuihouyigeshipin" title="下一个" @click="playNext()" />
          <a v-else style="color: #acacac; cursor: not-allowed" target="_blank" class="cloud-record-record-play-control-item iconfont icon-zuihouyigeshipin" title="下一个" @click="playNext()" />
          <el-dropdown @command="changePlaySpeed" :popper-append-to-body='false' >
            <a target="_blank" class="cloud-record-record-play-control-item record-play-control-speed" title="倍速播放">{{ playSpeed }}X</a>
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
        <div class="cloud-record-record-play-control" style="background-color: transparent; box-shadow: 0 0 10px transparent">
          <div class="cloud-record-record-play-control-item record-play-control-player">

            <el-dropdown @command="changePlayerType" :popper-append-to-body='false' >
              <a target="_blank" class="cloud-record-record-play-control-item record-play-control-speed" title="选择播放器">{{ playerLabel }}</a>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item v-for="p in playerList" :key="p.key" :command="p.key">{{ p.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
          <a v-if="!isFullScreen" target="_blank" class="cloud-record-record-play-control-item iconfont icon-fangdazhanshi" title="全屏" @click="fullScreen()" />
          <a v-else target="_blank" class="cloud-record-record-play-control-item iconfont icon-suoxiao1" title="全屏" @click="fullScreen()" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import playerTabs from '../common/playerTabs.vue'
import moment from 'moment'
import momentDurationFormatSetup from 'moment-duration-format'
import screenfull from 'screenfull'

momentDurationFormatSetup(moment)

export default {
  name: 'CloudRecordPlayer',
  components: { playerTabs },
  props: ['showListCallback', 'showNextCallback', 'showLastCallback', 'lastDiable', 'nextDiable'],
  data() {
    return {
      showSidebar: false,
      streamInfo: null,
      timeLen: null,
      startTime: null,
      showTimeLeft: null,
      isMousedown: false,
      loading: false,
      playerTime: null,
      playSpeed: 1,
      playLoading: false,
      isFullScreen: false,
      playing: false,
      initTime: null,
      playerList: [],
      playerLabel: 'Jessibuca',
      playSpeedRange: [1, 2, 4, 6, 8, 16, 20]
    }
  },
  computed: {
    playBoxStyle() {
      return this.isFullScreen ? { height: 'calc(100vh - 61px)' } : { height: '100%' }
    },
    showPlayTimeValue() {
      return this.streamInfo === null ? '--:--:--' : moment.duration(this.playerTime, 'milliseconds').format('hh:mm:ss', {
        trim: false
      })
    },
    playTimeValue() {
      return { width: this.playerTime/this.streamInfo.duration * 100 + '%' }
    },
    showPlayTimeTotal() {
      if (this.streamInfo === null) {
        return '--:--:--'
      }else {
        return moment.duration(this.streamInfo.duration, 'milliseconds').format('hh:mm:ss', {
          trim: false
        })
      }
    },
    playTimeTotal() {
      return { left: `calc(${this.playerTime/this.streamInfo.duration}*100% - 6px)` }
    },
    playTimeTitleStyle() {
      return { left: (this.showTimeLeft - 16) + 'px' }
    },
    showPlayTimeTitle() {
      if (this.showTimeLeft) {
        let time = this.showTimeLeft / this.$refs.timeProcess.clientWidth * this.streamInfo.duration
        let realTime = this.timeLen/this.streamInfo.duration * time + this.startTime
        return `${moment(time).format('mm:ss')}(${moment(realTime).format('HH:mm:ss')})`
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
    this.updatePlayerList()
  },
  destroyed() {
    this.$destroy('recordVideoPlayer')
  },
  methods: {
    updatePlayerList() {
      if (this.$refs.recordVideoPlayer) {
        this.playerList = this.$refs.recordVideoPlayer.getPlayerList()
        const active = this.$refs.recordVideoPlayer.getActivePlayer()
        const p = this.playerList.find(p => p.key === active)
        this.playerLabel = p ? p.label : 'Jessibuca'
      }
    },
    onPlayerChanged(key) {
      const p = this.playerList.find(p => p.key === key)
      this.playerLabel = p ? p.label : 'Jessibuca'
    },
    timeProcessMouseup(event) {
      this.isMousedown = false
    },
    timeProcessMousemove(event) {

    },
    timeProcessClick(event) {
      let x = event.offsetX
      let clientWidth = this.$refs.timeProcess.clientWidth
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
      this.showListCallback(this.showSidebar)
    },
    snap() {
      if (this.$refs.recordVideoPlayer) {
        this.$refs.recordVideoPlayer.screenshot()
      }
    },
    refresh() {
      if (this.$refs.recordVideoPlayer) {
        this.$refs.recordVideoPlayer.destroy()
      }
    },
    playLast() {
      this.showLastCallback()
    },
    playNext() {
      this.showNextCallback()
    },
    changePlaySpeed(speed) {
      this.playSpeed = speed
      this.$store.dispatch('cloudRecord/speed', {
        mediaServerId: this.streamInfo.mediaServerId,
        app: this.streamInfo.app,
        stream: this.streamInfo.stream,
        key: this.streamInfo.key,
        speed: this.playSpeed,
        schema: 'ts'
      })
      if (this.$refs.recordVideoPlayer) {
        this.$refs.recordVideoPlayer.setPlaybackRate(this.playSpeed)
      }
    },
    changePlayerType(playerType) {
      if (this.$refs.recordVideoPlayer) {
        this.$refs.recordVideoPlayer.switchPlayer(playerType)
        const p = this.playerList.find(p => p.key === playerType)
        this.playerLabel = p ? p.label : 'Jessibuca'
      }
    },
    seekBackward() {
      this.seekRecord(this.playerTime - 5 * 1000)
    },
    seekForward() {
      this.seekRecord(this.playerTime + 5 * 1000)
    },
    stopPLay() {
      if (this.$refs.recordVideoPlayer) {
        this.$refs.recordVideoPlayer.destroy()
      }
      this.streamInfo = null
      this.playerTime = null
      this.playSpeed = 1
    },
    pausePlay() {
      if (this.$refs.recordVideoPlayer) {
        this.$refs.recordVideoPlayer.pause()
      }
    },
    play() {
      if (this.$refs.recordVideoPlayer) {
        if (this.$refs.recordVideoPlayer.$refs[this.$refs.recordVideoPlayer.getActivePlayer()] &&
            this.$refs.recordVideoPlayer.$refs[this.$refs.recordVideoPlayer.getActivePlayer()].loaded) {
          this.$refs.recordVideoPlayer.$refs[this.$refs.recordVideoPlayer.getActivePlayer()].unPause()
        } else {
          this.playRecord()
        }
      }
    },
    fullScreen() {
      if (this.isFullScreen) {
        screenfull.exit()
        this.isFullScreen = false
        return
      }
      const playerWrapper = this.$refs.recordVideoPlayer ? this.$refs.recordVideoPlayer.$refs.playerWrapper : null
      const playerWidth = playerWrapper ? playerWrapper.clientWidth : 0
      const playerHeight = playerWrapper ? playerWrapper.clientHeight : 0
      screenfull.request(document.getElementById('cloudRecordPlayer'))
      screenfull.on('change', (event) => {
        if (this.$refs.recordVideoPlayer) {
          this.$refs.recordVideoPlayer.resize(playerWidth, playerHeight)
        }
        this.isFullScreen = screenfull.isFullscreen
      })
      this.isFullScreen = true
    },
    setStreamInfo(streamInfo, timeLen, startTime) {
      this.streamInfo = streamInfo
      this.timeLen = timeLen
      this.startTime = startTime
      this.$nextTick(() => {
        if (this.$refs.recordVideoPlayer) {
          console.log(streamInfo)
          this.$refs.recordVideoPlayer.setStreamInfo(streamInfo)
        }
      })
    },
    seekRecord(playSeekValue, callback) {
      this.$store.dispatch('cloudRecord/seek', {
        mediaServerId: this.streamInfo.mediaServerId,
        app: this.streamInfo.app,
        stream: this.streamInfo.stream,
        seek: playSeekValue,
        schema: 'fmp4'
      })
        .then((data) => {
          this.playerTime = playSeekValue
          if (callback) {
            callback(playSeekValue)
          }
        })
        .catch((error) => {
          console.log(error)
        })
    },
    showPlayTimeChange(val) {
      console.log(val)
      if (Number(val)) {
        this.playerTime = Number(val)
      }
    },
    playingChange(val) {
      this.playing = val
      if (!val) {
        this.stopPLay()
      }
    }
  }
}
</script>

<style>
.cloud-record-playBox {
  width: 100%;
  background-color: #000000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cloud-record-record-play-control {
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
.cloud-record-record-play-control-item {
  display: inline-block;
  padding: 0 10px;
  color: #fff;
  margin-right: 2px;
}
.cloud-record-record-play-control-item:hover {
  color: #1f83e6;
}
.cloud-record-record-play-control-speed {
  font-weight: bold;
  color: #fff;
  user-select: none;
}
.cloud-record-player-option-box {
  height: 20px;
  width: 100%;
  display: grid;
  grid-template-columns: 70px auto 70px;
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
.cloud-record-time-process-value1::after {
  content: '';
  display: block;
  width: 12px;
  height: 12px;
  background-color: rgb(192 190 190);
  border-radius: 5px;
  position: relative;
  top: -3px;
  right: -6px;
  float: right;
}
.cloud-record-time-process-title {
  width: fit-content;
  text-align: center;
  position: relative;
  top: -35px;
  color: rgb(217, 217, 217);
  font-size: 14px;
  text-shadow:
    -1px -1px 0 black,
    1px -1px 0 black,
    -1px 1px 0 black,
    1px 1px 0 black;
}
.record-play-control-player {
  width: fit-content;
  height: 32px;
}
</style>
