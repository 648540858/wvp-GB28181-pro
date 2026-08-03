<template>
  <div id="log" class="app-container">
    <div style="height: var(--wvp-page-content-height);">
      <showLog ref="recordVideoPlayer" :remote-url="remoteUrl" />
    </div>
  </div>
</template>

<script>

import showLog from './showLog.vue'

export default {
  name: 'OperationsRealLog',
  components: { showLog },
  data() {
    return {
      remoteUrl: this.getUrl()
    }
  },
  methods: {
    getUrl: function() {
      const baseUrl = process.env.NODE_ENV === 'development'
        ? process.env.VUE_APP_BASE_API
        : (window.baseUrl || '')
      const target = new URL(baseUrl || '/', window.location.origin)
      target.protocol = target.protocol === 'https:' ? 'wss:' : 'ws:'
      target.pathname = `${target.pathname.replace(/\/$/, '')}/channel/log`
      target.search = ''
      target.hash = ''
      return target.toString()
    }
  }
}
</script>
