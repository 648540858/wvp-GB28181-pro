<template>
  <a-card :bordered="false">
    <div slot="title">
      <a-button type="primary" style="float: right" @click="goBack">
        <font-awesome-icon :icon="['fas','arrow-left']" style="margin-right: 0.25rem"/>
        返回
      </a-button>
      媒体服务器配置信息
    </div>
    <a-descriptions
      bordered
      :column="{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }"
    >
      <a-descriptions-item v-for="(value, key, index) in mediaInfo" :key="index" :label="key">
        {{ value }}
      </a-descriptions-item>
    </a-descriptions>
  </a-card>
</template>

<script>
export default {
  name: "MediaInfo",
  props: ['mediaInfo'],
  methods: {
    goBack(){
      this.$emit('goBack')
    }
  }
}
</script>

<style scoped>

</style>