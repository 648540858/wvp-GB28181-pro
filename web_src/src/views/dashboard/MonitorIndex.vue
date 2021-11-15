<template>
  <div>
    <component @goMediaPage="handleMediaInfo" @goSipPage="handleSIPInfo" @goBack="handleGoBack" :mediaInfo="mediaInfo"
               :sipInfo="sipInfo" :is="currentComponet"></component>
  </div>
</template>

<script>
import MediaInfo from "@/views/dashboard/MediaInfo";
import SIPInfo from "@/views/dashboard/SIPInfo";
import Monitor from "@/views/dashboard/Monitor";

export default {
  name: "MonitorIndex",
  components: {
    MediaInfo,
    SIPInfo,
    Monitor
  },
  data() {
    return {
      currentComponet: 'Monitor',
      mediaInfo: {},
      sipInfo: {}
    }
  },
  methods: {
    handleMediaInfo(mediaInfo) {
      this.mediaInfo = mediaInfo || {}
      this.currentComponet = 'MediaInfo'
    },
    handleSIPInfo(sipInfo) {
      this.sipInfo = sipInfo || {}
      this.currentComponet = "SIPInfo"
    },
    handleGoBack() {
      this.mediaInfo = {}
      this.sipInfo = {}
      this.currentComponet = "Monitor"
    }
  }
}
</script>

<style scoped>

</style>