<template>
  <div style="padding-top: 10px">
    <a-row type="flex">
      <a-col :flex="1">
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
      </a-col>
    </a-row>
    <a-row type="flex" style="padding-top: 15px">
      <a-col :flex="1"></a-col>
      <a-col>
        <a-space>
          <a-button type="primary" :loading="loading" @click="onSubmit">提交</a-button>
          <a-button @click="$emit('prevStep', mediaServerConf)">上一步</a-button>
        </a-space>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import {addServer} from "@/api/mediaServer";

export default {
  name: "StepSubmitForm",
  props: ['mediaServerConf'],
  data() {
    return {
      loading: false
    }
  },
  computed: {
    formatConfig() {
      return [
        {label: '流媒体ID', 'value': this.mediaServerConf.id},
        {label: 'IP地址', 'value': this.mediaServerConf.ip},
        {label: 'HTTP端口', 'value': this.mediaServerConf.httpPort},
        {label: 'SECRET密钥', 'value': this.mediaServerConf.secret},
        {label: 'HOOK IP', 'value': this.mediaServerConf.hookIp},
        {label: 'SDP IP', 'value': this.mediaServerConf.sdpIp},
        {label: '流IP', 'value': this.mediaServerConf.streamIp},
        {label: 'HTTPS PORT', 'value': this.mediaServerConf.httpSSlPort},
        {label: 'RTSP PORT', 'value': this.mediaServerConf.rtspPort},
        {label: 'RTSPS PORT', 'value': this.mediaServerConf.rtspSSLPort},
        {label: 'RTMP PORT', 'value': this.mediaServerConf.rtmpPort},
        {label: 'RTMPS PORT', 'value': this.mediaServerConf.rtmpSSlPort},
        {label: '自动配置媒体服务', 'value': this.mediaServerConf.autoConfig},
        {label: '收流端口模式', 'value': this.mediaServerConf.rtpEnable},
        {label: '收流端口', 'value': this.mediaServerConf.rtpPortRange},
        {label: '推流端口', 'value': this.mediaServerConf.sendRtpPortRange},
        {label: '无人观看多久后停止拉流', 'value': this.mediaServerConf.streamNoneReaderDelayMS},
        {label: '录像管理服务端口', 'value': this.mediaServerConf.recordAssistPort}
      ]
    }
  },
  methods: {
    onSubmit() {
      this.loading = true
      addServer(this.mediaServerConf).then(data => {
        this.loading = false
        if (data.code === 0) {
          this.$message.success("保存成功");
          this.$emit('finish')
        } else {
          this.$message.error(data.msg);
        }
      }).catch(err => {
        this.$message.error(err);
      })
    }
  }
}
</script>

<style scoped>

</style>