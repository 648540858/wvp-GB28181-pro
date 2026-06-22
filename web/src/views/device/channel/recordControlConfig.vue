<template>
  <div class="record-control-form">
    <el-alert title="对当前通道下发录像控制指令" type="info" :closable="false" show-icon style="margin-bottom: 16px" />
    <el-button type="primary" :loading="startLoading" @click="handleRecord('Record')">开始录像</el-button>
    <el-button type="danger" :loading="stopLoading" @click="handleRecord('StopRecord')" style="margin-left: 12px">停止录像</el-button>
  </div>
</template>

<script>
export default {
  name: 'RecordControlConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      startLoading: false,
      stopLoading: false
    }
  },
  methods: {
    handleRecord(recordCmdStr) {
      const loadingKey = recordCmdStr === 'Record' ? 'startLoading' : 'stopLoading'
      this[loadingKey] = true
      const msg = recordCmdStr === 'Record' ? '开始录像' : '停止录像'
      this.$store.dispatch('device/deviceRecord', {
        deviceId: this.deviceId,
        channelId: this.channelDeviceId,
        recordCmdStr: recordCmdStr
      }).then(() => {
        this.$message({ showClose: true, message: msg + '成功', type: 'success' })
      }).catch((error) => {
        this.$message({ showClose: true, message: error.message || msg + '失败', type: 'error' })
      }).finally(() => {
        this[loadingKey] = false
      })
    }
  }
}
</script>

<style scoped>
.record-control-form {
  padding: 16px 0;
}
</style>
