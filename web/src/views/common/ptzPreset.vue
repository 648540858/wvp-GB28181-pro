<template>
  <div id="ptzPreset" style="width: 100%">
    <el-tag
      v-for="item in presetList"
      :key="item.presetId"
      closable
      size="mini"
      style="margin-right: 1rem; cursor: pointer; margin-bottom: 0.6rem"
      @close="delPreset(item)"
      @click="gotoPreset(item)"
    >
      {{ item.presetName?item.presetName:item.presetId }}
    </el-tag>
    <el-input
      v-if="inputVisible"
      ref="saveTagInput"
      v-model="ptzPresetId"
      min="1"
      max="255"
      placeholder="预置位编号"
      addon-before="预置位编号"
      addon-after="(1-255)"
      style="width: 300px; vertical-align: bottom;"
      size="small"
    >
      <template v-slot:append>
        <el-button @click="addPreset()">保存</el-button>
        <el-button @click="cancel()">取消</el-button>
      </template>
    </el-input>
    <el-button v-else size="small" @click="showInput">+ 添加</el-button>
  </div>
</template>

<script>

export default {
  name: 'PtzPreset',
  components: {},
  props: ['channelDeviceId', 'deviceId'],
  data() {
    return {
      presetList: [],
      inputVisible: false,
      ptzPresetId: ''
    }
  },
  created() {
    this.getPresetList()
  },
  methods: {
    getPresetList: function() {
      this.$store.dispatch('frontEnd/queryPreset', [this.deviceId, this.channelDeviceId])
        .then(data => {
          this.presetList = data
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
    },
    showInput() {
      this.inputVisible = true
      this.$nextTick(_ => {
        this.$refs.saveTagInput.$refs.input.focus()
      })
    },
    addPreset: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('frontEnd/addPreset', [this.deviceId, this.channelDeviceId, this.ptzPresetId])
        .then(data => {
          setTimeout(() => {
            this.inputVisible = false
            this.ptzPresetId = ''
            this.getPresetList()
          }, 1000)
        }).catch((error) => {
          loading.close()
          this.inputVisible = false
          this.ptzPresetId = ''
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          loading.close()
        })
    },
    cancel: function() {
      this.inputVisible = false
      this.ptzPresetId = ''
    },
    gotoPreset: function(preset) {
      console.log(preset)
      this.$store.dispatch('frontEnd/callPreset', [this.deviceId, this.channelDeviceId, preset.presetId])
        .then(data => {
          this.$message({
            showClose: true,
            message: '调用成功',
            type: 'success'
          })
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    delPreset: function(preset) {
      this.$confirm('确定删除此预置位', '提示', {
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
        this.$store.dispatch('frontEnd/deletePreset', [this.deviceId, this.channelDeviceId, preset.presetId])
          .then(data => {
            setTimeout(() => {
              this.getPresetList()
            }, 1000)
          }).catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          }).finally(() => {
            loading.close()
          })
      }).catch(() => {

      })
    }

  }
}
</script>
