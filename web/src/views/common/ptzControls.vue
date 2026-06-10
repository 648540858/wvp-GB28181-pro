<template>
  <div class="ptz-section-inner">
    <div class="ptz-left">
      <div class="ptz-dpad">
        <div class="dpad-ring"></div>
        <button class="dpad-btn card card-up" @mousedown.prevent="$emit('ptz-move', { direction: 'up', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')">▲</button>
        <button class="dpad-btn card card-right" @mousedown.prevent="$emit('ptz-move', { direction: 'right', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')">▶</button>
        <button class="dpad-btn card card-down" @mousedown.prevent="$emit('ptz-move', { direction: 'down', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')">▼</button>
        <button class="dpad-btn card card-left" @mousedown.prevent="$emit('ptz-move', { direction: 'left', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')">◀</button>
        <button class="dpad-btn diag diag-upright" @mousedown.prevent="$emit('ptz-move', { direction: 'upright', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')"><span style="display:inline-block;transform:rotate(45deg)">▲</span></button>
        <button class="dpad-btn diag diag-downright" @mousedown.prevent="$emit('ptz-move', { direction: 'downright', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')"><span style="display:inline-block;transform:rotate(135deg)">▲</span></button>
        <button class="dpad-btn diag diag-downleft" @mousedown.prevent="$emit('ptz-move', { direction: 'downleft', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')"><span style="display:inline-block;transform:rotate(225deg)">▲</span></button>
        <button class="dpad-btn diag diag-upleft" @mousedown.prevent="$emit('ptz-move', { direction: 'upleft', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')"><span style="display:inline-block;transform:rotate(-45deg)">▲</span></button>
        <button class="dpad-btn dpad-center" title="停止" @click="$emit('ptz-stop')">⏹</button>
      </div>
      <div class="ptz-speed-slider">
        <span class="ptz-speed-label">速度</span>
        <el-slider v-model="controSpeed" :max="8" :min="1" style="flex: 1" />
      </div>
    </div>
    <div class="ptz-right">
      <div class="ptz-func-group">
        <div class="ptz-func-row">
          <div class="ptz-func-btn" title="变倍+" @mousedown.prevent="$emit('ptz-move', { direction: 'zoomin', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')">
            <i class="el-icon-zoom-in" /><span>变倍+</span>
          </div>
          <div class="ptz-func-btn" title="变倍-" @mousedown.prevent="$emit('ptz-move', { direction: 'zoomout', speed: controSpeed })" @mouseup.prevent="$emit('ptz-stop')">
            <i class="el-icon-zoom-out" /><span>变倍-</span>
          </div>
        </div>
        <div class="ptz-func-row">
          <div class="ptz-func-btn" title="聚焦+" @mousedown.prevent="$emit('focus-move', { command: 'near', speed: controSpeed })" @mouseup.prevent="$emit('focus-stop')">
            <i class="iconfont icon-bianjiao-fangda" /><span>聚焦+</span>
          </div>
          <div class="ptz-func-btn" title="聚焦-" @mousedown.prevent="$emit('focus-move', { command: 'far', speed: controSpeed })" @mouseup.prevent="$emit('focus-stop')">
            <i class="iconfont icon-bianjiao-suoxiao" /><span>聚焦-</span>
          </div>
        </div>
        <div class="ptz-func-row">
          <div class="ptz-func-btn" title="光圈+" @mousedown.prevent="$emit('iris-move', { command: 'in', speed: controSpeed })" @mouseup.prevent="$emit('iris-stop')">
            <i class="iconfont icon-guangquan" /><span>光圈+</span>
          </div>
          <div class="ptz-func-btn" title="光圈-" @mousedown.prevent="$emit('iris-move', { command: 'out', speed: controSpeed })" @mouseup.prevent="$emit('iris-stop')">
            <i class="iconfont icon-guangquan-" /><span>光圈-</span>
          </div>
        </div>
      </div>
      <ptzPrecise v-if="showPrecise" :device-id="deviceId" :channel-device-id="channelId" @position="$emit('precise-position', $event)" style="margin-top: 6px" />
    </div>
  </div>
</template>

<script>
import ptzPrecise from './ptzPrecise.vue'

export default {
  name: 'PtzControls',
  components: { ptzPrecise },
  props: {
    deviceId: { type: String, default: null },
    channelId: { type: String, default: null },
    showPrecise: { type: Boolean, default: true }
  },
  data() {
    return {
      controSpeed: 5
    }
  },
  mounted() {
    window.addEventListener('mouseup', this.onWindowMouseUp)
  },
  beforeDestroy() {
    window.removeEventListener('mouseup', this.onWindowMouseUp)
  },
  methods: {
    onWindowMouseUp() {
      this.$emit('ptz-stop')
    }
  }
}
</script>

<style scoped>
.ptz-section-inner {
  display: flex;
  padding: 8px 4px;
  overflow-y: auto;
}
.ptz-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 12px;
}
.ptz-dpad {
  position: relative;
  width: 180px;
  height: 180px;
  flex: none;
}
.dpad-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 130px;
  height: 130px;
  border-radius: 50%;
  background: #f5f7fa;
  pointer-events: none;
}
.dpad-btn {
  position: absolute;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: transparent;
  border: none;
  outline: none;
  padding: 0;
  user-select: none;
  transition: all 0.15s;
  -webkit-tap-highlight-color: transparent;
}
.card {
  width: 46px;
  height: 46px;
  font-size: 18px;
  color: #303133;
}
.card:hover {
  background: #409EFF;
  color: #fff;
  box-shadow: 0 3px 10px rgba(64,158,255,0.4);
  transform: scale(1.1);
}
.card:active {
  background: #337ecc;
  transform: scale(0.92);
}
.card-up { top: 18px; left: 67px; }
.card-right { top: 67px; left: 116px; }
.card-down { top: 116px; left: 67px; }
.card-left { top: 67px; left: 18px; }
.diag {
  width: 36px;
  height: 36px;
  font-size: 14px;
  color: #a8abb2;
}
.diag:hover {
  background: #409EFF;
  color: #fff;
  box-shadow: 0 2px 8px rgba(64,158,255,0.35);
  transform: scale(1.1);
}
.diag:active {
  background: #337ecc;
  transform: scale(0.9);
}
.diag-upright { top: 40px; left: 110px; }
.diag-downright { top: 110px; left: 110px; }
.diag-downleft { top: 110px; left: 34px; }
.diag-upleft { top: 40px; left: 34px; }
.dpad-center {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #eef0f4, #e0e3e8);
  font-size: 20px;
  color: #909399;
  line-height: 1;
}
.dpad-center:hover {
  background: #409EFF;
  color: #fff;
  box-shadow: 0 3px 10px rgba(64,158,255,0.4);
  transform: translate(-50%, -50%) scale(1.1);
}
.dpad-center:active {
  background: #337ecc;
  transform: translate(-50%, -50%) scale(0.92);
}
.ptz-speed-slider {
  display: flex;
  align-items: center;
  width: 120px;
  margin-top: 8px;
}
.ptz-speed-label {
  font-size: 12px;
  color: #606266;
  margin-right: 6px;
  white-space: nowrap;
}
.ptz-right { flex: 1; }
.ptz-func-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.ptz-func-row {
  display: flex;
  gap: 4px;
  width: 100%;
}
.ptz-func-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 44px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  background: #fff;
  user-select: none;
  font-size: 11px;
}
.ptz-func-btn:hover {
  background: #409EFF;
  color: #fff;
}
.ptz-func-btn:active {
  background: #337ecc;
}
.ptz-func-btn i { font-size: 14px; margin-bottom: 2px; }
</style>
