<template>
  <a-card :bordered="false">
    <span slot="title">
      流媒体服务器详细信息
      <a-button type="primary" style="float: right" @click="$emit('goBack')">
        <font-awesome-icon :icon="['fas','arrow-left']" style="margin-right: 0.25rem"/>
        返回
      </a-button>
    </span>

    <a-descriptions bordered :column="{xxl: 3, xl: 2, lg: 2, md: 2, xs: 1}">
      <a-descriptions-item
        v-for="(item, index) in formatConfig"
        :key="index"
        :label="item.label">
        <a-tag v-if="!item.value" color="gold">
          未填写
        </a-tag>
        <span v-else>{{ item.value }}</span>
      </a-descriptions-item>
    </a-descriptions>

  </a-card>
</template>

<script>
export default {
  name: "MediaServerDetail",
  props: ['record'],
  computed: {
    formatConfig() {
      return [
        {label: '流媒体ID', 'value': this.record.id},
        {label: 'IP地址', 'value': this.record.ip},
        {label: 'HTTP端口', 'value': this.record.httpPort},
        {label: 'SECRET密钥', 'value': this.record.secret},
        {label: 'HOOK IP', 'value': this.record.hookIp},
        {label: 'SDP IP', 'value': this.record.sdpIp},
        {label: '流IP', 'value': this.record.streamIp},
        {label: 'HTTPS PORT', 'value': this.record.httpSSlPort},
        {label: 'RTSP PORT', 'value': this.record.rtspPort},
        {label: 'RTSPS PORT', 'value': this.record.rtspSSLPort},
        {label: 'RTMP PORT', 'value': this.record.rtmpPort},
        {label: 'RTMPS PORT', 'value': this.record.rtmpSSlPort},
        {label: '自动配置媒体服务', 'value': this.record.autoConfig},
        {label: '收流端口模式', 'value': this.record.rtpEnable},
        {label: '收流端口', 'value': this.record.rtpPortRange},
        {label: '推流端口', 'value': this.record.sendRtpPortRange},
        {label: '无人观看多久后停止拉流', 'value': this.record.streamNoneReaderDelayMS},
        {label: '录像管理服务端口', 'value': this.record.recordAssistPort}
      ]
    }
  }
}
</script>

<style scoped>

</style>