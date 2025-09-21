<template>
  <div id="playerDialog" >
    <el-dialog
      v-el-drag-dialog
      top="2rem"
      width="1460px"
      height="400px"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <cloudRecordPlayer ref="cloudRecordPlayer" ></cloudRecordPlayer>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import cloudRecordPlayer from './cloudRecordPlayer.vue'

export default {
  name: 'PlayerDialog',
  components: { cloudRecordPlayer },
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      streamInfo: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(streamInfo) {
      console.log(streamInfo)
      this.showDialog = true
      this.streamInfo = streamInfo
      this.$nextTick(() => {
          this.$refs.cloudRecordPlayer.setStreamInfo(streamInfo)
      })
    },
    stopPlay: function() {
      if (this.$refs.cloudRecordPlayer) {
        this.$refs.cloudRecordPlayer.stopPLay()
      }
    },
    close: function() {
      if (this.$refs.cloudRecordPlayer) {
        this.$refs.cloudRecordPlayer.stopPLay()
      }
      this.showDialog = false
    }
  }
}
</script>
