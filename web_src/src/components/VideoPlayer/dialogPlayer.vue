<template>
  <a-modal
    title="视频播放"
    :visible="isShowPlayer"
    :width="900"
    :footer="null"
    @cancel="close()">
    <player
      ref="videoPlayer"
      :visible="showVideoDialog"
      :videoUrl="videoUrl"
      :error="videoError"
      :message="videoError"
      :height="false"
      :hasAudio="hasAudio"
      @updateData = 'updateTable'
      fluent autoplay live>
    </player>
    <a-tabs default-active-key="ptz" @tabClick="tabHandleClick">
      <a-tab-pane key="ptz" tab="云台控制">
        <div style="display: flex; justify-content: left;">
          <a-row :gutter="[16,0]">
            <a-col :span="6">
              <div class="control-wrapper">
                <div class="control-btn control-top" @mousedown="ptzCamera('up')" @mouseup="ptzCamera('stop')">
                  <a-icon type="caret-up"/>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-left" @mousedown="ptzCamera('left')" @mouseup="ptzCamera('stop')">
                  <a-icon type="caret-left"/>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-bottom" @mousedown="ptzCamera('down')" @mouseup="ptzCamera('stop')">
                  <a-icon type="caret-down"/>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-btn control-right" @mousedown="ptzCamera('right')" @mouseup="ptzCamera('stop')">
                  <a-icon type="caret-right"/>
                  <div class="control-inner-btn control-inner"></div>
                </div>
                <div class="control-round">
                  <div class="control-round-inner">
                    <a-icon type="pause-circle"/>
                  </div>
                </div>
                <div style="position: absolute; left: 7.25rem; top: 1.25rem" @mousedown="ptzCamera('zoomin')"
                     @mouseup="ptzCamera('stop')">
                  <a-icon type="zoom-in" class="control-zoom-btn" style="font-size: 1.875rem;"/>
                </div>
                <div style="position: absolute; left: 7.25rem; top: 3.25rem; font-size: 1.875rem;"
                     @mousedown="ptzCamera('zoomout')" @mouseup="ptzCamera('stop')">
                  <a-icon type="zoom-out" class="control-zoom-btn"/>
                </div>
                <div style="position: absolute; left: 4px; top: 7rem; width: 9rem;">
                  <a-slider v-model="controSpeed" :max="255"/>
                </div>
              </div>
            </a-col>
            <a-col :span="18">
              <div class="control-panel">
                <a-space>
                  <div>
                    <a-tag style="width: 5rem;text-align: center;font-size: 12px">预置位编号</a-tag>
                    <a-input-number size="small"
                                    v-model="presetPos"
                                    :precision="0"
                                    :step="1"
                                    :min="1"
                                    :max="255"></a-input-number>
                  </div>
                  <a-button size="small" style="font-size: 12px" @click="presetPosition(129, presetPos)">设置</a-button>
                  <a-button size="small" style="font-size: 12px" @click="presetPosition(131, presetPos)">删除</a-button>
                  <a-button size="small" style="font-size: 12px;margin-left: 12rem" type="primary"
                            @click="presetPosition(130, presetPos)">调用
                  </a-button>
                </a-space>
                <a-divider style="margin: 0.5rem 0"/>
                <a-space>
                  <div>
                    <a-tag style="width: 5rem;text-align: center;font-size: 12px">巡航速度</a-tag>
                    <a-input-number
                      size="small"
                      v-model="cruisingSpeed"
                      :precision="0"
                      :min="1"
                      :max="4095"></a-input-number>
                  </div>
                  <a-button size="small" style="font-size: 12px"
                            @click="setSpeedOrTime(134, cruisingGroup, cruisingSpeed)">设置
                  </a-button>
                  <div>
                    <a-tag style="width: 5rem;text-align: center;font-size: 12px">停留时间</a-tag>
                    <a-input-number
                      size="small"
                      v-model="cruisingTime"
                      :precision="0"
                      :min="1"
                      :max="4095"></a-input-number>
                  </div>
                  <a-button style="font-size: 12px;" size="small"
                            @click="setSpeedOrTime(135, cruisingGroup, cruisingTime)">设置
                  </a-button>
                </a-space>
                <a-space style="margin-top: 0.5rem">
                  <div>
                    <a-tag style="width: 5rem;text-align: center;font-size: 12px">巡航组编号</a-tag>
                    <a-input-number
                      size="small"
                      v-model="cruisingGroup"
                      :precision="0"
                      :min="0"
                      :max="255"></a-input-number>
                  </div>
                  <a-button size="small" style="font-size: 12px" @click="setCommand(132, cruisingGroup, presetPos)">
                    添加点
                  </a-button>
                  <a-button size="small" style="font-size: 12px" @click="setCommand(133, cruisingGroup, presetPos)">
                    删除点
                  </a-button>
                  <a-button size="small" style="font-size: 12px" @click="setCommand(133, cruisingGroup, 0)">删除组
                  </a-button>
                  <a-button size="small" style="font-size: 12px;margin-left: 7.2rem" type="primary"
                            @click="setCommand(136, cruisingGroup, 0)">巡航
                  </a-button>
                </a-space>
                <a-divider style="margin: 0.5rem 0"/>
                <a-space>
                  <div>
                    <a-tag style="width: 5rem;text-align: center;font-size: 12px">扫描速度</a-tag>
                    <a-input-number
                      size="small"
                      v-model="scanSpeed"
                      :precision="0"
                      :min="1"
                      :max="4095"></a-input-number>
                  </div>
                  <a-button size="small" style="font-size: 12px" @click="setSpeedOrTime(138, scanGroup, scanSpeed)">设置
                  </a-button>
                  <a-button size="small" style="font-size: 12px;margin-left: 15.25rem" type="primary"
                            @click="setCommand(137, scanGroup, 0)">扫描
                  </a-button>
                </a-space>
                <a-space style="margin-top: 0.5rem">
                  <div>
                    <a-tag style="width: 5rem;text-align: center;font-size: 12px">扫描组编号</a-tag>
                    <a-input-number
                      size="small"
                      v-model="scanGroup"
                      :precision="0"
                      :step="1"
                      :min="0"
                      :max="255"></a-input-number>
                  </div>
                  <a-button size="small" style="font-size: 12px" @click="setCommand(137, scanGroup, 1)">左边界</a-button>
                  <a-button size="small" style="font-size: 12px" @click="setCommand(137, scanGroup, 2)">右边界</a-button>
                  <a-button size="small" style="font-size: 12px;margin-left: 10.95rem" type="danger"
                            @click="ptzCamera('stop')">停止
                  </a-button>
                </a-space>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-tab-pane>
      <a-tab-pane key="codec" tab="编码信息">
        <a-skeleton :loading="tracksNotLoaded" active/>
        <a-card v-if="!tracksNotLoaded" size="small" :bordered="false"
                :body-style="{'text-align': 'right','margin-top' : '0','padding-top' : '0'}">
          无法播放或者没有声音?&nbsp&nbsp&nbsp试一试&nbsp
          <a-button size="small" type="primary" v-if="!coverPlaying" @click="coverPlay">转码播放</a-button>
          <a-button size="small" type="danger" v-if="coverPlaying" @click="convertStopClick">停止转码</a-button>
        </a-card>
        <div v-for="(item, index) in tracks" style="width: 50%;float: left;">
          <a-card size="small" title="视频编码信息" v-if="item.codec_type == 0" style="margin-right: 1rem">
            <p>格式: {{ item.codec_id_name }}</p>
            <p>类型: 视频</p>
            <p>分辨率: {{ item.width }} x {{ item.height }}</p>
            <p>帧率: {{ item.fps }}</p>
          </a-card>
          <a-card size="small" title="音频编码信息" v-if="item.codec_type == 1">
            <p>格式: {{ item.codec_id_name }}</p>
            <p>类型: 音频</p>
            <p>采样位数: {{ item.sample_bit }}</p>
            <p>采样率: {{ item.sample_rate }}</p>
          </a-card>
        </div>
      </a-tab-pane>
    </a-tabs>

  </a-modal>

