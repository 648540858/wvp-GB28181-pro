<template>
  <div id="devicePlayer" v-loading="isLoging">
    <el-dialog
      v-if="showVideoDialog"
      v-el-drag-dialog
      title="视频播放"
      top="10vh"
      width="65vw"
      :close-on-click-modal="false"
      :visible.sync="showVideoDialog"
      @close="close()"
    >
      <div class="dhsdk-player-body">

        <div class="player-side">
          <div class="player-container" :style="{ height: playerHeight }">
            <playerTabs ref="playerTabs" :has-audio="hasAudio" :show-button="true"
              @playerChanged="playerChanged" />
          </div>
        </div>

        <div class="control-side">
          <jtDevicePtzPanel v-if="showPtz" :device-id="deviceId" :channel-id="channelId" />
          <el-tabs v-model="tabActiveName" @tab-click="tabHandleClick" class="control-tabs">
            <el-tab-pane label="实时视频" name="media">
              <streamMediaPanel v-if="tabActiveName === 'media'" :player-url="playerUrlInfo.playerUrl" :play-url="playerUrlInfo.playUrl" :stream-info="streamInfo" />
            </el-tab-pane>
            <el-tab-pane label="编码信息" name="codec">
              <mediaInfo v-if="tabActiveName === 'codec'" ref="mediaInfo" :app="app" :stream="streamId" :media-server-id="mediaServerId" />
            </el-tab-pane>
            <el-tab-pane label="辅助开关" name="switch">
              <div style="display: flex; gap: 12px; justify-content: center; margin-top: 16px;">
                <el-button-group>
                  <el-button size="small" type="primary" @click="wiper('on')">开启雨刷</el-button>
                  <el-button size="small" @click="wiper('off')">关闭雨刷</el-button>
                </el-button-group>
                <el-button-group>
                  <el-button size="small" type="primary" @click="fillLight('on')">开补光灯</el-button>
                  <el-button size="small" @click="fillLight('off')">关补光灯</el-button>
                </el-button-group>
              </div>
            </el-tab-pane>
            <el-tab-pane label="语音对讲" name="broadcast">
              <div class="trank" style="text-align: center; width: 100%;">
                <el-button
                  :type="getBroadcastStatus()"
                  :disabled="broadcastStatus === -2"
                  circle
                  icon="el-icon-microphone"
                  style="font-size: 32px; padding: 24px;margin-top: 24px;"
                  @click="broadcastStatusClick()"
                />
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
import playerTabs from '../../common/playerTabs.vue'
import streamMediaPanel from '../../common/streamMediaPanel.vue'
import jtDevicePtzPanel from '../common/jtDevicePtzPanel.vue'
import elDragDialog from '@/directive/el-drag-dialog'
import crypto from 'crypto'
import mediaInfo from '../../common/mediaInfo.vue'

