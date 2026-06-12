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
  name: 'ChPtzPreset',
  props: {
    channelId: { type: String, default: null }
  },
  data() {
    return {
      presetList: []
    }
  },
  created() {
    this.getPresetList()
  },
  methods: {
    getPresetList() {
      this.$store.dispatch('commonChanel/queryPreset', this.channelId)
        .then(data => {
          this.presetList = data
        })
    },
    gotoPreset(preset) {
      this.$store.dispatch('commonChanel/callPreset', { channelId: this.channelId, presetId: preset.presetId })
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
