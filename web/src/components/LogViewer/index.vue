<template>
  <pre ref="viewer" class="log-viewer" :style="{ height: normalizedHeight }" aria-live="polite">{{ plainLog }}</pre>
</template>

<script>
import stripAnsi from 'strip-ansi'

export default {
  name: 'LogViewer',
  props: {
    log: { type: String, default: '' },
    autoScroll: { type: Boolean, default: true },
    height: { type: [String, Number], default: '100%' }
  },
  computed: {
    plainLog() {
      return stripAnsi(this.log)
    },
    normalizedHeight() {
      return typeof this.height === 'number' ? `${this.height}px` : this.height
    }
  },
  watch: {
    log() {
      if (!this.autoScroll) return
      this.$nextTick(() => {
        const viewer = this.$refs.viewer
        if (viewer) viewer.scrollTop = viewer.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.log-viewer {
  box-sizing: border-box;
  width: 100%;
  min-height: 200px;
  margin: 0;
  padding: 12px 16px;
  overflow: auto;
  color: #d9e2ec;
  font: 13px/1.6 Consolas, "Courier New", monospace;
  text-align: left;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 4px;
}
</style>
