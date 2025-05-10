<template>
  <div id="SyncChannelProgress" v-loading="isLoging">
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
      <el-progress type="circle" :percentage="percentage" :status="syncStatus" />
      <div style="text-align: center">
        {{ msg }}
      </div>
    </el-dialog>
  </div>
</template>

<script>
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'SyncChannelProgress',
  directives: { elDragDialog },
  props: ['platformId'],
  data() {
    return {
      endCallBack: null,
      syncStatus: null,
      percentage: 0,
      total: 0,
      current: 0,
      showDialog: false,
      isLoging: false,
      syncFlag: false,
      deviceId: null,
      timer: null,
      errorTimer: null,
      msg: '正在同步'
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(deviceId, endCallBack) {
      console.log('deviceId: ' + deviceId)
      this.deviceId = deviceId
      this.showDialog = true
      this.msg = ''
      this.percentage = 0
      this.total = 0
      this.current = 0
      this.syncFlag = false
      this.syncStatus = null
      this.endCallBack = endCallBack
      this.getProgress()
    },
    getProgress() {
      this.$store.dispatch('device/queryDeviceSyncStatus', this.deviceId)
        .then(({ data, code, msg }) => {
          if (data === null) {
            this.msg = msg
            this.timer = setTimeout(this.getProgress, 300)
          } else {
            if (data.syncIng) {
              if (data.total === 0) {
                this.msg = `等待同步中`
                this.timer = setTimeout(this.getProgress, 300)
              } else {
                this.syncFlag = true
                this.total = data.total
                this.current = data.current
                this.percentage = Math.floor(Number(data.current) / Number(data.total) * 10000) / 100
                this.msg = `同步中...[${data.current}/${data.total}]`
                this.timer = setTimeout(this.getProgress, 300)
              }
            } else {
              if (data.errorMsg) {
                this.msg = data.errorMsg
                this.syncStatus = 'exception'
              } else {
                this.syncStatus = 'success'
                this.percentage = 100
                this.msg = '同步成功'
                setTimeout(() => {
                  this.showDialog = false
                }, 3000)
              }
            }
          }
        }).catch((error) => {
          console.log(error)
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
