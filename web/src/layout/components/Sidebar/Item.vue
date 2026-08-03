<template>
  <span>
    <ant-icon v-if="icon && icon.includes('el-icon')" :name="icon" :class="[icon, 'sub-el-icon']" />
    <svg-icon v-else-if="icon" :icon-class="icon" />
    <span v-if="title">{{ title }}</span>
  </span>
</template>

<script>
export default {
  name: 'MenuItem',
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.sub-el-icon {
  color: currentColor;
  width: 1em;
  height: 1em;
}
</style>
