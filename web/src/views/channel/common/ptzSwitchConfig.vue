<template>
  <div>
    <el-form inline label-width="120px" size="small">
      <el-form-item label="开关编号" style="margin-bottom: 0;">
        <el-input-number v-model="switchId" :min="1" :max="255" controls-position="right" style="width: 140px" />
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button type="primary" :loading="loading" :disabled="loading" @click="control('on')">开启</el-button>
        <el-button :loading="loading" :disabled="loading" @click="control('off')">关闭</el-button>
      </el-form-item>
      <el-divider />

      <el-form-item style="margin-bottom: 0;" label="雨刷">
        <el-button type="primary" :loading="wiperLoading" :disabled="wiperLoading" @click="wiperControl('on')">开启</el-button>
        <el-button :loading="wiperLoading" :disabled="wiperLoading" @click="wiperControl('off')">关闭</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'ChPtzSwitchConfig',
  props: {
    channelId: { type: String, default: null }
  },
  data() {
    return {
      switchId: 1,
      loading: false,
      wiperLoading: false
    }
  },
  methods: {
    wiperControl(command) {
      this.wiperLoading = true
      this.$store.dispatch('commonChanel/wiper', { channelId: this.channelId, command })
        .then(() => {
          this.$message({ showClose: true, message: command === 'on' ? '雨刷已开启' : '雨刷已关闭', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.wiperLoading = false
        })
    },
    control(command) {
      this.loading = true
      this.$store.dispatch('commonChanel/auxiliary', { channelId: this.channelId, command, auxiliaryId: this.switchId })
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
