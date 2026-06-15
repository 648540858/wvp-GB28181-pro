<template>
  <div class="live-ptz-preset">
    <div v-for="item in presetList" :key="item.presetId" class="preset-item" @click="gotoPreset(item)">
      <span class="preset-idx">{{ item.presetId }}</span>
      <span class="preset-name">{{ item.presetName || '预置位 ' + item.presetId }}</span>
    </div>
    <div v-if="!presetList.length" class="preset-empty">暂无预置位</div>
  </div>
</template>

<script>
export default {
  name: 'LivePtzPreset',
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
    }
  }
}
</script>

<style scoped>
.live-ptz-preset {
  width: 100%;
}
.preset-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}
.preset-item:hover {
  background: #ecf5ff;
}
.preset-item:active {
  background: #d9ecff;
}
.preset-idx {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e6f7ff;
  color: #1890ff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.preset-name {
  font-size: 13px;
  color: #303133;
}
.preset-empty {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 20px 0;
}
</style>
