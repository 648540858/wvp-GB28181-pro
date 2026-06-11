<template>
  <div id="devicePlayer" v-loading="isLoging">

    <el-dialog
      v-if="showVideoDialog"
      v-el-drag-dialog
      title="视频播放"
      top="2vh"
      width="65vw"
      :close-on-click-modal="false"
      :visible.sync="showVideoDialog"
      @close="close()"
    >
      <div class="dhsdk-player-body">

        <div class="player-side">
          <div class="player-container" :style="{ height: playerHeight }">
            <playerTabs ref="playerTabs" :has-audio="hasAudio" :show-button="true" @playerChanged="playerChanged"/>
          </div>
        </div>

        <div class="control-side">
          <div class="ptz-section">
            <ptzControls
              btn-layout="row"
              @ptz-move="onPtzMove"
              @ptz-stop="onPtzStop"
              @focus-move="onFocusMove"
              @focus-stop="onFocusStop"
              @iris-move="onIrisMove"
              @iris-stop="onIrisStop"
            />
          </div>

          <el-tabs v-model="tabActiveName" @tab-click="tabHandleClick" class="control-tabs">
            <el-tab-pane label="预置位" name="preset">
              <ptzPreset
                v-if="tabActiveName === 'preset'"
                :device-id="deviceId"
                :channel-device-id="channelId"
                style="margin-top: 8px;"
              />
            </el-tab-pane>
            <el-tab-pane label="实时视频" name="media">
              <div v-if="tabActiveName === 'media'" class="media-info-content">
                <div class="media-row">
                  <span class="media-label">播放地址：</span>
                  <el-input v-model="playerUrlInfo.playerUrl" :disabled="true">
                    <template slot="append">
                      <i class="cpoy-btn el-icon-document-copy" title="点击拷贝" style="cursor: pointer" @click="copyUrl(playerUrlInfo.playerUrl)" />
                    </template>
                  </el-input>
                </div>
                <div class="media-row">
                  <span class="media-label">iframe：</span>
                  <el-input v-model="sharedIframe" :disabled="true">
                    <template slot="append">
                      <i class="cpoy-btn el-icon-document-copy" title="点击拷贝" style="cursor: pointer" @click="copyUrl(sharedIframe)" />
                    </template>
                  </el-input>
                </div>
                <div class="media-row">
                  <span class="media-label">资源地址：</span>
                  <el-input v-model="playerUrlInfo.playUrl" :disabled="true">
                    <el-button slot="append" icon="el-icon-document-copy" title="点击拷贝" style="cursor: pointer" @click="copyUrl(playerUrlInfo.playUrl)" />
                    <el-dropdown v-if="streamInfo" slot="prepend" trigger="click" @command="copyUrl">
                      <el-button>更多地址<i class="el-icon-arrow-down el-icon--right" /></el-button>
                      <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item v-if="streamInfo.flv" :command="streamInfo.flv"><el-tag>FLV:</el-tag><span>{{ streamInfo.flv }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.https_flv" :command="streamInfo.https_flv"><el-tag>FLV(https):</el-tag><span>{{ streamInfo.https_flv }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.ws_flv" :command="streamInfo.ws_flv"><el-tag>FLV(ws):</el-tag><span>{{ streamInfo.ws_flv }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.wss_flv" :command="streamInfo.wss_flv"><el-tag>FLV(wss):</el-tag><span>{{ streamInfo.wss_flv }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.fmp4" :command="streamInfo.fmp4"><el-tag>FMP4:</el-tag><span>{{ streamInfo.fmp4 }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.https_fmp4" :command="streamInfo.https_fmp4"><el-tag>FMP4(https):</el-tag><span>{{ streamInfo.https_fmp4 }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.ws_fmp4" :command="streamInfo.ws_fmp4"><el-tag>FMP4(ws):</el-tag><span>{{ streamInfo.ws_fmp4 }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.wss_fmp4" :command="streamInfo.wss_fmp4"><el-tag>FMP4(wss):</el-tag><span>{{ streamInfo.wss_fmp4 }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.hls" :command="streamInfo.hls"><el-tag>HLS:</el-tag><span>{{ streamInfo.hls }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.https_hls" :command="streamInfo.https_hls"><el-tag>HLS(https):</el-tag><span>{{ streamInfo.https_hls }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.ws_hls" :command="streamInfo.ws_hls"><el-tag>HLS(ws):</el-tag><span>{{ streamInfo.ws_hls }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.wss_hls" :command="streamInfo.wss_hls"><el-tag>HLS(wss):</el-tag><span>{{ streamInfo.wss_hls }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.ts" :command="streamInfo.ts"><el-tag>TS:</el-tag><span>{{ streamInfo.ts }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.https_ts" :command="streamInfo.https_ts"><el-tag>TS(https):</el-tag><span>{{ streamInfo.https_ts }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.ws_ts" :command="streamInfo.ws_ts"><el-tag>TS(ws):</el-tag><span>{{ streamInfo.ws_ts }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.wss_ts" :command="streamInfo.wss_ts"><el-tag>TS(wss):</el-tag><span>{{ streamInfo.wss_ts }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.rtc" :command="streamInfo.rtc"><el-tag>RTC:</el-tag><span>{{ streamInfo.rtc }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.rtcs" :command="streamInfo.rtcs"><el-tag>RTCS:</el-tag><span>{{ streamInfo.rtcs }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.rtmp" :command="streamInfo.rtmp"><el-tag>RTMP:</el-tag><span>{{ streamInfo.rtmp }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.rtmps" :command="streamInfo.rtmps"><el-tag>RTMPS:</el-tag><span>{{ streamInfo.rtmps }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.rtsp" :command="streamInfo.rtsp"><el-tag>RTSP:</el-tag><span>{{ streamInfo.rtsp }}</span></el-dropdown-item>
                        <el-dropdown-item v-if="streamInfo.rtsps" :command="streamInfo.rtsps"><el-tag>RTSPS:</el-tag><span>{{ streamInfo.rtsps }}</span></el-dropdown-item>
                      </el-dropdown-menu>
                    </el-dropdown>
                  </el-input>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="编码信息" name="codec">
              <mediaInfo v-if="tabActiveName === 'codec'" ref="mediaInfo" :app="app" :stream="streamId" :media-server-id="mediaServerId" />
            </el-tab-pane>
            <el-tab-pane v-if="showBroadcast" label="语音对讲" name="broadcast">
              <div style="padding: 0 10px">
                <el-radio-group v-model="broadcastMode" :disabled="broadcastStatus !== -1">
                  <el-radio :label="true">喊话(Broadcast)</el-radio>
                  <el-radio :label="false">对讲(Talk)</el-radio>
                </el-radio-group>
              </div>
              <div class="trank" style="text-align: center;">
                <el-button :type="getBroadcastStatus()" :disabled="broadcastStatus === -2" circle icon="el-icon-microphone" style="font-size: 32px; padding: 24px;margin-top: 24px;" @click="broadcastStatusClick()" />
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

      </div>
    </el-dialog>
  </div>
</template>

<script>
import elDragDialog from '@/directive/el-drag-dialog'
import crypto from 'crypto'
import playerTabs from '../common/playerTabs.vue'
import ptzControls from '../common/ptzControls.vue'
import PtzPreset from '../common/ptzPreset.vue'
import mediaInfo from '../common/mediaInfo.vue'

export default {
  name: 'DevicePlayer',
  directives: { elDragDialog },
  components: { playerTabs, ptzControls, PtzPreset, mediaInfo },
  props: {},
  data() {
    return {
      videoUrl: '',
      streamId: '',
      app: '',
      mediaServerId: '',
      deviceId: '',
      channelId: '',
      tabActiveName: 'preset',
      hasAudio: false,
      isLoging: false,
      showVideoDialog: false,
      showBroadcast: true,
      streamInfo: null,
      broadcastMode: true,
      broadcastRtc: null,
      broadcastStatus: -1,
      playerHeight: '48vh',
      playerUrlInfo: {
        playerUrl: null,
        playUrl: null,
      }
    }
  },
  computed: {
    sharedIframe: function(){
      return `<iframe src="${this.playerUrlInfo.playerUrl}"></iframe>`
    }
  },
  created() {
    this.broadcastStatus = -1
  },
  methods: {
    ptzSpeed(speed) {
      return parseInt(speed * 255 / 100)
    },
    onPtzMove(e) {
      const speedVal = this.ptzSpeed(e.speed)
      this.$store.dispatch('frontEnd/ptz', {
        deviceId: this.deviceId,
        channelId: this.channelId,
        command: e.direction,
        horizonSpeed: speedVal,
        verticalSpeed: speedVal,
        zoomSpeed:  parseInt(e.speed * 15 / 100)
      })
    },
    onPtzStop() {
      this.$store.dispatch('frontEnd/ptz', {
        deviceId: this.deviceId,
        channelId: this.channelId,
        command: 'stop',
        horizonSpeed: 0,
        verticalSpeed: 0,
        zoomSpeed: 0
      })
    },
    onFocusMove(e) {
      const speedVal = this.ptzSpeed(e.speed)
      this.$store.dispatch('frontEnd/focus', [this.deviceId, this.channelId, e.command, speedVal])
    },
    onFocusStop() {
      this.$store.dispatch('frontEnd/focus', [this.deviceId, this.channelId, 'stop', 0])
    },
    onIrisMove(e) {
      const speedVal = this.ptzSpeed(e.speed)
      this.$store.dispatch('frontEnd/iris', [this.deviceId, this.channelId, e.command, speedVal])
    },
    onIrisStop() {
      this.$store.dispatch('frontEnd/iris', [this.deviceId, this.channelId, 'stop', 0])
    },
    tabHandleClick: function(tab) {
      if (tab.name === 'codec') {
        this.$refs.mediaInfo && this.$refs.mediaInfo.startTask()
      } else {
        this.$refs.mediaInfo && this.$refs.mediaInfo.stopTask()
      }
    },
    openDialog: function(tab, deviceId, channelId, param) {
      if (this.showVideoDialog) return
      this.tabActiveName = tab === 'streamPlay' ? 'media' : (tab || 'preset')
      this.deviceId = deviceId
      this.channelId = channelId
      this.streamId = ''
      this.mediaServerId = ''
      this.app = ''
      this.videoUrl = ''
      if (param && param.streamInfo) {
        this.play(param.streamInfo, param.hasAudio)
      }
    },
    play: function(streamInfo, hasAudio) {
      this.streamInfo = streamInfo
      this.hasAudio = hasAudio
      this.isLoging = false
      this.streamId = streamInfo.stream
      this.app = streamInfo.app
      this.mediaServerId = streamInfo.mediaServerId
      this.showVideoDialog = true
      this.$nextTick(() => {
        if (this.$refs.playerTabs) {
          this.$refs.playerTabs.setStreamInfo(streamInfo)
        }
      })
    },
    playerChanged: function(playerUrlInfo) {
      this.playerUrlInfo = playerUrlInfo
    },
    close: function() {
      if (this.$refs.playerTabs) {
        this.$refs.playerTabs.stop()
      }
      this.videoUrl = ''
      this.showVideoDialog = false
      this.stopBroadcast()
    },
    copyUrl: function(dropdownItem) {
      this.$copyText(dropdownItem).then(() => {
        this.$message.success({ showClose: true, message: '成功拷贝到粘贴板' })
      }, () => {})
    },
    getBroadcastStatus() {
      if (this.broadcastStatus == -2) return 'primary'
      if (this.broadcastStatus == -1) return 'primary'
      if (this.broadcastStatus == 0) return 'warning'
      if (this.broadcastStatus === 1) return 'danger'
    },
    broadcastStatusClick() {
      if (this.broadcastStatus === -1) {
        this.broadcastStatus = 0
        this.$store.dispatch('play/broadcastStart', [this.deviceId, this.channelId, this.broadcastMode])
          .then(data => {
            const si = data.streamInfo
            if (document.location.protocol.includes('https')) {
              this.startBroadcast(si.rtcs)
            } else {
              this.startBroadcast(si.rtc)
            }
          })
      } else if (this.broadcastStatus === 1) {
        this.broadcastStatus = -1
        this.broadcastRtc.close()
      }
    },
    startBroadcast(url) {
      this.$store.dispatch('user/getUserInfo')
        .then((data) => {
          if (data === null) { this.broadcastStatus = -1; return }
          const pushKey = data.pushKey
          url += '&sign=' + crypto.createHash('md5').update(pushKey, 'utf8').digest('hex')
          this.broadcastRtc = new ZLMRTCClient.Endpoint({
            debug: true, zlmsdpUrl: url, simulecast: false, useCamera: false,
            audioEnable: true, videoEnable: false, recvOnly: false
          })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_NOT_SUPPORT, () => { this.broadcastStatus = -1 })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, () => { this.broadcastStatus = -1 })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, () => { this.broadcastStatus = -1 })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (e) => {
            if (e === 'connecting') this.broadcastStatus = 0
            else if (e === 'connected') this.broadcastStatus = 1
            else if (e === 'disconnected') this.broadcastStatus = -1
          })
          this.broadcastRtc.on(ZLMRTCClient.Events.CAPTURE_STREAM_FAILED, () => { this.broadcastStatus = -1 })
        }).catch(() => { this.broadcastStatus = -1 })
    },
    stopBroadcast() {
      this.broadcastRtc && this.broadcastRtc.close()
      this.broadcastStatus = -1
      this.$store.dispatch('play/broadcastStop', [this.deviceId, this.channelId])
    }
  }
}
</script>

<style>
#devicePlayer .el-dialog { margin-top: 2vh !important; }
#devicePlayer .el-dialog__body { padding: 10px 20px; }
.dhsdk-player-body { display: flex; gap: 16px; }
.player-side { flex: 3; min-width: 0; }
.player-container { width: 100%; }
.control-side { flex: 2; min-width: 340px; display: flex; flex-direction: column; }
.ptz-section { flex-shrink: 0; margin-bottom: 8px; }
.control-tabs { flex: 1; display: flex; flex-direction: column; min-height: 180px}
.control-tabs .el-tabs__content { flex: 1; overflow: auto; }
.media-info-content { overflow: auto; }
.media-row { display: flex; margin-bottom: 0.5rem; height: 2.5rem; }
.media-label { width: 6rem; line-height: 2.5rem; text-align: right; flex-shrink: 0; }
.trank { width: 80%; height: 180px; text-align: left; padding: 0 10%; overflow: auto; }
</style>
