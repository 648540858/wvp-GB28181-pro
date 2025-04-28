<template>
  <div id="log" class="app-container">
    <div style="height: calc(100vh - 124px);">
      <showLog ref="recordVideoPlayer" :remote-url="removeUrl" />
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
      loading: false,
      removeUrl: this.getURl(),
      winHeight: window.innerHeight - 220
    }
  },
  created() {
    console.log('removeUrl11 == ' + this.removeUrl)
  },
  methods: {
    getURl: function() {
      if (process.env.NODE_ENV !== 'development') {
        if (location.protocol === 'https:') {
          return `wss://${window.location.host}/channel/log`
        } else {
          return `ws://${window.location.host}/channel/log`
        }
      } else {
        return `ws://${window.location.host}${process.env.VUE_APP_BASE_API}/channel/log`
      }
    }
  }
}
</script>