</template>

<script>
import player from './jessibuca'
import {convertStop, coverPlay, getMediaInfo, ptzCamera, ptzController} from "@/api/deviceList";

export default {
  name: 'dialogPlayer',
  props: {},
  components: {
    player
  },
  computed: {
    getPlayerShared: function () {
      return {
        sharedUrl: window.location.origin + '/#/play/wasm/' + encodeURIComponent(this.videoUrl),
        sharedIframe: '<iframe src="' + window.location.origin + '/#/play/wasm/' + encodeURIComponent(this.videoUrl) + '"></iframe>',
        sharedRtmp: this.videoUrl
      };
    }
  },
  created() {
  },
  data() {
    return {
      video: 'http://lndxyj.iqilu.com/public/upload/2019/10/14/8c001ea0c09cdc59a57829dabc8010fa.mp4',
      videoUrl: '',
      videoHistory: {
        date: '',
        searchHistoryResult: [] //媒体流历史记录搜索结果
      },
      isShowPlayer: false,
      showVideoDialog: true,
      streamId: '',
      app: '',
      mediaServerId: '',
      convertKey: '',
      deviceId: '',
      channelId: '',
      tabActiveName: 'media',
      hasAudio: false,
      loadingRecords: false,
      recordsLoading: false,
      controSpeed: 30,
      timeVal: 0,
      timeMin: 0,
      timeMax: 1440,
      presetPos: 1,
      cruisingSpeed: 100,
      cruisingTime: 5,
      cruisingGroup: 0,
      scanSpeed: 100,
      scanGroup: 0,
      tracks: [],
      coverPlaying: false,
      recordPlay: "",
      showPtz: true,
      showRrecord: true,
      tracksNotLoaded: true,
    };
  },
  methods: {
    updateTable(){
      this.$emit('updateTable')
    },
    tabHandleClick: function (tab, event) {
      this.tracks = [];
      this.tracksNotLoaded = true;
      if (tab === "codec") {
        let params = {
          mediaServerId: this.mediaServerId,
          app: this.app,
          streamId: this.streamId
        }
        getMediaInfo(params).then(data => {
          console.log(data)
          this.tracksNotLoaded = false
          if (data && data.code === 0 && data.online) {
            this.tracks = data.tracks
          } else {
            this.$message.error("获取编码信息失败")
          }
        }).catch(err => {
          console.log(err)
        })
      }
    },
    openDialog(deviceId, channelId, param) {
      this.channelId = channelId;
      this.deviceId = deviceId;
      this.streamId = "";
      this.mediaServerId = "";
      this.app = "";
      this.videoUrl = "";
      this.isShowPlayer = true;
      if (!!this.$refs.videoPlayer) {
        this.$refs.videoPlayer.pause();
      }
      this.play(param.streamInfo, param.hasAudio)
    },
    timeAxisSelTime: function (val) {
      console.log(val)
    },
    play(streamInfo, hasAudio) {
      this.hasAudio = hasAudio;
      this.videoUrl = this.getUrlByStreamInfo(streamInfo);
      this.streamId = streamInfo.streamId;
      this.app = streamInfo.app;
      this.mediaServerId = streamInfo.mediaServerId;
      this.playFromStreamInfo(false, streamInfo)
    },
    getUrlByStreamInfo(streamInfo) {
      if (location.protocol === "https:") {
        if (streamInfo.wss_flv === null) {
          this.$message({
            showClose: true,
            message: '媒体服务器未配置ssl端口',
            type: 'error'
          });
        } else {
          return streamInfo.wss_flv;
        }

      } else {
        return streamInfo.ws_flv;
      }
    },
    coverPlay() {
      this.coverPlaying = true;
      this.$refs.videoPlayer.pause()
      coverPlay({streamId: this.streamId}).then(res => {
        console.log(res.code, res.key)
        if (res.code === 0) {
          this.convertKey = res.key
          if (res.data){
            setTimeout(() => {
              this.playFromStreamInfo(false, res.data);
            }, 2000)
          }else{
            this.coverPlaying = false;
            this.$message.error('转码失败');
          }
        } else {
          this.coverPlaying = false;
          this.$message.error('转码失败');
        }
      }).catch(err => {
        console.error(err)
        this.coverPlaying = false;
        this.$message.error('播放错误');
      })
    },
    convertStopClick() {
      this.convertStop(() => {
        this.$refs.videoPlayer.play(this.videoUrl)
      });
    },
    convertStop(callback) {
      this.coverPlaying = false;
      this.convertKey = "";
      this.$refs.videoPlayer.pause()
      convertStop({convertKey: this.convertKey}).then(res => {
        if (res.code === 0) {
          console.log(res.msg)
        } else {
          console.error(res.msg)
        }
        if (callback) callback();
      }).catch(err => {
        console.error(err)
      })
    },

    playFromStreamInfo: function (realHasAudio, streamInfo) {
      this.showVideoDialog = true;
      this.hasaudio = realHasAudio && this.hasaudio;
      this.$refs.videoPlayer.play(this.getUrlByStreamInfo(streamInfo))
    },

    close() {
      console.log('关闭视频');
      if (!!this.$refs.videoPlayer) {
        this.$refs.videoPlayer.pause();
      }
      this.videoUrl = '';
      this.coverPlaying = false;
      this.showVideoDialog = false;
      if (this.convertKey != '') {
        this.convertStop();
      }
      this.convertKey = ''
      if (this.recordPlay != '') {
        this.stopPlayRecord();
      }
      this.recordPlay = ''
      this.isShowPlayer = false
    },

    copySharedInfo: function (data) {
      console.log('复制内容：' + data);
      this.coverPlaying = false;
      this.tracks = []
      let _this = this;
      this.$copyText(data).then(
        function (e) {
          _this.$message({
            showClose: true,
            message: '复制成功',
            type: 'success'
          });
        },
        function (e) {
          _this.$message({
            showClose: true,
            message: '复制失败，请手动复制',
            type: 'error'
          });
        }
      );
    },

    queryRecords: function () {
      if (!this.videoHistory.date) {
        return;
      }
      this.recordsLoading = true;
      this.videoHistory.searchHistoryResult = [];
      let that = this;
      var startTime = this.videoHistory.date + " 00:00:00";
      var endTime = this.videoHistory.date + " 23:59:59";
      this.$axios({
        method: 'get',
        url: '/api/gb_record/query/' + this.deviceId + '/' + this.channelId + '?startTime=' + startTime + '&endTime=' + endTime
      }).then(function (res) {
        // 处理时间信息
        that.videoHistory.searchHistoryResult = res.data.recordList;
        that.recordsLoading = false;
      }).catch(function (e) {
        console.log(e.message);
        // that.videoHistory.searchHistoryResult = falsificationData.recordData;
      });

    },
    onTimeChange: function (video) {
      // this.queryRecords()
    },
    playRecord: function (row) {
      let that = this;
      if (that.streamId != "") {
        that.stopPlayRecord(function () {
          that.streamId = "",
            that.playRecord(row);
        })
      } else {
        this.$axios({
          method: 'get',
          url: '/api/playback/start/' + this.deviceId + '/' + this.channelId + '?startTime=' + row.startTime + '&endTime=' +
            row.endTime
        }).then(function (res) {
          var streamInfo = res.data;
          that.app = streamInfo.app;
          that.streamId = streamInfo.streamId;
          that.mediaServerId = streamInfo.mediaServerId;
          that.videoUrl = that.getUrlByStreamInfo(streamInfo);
          that.recordPlay = true;
        });
      }
    },
    stopPlayRecord: function (callback) {
      this.$refs.videoPlayer.pause();
      this.videoUrl = '';
      this.$axios({
        method: 'get',
        url: '/api/playback/stop/' + this.deviceId + "/" + this.channelId
      }).then(function (res) {
        if (callback) callback()
      });
    },
    downloadRecord: function (row) {
      let that = this;
      if (that.streamId != "") {
        that.stopDownloadRecord(function () {
          that.streamId = "",
            that.downloadRecord(row);
        })
      } else {
        this.$axios({
          method: 'get',
          url: '/api/download/start/' + this.deviceId + '/' + this.channelId + '?startTime=' + row.startTime + '&endTime=' +
            row.endTime + '&downloadSpeed=4'
        }).then(function (res) {
          var streamInfo = res.data;
          that.app = streamInfo.app;
          that.streamId = streamInfo.streamId;
          that.mediaServerId = streamInfo.mediaServerId;
          that.videoUrl = that.getUrlByStreamInfo(streamInfo);
          that.recordPlay = true;
        });
      }
    },
    stopDownloadRecord: function (callback) {
      this.$refs.videoPlayer.pause();
      this.videoUrl = '';
      this.$axios({
        method: 'get',
        url: '/api/download/stop/' + this.deviceId + "/" + this.channelId
      }).then(function (res) {
        if (callback) callback()
      });
    },
    ptzCamera(command) {
      console.log('云台控制：' + command);
      let params = {
        deviceId: this.deviceId,
        channelId: this.channelId,
        command: command,
        horizonSpeed: this.controSpeed,
        verticalSpeed: this.controSpeed,
        zoomSpeed: this.controSpeed
      }
      ptzCamera(params).then(res => {
        if (res.success) {
          this.$message.success("请求成功")
        }
      })
    },
    //////////////////////播放器事件处理//////////////////////////
    videoError: function (e) {
      console.log("播放器错误：" + JSON.stringify(e));
    },
    presetPosition(cmdCode, presetPos) {
      console.log('预置位控制：' + this.presetPos + ' : 0x' + cmdCode.toString(16));
      let params = {
        deviceId: this.deviceId,
        channelId: this.channelId,
        cmdCode: cmdCode,
        parameter1: 0,
        parameter2: presetPos,
        combindCode2: 0
      }
      ptzController(params).then(res => {
        if (res.success) {
          this.$message.success("请求成功")
        }
      })
    },
    setSpeedOrTime(cmdCode, groupNum, parameter) {
      let parameter2 = parameter % 256;
      let combindCode2 = Math.floor(parameter / 256) * 16;
      console.log('前端控制：0x' + cmdCode.toString(16) + ' 0x' + groupNum.toString(16) + ' 0x' + parameter2.toString(16) + ' 0x' + combindCode2.toString(16));
      let params = {
        deviceId: this.deviceId,
        channelId: this.channelId,
        cmdCode: cmdCode,
        parameter1: groupNum,
        parameter2: parameter2,
        combindCode2: combindCode2
      }
      ptzController(params).then(res => {
        if (res.success) {
          this.$message.success("请求成功")
        }
      })
    },
    setCommand(cmdCode, groupNum, parameter) {
      console.log('前端控制：0x' + cmdCode.toString(16) + ' 0x' + groupNum.toString(16) + ' 0x' + parameter.toString(16) + ' 0x0');
      let params = {
        deviceId: this.deviceId,
        channelId: this.channelId,
        cmdCode: cmdCode,
        parameter1: groupNum,
        parameter2: parameter,
        combindCode2: 0
      }
      ptzController(params).then(res => {
        if (res.success) {
          this.$message.success("请求成功")
        }
      });
    },
    formatTooltip: function (val) {
      var h = parseInt(val / 60);
      var hStr = h < 10 ? ("0" + h) : h;
      var s = val % 60;
      var sStr = s < 10 ? ("0" + s) : s;
      return h + ":" + sStr;
    },
    timeFormatter: function (row, column, cellValue, index) {
      return cellValue.split(" ")[1];
    },
    mergeTime: function (timeArray) {
      var resultArray = [];
      for (let i = 0; i < timeArray.length; i++) {
        var startTime = new Date(timeArray[i].startTime);
        var endTime = new Date(timeArray[i].endTime);
        if (i == 0) {
          resultArray[0] = {
            startTime: startTime,
            endTime: endTime
          }
        }
        for (let j = 0; j < resultArray.length; j++) {
          if (startTime > resultArray[j].endTime) { // 合并
            if (startTime - resultArray[j].endTime <= 1000) {
              resultArray[j].endTime = endTime;
            } else {
              resultArray[resultArray.length] = {
                startTime: startTime,
                endTime: endTime
              }
            }
          } else if (resultArray[j].startTime > endTime) { // 合并
            if (resultArray[j].startTime - endTime <= 1000) {
              resultArray[j].startTime = startTime;
            } else {
              resultArray[resultArray.length] = {
                startTime: startTime,
                endTime: endTime
              }
            }
          }
        }
      }
      console.log(resultArray)
      return resultArray;
    }
  }
};
</script>

