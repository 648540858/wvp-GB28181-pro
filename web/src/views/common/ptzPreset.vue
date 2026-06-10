<template>
  <div id="ptzPreset" style="width: 100%">
    <el-tag
      v-for="item in presetList"
      :key="item.presetId"
      size="mini"
      style="margin-right: 1rem; cursor: pointer; margin-bottom: 0.6rem"
      @click="gotoPreset(item)"
    >
      {{ item.presetName || item.presetId }}
    </el-tag>
  </div>
</template>

<script>
export default {
  name: 'PtzPreset',
  props: ['channelDeviceId', 'deviceId'],
  data() {
    return {
      presetList: []
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
        })
    },
    gotoPreset: function(preset) {
      this.$store.dispatch('frontEnd/callPreset', [this.deviceId, this.channelDeviceId, preset.presetId])
        .then(() => {
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
    }
  }
}
</script>
