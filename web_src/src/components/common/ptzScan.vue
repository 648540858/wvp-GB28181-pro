<template>
  <div id="ptzScan">
    <div style="display: grid; grid-template-columns: 80px auto; line-height: 28px">
      <span>扫描组号: </span>
      <el-input
        min="1"
        max="255"
        placeholder="扫描组号"
        addonBefore="扫描组号"
        addonAfter="(1-255)"
        v-model="scanId"
        size="mini"
      >
      </el-input>
    </div>

    <el-button size="mini" @click="setScanLeft">设置左边界</el-button>
    <el-button size="mini" @click="setScanRight">设置右边界</el-button>

    <el-form size="mini" :inline="true" v-if="setSpeedVisible">
      <el-form-item >
        <el-input
          min="1"
          max="4095"
          placeholder="巡航速度"
          addonBefore="巡航速度"
          addonAfter="(1-4095)"
          v-if="setSpeedVisible"
          v-model="speed"
          size="mini"
        >
        </el-input>
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
  name: "ptzScan",
  props: [ 'channelDeviceId', 'deviceId'],
  components: {},
  created() {
  },
  data() {
    return {
      scanId: 1,
      setSpeedVisible: false,
      speed: '',
    };
  },
  methods: {
    setSpeed: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/scan/set/speed/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          scanId: this.scanId,
          speed: this.speed
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: "保存成功",
            type: 'success'
          });
        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error)=> {
        this.$message({
          showClose: true,
          message: error,
          type: 'error'
        });
      }).finally(()=>{
        this.speed = ""
        this.setSpeedVisible = false
        loading.close()
      })
    },
    cancelSetSpeed: function (){
      this.speed = ""
      this.setSpeedVisible = false
    },
    setScanLeft: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/scan/set/left/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          scanId: this.scanId,
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: "保存成功",
            type: 'success'
          });
        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error)=> {
        this.$message({
          showClose: true,
          message: error,
          type: 'error'
        });
      }).finally(()=>{
        loading.close()
      })
    },
    setScanRight: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/scan/set/right/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          scanId: this.scanId,
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: "保存成功",
            type: 'success'
          });
        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error)=> {
        this.$message({
          showClose: true,
          message: error,
          type: 'error'
        });
      }).finally(()=>{
        this.setSpeedVisible = false;
        this.speed = "";
        loading.close()
      })
    },
    startScan: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/scan/start/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          scanId: this.scanId
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: "发送成功",
            type: 'success'
          });
        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error)=> {
        this.$message({
          showClose: true,
          message: error,
          type: 'error'
        });
      }).finally(()=>{
        loading.close()
      })
    },
    stopScan: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/scan/stop/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          scanId: this.scanId
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: "发送成功",
            type: 'success'
          });
        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error)=> {
        this.$message({
          showClose: true,
          message: error,
          type: 'error'
        });
      }).finally(()=>{
        loading.close()
      })
    },
  },
};
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