<style>
.control-wrapper {
  position: relative;
  width: 6.25rem;
  height: 6.25rem;
  max-width: 6.25rem;
  max-height: 6.25rem;
  border-radius: 100%;
  margin-top: 1.8rem;
  margin-left: 0.5rem;
  float: left;
}

.control-panel {
  position: relative;
  height: 11rem;
  max-height: 11rem;
}

.control-btn {
  display: flex;
  justify-content: center;
  position: absolute;
  width: 44%;
  height: 44%;
  border-radius: 5px;
  border: 1px solid #78aee4;
  box-sizing: border-box;
  transition: all 0.3s linear;
}

.control-btn:hover {
  cursor: pointer
}

.control-btn i {
  font-size: 20px;
  color: #78aee4;
  display: flex;
  justify-content: center;
  align-items: center;
}

.control-btn i:hover {
  cursor: pointer
}

.control-zoom-btn:hover {
  cursor: pointer
}

.control-round {
  position: absolute;
  top: 21%;
  left: 21%;
  width: 58%;
  height: 58%;
  background: #fff;
  border-radius: 100%;
}

.control-round-inner {
  position: absolute;
  left: 13%;
  top: 13%;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 70%;
  height: 70%;
  font-size: 40px;
  color: #78aee4;
  border: 1px solid #78aee4;
  border-radius: 100%;
  transition: all 0.3s linear;
}

