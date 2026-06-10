<template>
  <div>
    <el-button size="small" :loading="loading" :disabled="loading" @click="control('on')">开启</el-button>
    <el-button size="small" :loading="loading" :disabled="loading" @click="control('off')">关闭</el-button>
  </div>
</template>

<script>
export default {
  name: 'PtzWiperConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      loading: false
    }
  },
  methods: {
    control(command) {
      this.loading = true
      this.$store.dispatch('frontEnd/wiper', [this.deviceId, this.channelDeviceId, command])
        .then(() => {
          this.$message({ showClose: true, message: command === 'on' ? '雨刷已开启' : '雨刷已关闭', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
