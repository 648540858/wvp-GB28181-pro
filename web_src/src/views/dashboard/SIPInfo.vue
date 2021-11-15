<template>
  <a-card :bordered="false">
    <div slot="title">
      <span>信令服务器配置信息</span>
      <a-button type="primary" style="float: right" @click="goBack">
        <font-awesome-icon :icon="['fas', 'arrow-left']" style="margin-right: 0.25rem"/>
        返回
      </a-button>
    </div>
    <a-descriptions
      bordered
      :column="{ xxl: 2, xl: 2, lg: 2, md: 1, sm: 1, xs: 1 }"
    >
      <a-descriptions-item v-for="(value, key, index) in wvpServerConfig" :key="index" :label="key">
        {{ value }}
      </a-descriptions-item>
    </a-descriptions>
  </a-card>
</template>

<script>
export default {
  name: "SIPInfo",
  props: ['sipInfo'],
  methods: {
    goBack() {
      this.$emit('goBack')
    }
  },
  data() {
    return {
      wvpServerConfig: {}
    }
  },
  created() {
    let sipInfoArr = Object.keys(this.sipInfo)
    for (const sipInfoArrKey of sipInfoArr) {
      let tempObj = this.sipInfo[sipInfoArrKey]
      if (sipInfoArrKey === 'server.port') {
        this.wvpServerConfig['server.port'] = this.sipInfo['server.port']
      } else {
        let tempArr = Object.keys(tempObj);
        for (const tempArrKey of tempArr) {
          this.wvpServerConfig[tempArrKey] = tempObj[tempArrKey]
        }
      }
    }
    console.log(this.wvpServerConfig)
  }
}
</script>

<style scoped>

</style>