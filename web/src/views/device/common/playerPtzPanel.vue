<template>
  <div class="player-ptz-panel">
    <div class="player-section">
      <div class="player-wrapper" :style="{ height: playerHeight }">
        <playerTabs ref="playerTabs" :has-audio="hasAudio" :show-button="true" />
      </div>
    </div>
    <div class="ptz-section">
      <ptzControls
        :device-id="deviceId"
        :channel-id="channelDeviceId"
        :show-precise="false"
        @ptz-move="onPtzMove"
        @ptz-stop="onPtzStop"
        @focus-move="onFocusMove"
        @focus-stop="onFocusStop"
        @iris-move="onIrisMove"
        @iris-stop="onIrisStop"
      />
    </div>
  </div>
</template>

<script>
import playerTabs from '../../common/playerTabs.vue'
import ptzControls from '../../common/ptzControls.vue'

export default {
  name: 'PlayerPtzPanel',
  components: { playerTabs, ptzControls },
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      hasAudio: false,
      playerHeight: '36vh'
    }
  },
  mounted() {
    this.startPlay()
  },
  beforeDestroy() {
    this.stopPlay()
  },
  methods: {
    ptzSpeed(speed) {
      return parseInt(speed * 255 / 100)
    },
    onPtzMove(e) {
      const speedVal = this.ptzSpeed(e.speed)
      this.$store.dispatch('frontEnd/ptz', {
        deviceId: this.deviceId,
        channelId: this.channelDeviceId,
        command: e.direction,
        horizonSpeed: speedVal,
        verticalSpeed: speedVal,
        zoomSpeed: parseInt(e.speed * 15 / 100)
      })
    },
    onPtzStop() {
      this.$store.dispatch('frontEnd/ptz', {
        deviceId: this.deviceId,
        channelId: this.channelDeviceId,
        command: 'stop',
        horizonSpeed: 0,
        verticalSpeed: 0,
        zoomSpeed: 0
      })
    },
    onFocusMove(e) {
      const speedVal = this.ptzSpeed(e.speed)
      this.$store.dispatch('frontEnd/focus', [this.deviceId, this.channelDeviceId, e.command, speedVal])
    },
    onFocusStop() {
      this.$store.dispatch('frontEnd/focus', [this.deviceId, this.channelDeviceId, 'stop', 0])
    },
    onIrisMove(e) {
      const speedVal = this.ptzSpeed(e.speed)
      this.$store.dispatch('frontEnd/iris', [this.deviceId, this.channelDeviceId, e.command, speedVal])
    },
    onIrisStop() {
      this.$store.dispatch('frontEnd/iris', [this.deviceId, this.channelDeviceId, 'stop', 0])
    },
    startPlay() {
      this.$store.dispatch('play/play', [this.deviceId, this.channelDeviceId])
        .then(data => {
          this.hasAudio = data.hasAudio
          this.$nextTick(() => {
            if (this.$refs.playerTabs) {
              this.$refs.playerTabs.setStreamInfo(data)
            }
          })
        })
        .catch(e => {
          this.$message({ showClose: true, message: e || '播放失败', type: 'error' })
        })
    },
    stopPlay() {
      this.$store.dispatch('play/stop', { deviceId: this.deviceId, channelId: this.channelDeviceId })
        .catch(() => {})
    },

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
.ptz-section {
  flex-shrink: 0;
  display: flex;
}
.player-wrapper {
  position: relative;
  width: 100%;
}
</style>
