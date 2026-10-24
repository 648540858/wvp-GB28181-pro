<template>
  <span class="restart-field-label">
    <span>{{ text }}</span>
    <el-tag
      v-if="show"
      size="mini"
      class="restart-tag"
      :type="required ? 'warning' : 'success'"
    >
      {{ required ? '重启' : '即时' }}
    </el-tag>
  </span>
</template>

<script>
/**
 * 配置项标签
 * <p>
 * 与「全局策略」的展示保持一致: 字段名后跟随“重启/即时”标签,
 * 用于标识该字段保存后是否需要重启服务才能生效。
 */
export default {
  name: 'RestartFieldLabel',
  props: {
    // 字段名称
    text: {
      type: String,
      required: true
    },
    // 保存后是否需要重启服务才能生效
    required: {
      type: Boolean,
      default: true
    },
    // 后端未返回重启字段列表时不展示标签, 与全局策略的降级行为一致
    show: {
      type: Boolean,
      default: true
    }
  }
}
</script>

<style scoped>
.restart-tag {
  margin-left: 6px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
  font-size: 10px;
  border-radius: 3px;
}
</style>
