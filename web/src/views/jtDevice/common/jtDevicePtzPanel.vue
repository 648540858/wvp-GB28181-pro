<template>
  <div class="ptz-section">
    <ptzControls
      btn-layout="row"
      :show-diagonals="false"
      @ptz-move="onPtzMove"
      @ptz-stop="onPtzStop"
      @focus-move="onFocusMove"
      @focus-stop="onFocusStop"
      @iris-move="onIrisMove"
      @iris-stop="onIrisStop"
    />
  </div>
</template>

<script>
import ptzControls from '../../common/ptzControls.vue'

export default {
  name: 'JtDevicePtzPanel',
  components: { ptzControls },
  props: {
    deviceId: { type: String, default: null },
    channelId: { type: String, default: null }
  },
  methods: {
    onPtzMove(e) {
      this.$store.dispatch('jtDevice/ptz', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: e.direction,
        speed: e.speed
      })
    },
    onPtzStop() {
      this.$store.dispatch('jtDevice/ptz', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: 'stop',
        speed: 0
      })
    },
    onFocusMove(e) {
      const command = e.command === 'near' ? 'focusnear' : 'focusfar'
      this.$store.dispatch('jtDevice/ptz', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: command,
        speed: e.speed
      })
    },
    onFocusStop() {
      this.$store.dispatch('jtDevice/ptz', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: 'stop',
        speed: 0
      })
    },
    onIrisMove(e) {
      const command = e.command === 'in' ? 'irisin' : 'irisout'
      this.$store.dispatch('jtDevice/ptz', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: command,
        speed: e.speed
      })
    },
    onIrisStop() {
      this.$store.dispatch('jtDevice/ptz', {
        phoneNumber: this.deviceId,
        channelId: this.channelId,
        command: 'stop',
        speed: 0
      })
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