export default {
  name: 'JtDevicePlayer',
  directives: { elDragDialog },
  components: { playerTabs, streamMediaPanel, jtDevicePtzPanel, mediaInfo },
  props: {},
  data() {
    return {
      videoUrl: '',
      showVideoDialog: false,
      streamId: '',
      app: '',
      mediaServerId: '',
      deviceId: '',
      channelId: '',
      tabActiveName: 'media',
      hasAudio: false,
      isLoging: false,
      showPtz: true,
      streamInfo: null,
      playerHeight: '48vh',
      playerUrlInfo: {
        playerUrl: null,
        playUrl: null
      },
      broadcastRtc: null,
      broadcastStatus: -1
    }
  },
  methods: {
    tabHandleClick(tab) {
      if (tab.name === 'codec') {
        this.$refs.mediaInfo && this.$refs.mediaInfo.startTask()
      } else {
        this.$refs.mediaInfo && this.$refs.mediaInfo.stopTask()
      }
    },
    openDialog(tab, deviceId, channelId, param) {
      if (this.showVideoDialog) return
      this.tabActiveName = tab || 'media'
      if (tab === 'streamPlay') {
        this.showPtz = false
        this.tabActiveName = 'media'
      } else {
        this.showPtz = true
      }
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
    play(streamInfo, hasAudio) {
      this.streamInfo = streamInfo
      this.hasAudio = hasAudio
      this.isLoging = false
      this.streamId = streamInfo.stream
      this.app = streamInfo.app
      this.mediaServerId = streamInfo.mediaServerId
      this.showVideoDialog = true
      this.$nextTick(() => {
        if (this.$refs.playerTabs) {
          this.$refs.playerTabs.setStreamInfo(streamInfo.transcodeStream || streamInfo)
        }
      })
    },
    playerChanged(playerUrlInfo) {
      this.playerUrlInfo = playerUrlInfo
    },
    close() {
      if (this.$refs.playerTabs) {
        this.$refs.playerTabs.stop()
      }
      this.videoUrl = ''
      this.showVideoDialog = false
      this.stopBroadcast()
    },
    wiper(command) {
      this.$store.dispatch('jtDevice/wiper', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: command
      }).catch(e => {
        console.log(e)
      })
    },
    fillLight(command) {
      this.$store.dispatch('jtDevice/fillLight', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: command
      }).catch(e => {
        console.log(e)
      })
    },
    getBroadcastStatus() {
      if (this.broadcastStatus === -2) return 'primary'
      if (this.broadcastStatus === -1) return 'primary'
      if (this.broadcastStatus === 0) return 'warning'
      if (this.broadcastStatus === 1) return 'danger'
    },
    broadcastStatusClick() {
      if (this.broadcastStatus === -1) {
        this.broadcastStatus = 0
        this.$store.dispatch('jtDevice/startTalk', {
          phoneNumber: this.deviceId,
          channelId: this.channelId
        }).then(data => {
          const streamInfo = data
          if (document.location.protocol.includes('https')) {
            this.startBroadcast(streamInfo.rtcs)
          } else {
            this.startBroadcast(streamInfo.rtc)
          }
        }).catch(error => {
          this.$message.error(error)
          this.broadcastStatus = -1
        })
      } else if (this.broadcastStatus === 1) {
        this.broadcastStatus = -1
        if (this.broadcastRtc) {
          this.broadcastRtc.close()
        }
      }
    },
    startBroadcast(url) {
      this.$store.dispatch('user/getUserInfo')
        .then((data) => {
          if (data === null) {
            this.broadcastStatus = -1
            return
          }
          const pushKey = data.pushKey
          url += '&sign=' + crypto.createHash('md5').update(pushKey, 'utf8').digest('hex')
          console.log('开始语音喊话： ' + url)
          this.broadcastRtc = new ZLMRTCClient.Endpoint({
            debug: true,
            zlmsdpUrl: url,
            simulecast: false,
            useCamera: false,
            audioEnable: true,
            videoEnable: false,
            recvOnly: false
          })

          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_NOT_SUPPORT, (e) => {
            console.error('不支持webrtc', e)
            this.$message({ showClose: true, message: '不支持webrtc, 无法进行语音喊话', type: 'error' })
            this.broadcastStatus = -1
          })

          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, (e) => {
            console.error('ICE 协商出错')
            this.$message({ showClose: true, message: 'ICE 协商出错', type: 'error' })
            this.broadcastStatus = -1
          })

          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => {
            console.error('offer anwser 交换失败', e)
            this.$message({ showClose: true, message: 'offer anwser 交换失败' + e, type: 'error' })
            this.broadcastStatus = -1
          })

          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (e) => {
            console.log('状态改变', e)
            if (e === 'connecting') {
              this.broadcastStatus = 0
            } else if (e === 'connected') {
              this.broadcastStatus = 1
            } else if (e === 'disconnected') {
              this.broadcastStatus = -1
            }
          })

          this.broadcastRtc.on(ZLMRTCClient.Events.CAPTURE_STREAM_FAILED, (e) => {
            console.log('捕获流失败', e)
            this.$message({ showClose: true, message: '捕获流失败' + e, type: 'error' })
            this.broadcastStatus = -1
          })
        }).catch(e => {
          this.$message({ showClose: true, message: e, type: 'error' })
          this.broadcastStatus = -1
        })
    },
    stopBroadcast() {
      if (this.broadcastRtc) {
        this.broadcastRtc.close()
      }
      this.broadcastStatus = -1
      this.$store.dispatch('jtDevice/stopTalk', {
        phoneNumber: this.deviceId,
        channelId: this.channelId
      })
    }
  }
}
</script>

<style>
#devicePlayer .el-dialog__body { padding: 10px 20px; }
.dhsdk-player-body { display: flex; gap: 16px; }
.player-side { flex: 3; min-width: 0; }
.player-container { width: 100%; }
.control-side { flex: 2; min-width: 340px; display: flex; flex-direction: column; }
.control-tabs { flex: 1; display: flex; flex-direction: column; min-height: 220px }
.control-tabs .el-tabs__content { flex: 1; overflow: auto; }
.media-info-content { overflow: auto; }
.media-row { display: flex; margin-bottom: 0.5rem; height: 2.5rem; }
.media-label { width: 6rem; line-height: 2.5rem; text-align: right; flex-shrink: 0; }

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