.control-inner-btn {
  position: absolute;
  width: 60%;
  height: 60%;
  background: #fafafa;
}

.control-top {
  top: -8%;
  left: 27%;
  transform: rotate(-45deg);
  border-radius: 5px 100% 5px 0;
}

.control-top i {
  transform: rotate(45deg);
  border-radius: 5px 100% 5px 0;
}

.control-top .control-inner {
  left: -1px;
  bottom: 0;
  border-top: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 100% 0 0;
}

.control-top .fa {
  transform: rotate(45deg) translateY(-7px);
}

.control-left {
  top: 27%;
  left: -8%;
  transform: rotate(45deg);
  border-radius: 5px 0 5px 100%;
}

.control-left i {
  transform: rotate(-45deg);
}

.control-left .control-inner {
  right: -1px;
  top: -1px;
  border-bottom: 1px solid #78aee4;
  border-left: 1px solid #78aee4;
  border-radius: 0 0 0 100%;
}

.control-left .fa {
  transform: rotate(-45deg) translateX(-7px);
}

.control-right {
  top: 27%;
  right: -8%;
  transform: rotate(45deg);
  border-radius: 5px 100% 5px 0;
}

.control-right i {
  transform: rotate(-45deg);
}

.control-right .control-inner {
  left: -1px;
  bottom: -1px;
  border-top: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 100% 0 0;
}

.control-right .fa {
  transform: rotate(-45deg) translateX(7px);
}

.control-bottom {
  left: 27%;
  bottom: -8%;
  transform: rotate(45deg);
  border-radius: 0 5px 100% 5px;
}

.control-bottom i {
  transform: rotate(-45deg);
}

.control-bottom .control-inner {
  top: -1px;
  left: -1px;
  border-bottom: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 0 100% 0;
}

.control-bottom .fa {
  transform: rotate(-45deg) translateY(7px);
}

.trank {
  width: 80%;
  height: 180px;
  text-align: left;
  padding: 0 10%;
  overflow: auto;
}

.trankInfo {
  width: 80%;
  padding: 0 10%;
}
</style>
