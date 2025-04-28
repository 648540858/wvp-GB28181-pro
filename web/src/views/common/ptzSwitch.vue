<template>
  <div id="ptzScan">
    <el-form size="mini" :inline="true">
      <el-form-item>
        <el-input
          v-model="switchId"
          min="1"
          max="4095"
          placeholder="开关编号"
          addon-before="开关编号"
          addon-after="(2-255)"
          size="mini"
        />
      </el-form-item>
      <el-form-item>
        <el-button size="mini" @click="open('on')">开启</el-button>
        <el-button size="mini" @click="open('off')">关闭</el-button>
      </el-form-item>
    </el-form>

  </div>
</template>

<script>

export default {
  name: 'PtzScan',
  components: {},
  props: ['channelDeviceId', 'deviceId'],
  data() {
    return {
      switchId: 1
    }
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
      this.$store.dispatch('frontEnd/auxiliary', [this.deviceId, this.channelDeviceId, command, this.switchId])
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
