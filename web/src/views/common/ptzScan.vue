<template>
  <div id="ptzScan">
    <div style="display: grid; grid-template-columns: 80px auto; line-height: 28px">
      <span>扫描组号: </span>
      <el-input
        v-model="scanId"
        min="1"
        max="255"
        placeholder="扫描组号"
        addon-before="扫描组号"
        addon-after="(1-255)"
        size="mini"
      />
    </div>

    <el-button size="mini" @click="setScanLeft">设置左边界</el-button>
    <el-button size="mini" @click="setScanRight">设置右边界</el-button>

    <el-form v-if="setSpeedVisible" size="mini" :inline="true">
      <el-form-item>
        <el-input
          v-if="setSpeedVisible"
          v-model="speed"
          min="1"
          max="4095"
          placeholder="巡航速度"
          addon-before="巡航速度"
          addon-after="(1-4095)"
          size="mini"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setSpeed">保存</el-button>
        <el-button @click="cancelSetSpeed">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setSpeedVisible = true">设置扫描速度</el-button>

    <el-button size="mini" @click="startScan">开始自动扫描</el-button>
    <el-button size="mini" @click="stopScan">停止自动扫描</el-button>
  </div>
</template>

<script>

export default {
  name: 'PtzScan',
  components: {},
  props: ['channelDeviceId', 'deviceId'],
  data() {
    return {
      scanId: 1,
      setSpeedVisible: false,
      speed: ''
    }
  },
  created() {
  },
  methods: {
    setSpeed: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('frontEnd/setSpeedForScan', [this.deviceId, this.channelDeviceId, this.scanId, this.speed])
        .then(data => {
          this.$message({
            showClose: true,
            message: '保存成功',
            type: 'success'
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.speed = ''
          this.setSpeedVisible = false
          loading.close()
        })
    },
    cancelSetSpeed: function() {
      this.speed = ''
      this.setSpeedVisible = false
    },
    setScanLeft: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('frontEnd/setLeftForScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(data => {
          this.$message({
            showClose: true,
            message: '保存成功',
            type: 'success'
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.speed = ''
          this.setSpeedVisible = false
          loading.close()
        })
    },
    setScanRight: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('frontEnd/setRightForScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(data => {
          this.$message({
            showClose: true,
            message: '保存成功',
            type: 'success'
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.speed = ''
          this.setSpeedVisible = false
          loading.close()
        })
    },
    startScan: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('frontEnd/startScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(data => {
          this.$message({
            showClose: true,
            message: '发送成功',
            type: 'success'
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          loading.close()
        })
    },
    stopScan: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('frontEnd/stopScan', [this.deviceId, this.channelDeviceId, this.scanId])
        .then(data => {
          this.$message({
            showClose: true,
            message: '发送成功',
            type: 'success'
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          loading.close()
        })
    }
  }
}
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}

</style>
