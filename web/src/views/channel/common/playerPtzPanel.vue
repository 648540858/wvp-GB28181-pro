<template>
  <div class="player-ptz-panel">
    <div class="player-section">
      <div class="player-wrapper" :style="{ height: playerHeight }">
        <playerTabs ref="playerTabs" :has-audio="hasAudio" :show-button="true" />
      </div>
    </div>
    <channelPtzPanel
      style="margin-top: 5vh"
      :channel-id="channelId"
      @drag-zoom-start="toggleDragZoom"
    />
  </div>
</template>

<script>
import playerTabs from '../../common/playerTabs.vue'
import channelPtzPanel from './channelPtzPanel.vue'

export default {
  name: 'ChPlayerPtzPanel',
  components: { playerTabs, channelPtzPanel },
  props: {
    channelId: { type: String, default: null }
  },
  data() {
    return {
      hasAudio: false,
      playerHeight: '40vh',
      dragZoomDirection: ''
    }
  },
  mounted() {
    this.startPlay()
  },
  beforeDestroy() {
    this.stopPlay()
  },
  methods: {
    startPlay() {
      this.$store.dispatch('commonChanel/playChannel', this.channelId)
        .then(data => {
          this.hasAudio = data.hasAudio
          this.$nextTick(() => {
            if (this.$refs.playerTabs) {
              this.$refs.playerTabs.setStreamInfo(data.transcodeStream || data)
            }
          })
        })
        .catch(e => {
          this.$message({ showClose: true, message: e || '播放失败', type: 'error' })
        })
    },
    stopPlay() {
      this.$store.dispatch('commonChanel/stopPlayChannel', this.channelId)
        .catch(() => {})
    },
    toggleDragZoom(direction) {
      this.dragZoomDirection = direction
      this.$refs.playerTabs.startDragZoom((params) => {
        params.deviceId = this.channelId
        params.channelId = this.channelId
        const action = this.dragZoomDirection === 'in' ? 'commonChanel/dragZoomIn' : 'commonChanel/dragZoomOut'
        const successMsg = this.dragZoomDirection === 'in' ? '拉框放大成功' : '拉框缩小成功'
        const failMsg = this.dragZoomDirection === 'in' ? '拉框放大失败' : '拉框缩小失败'
        this.$store.dispatch(action, params).then(() => {
          this.$message({ showClose: true, message: successMsg, type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: failMsg, type: 'error' })
        })
        this.dragZoomDirection = ''
      })
    }
  }
}
</script>

<style scoped>
.player-ptz-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.player-section {
  flex: 0.8;
}
.player-wrapper {
  position: relative;
  width: 100%;
}
</style>
