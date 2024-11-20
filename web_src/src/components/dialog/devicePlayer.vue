<template>
  <div id="devicePlayer" v-loading="isLoging">

    <el-dialog title="视频播放" top="0" :close-on-click-modal="false" :visible.sync="showVideoDialog" @close="close()" v-if="showVideoDialog">
      <div style="width: 100%; height: 100%">
        <el-tabs type="card" :stretch="true" v-model="activePlayer" @tab-click="changePlayer"
                 v-if="Object.keys(this.player).length > 1">
          <el-tab-pane label="Jessibuca" name="jessibuca">
            <jessibucaPlayer v-if="activePlayer === 'jessibuca'" ref="jessibuca" :visible.sync="showVideoDialog"
                             :videoUrl="videoUrl" :error="videoError" :message="videoError"
                             :hasAudio="hasAudio" fluent autoplay live></jessibucaPlayer>
          </el-tab-pane>
          <el-tab-pane label="WebRTC" name="webRTC">
            <rtc-player v-if="activePlayer === 'webRTC'" ref="webRTC" :visible.sync="showVideoDialog"
                        :videoUrl="videoUrl" :error="videoError" :message="videoError" height="100px"
                        :hasAudio="hasAudio" fluent autoplay live></rtc-player>
          </el-tab-pane>
          <el-tab-pane label="h265web">h265web敬请期待</el-tab-pane>
        </el-tabs>
        <jessibucaPlayer v-if="Object.keys(this.player).length == 1 && this.player.jessibuca" ref="jessibuca"
                         :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError"
                         :hasAudio="hasAudio" fluent autoplay live></jessibucaPlayer>
        <rtc-player v-if="Object.keys(this.player).length == 1 && this.player.webRTC" ref="jessibuca"
                    :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError"
                    height="100px" :hasAudio="hasAudio" fluent autoplay live></rtc-player>

      </div>
      <div id="shared" style="text-align: right; margin-top: 1rem;">

        <el-tabs v-model="tabActiveName" @tab-click="tabHandleClick">
          <el-tab-pane label="实时视频" name="media">
            <div style="display: flex; margin-bottom: 0.5rem; height: 2.5rem;">
              <span style="width: 5rem; line-height: 2.5rem; text-align: right;">播放地址：</span>
              <el-input v-model="getPlayerShared.sharedUrl" :disabled="true">
                <template slot="append">
                  <i class="cpoy-btn el-icon-document-copy" title="点击拷贝" v-clipboard="getPlayerShared.sharedUrl"
                     @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
                </template>
              </el-input>
            </div>
            <div style="display: flex; margin-bottom: 0.5rem; height: 2.5rem;">
              <span style="width: 5rem; line-height: 2.5rem; text-align: right;">iframe：</span>
              <el-input v-model="getPlayerShared.sharedIframe" :disabled="true">
                <template slot="append">
                  <i class="cpoy-btn el-icon-document-copy" title="点击拷贝" v-clipboard="getPlayerShared.sharedIframe"
                     @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
                </template>
              </el-input>
            </div>
            <div style="display: flex; margin-bottom: 0.5rem; height: 2.5rem;">
              <span style="width: 5rem; line-height: 2.5rem; text-align: right;">资源地址：</span>
              <el-input v-model="getPlayerShared.sharedRtmp" :disabled="true">
                <el-button slot="append" icon="el-icon-document-copy" title="点击拷贝"
                           v-clipboard="getPlayerShared.sharedRtmp"
                           @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>
                <el-dropdown slot="prepend" v-if="streamInfo" trigger="click" @command="copyUrl">
                  <el-button>
                    更多地址<i class="el-icon-arrow-down el-icon--right"></i>
                  </el-button>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item v-if="streamInfo.flv" :command="streamInfo.flv">
                      <el-tag>FLV:</el-tag>
                      <span>{{ streamInfo.flv }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.https_flv" :command="streamInfo.https_flv">
                      <el-tag>FLV(https):</el-tag>
                      <span>{{ streamInfo.https_flv }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.ws_flv" :command="streamInfo.ws_flv">
                      <el-tag>FLV(ws):</el-tag>
                      <span>{{ streamInfo.ws_flv }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.wss_flv" :command="streamInfo.wss_flv">
                      <el-tag>FLV(wss):</el-tag>
                      <span>{{ streamInfo.wss_flv }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.fmp4" :command="streamInfo.fmp4">
                      <el-tag>FMP4:</el-tag>
                      <span>{{ streamInfo.fmp4 }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.https_fmp4" :command="streamInfo.https_fmp4">
                      <el-tag>FMP4(https):</el-tag>
                      <span>{{ streamInfo.https_fmp4 }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.ws_fmp4" :command="streamInfo.ws_fmp4">
                      <el-tag>FMP4(ws):</el-tag>
                      <span>{{ streamInfo.ws_fmp4 }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.wss_fmp4" :command="streamInfo.wss_fmp4">
                      <el-tag>FMP4(wss):</el-tag>
                      <span>{{ streamInfo.wss_fmp4 }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.hls" :command="streamInfo.hls">
                      <el-tag>HLS:</el-tag>
                      <span>{{ streamInfo.hls }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.https_hls" :command="streamInfo.https_hls">
                      <el-tag>HLS(https):</el-tag>
                      <span>{{ streamInfo.https_hls }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.ws_hls" :command="streamInfo.ws_hls">
                      <el-tag>HLS(ws):</el-tag>
                      <span>{{ streamInfo.ws_hls }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.wss_hls" :command="streamInfo.wss_hls">
                      <el-tag>HLS(wss):</el-tag>
                      <span>{{ streamInfo.wss_hls }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.ts" :command="streamInfo.ts">
                      <el-tag>TS:</el-tag>
                      <span>{{ streamInfo.ts }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.https_ts" :command="streamInfo.https_ts">
                      <el-tag>TS(https):</el-tag>
                      <span>{{ streamInfo.https_ts }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.ws_ts" :command="streamInfo.ws_ts">
                      <el-tag>TS(ws):</el-tag>
                      <span>{{ streamInfo.ws_ts }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.wss_ts" :command="streamInfo.wss_ts">
                      <el-tag>TS(wss):</el-tag>
                      <span>{{ streamInfo.wss_ts }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.rtc" :command="streamInfo.rtc">
                      <el-tag>RTC:</el-tag>
                      <span>{{ streamInfo.rtc }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.rtcs" :command="streamInfo.rtcs">
                      <el-tag>RTCS:</el-tag>
                      <span>{{ streamInfo.rtcs }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.rtmp" :command="streamInfo.rtmp">
                      <el-tag>RTMP:</el-tag>
                      <span>{{ streamInfo.rtmp }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.rtmps" :command="streamInfo.rtmps">
                      <el-tag>RTMPS:</el-tag>
                      <span>{{ streamInfo.rtmps }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.rtsp" :command="streamInfo.rtsp">
                      <el-tag>RTSP:</el-tag>
                      <span>{{ streamInfo.rtsp }}</span>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="streamInfo.rtsps" :command="streamInfo.rtsps">
                      <el-tag>RTSPS:</el-tag>
                      <span>{{ streamInfo.rtsps }}</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </el-input>

            </div>
          </el-tab-pane>
          <!--{"code":0,"data":{"paths":["22-29-30.mp4"],"rootPath":"/home/kkkkk/Documents/ZLMediaKit/release/linux/Debug/www/record/hls/kkkkk/2020-05-11/"}}-->
          <!--遥控界面-->
          <el-tab-pane label="云台控制" name="control" v-if="showPtz">
            <div style="display: grid; grid-template-columns: 240px auto; height: 180px; overflow: auto">
              <div style="display: grid; grid-template-columns: 6.25rem auto;">

                <div class="control-wrapper">
                  <div class="control-btn control-top" @mousedown="ptzCamera('up')" @mouseup="ptzCamera('stop')">
                    <i class="el-icon-caret-top"></i>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-left" @mousedown="ptzCamera('left')" @mouseup="ptzCamera('stop')">
                    <i class="el-icon-caret-left"></i>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-bottom" @mousedown="ptzCamera('down')" @mouseup="ptzCamera('stop')">
                    <i class="el-icon-caret-bottom"></i>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-btn control-right" @mousedown="ptzCamera('right')" @mouseup="ptzCamera('stop')">
                    <i class="el-icon-caret-right"></i>
                    <div class="control-inner-btn control-inner"></div>
                  </div>
                  <div class="control-round">
                    <div class="control-round-inner"><i class="fa fa-pause-circle"></i></div>
                  </div>
                  <div class="contro-speed" style="position: absolute; left: 4px; top: 7rem; width: 6.25rem;">
                    <el-slider v-model="controSpeed" :max="100"></el-slider>
                  </div>
                </div>
                <div>
                  <div class="ptz-btn-box">
                    <div style="" @mousedown="ptzCamera('zoomin')" @mouseup="ptzCamera('stop')" title="变倍+">
                      <i class="el-icon-zoom-in control-zoom-btn" style="font-size: 1.5rem;"></i>
                    </div>
                    <div style="" @mousedown="ptzCamera('zoomout')" @mouseup="ptzCamera('stop')" title="变倍-">
                      <i class="el-icon-zoom-out control-zoom-btn" style="font-size: 1.5rem;"></i>
                    </div>
                  </div>
                  <div class="ptz-btn-box">
                    <div @mousedown="focusCamera('near')"  @mouseup="focusCamera('stop')" title="聚焦+">
                      <i class="iconfont icon-bianjiao-fangda control-zoom-btn" style="font-size: 1.5rem;"></i>
                    </div>
                    <div @mousedown="focusCamera('far')" @mouseup="focusCamera('stop')" title="聚焦-">
                      <i class="iconfont icon-bianjiao-suoxiao control-zoom-btn" style="font-size: 1.5rem;"></i>
                    </div>
                  </div>
                  <div class="ptz-btn-box">
                    <div @mousedown="irisCamera('in')"  @mouseup="irisCamera('stop')" title="光圈+">
                      <i class="iconfont icon-guangquan control-zoom-btn" style="font-size: 1.5rem;"></i>
                    </div>
                    <div @mousedown="pirisCamera('out')" @mouseup="irisCamera('stop')" title="光圈-">
                      <i class="iconfont icon-guangquan- control-zoom-btn" style="font-size: 1.5rem;"></i>
                    </div>
                  </div>
                </div>
              </div>
              <div style="text-align: left" >
                <el-select
                  v-model="ptzMethod"
                  style="width: 100%"
                  size="mini"
                  placeholder="请选择云台功能"
                >
                  <el-option label="预置点" value="preset"></el-option>
                  <el-option label="巡航组" value="cruise"></el-option>
                  <el-option label="自动扫描" value="scan"></el-option>
                  <el-option label="雨刷" value="wiper"></el-option>
                  <el-option label="辅助开关" value="switch"></el-option>
                </el-select>

                <ptzPreset :channelDeviceId="channelId" :deviceId="deviceId" v-if="ptzMethod === 'preset'" style="margin-top: 1rem"></ptzPreset>
                <ptzCruising :channelDeviceId="channelId" :deviceId="deviceId" v-if="ptzMethod === 'cruise'" style="margin-top: 1rem"></ptzCruising>
                <ptzScan :channelDeviceId="channelId" :deviceId="deviceId" v-if="ptzMethod === 'scan'" style="margin-top: 1rem"></ptzScan>
                <ptzWiper :channelDeviceId="channelId" :deviceId="deviceId" v-if="ptzMethod === 'wiper'" style="margin-top: 1rem"></ptzWiper>
                <ptzSwitch :channelDeviceId="channelId" :deviceId="deviceId" v-if="ptzMethod === 'switch'" style="margin-top: 1rem"></ptzSwitch>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="编码信息" name="codec" v-loading="tracksLoading">
            <mediaInfo :app="app" :stream="streamId" :mediaServerId="mediaServerId"></mediaInfo>
          </el-tab-pane>
          <el-tab-pane label="语音对讲" name="broadcast">
            <div style="padding: 0 10px">
              <el-switch v-model="broadcastMode" :disabled="broadcastStatus !== -1" active-color="#409EFF"
                         active-text="喊话(Broadcast)"
                         inactive-text="对讲(Talk)"></el-switch>
            </div>
            <div class="trank" style="text-align: center;">
              <el-button @click="broadcastStatusClick()" :type="getBroadcastStatus()" :disabled="broadcastStatus === -2"
                         circle icon="el-icon-microphone" style="font-size: 32px; padding: 24px;margin-top: 24px;"/>
              <p>
                <span v-if="broadcastStatus === -2">正在释放资源</span>
                <span v-if="broadcastStatus === -1">点击开始对讲</span>
                <span v-if="broadcastStatus === 0">等待接通中...</span>
                <span v-if="broadcastStatus === 1">请说话</span>
              </p>

            </div>
          </el-tab-pane>

        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import rtcPlayer from '../dialog/rtcPlayer.vue'
import LivePlayer from '@liveqing/liveplayer'
import crypto from 'crypto'
import jessibucaPlayer from '../common/jessibuca.vue'
import PtzPreset from "../common/ptzPreset.vue";
import PtzCruising from "../common/ptzCruising.vue";
import ptzScan from "../common/ptzScan.vue";
import ptzWiper from "../common/ptzWiper.vue";
import ptzSwitch from "../common/ptzSwitch.vue";
import mediaInfo from "../common/mediaInfo.vue";

export default {
  name: 'devicePlayer',
  props: {},
  components: {
    PtzPreset,PtzCruising,ptzScan,ptzWiper,ptzSwitch,mediaInfo,
    LivePlayer, jessibucaPlayer, rtcPlayer,
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
    console.log("created")
    console.log(this.player)
    this.broadcastStatus = -1;
    if (Object.keys(this.player).length === 1) {
      this.activePlayer = Object.keys(this.player)[0]
    }
  },
  data() {
    return {
      video: 'http://lndxyj.iqilu.com/public/upload/2019/10/14/8c001ea0c09cdc59a57829dabc8010fa.mp4',
      videoUrl: '',
      activePlayer: "jessibuca",
      // 如何你只是用一种播放器，直接注释掉不用的部分即可
      player: {
        jessibuca: ["ws_flv", "wss_flv"],
        webRTC: ["rtc", "rtcs"],
      },
      showVideoDialog: false,
      streamId: '',
      ptzMethod: 'preset',
      ptzPresetId: '',
      app: '',
      mediaServerId: '',
      deviceId: '',
      channelId: '',
      tabActiveName: 'media',
      hasAudio: false,
      loadingRecords: false,
      recordsLoading: false,
      isLoging: false,
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
      tracksLoading: false,
      showPtz: true,
      showRrecord: true,
      tracksNotLoaded: false,
      sliderTime: 0,
      seekTime: 0,
      recordStartTime: 0,
      showTimeText: "00:00:00",
      streamInfo: null,
      broadcastMode: true,
      broadcastRtc: null,
      broadcastStatus: -1, // -2 正在释放资源 -1 默认状态 0 等待接通 1 接通成功
    };
  },
  methods: {
    tabHandleClick: function (tab, event) {
      console.log(tab)
      var that = this;
      that.tracks = [];
      that.tracksLoading = true;
      that.tracksNotLoaded = false;
      if (tab.name === "codec") {
        this.$axios({
          method: 'get',
          url: '/zlm/' + this.mediaServerId + '/index/api/getMediaInfo?vhost=__defaultVhost__&schema=rtsp&app=' + this.app + '&stream=' + this.streamId
        }).then(function (res) {
          that.tracksLoading = false;
          if (res.data.code == 0 && res.data.tracks) {
            that.tracks = res.data.tracks;
          } else {
            that.tracksNotLoaded = true;
            that.$message({
              showClose: true,
              message: '获取编码信息失败,',
              type: 'warning'
            });
          }
        }).catch(function (e) {
        });
      }
    },
    changePlayer: function (tab) {
      console.log(this.player[tab.name][0])
      this.activePlayer = tab.name;
      this.videoUrl = this.getUrlByStreamInfo()
      console.log(this.videoUrl)
    },
    openDialog: function (tab, deviceId, channelId, param) {
      if (this.showVideoDialog) {
        return;
      }
      this.tabActiveName = tab;
      this.channelId = channelId;
      this.deviceId = deviceId;
      this.streamId = "";
      this.mediaServerId = "";
      this.app = "";
      this.videoUrl = ""
      if (!!this.$refs[this.activePlayer]) {
        this.$refs[this.activePlayer].pause();
      }
      switch (tab) {
        case "media":
          this.play(param.streamInfo, param.hasAudio)
          break;
        case "streamPlay":
          this.tabActiveName = "media";
          this.showRrecord = false;
          this.showPtz = false;
          this.play(param.streamInfo, param.hasAudio)
          break;
        case "control":
          break;
      }
    },
    play: function (streamInfo, hasAudio) {
      this.streamInfo = streamInfo;
      this.hasAudio = hasAudio;
      this.isLoging = false;
      // this.videoUrl = streamInfo.rtc;
      this.videoUrl = this.getUrlByStreamInfo();
      this.streamId = streamInfo.stream;
      this.app = streamInfo.app;
      this.mediaServerId = streamInfo.mediaServerId;
      this.playFromStreamInfo(false, streamInfo)
    },
    getUrlByStreamInfo() {
      console.log(this.streamInfo)
      let streamInfo = this.streamInfo
      if (this.streamInfo.transcodeStream) {
        streamInfo = this.streamInfo.transcodeStream;
      }
      if (location.protocol === "https:") {
        this.videoUrl = streamInfo[this.player[this.activePlayer][1]]
      } else {
        this.videoUrl = streamInfo[this.player[this.activePlayer][0]]
      }
      return this.videoUrl;

    },

        playFromStreamInfo: function (realHasAudio, streamInfo) {
          this.showVideoDialog = true;
          this.hasaudio = realHasAudio && this.hasaudio;
          if (this.$refs[this.activePlayer]) {
            this.$refs[this.activePlayer].play(this.getUrlByStreamInfo(streamInfo))
          }else {
            this.$nextTick(() => {
              this.$refs[this.activePlayer].play(this.getUrlByStreamInfo(streamInfo))
            });
          }
        },
        close: function () {
            console.log('关闭视频');
            if (!!this.$refs[this.activePlayer]){
              this.$refs[this.activePlayer].pause();
            }
            this.videoUrl = '';
            this.coverPlaying = false;
            this.showVideoDialog = false;
            this.stopBroadcast()
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
    ptzCamera: function (command) {
      console.log('云台控制：' + command);
      let that = this;
      this.$axios({
        method: 'get',
        url: '/api/front-end/ptz/' + this.deviceId + '/' + this.channelId + '?command=' + command + '&horizonSpeed=' + parseInt(this.controSpeed * 255/100) + '&verticalSpeed=' + parseInt(this.controSpeed * 255/100) + '&zoomSpeed=' + parseInt(this.controSpeed * 16/100)
      }).then(function (res) {
      });
    },
    irisCamera: function (command) {
      this.$axios({
        method: 'get',
        url: '/api/front-end/fi/iris/' + this.deviceId + '/' + this.channelId + '?command=' + command + '&speed=' + parseInt(this.controSpeed * 255/100)
      }).then(function (res) {
      });
    },
    focusCamera: function (command) {
      this.$axios({
        method: 'get',
        url: '/api/front-end/fi/focus/' + this.deviceId + '/' + this.channelId + '?command=' + command + '&speed=' + parseInt(this.controSpeed * 255/100)
      }).then(function (res) {
      });
    },
    //////////////////////播放器事件处理//////////////////////////
    videoError: function (e) {
      console.log("播放器错误：" + JSON.stringify(e));
    },
    presetPosition: function (cmdCode, presetPos) {
      console.log('预置位控制：' + this.presetPos + ' : 0x' + cmdCode.toString(16));
      let that = this;
      this.$axios({
        method: 'post',
        url: '/api/ptz/front_end_command/' + this.deviceId + '/' + this.channelId + '?cmdCode=' + cmdCode + '&parameter1=0&parameter2=' + presetPos + '&combindCode2=0'
      }).then(function (res) {
      });
    },
    setSpeedOrTime: function (cmdCode, groupNum, parameter) {
      let that = this;
      let parameter2 = parameter % 256;
      let combindCode2 = Math.floor(parameter / 256) * 16;
      console.log('前端控制：0x' + cmdCode.toString(16) + ' 0x' + groupNum.toString(16) + ' 0x' + parameter2.toString(16) + ' 0x' + combindCode2.toString(16));
      this.$axios({
        method: 'post',
        url: '/api/ptz/front_end_command/' + this.deviceId + '/' + this.channelId + '?cmdCode=' + cmdCode + '&parameter1=' + groupNum + '&parameter2=' + parameter2 + '&combindCode2=' + combindCode2
      }).then(function (res) {
      });
    },
    setCommand: function (cmdCode, groupNum, parameter) {
      let that = this;
      console.log('前端控制：0x' + cmdCode.toString(16) + ' 0x' + groupNum.toString(16) + ' 0x' + parameter.toString(16) + ' 0x0');
      this.$axios({
        method: 'post',
        url: '/api/ptz/front_end_command/' + this.deviceId + '/' + this.channelId + '?cmdCode=' + cmdCode + '&parameter1=' + groupNum + '&parameter2=' + parameter + '&combindCode2=0'
      }).then(function (res) {
      });
    },
    copyUrl: function (dropdownItem) {
      console.log(dropdownItem)
      this.$copyText(dropdownItem).then((e) => {
        this.$message.success({
          showClose: true,
          message: "成功拷贝到粘贴板"
        })
      }, (e) => {

      })
    },
    getBroadcastStatus() {
      if (this.broadcastStatus == -2) {
        return "primary"
      }
      if (this.broadcastStatus == -1) {
        return "primary"
      }
      if (this.broadcastStatus == 0) {
        return "warning"
      }
      if (this.broadcastStatus == 1) {
        return "danger"
      }

    },
    broadcastStatusClick() {
      if (this.broadcastStatus == -1) {
        // 默认状态， 开始
        this.broadcastStatus = 0
        // 发起语音对讲
        this.$axios({
          method: 'get',
          url: '/api/play/broadcast/' + this.deviceId + '/' + this.channelId + "?timeout=30&broadcastMode=" + this.broadcastMode
        }).then((res) => {
          if (res.data.code === 0) {
            let streamInfo = res.data.data.streamInfo;
            if (document.location.protocol.includes("https")) {
              this.startBroadcast(streamInfo.rtcs)
            } else {
              this.startBroadcast(streamInfo.rtc)
            }
          } else {
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: "error",
            });
          }
        });
      } else if (this.broadcastStatus === 1) {
        this.broadcastStatus = -1;
        this.broadcastRtc.close()
      }
    },
    startBroadcast(url) {
      // 获取推流鉴权Key
      this.$axios({
        method: 'post',
        url: '/api/user/userInfo',
      }).then((res) => {
        if (res.data.code !== 0) {
          this.$message({
            showClose: true,
            message: "获取推流鉴权Key失败",
            type: "error",
          });
          this.broadcastStatus = -1;
        } else {
          let pushKey = res.data.data.pushKey;
          // 获取推流鉴权KEY
          url += "&sign=" + crypto.createHash('md5').update(pushKey, "utf8").digest('hex')
          console.log("开始语音喊话： " + url)
          this.broadcastRtc = new ZLMRTCClient.Endpoint({
            debug: true, // 是否打印日志
            zlmsdpUrl: url, //流地址
            simulecast: false,
            useCamera: false,
            audioEnable: true,
            videoEnable: false,
            recvOnly: false,
          })

          // webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS,(e)=>{//获取到了远端流，可以播放
          //   console.error('播放成功',e.streams)
          //   this.broadcastStatus = 1;
          // });
          //
          // webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_LOCAL_STREAM,(s)=>{// 获取到了本地流
          //   this.broadcastStatus = 1;
          //   // document.getElementById('selfVideo').srcObject=s;
          //   // this.eventcallbacK("LOCAL STREAM", "获取到了本地流")
          // });

          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_NOT_SUPPORT, (e) => {// 获取到了本地流
            console.error('不支持webrtc', e)
            this.$message({
              showClose: true,
              message: '不支持webrtc, 无法进行语音喊话',
              type: 'error'
            });
            this.broadcastStatus = -1;
          });

          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, (e) => {// ICE 协商出错
            console.error('ICE 协商出错')
            this.$message({
              showClose: true,
              message: 'ICE 协商出错',
              type: 'error'
            });
            this.broadcastStatus = -1;
          });

          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => {// offer anwser 交换失败
            console.error('offer anwser 交换失败', e)
            this.$message({
              showClose: true,
              message: 'offer anwser 交换失败' + e,
              type: 'error'
            });
            this.broadcastStatus = -1;
          });
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (e) => {// offer anwser 交换失败
            console.log('状态改变', e)
            if (e === "connecting") {
              this.broadcastStatus = 0;
            } else if (e === "connected") {
              this.broadcastStatus = 1;
            } else if (e === "disconnected") {
              this.broadcastStatus = -1;
            }
          });
          this.broadcastRtc.on(ZLMRTCClient.Events.CAPTURE_STREAM_FAILED, (e) => {// offer anwser 交换失败
            console.log('捕获流失败', e)
            this.$message({
              showClose: true,
              message: '捕获流失败' + e,
              type: 'error'
            });
            this.broadcastStatus = -1;
          });
        }
      }).catch((e) => {
        this.$message({
          showClose: true,
          message: e,
          type: 'error'
        });
        this.broadcastStatus = -1;
      });


    },
    stopBroadcast() {
      this.broadcastRtc.close();
      this.broadcastStatus = -1;
      this.$axios({
        method: 'get',
        url: '/api/play/broadcast/stop/' + this.deviceId + '/' + this.channelId
      }).then((res) => {
        if (res.data.code == 0) {
          // this.broadcastStatus = -1;
          // this.broadcastRtc.close()
        } else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: "error",
          });
        }
      });
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
  margin-top: 1.5rem;
  margin-left: 0.5rem;
  float: left;
}

.control-panel {
  position: relative;
  top: 0;
  left: 5rem;
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
.el-dialog__body{
  padding: 10px 20px;
}
.ptz-btn-box {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 0 2rem;
  height: 3rem;
  line-height: 4rem;
}
</style>
