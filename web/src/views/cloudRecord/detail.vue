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
        <cloudRecordPlayer ref="cloudRecordPlayer" :showListCallback="sidebarControl" :showNextCallback="playNext"
                             :showLastCallback="playLast" :lastDiable="lastBtnDiable" :nextDiable="nextBtnDiable" ></cloudRecordPlayer>
      </div>
    </div>
  </div>
</template>

<script>

import moment from 'moment'
import momentDurationFormatSetup from 'moment-duration-format'
import screenfull from 'screenfull'
import cloudRecordPlayer from './cloudRecordPlayer.vue'

momentDurationFormatSetup(moment)

export default {
  name: 'CloudRecordDetail',
  components: {
    cloudRecordPlayer
  },
  data() {
    return {
      showSidebar: false,
      app: this.$route.params.app,
      stream: this.$route.params.stream,
      mediaServerId: null,
      dateFilesObj: {},
      mediaServerList: [],
      detailFiles: [],
      videoUrl: null,
      streamInfo: null,
      showTimeLeft: null,
      isMousedown: false,
      loading: false,
      chooseDate: null,
      playerTime: null,
      playSpeed: 1,
      chooseFileIndex: null,
      queryDate: new Date(),
      currentPage: 1,
      count: 1000000, // TODO 分页导致滑轨视频有效值无法获取完全
      total: 0,
      playLoading: false,
      isFullScreen: false,
      playing: false,
      taskTimeRange: [],
      initTime: null,
      timelineControl: false,
      showOtherSpeed: true,
      lastBtnDiable: this.chooseFileIndex - 1 <= 0,
      nextBtnDiable: false,
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
    sidebarControl(status) {
      this.showSidebar = status
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
        this.nextBtnDiable = true
        return
      }
      if (this.chooseFileIndex < Object.values(this.dateFilesObj).length - 1) {
        this.nextBtnDiable = true
      }
      this.chooseFile(this.chooseFileIndex + 1)

    },
    stopPLay() {
      // 停止
      this.$refs.cloudRecordPlayer.stopPLay()
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
      this.$refs.cloudRecordPlayer.stopPLay()
      this.streamInfo = null
      this.chooseFileIndex = null
      this.detailFiles = []
      this.currentPage = 1
      const chooseFullDate = new Date(this.chooseDate + ' 00:00:00')
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
      this.$refs.cloudRecordPlayer.stopPLay()
      this.$store.dispatch('cloudRecord/loadRecord', {
        app: this.app,
        stream: this.stream,
        cloudRecordId: this.detailFiles[this.chooseFileIndex].id
      })
        .then(data => {
          this.playerTime = 0
          this.$refs.cloudRecordPlayer.setStreamInfo(data)
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.playLoading = false
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
    getFileShowName(item) {
      return moment(item.startTime).format('HH:mm:ss') + '-' + moment(item.endTime).format('HH:mm:ss')
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
  }
}
</script>

<style scoped>
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
.cloud-record-time-process-value::after {
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
    -1px -1px 0 black, /* 左上角阴影 */
    1px -1px 0 black, /* 右上角阴影 */
    -1px 1px 0 black, /* 左下角阴影 */
    1px 1px 0 black; /* 右下角阴影 */
}
</style>
