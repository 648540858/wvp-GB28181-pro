<template>
  <div id="drawThinProgress" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      width="240px"
      top="13%"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      :show-close="true"
      style="text-align: center"
      @close="close()"
    >
      <el-progress type="circle" :percentage="percentage"  />
      <div style="text-align: center">
        {{ msg }}
      </div>
    </el-dialog>
  </div>
</template>

<script>
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'drawThinProgress',
  directives: { elDragDialog },
  props: ['platformId'],
  data() {
    return {
      endCallBack: null,
      syncStatus: null,
      percentage: 0,
      showDialog: false,
      isLoging: false,
      syncFlag: false,
      drawThinId: null,
      timer: null,
      errorTimer: null,
      msg: '正在同步'
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(drawThinId, endCallBack) {
      console.log('drawThinId: ' + drawThinId)
      this.drawThinId = drawThinId
      this.showDialog = true
      this.msg = ''
      this.percentage = 0
      this.syncFlag = false
      this.syncStatus = null
      this.endCallBack = endCallBack
      this.getProgress()
    },
    getProgress() {
      this.$store.dispatch('commonChanel/thinProgress', this.drawThinId)
        .then(({ data }) => {
          this.syncFlag = true
          this.percentage = data.process * 100
          this.msg = data.msg
          console.log('drawThinId: ' + data.drawThinId)
          this.timer = setTimeout(this.getProgress, 300)

        }).catch((error) => {
          this.syncStatus = 'error'
          this.msg = error
          window.clearTimeout(this.errorTimer)
          this.errorTimer = setTimeout(() => {
            this.showDialog = false
          }, 2000)
        })
    },
    close: function() {
      if (this.endCallBack) {
        this.endCallBack()
      }
      window.clearTimeout(this.timer)
    }
  }
}
</script>
