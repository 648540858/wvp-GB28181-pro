<template>
  <div>
    <el-form size="mini" inline>
      <el-form-item label="开关编号" style="margin-bottom: 0;">
        <el-input-number v-model="switchId" :min="1" :max="255" controls-position="right" style="width: 140px" />
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button size="small" :loading="loading" :disabled="loading" @click="control('on')">开启</el-button>
        <el-button size="small" :loading="loading" :disabled="loading" @click="control('off')">关闭</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'PtzSwitchConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      switchId: 1,
      loading: false
    }
  },
  methods: {
    control(command) {
      this.loading = true
      this.$store.dispatch('frontEnd/auxiliary', [this.deviceId, this.channelDeviceId, command, this.switchId])
        .then(() => {
          this.$message({ showClose: true, message: command === 'on' ? '开关已开启' : '开关已关闭', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
