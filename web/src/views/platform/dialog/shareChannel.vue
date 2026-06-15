<template>
  <div id="chooseChannel">
    <el-dialog v-if="showDialog" v-el-drag-dialog v-loading="loading" title="通道共享" top="2rem" width="80%" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()">
      <shareChannelAdd ref="shareChannelAdd" :platform-id="platformId" />
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import shareChannelAdd from './shareChannelAdd.vue'

export default {
  name: 'ChooseChannel',
  directives: { elDragDialog },
  components: { shareChannelAdd },
  props: {},
  data() {
    return {
      loading: false,
      tabActiveName: 'gbChannel',
      catalogTabActiveName: 'addShare',
      platformId: '',
      showDialog: false,
      chooseData: {}
    }
  },
  computed: {},
  methods: {
    openDialog(platformId, closeCallback) {
      this.platformId = platformId
      this.showDialog = true
      this.closeCallback = closeCallback
    },
    close: function() {
      this.closeCallback()
    }
  }
}
</script>

<style>

</style>
