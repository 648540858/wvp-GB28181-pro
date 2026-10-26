<template>
  <div class="restart-tag-toggle">
    <span class="toggle-label">显示重启标识</span>
    <el-switch :value="value" :disabled="disabled" @change="handleChange" />
  </div>
</template>

<script>
/**
 * 重启标识显隐开关
 * <p>
 * 放置在系统设置各配置面板标题栏最右侧，用于控制字段后“重启/即时”标签的显示。
 */
export default {
  name: 'RestartTagToggle',
  props: {
    // 是否显示重启标识
    value: {
      type: Boolean,
      default: false
    },
    // 后端未返回重启字段列表时置灰
    disabled: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    handleChange(val) {
      this.$emit('input', val)
    }
  }
}
</script>

<style scoped>
.restart-tag-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.toggle-label {
  font-size: 12px;
  color: #606266;
}
</style>
