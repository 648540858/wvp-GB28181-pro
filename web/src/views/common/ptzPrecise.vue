<template>
  <div id="ptzPrecise">
    <div class="precise-title">精确控制</div>
    <div class="precise-row">
      <span class="precise-label">水平</span>
      <el-input-number size="mini" v-model="form.pan" :min="0" :max="3600" style="width: 100%" />
    </div>
    <div class="precise-row">
      <span class="precise-label">垂直</span>
      <el-input-number size="mini" v-model="form.tilt" :min="-1800" :max="1800" style="width: 100%" />
    </div>
    <div class="precise-row">
      <span class="precise-label">变倍</span>
      <el-input-number size="mini" v-model="form.zoom" :min="1" :max="128" style="width: 100%" />
    </div>
    <el-button size="mini" type="primary" style="width: 100%; margin-top: 4px" @click="emitPosition">定位</el-button>
  </div>
</template>

<script>
export default {
  name: 'PtzPrecise',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      form: { pan: 0, tilt: 0, zoom: 1 }
    }
  },
  methods: {
    emitPosition() {
      this.$emit('position', { ...this.form })
    }
  }
}
</script>

<style scoped>
#ptzPrecise {
  padding: 6px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
}
.precise-title {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
  font-weight: 500;
}
.precise-row {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.precise-label {
  font-size: 12px;
  color: #909399;
  width: 32px;
  text-align: left;
  flex-shrink: 0;
}
</style>
