<template>
  <div>
    <component @goChannelPage="handleChannel" @goDevicePosition="handlePosition" @goGBRecords="handleGBRecords" @goBack="handleGoBack" :record="record" :is="currentComponet"></component>
  </div>
</template>

<script>
// 动态切换组件
import DeviceList from '@/views/device/DeviceList'
import DeviceChannelList from '@/views/device/DeviceChannelList'
import DevicePosition from "@/views/device/DevicePosition";
import GBRecords from "@/views/device/GBRecords";

export default {
  name: 'DeviceListWrapper',
  components: {
    DeviceList,
    DeviceChannelList,
    DevicePosition,
    GBRecords
  },
  data() {
    return {
      currentComponet: 'DeviceList',
      record: ''
    }
  },
  created() {

  },
  methods: {
    handleChannel(record) {
      this.record = record || ''
      this.currentComponet = 'DeviceChannelList'
    },
    handlePosition(record){
      this.record = record || ''
      this.currentComponet = 'DevicePosition'
    },
    handleGBRecords(record){
      this.record = record || ''
      this.currentComponet = 'GBRecords'
    },
    handleGoBack() {
      this.record = ''
      this.currentComponet = 'DeviceList'
    }
  },
  watch: {
    '$route.path'() {
      this.record = ''
      this.currentComponet = 'DeviceList'
    }
  }
}
</script>
