<template>
  <div id="streamPushPlayer" v-loading="isLoging">
    <el-dialog
      v-if="showVideoDialog"
      v-el-drag-dialog
      title="视频播放"
      top="5vh"
      width="70vw"
      :close-on-click-modal="false"
      :visible.sync="showVideoDialog"
      @close="close()"
    >
      <div class="push-player-body">
        <div class="player-side">
          <div class="player-container">
            <playerTabs ref="playerTabs" :has-audio="hasAudio" :show-button="true"
              @playerChanged="playerChanged" />
          </div>
        </div>

        <div class="control-side">
          <div class="info-card info-card--fill">
            <div class="info-card__title">编码信息</div>
            <div class="info-card__body">
              <mediaInfo ref="mediaInfo" :app="app" :stream="streamId" :media-server-id="mediaServerId" />
            </div>
          </div>
          <div class="info-card info-card--bottom">
            <streamMediaPanel :player-url="playerUrlInfo.playerUrl" :play-url="playerUrlInfo.playUrl" :stream-info="streamInfo" />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import elDragDialog from '@/directive/el-drag-dialog'
import playerTabs from '../common/playerTabs.vue'
import mediaInfo from '../common/mediaInfo.vue'
import streamMediaPanel from '../common/streamMediaPanel.vue'

export default {
  name: 'StreamPushPlayer',
  directives: { elDragDialog },
  components: { playerTabs, mediaInfo, streamMediaPanel },
  props: {},
  data() {
    return {
      videoUrl: '',
      streamId: '',
      app: '',
      mediaServerId: '',
      hasAudio: false,
      isLoging: false,
      showVideoDialog: false,
      streamInfo: null,
      playerHeight: '48vh',
      playerUrlInfo: {
        playerUrl: null,
        playUrl: null,
      }
    }
  },
  methods: {
    openDialog: function(streamInfo, hasAudio) {
      if (this.showVideoDialog) return
      this.tabActiveName = 'media'
      this.streamId = ''
      this.mediaServerId = ''
      this.app = ''
      this.videoUrl = ''
      if (streamInfo) {
        this.play(streamInfo, hasAudio)
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
          this.$refs.playerTabs.setStreamInfo(streamInfo.transcodeStream || streamInfo)
        }
        this.$refs.mediaInfo && this.$refs.mediaInfo.startTask()
      })
    },
    playerChanged: function(playerUrlInfo) {
      this.playerUrlInfo = playerUrlInfo
    },
    close: function() {
      if (this.$refs.playerTabs) {
        this.$refs.playerTabs.stop()
      }
      this.$refs.mediaInfo && this.$refs.mediaInfo.stopTask()
      this.videoUrl = ''
      this.showVideoDialog = false
    },
  }
}
</script>

<style>
#streamPushPlayer .el-dialog__body { padding: 12px 20px; }
.push-player-body { display: flex; gap: 16px; height: 100%; }
.player-side { flex: 3; min-width: 0; }
.player-container { width: 100%; height: calc(75vh - 80px); }
.control-side { flex: 2; min-width: 340px; display: flex; flex-direction: column; overflow-y: auto; }
.info-card { border-radius: 6px; padding: 12px; border: 1px solid #e8eaed; }
.info-card__title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #e8eaed;text-align: center; }
.info-card--fill { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.info-card--fill .info-card__body { flex: 1; overflow: auto; }
.info-card--bottom { flex-shrink: 0; margin-top: auto; }
.info-card .media-info-content { margin: -4px; }
</style>
