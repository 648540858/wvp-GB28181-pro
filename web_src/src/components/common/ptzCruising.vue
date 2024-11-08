<template>
  <div id="ptzPreset" style="width: 100%">
    <el-tag v-for="item in presetList"
            key="item.presetId"
            closable
            @close="delPreset(item)"
            @click="gotoPreset(item)"
            style="margin-right: 1rem; cursor: pointer"
    >
      {{item.presetName?item.presetName:item.presetId}}
    </el-tag>
    <el-input
      min="1"
      max="255"
      placeholder="预置位编号"
      addonBefore="预置位编号"
      addonAfter="(1-255)"
      style="width: 120px; vertical-align: bottom;"
      v-if="inputVisible"
      v-model="ptzPresetId"
      ref="saveTagInput"
      size="small"
    >
      <template v-slot:append>
        <el-button  @click="addPreset()">保存</el-button>
      </template>
    </el-input>
    <el-button v-else size="small" @click="showInput">+ 添加</el-button>
  </div>
</template>

<script>

export default {
  name: "ptzPreset",
  props: [ 'channelDeviceId', 'deviceId'],
  components: {},
  created() {
    this.getPresetList()
  },
  data() {
    return {
      presetList: [],
      inputVisible: false,
      ptzPresetId: '',
    };
  },
  methods: {
    getPresetList: function () {
      this.$axios({
        method: 'get',
        url: `/api/ptz/preset/query/${this.deviceId}/${this.channelDeviceId}`,
      }).then((res)=> {
        if (res.data.code === 0) {
          this.presetList = res.data.data;
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout();
          })
        }

      }).catch((error)=> {

        console.log(error);
      });
    },
    showInput() {
      this.inputVisible = true;
      this.$nextTick(_ => {
        this.$refs.saveTagInput.$refs.input.focus();
      });
    },
    addPreset: function (){
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$axios({
        method: 'get',
        url: `/api/ptz/preset/add/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          presetId: this.ptzPresetId
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          setTimeout(()=>{
            loading.close()
            this.inputVisible = false;
            this.ptzPresetId = ""
            this.getPresetList()
          }, 1000)
        }else {
          loading.close()
          this.inputVisible = false;
          this.ptzPresetId = ""
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error)=> {
        loading.close()
        this.inputVisible = false;
        this.ptzPresetId = ""
        this.$message({
          showClose: true,
          message: error,
          type: 'error'
        });
      });
    },
    gotoPreset: function (preset){
      console.log(preset)
      this.$axios({
        method: 'get',
        url: `/api/ptz/preset/call/${this.deviceId}/${this.channelDeviceId}`,
        params: {
          presetId: preset.presetId
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: '调用成功',
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
      });
    },
    delPreset: function (preset){
      this.$confirm("确定删除此预置位", '提示', {
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
          url: `/api/ptz/preset/delete/${this.deviceId}/${this.channelDeviceId}`,
          params: {
            presetId: preset.presetId
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            setTimeout(()=>{
              loading.close()
              this.getPresetList()
            }, 1000)
          }else {
            loading.close()
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: 'error'
            });
          }

        }).catch((error)=> {
          loading.close()
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          });
        });
      }).catch(() => {

      });

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
