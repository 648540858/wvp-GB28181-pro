<template>
  <div id="ptzWiper">
    <el-button size="mini" @click="open('on')">开启</el-button>
    <el-button size="mini" @click="open('off')">关闭</el-button>
  </div>
</template>

<script>

export default {
  name: 'PtzWiper',
  components: {},
  props: ['channelDeviceId', 'deviceId'],
  data() {
    return {}
  },
  created() {
  },
  methods: {
    open: function(command) {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('frontEnd/wiper', [this.deviceId, this.channelDeviceId, command])
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
