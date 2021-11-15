<template>
  <a-row type="flex" justify="center">
    <a-col :span="10">
      <a-form-model ref="form1" :rules="rules" :model="mediaServerConf" :layout="form.layout" v-bind="formItemLayout">
        <a-form-model-item label="IP" prop="ip">
          <a-input v-model="mediaServerConf.ip" :disabled="true"/>
        </a-form-model-item>
        <a-form-model-item label="HTTP端口" prop="httpPort">
          <a-input v-model="mediaServerConf.httpPort" :disabled="true"/>
        </a-form-model-item>
        <a-form-model-item label="SECRET" prop="secret">
          <a-input v-model="mediaServerConf.secret" :disabled="true"/>
        </a-form-model-item>
        <a-form-model-item label="HOOK IP" prop="ip">
          <a-input v-model="mediaServerConf.hookIp" placeholder="媒体服务HOOK_IP"/>
        </a-form-model-item>
        <a-form-model-item label="SDP IP" prop="ip">
          <a-input v-model="mediaServerConf.sdpIp" placeholder="媒体服务SDP_IP"/>
        </a-form-model-item>
        <a-form-model-item label="流IP" prop="ip">
          <a-input v-model="mediaServerConf.streamIp" placeholder="媒体服务流IP"/>
        </a-form-model-item>
        <a-form-model-item label="HTTPS PORT" prop="httpSSlPort">
          <a-input-number v-model="mediaServerConf.httpSSlPort" placeholder="媒体服务HTTPS_PORT" style="width: 100%"/>
        </a-form-model-item>
        <a-form-model-item label="RTSP PORT" prop="rtspPort">
          <a-input-number v-model="mediaServerConf.rtspPort" placeholder="媒体服务RTSP_PORT" style="width: 100%"/>
        </a-form-model-item>
        <a-form-model-item label="RTSPS PORT" prop="rtspSSLPort">
          <a-input-number v-model="mediaServerConf.rtspSSLPort" placeholder="媒体服务RTSPS_PORT" style="width: 100%"/>
        </a-form-model-item>
      </a-form-model>
    </a-col>
    <a-col :span="10">
      <a-form-model ref="form2" :rules="rules" :model="mediaServerConf" :layout="form.layout" v-bind="formItemLayout">
        <a-form-model-item label="RTMP PORT" prop="rtmpPort">
          <a-input-number v-model="mediaServerConf.rtmpPort" placeholder="媒体服务RTMP_PORT" style="width: 100%"/>
        </a-form-model-item>
        <a-form-model-item label="RTMPS PORT" prop="rtmpSSlPort">
          <a-input-number v-model="mediaServerConf.rtmpSSlPort" placeholder="媒体服务RTMPS_PORT" style="width: 100%"/>
        </a-form-model-item>
        <a-form-model-item label="自动配置媒体服务">
          <a-switch checked-children="开" un-checked-children="关" v-model="mediaServerConf.autoConfig"/>
        </a-form-model-item>
        <a-form-model-item label="收流端口模式">
          <a-radio-group v-model="mediaServerConf.rtpEnable">
            <a-radio :value="true">
              多端口
            </a-radio>
            <a-radio :value="false">
              单端口
            </a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item v-if="!mediaServerConf.rtpEnable" label="收流端口" prop="rtpProxyPort">
          <a-input-number v-model="mediaServerConf.rtpProxyPort"/>
        </a-form-model-item>
        <a-form-model-item v-if="mediaServerConf.rtpEnable" label="收流端口">
          <a-input-number v-model="rtpPortRange1" placeholder="起始" @change="portRangeChange" style="width: 100px"
                   prop="rtpPortRange1"/>
          -
          <a-input-number v-model="rtpPortRange2" placeholder="终止" @change="portRangeChange" style="width: 100px"
                   prop="rtpPortRange2"/>
        </a-form-model-item>
        <a-form-model-item label="推流端口" prop="[sendRtpPortRange1,sendRtpPortRange2]">
          <a-input-number v-model="sendRtpPortRange1" placeholder="起始" @change="portRangeChange" style="width: 100px"
                   prop="sendRtpPortRange1"/>
          -
          <a-input-number v-model="sendRtpPortRange2" placeholder="终止" @change="portRangeChange" style="width: 100px"
                   prop="sendRtpPortRange2"/>
        </a-form-model-item>
        <a-form-model-item label="无人观看多久后停止拉流">
          <a-input-number v-model="mediaServerConf.streamNoneReaderDelayMS"/>
        </a-form-model-item>
        <a-form-model-item label="录像管理服务端口" prop="recordAssistPort">
          <a-input-search v-model="mediaServerConf.recordAssistPort" enter-button="检测端口" @search="checkRecordServer"/>
          <a-icon v-if="recordServerCheck === 1" type="check-circle"
                  style="color: #3caf36; position: absolute;top: 5px; right: -20px"/>
          <a-icon v-if="recordServerCheck === 2" type="loading"
                  style="color: #3caf36; position: absolute;top: 5px; right: -20px"/>
          <a-icon v-if="recordServerCheck === -1" type="close-circle"
                  style="color: #c80000; position: absolute;top: 5px; right: -20px"/>
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ offset: 8 }">
          <a-button type="primary" @click="prevStep">
            上一步
          </a-button>
          <a-button style="margin-left: 10px;" @click="nextStep">
            下一步
          </a-button>
        </a-form-model-item>
      </a-form-model>
    </a-col>
  </a-row>
