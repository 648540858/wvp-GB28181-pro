<template>
  <div style="height: 100%; display: flex; flex-direction: column;">
    <el-form size="small" inline style="margin-bottom: 12px; padding: 16px 8px; border: 1px solid #e6e6e6; border-radius: 4px;">
      <el-form-item label="扫描组号" style="margin-bottom: 0;">
        <el-input-number v-model="scanId" :min="1" :max="255" controls-position="right" style="width: 140px" />
      </el-form-item>
    </el-form>
    <div style="margin-bottom: 8px;">
      <el-button size="small" :loading="leftLoading" :disabled="leftLoading" @click="setLeft">设置左边界</el-button>
      <el-button size="small" :loading="rightLoading" :disabled="rightLoading" @click="setRight">设置右边界</el-button>
    </div>
    <el-form v-if="showSpeedInput" size="mini" inline style="margin-bottom: 8px;">
      <el-form-item label="扫描速度" style="margin-bottom: 0;">
        <el-input-number v-model="scanSpeed" :min="1" :max="255" controls-position="right" style="width: 120px" />
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button type="primary" @click="setSpeed">确定</el-button>
        <el-button @click="cancelSpeed">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="small" style="margin-bottom: 8px;" @click="showSpeedInput = true">设置扫描速度</el-button>
    <div style="margin-top: 8px;">
      <el-button size="small" type="primary" :loading="starting" :disabled="starting" @click="startScan">开始自动扫描</el-button>
      <el-button size="small" :loading="stopping" :disabled="stopping" @click="stopScan">停止自动扫描</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PtzScanConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      scanId: 1,
      showSpeedInput: false,
      scanSpeed: 5,
      leftLoading: false,
      rightLoading: false,
      starting: false,
      stopping: false
    }
  },
  methods: {
    setLeft() {
      this.leftLoading = true
      this.$store.dispatch('frontEnd/setLeftForScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(() => {
          this.$message({ showClose: true, message: '左边界设置成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.leftLoading = false
        })
    },
    setRight() {
      this.rightLoading = true
      this.$store.dispatch('frontEnd/setRightForScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(() => {
          this.$message({ showClose: true, message: '右边界设置成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.rightLoading = false
        })
    },
    setSpeed() {
      this.$store.dispatch('frontEnd/setSpeedForScan', [this.deviceId, this.channelDeviceId, this.scanId, this.scanSpeed])
        .then(() => {
          this.showSpeedInput = false
          this.$message({ showClose: true, message: '速度设置成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
    },
    cancelSpeed() {
      this.showSpeedInput = false
      this.scanSpeed = 5
    },
    startScan() {
      this.starting = true
      this.$store.dispatch('frontEnd/startScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(() => {
          this.$message({ showClose: true, message: '扫描启动成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.starting = false
        })
    },
    stopScan() {
      this.stopping = true
      this.$store.dispatch('frontEnd/stopScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(() => {
          this.$message({ showClose: true, message: '扫描停止成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.stopping = false
        })
    }
  }
}
</script>
