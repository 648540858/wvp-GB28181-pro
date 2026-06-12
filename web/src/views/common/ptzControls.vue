<template>
  <div class="ptz-section-inner">
    <div class="ptz-top">
      <div v-if="hasPtzDirection" class="ptz-dpad">
        <div class="dpad-ring"></div>
        <button class="dpad-btn card card-up" @mousedown.prevent="handlePtzMove('up')" @mouseup.prevent="handlePtzStop()">▲</button>
        <button class="dpad-btn card card-right" @mousedown.prevent="handlePtzMove('right')" @mouseup.prevent="handlePtzStop()">▶</button>
        <button class="dpad-btn card card-down" @mousedown.prevent="handlePtzMove('down')" @mouseup.prevent="handlePtzStop()">▼</button>
        <button class="dpad-btn card card-left" @mousedown.prevent="handlePtzMove('left')" @mouseup.prevent="handlePtzStop()">◀</button>
        <button v-if="showDiagonals" class="dpad-btn diag diag-upright" @mousedown.prevent="handlePtzMove('upright')" @mouseup.prevent="handlePtzStop()"><span style="display:inline-block;transform:rotate(45deg)">▲</span></button>
        <button v-if="showDiagonals" class="dpad-btn diag diag-downright" @mousedown.prevent="handlePtzMove('downright')" @mouseup.prevent="handlePtzStop()"><span style="display:inline-block;transform:rotate(135deg)">▲</span></button>
        <button v-if="showDiagonals" class="dpad-btn diag diag-downleft" @mousedown.prevent="handlePtzMove('downleft')" @mouseup.prevent="handlePtzStop()"><span style="display:inline-block;transform:rotate(225deg)">▲</span></button>
        <button v-if="showDiagonals" class="dpad-btn diag diag-upleft" @mousedown.prevent="handlePtzMove('upleft')" @mouseup.prevent="handlePtzStop()"><span style="display:inline-block;transform:rotate(-45deg)">▲</span></button>
        <button class="dpad-btn dpad-center" title="停止" @click="$emit('ptz-stop')">⏹</button>
      </div>
      <div class="ptz-func-col">
        <div class="ptz-func-group" :class="{ row: btnLayout === 'row' }">
          <div class="ptz-func-row" v-if="homePosition && hasGuard">
            <div class="ptz-func-row">
              <div class="ptz-func-btn" title="看守位" @click.prevent="$emit('ptz-guard')">
                <i class="el-icon-s-home" /><span>看守位</span>
              </div>
            </div>
          </div>
          <div v-if="hasPtzDirection" class="ptz-func-row">
            <div class="ptz-func-btn" title="变倍+" @mousedown.prevent="handlePtzMove('zoomin')" @mouseup.prevent="handlePtzStop()">
              <i class="el-icon-zoom-in" /><span>变倍+</span>
            </div>
            <div class="ptz-func-btn" title="变倍-" @mousedown.prevent="handlePtzMove('zoomout')" @mouseup.prevent="handlePtzStop()">
              <i class="el-icon-zoom-out" /><span>变倍-</span>
            </div>
          </div>
          <div v-if="hasFocus" class="ptz-func-row">
            <div class="ptz-func-btn" title="聚焦+" @mousedown.prevent="$emit('focus-move', { command: 'near', speed: controSpeed })" @mouseup.prevent="$emit('focus-stop')">
              <i class="iconfont icon-bianjiao-fangda" /><span>聚焦+</span>
            </div>
            <div class="ptz-func-btn" title="聚焦-" @mousedown.prevent="$emit('focus-move', { command: 'far', speed: controSpeed })" @mouseup.prevent="$emit('focus-stop')">
              <i class="iconfont icon-bianjiao-suoxiao" /><span>聚焦-</span>
            </div>
          </div>
          <div v-if="hasIris" class="ptz-func-row">
            <div class="ptz-func-btn" title="光圈+" @mousedown.prevent="$emit('iris-move', { command: 'in', speed: controSpeed })" @mouseup.prevent="$emit('iris-stop')">
              <i class="iconfont icon-guangquan" /><span>光圈+</span>
            </div>
            <div class="ptz-func-btn" title="光圈-" @mousedown.prevent="$emit('iris-move', { command: 'out', speed: controSpeed })" @mouseup.prevent="$emit('iris-stop')">
              <i class="iconfont icon-guangquan-" /><span>光圈-</span>
            </div>
          </div>
          <div v-if="hasDragZoom" class="ptz-func-row">
            <div class="ptz-func-btn" title="拉框放大" @click="$emit('toggle-drag-zoom')">
              <i class="iconfont icon-guangquan" /><span>拉框放大</span>
            </div>
            <div class="ptz-func-btn" title="拉框缩小" @click="$emit('toggle-drag-zoom-out')">
              <i class="iconfont icon-guangquan-" /><span>拉框缩小</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="hasAnyPtz" class="ptz-bottom">
      <div class="slider-with-controls">
        <span class="slider-label">速度</span>
        <el-button type="text" icon="el-icon-minus" class="slider-btn" @click="adjustSpeed(-1)" />
        <el-slider v-model="controSpeed" :max="100" :min="1" />
        <el-button type="text" icon="el-icon-plus" class="slider-btn" @click="adjustSpeed(1)" />
        <span class="slider-value">{{ controSpeed }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PtzControls',
  props: {
    btnLayout: { type: String, default: 'column' },
    homePosition: { type: Boolean, default: false },
    showDiagonals: { type: Boolean, default: true }
  },
  computed: {
    hasPtzDirection() { return 'ptz-move' in this.$listeners },
    hasFocus() { return 'focus-move' in this.$listeners },
    hasIris() { return 'iris-move' in this.$listeners },
    hasDragZoom() { return 'toggle-drag-zoom' in this.$listeners || 'toggle-drag-zoom-out' in this.$listeners },
    hasGuard() { return 'ptz-guard' in this.$listeners },
    hasAnyPtz() { return this.hasPtzDirection || this.hasFocus || this.hasIris || this.hasDragZoom || this.hasGuard }
  },
  data() {
    return {
      controSpeed: 50,
      currentCommand: null
    }
  },
  mounted() {
    window.addEventListener('mouseup', this.onWindowMouseUp)
  },
  beforeDestroy() {
    window.removeEventListener('mouseup', this.onWindowMouseUp)
  },
  methods: {
    adjustSpeed(delta) {
      const newVal = this.controSpeed + delta
      if (newVal >= 1 && newVal <= 100) {
        this.controSpeed = newVal
      }
    },
    handlePtzMove(direction) {
      this.currentCommand = direction
      this.$emit('ptz-move', { direction, speed: this.controSpeed })
    },
    handlePtzStop() {
      this.$emit('ptz-stop', { direction: this.currentCommand })
      this.currentCommand = null
    },
    onWindowMouseUp() {
      if (this.currentCommand) {
        this.handlePtzStop()
      }
    }
  }
}
</script>

<style scoped>
.ptz-section-inner {
  display: flex;
  flex-direction: column;
  padding: 8px 4px;
  overflow-y: auto;
}
.ptz-top {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
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
.ptz-func-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
}
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
  font-size: 12px;
}
.ptz-func-btn:hover {
  background: #409EFF;
  color: #fff;
}
.ptz-func-btn:active {
  background: #337ecc;
}
.ptz-func-btn i { font-size: 14px; margin-bottom: 2px; }
.ptz-func-group.row .ptz-func-btn {
  flex-direction: row;
  gap: 4px;
}
.ptz-func-group.row .ptz-func-btn i {
  margin-bottom: 0;
  margin-right: 4px;
}
.ptz-bottom {
  margin-top: 12px;
  padding: 0 4px;
}
.slider-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.slider-btn {
  font-weight: bold;
  color: #1a1a1a;
}
.slider-with-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.slider-with-controls .el-slider {
  flex: 1;
}
.slider-value {
  min-width: 28px;
  text-align: center;
  font-size: 13px;
  color: #606266;
}
</style>
