<template>
  <div>
    <component @goGbChannelPage="goGbChannelPage" @goPlatformListPage="goPlatformListPage" @goStreamPage="goStreamPage" @goBack="handleGoBack" :platformId="platformId" :is="currentComponent"></component>
  </div>
</template>

<script>
// 动态切换组件
import GbPlatformList from "@/views/gbPlatform/GbPlatformList";
import ChannelForGb from "@/views/gbPlatform/ChannelForGb";
import ChannelForStream from "@/views/gbPlatform/ChannelForStream";

export default {
  name: 'DeviceListWrapper',
  components: {
    GbPlatformList,
    ChannelForStream,
    ChannelForGb
  },
  data() {
    return {
      currentComponent: 'GbPlatformList',
      platformId: ''
    }
  },
  methods: {
    goGbChannelPage(platformId) {
      this.platformId = platformId || ''
      this.currentComponent = 'ChannelForGb'
    },
    goPlatformListPage(){
      this.currentComponent = 'GbPlatformList'
    },
    goStreamPage(platformId){
      this.platformId = platformId || ''
      this.currentComponent = 'ChannelForStream'
    },
    handleGoBack() {
      this.record = {}
      this.currentComponent = 'GbPlatformList'
    }
  },
  watch: {
    '$route.path'() {
      this.platformId = ''
      this.currentComponent = 'GbPlatformList'
    }
  }
}
</script>
