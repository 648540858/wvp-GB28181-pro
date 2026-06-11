<template>
  <div class="ptz-section">
    <ptzControls
      btn-layout="row"
      @ptz-move="onPtzMove"
      @ptz-stop="onPtzStop"
      @focus-move="onFocusMove"
      @focus-stop="onFocusStop"
      @iris-move="onIrisMove"
      @iris-stop="onIrisStop"
      @toggle-drag-zoom="$emit('drag-zoom-start', 'in')"
      @toggle-drag-zoom-out="$emit('drag-zoom-start', 'out')"
    />
  </div>
</template>

<script>
import ptzControls from '../../common/ptzControls.vue'

export default {
  name: 'DevicePtzPanel',
  components: { ptzControls },
  props: {
    deviceId: { type: String, default: null },
    channelId: { type: String, default: null }
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
        zoomSpeed: parseInt(e.speed * 15 / 100)
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
    }
  }
}
</script>

<style scoped>
.ptz-section {
  flex-shrink: 0;
  margin-bottom: 8px;
}
</style>
