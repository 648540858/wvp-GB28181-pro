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
  name: 'ChannelPtzPanel',
  components: { ptzControls },
  props: {
    channelId: { type: String, default: null }
  },
  methods: {
    onPtzMove(e) {
      this.$store.dispatch('commonChanel/ptz', {
        channelId: this.channelId,
        command: e.direction,
        panSpeed: e.speed,
        tiltSpeed: e.speed,
        zoomSpeed: e.speed
      })
    },
    onPtzStop() {
      this.$store.dispatch('commonChanel/ptz', {
        channelId: this.channelId,
        command: 'stop',
        panSpeed: 0,
        tiltSpeed: 0,
        zoomSpeed: 0
      })
    },
    onFocusMove(e) {
      this.$store.dispatch('commonChanel/focus', {
        channelId: this.channelId,
        command: e.command,
        speed: e.speed
      })
    },
    onFocusStop() {
      this.$store.dispatch('commonChanel/focus', {
        channelId: this.channelId,
        command: 'stop',
        speed: 0
      })
    },
    onIrisMove(e) {
      this.$store.dispatch('commonChanel/iris', {
        channelId: this.channelId,
        command: e.command,
        speed: e.speed
      })
    },
    onIrisStop() {
      this.$store.dispatch('commonChanel/iris', {
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
