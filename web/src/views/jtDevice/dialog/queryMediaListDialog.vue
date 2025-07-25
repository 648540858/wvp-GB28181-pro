<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="存储多媒体数据检索"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <queryMediaList :phoneNumber="phoneNumber" :deviceId="deviceId" :channelList="channelList"/>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import queryMediaList from './queryMediaList.vue'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  components: { queryMediaList },
  props: {},
  data() {
    return {
      showDialog: false,
      phoneNumber: null,
      deviceId: null,
      channelList: null
      }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(phoneNumber, deviceId) {
      this.showDialog = true
      this.phoneNumber = phoneNumber
      this.deviceId = deviceId
      this.$store.dispatch('jtDevice/queryChannels', {
        page: 1,
        count: 1000,
        deviceId: this.deviceId
      })
        .then(data => {
          this.channelList = data.list
        })
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>
