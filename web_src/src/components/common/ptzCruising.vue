<template>
  <div id="ptzCruising">
    <div style="display: grid; grid-template-columns: 80px auto; line-height: 28px">
      <span>巡航组号: </span>
      <el-input
        min="1"
        max="255"
        placeholder="巡航组号"
        addonBefore="巡航组号"
        addonAfter="(1-255)"
        v-model="cruiseId"
        size="mini"
      >
      </el-input>
    </div>
    <p>
      <el-tag v-for="(item, index) in presetList"
              key="item.presetId"
              closable
              @close="delPreset(item, index)"
              style="margin-right: 1rem; cursor: pointer"
      >
        {{item.presetName?item.presetName:item.presetId}}
      </el-tag>
    </p>

    <el-form size="mini" :inline="true" v-if="selectPresetVisible">
      <el-form-item >
        <el-select v-model="selectPreset" placeholder="请选择预置点">
          <el-option
            v-for="item in allPresetList"
            :key="item.presetId"
            :label="item.presetName"
            :value="item">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="addCruisePoint">保存</el-button>
        <el-button type="primary" @click="cancelAddCruisePoint">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button size="mini" v-else @click="selectPresetVisible=true">添加巡航点</el-button>

    <el-form size="mini" :inline="true" v-if="setSpeedVisible">
      <el-form-item >
        <el-input
          min="1"
          max="4095"
          placeholder="巡航速度"
          addonBefore="巡航速度"
          addonAfter="(1-4095)"
          v-if="setSpeedVisible"
          v-model="cruiseSpeed"
          size="mini"
        >
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setCruiseSpeed">保存</el-button>
        <el-button @click="cancelSetCruiseSpeed">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setSpeedVisible = true">设置巡航速度</el-button>



    <el-form size="mini" :inline="true" v-if="setTimeVisible">
      <el-form-item >
        <el-input
          min="1"
          max="4095"
          placeholder="巡航停留时间(秒)"
          addonBefore="巡航停留时间(秒)"
          addonAfter="(1-4095)"
          style="width: 100%;"
          v-model="cruiseTime"
        >
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setCruiseTime">保存</el-button>
        <el-button @click="cancelSetCruiseTime">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setTimeVisible = true">设置巡航时间</el-button>
    <el-button size="mini" @click="startCruise">开始巡航</el-button>
    <el-button size="mini" @click="stopCruise">停止巡航</el-button>
    <el-button size="mini" type="danger" @click="deleteCruise">删除巡航</el-button>
  </div>
</template>

<script>

export default {
  name: "ptzCruising",
  props: [ 'channelDeviceId', 'deviceId'],
  components: {},
  created() {
    this.getPresetList()
  },
  data() {
    return {
      cruiseId: 1,
      presetList: [],
      allPresetList: [],
      selectPreset: "",
      inputVisible: false,
      selectPresetVisible: false,
      setSpeedVisible: false,
      setTimeVisible: false,
      cruiseSpeed: '',
      cruiseTime: '',
    };
  },
  methods: {
    getPresetList: function () {
      this.$axios({
        method: 'get',
        url: `/api/front-end/preset/query/${this.deviceId}/${this.channelDeviceId}`,
      }).then((res)=> {
        if (res.data.code === 0) {
          this.allPresetList = res.data.data;
        }

      }).catch((error)=> {

        console.log(error);
      });
    },
    addCruisePoint: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/cruise/point/add/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          cruiseId: this.cruiseId,
          presetId: this.selectPreset.presetId
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.presetList.push(this.selectPreset)
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
        this.selectPreset = ""
        this.selectPresetVisible = false;
        loading.close()
      })
    },
    cancelAddCruisePoint: function () {
      this.selectPreset = ""
      this.selectPresetVisible = false;
    },
    delPreset: function (preset, index){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/cruise/point/delete/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          cruiseId: this.cruiseId,
          presetId: preset.presetId
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.presetList.splice(index, 1)
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
    deleteCruise: function (preset, index){
      this.$confirm("确定删除此巡航组", '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const loading = this.$loading({
          lock: true,
          fullscreen: true,
          text: '正在发送指令',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        })
        this.$axios({
          method: 'get',
          url: `/api/front-end/cruise/point/delete/${this.deviceId}/${this.channelDeviceId}`,
          params: {
            cruiseId: this.cruiseId,
            presetId: 0
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            this.presetList = []
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
      })
    },
    setCruiseSpeed: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/cruise/speed/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          cruiseId: this.cruiseId,
          speed: this.cruiseSpeed
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
        this.cruiseSpeed = ""
        this.setSpeedVisible = false
        loading.close()
      })
    },
    cancelSetCruiseSpeed: function (){
      this.cruiseSpeed = ""
      this.setSpeedVisible = false
    },
    setCruiseTime: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/cruise/time/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          cruiseId: this.cruiseId,
          time: this.cruiseTime
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
        this.setTimeVisible = false;
        this.cruiseTime = "";
        loading.close()
      })
    },
    cancelSetCruiseTime: function (){
      this.setTimeVisible = false;
      this.cruiseTime = "";
    },
    startCruise: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/cruise/start/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          cruiseId: this.cruiseId
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
    stopCruise: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/front-end/cruise/stop/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          cruiseId: this.cruiseId
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
