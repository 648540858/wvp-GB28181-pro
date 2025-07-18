<template>
  <div id="JTDevice" class="app-container">
    <deviceList v-show="show === 'device'" @show-channel="showChannelList" @show-param="showParam" />
    <channelList v-if="show === 'channel'" :device-id="deviceId" @show-device="showDevice" />
    <deviceParam v-if="show === 'param'" :phone-number="phoneNumber" @show-device="showDevice" />
  </div>
</template>

<script>
import deviceList from './list.vue'
import channelList from './channel/index.vue'
import deviceParam from './deviceParam/index.vue'

export default {
  name: 'JTDevice',
  components: {
    deviceList,
    channelList,
    deviceParam
  },
  data() {
    return {
      show: 'device',
      deviceId: null,
      phoneNumber: null
    }
  },
  methods: {
    showChannelList: function(deviceId) {
      this.deviceId = deviceId
      this.show = 'channel'
    },
    showParam: function(phoneNumber) {
      this.phoneNumber = phoneNumber
      this.show = 'param'
    },
    showDevice: function() {
      this.deviceId = null
      this.phoneNumber = null
      this.show = 'device'
    }
  }
}
</script>