</template>

<script>
import {checkRecordServer} from "@/api/mediaServer";

export default {
  name: "StepDetailForm",
  props: ['mediaServerConf'],
  computed: {
    formItemLayout() {
      const {layout} = this.form;
      return layout === 'horizontal'
        ? {
          labelCol: {
            xs: {span: 8},
            sm: {span: 8}
          },
          wrapperCol: {
            xs: {span: 12},
            sm: {span: 12}
          }
        }
        : {};
    }
  },
  data() {
    let validatePort = (rule, value, callback) => { // 校验port是否符合规则
      let reg = /^(([0-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-5]{2}[0-3][0-5]))$/
      if (!reg.test(value)) {
        return callback(new Error('请输入有效的端口号'))
      } else {
        callback()
      }
      return true
    }
    return {
      form: {
        layout: 'horizontal'
      },
      rtpPortRange1: 30000,
      rtpPortRange2: 30500,
      sendRtpPortRange1: 30000,
      sendRtpPortRange2: 30500,
      recordServerCheck: 0,
      rules: {
        httpSSlPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        recordAssistPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        rtmpPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        rtmpSSlPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        rtpPortRange1: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        rtpPortRange2: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        sendRtpPortRange1: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        sendRtpPortRange2: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        rtpProxyPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        rtspPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        rtspSSLPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
        secret: [{required: true, message: "请输入secret", trigger: "blur"}],
        timeout_ms: [{required: true, message: "请输入FFmpeg推流成功超时时间", trigger: "blur"}],
        ffmpeg_cmd_key: [{required: false, message: "请输入FFmpeg命令参数模板（可选）", trigger: "blur"}]
      },
      form1Valid: false,
      form2Valid: false
    }
  },
  methods: {
    nextStep() {
      this.$refs.form1.validate(valid => {
        this.form1Valid = valid
        this.readyNextStep()
      })
      this.$refs.form2.validate(valid => {
        this.form2Valid = valid
        this.readyNextStep()
      })
    },
    readyNextStep() {
      if (this.form1Valid && this.form2Valid) {
        this.$emit('nextStep', this.mediaServerConf)
        this.form1Valid = false
        this.form2Valid = false
      }
    },
    prevStep() {
      this.$emit('prevStep', this.mediaServerConf)
    },
    portRangeChange() {
      this.mediaServerConf.sendRtpPortRange = this.sendRtpPortRange1 + "," + this.sendRtpPortRange2
      this.mediaServerConf.rtpPortRange = this.rtpPortRange1 + "," + this.rtpPortRange2
    },
    checkRecordServer() {
      let reg = /^(([0-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-5]{2}[0-3][0-5]))$/
      if (!reg.test(this.mediaServerConf.recordAssistPort)) {
        this.recordServerCheck = -1
        return
      }
      this.recordServerCheck = 2
      checkRecordServer({ip: this.mediaServerConf.ip, port: this.mediaServerConf.recordAssistPort}).then(data => {
        if (data.code === 0) {
          this.recordServerCheck = 1
        } else {
          this.recordServerCheck = -1
          this.$message.error(data.msg)
        }
      }).catch(err => {
        console.log(err)
      })
    }
  }
}
</script>

<style scoped>

</style>