<template>
  <div>
    <a-card :bordered="false">
      <div slot="title">
        添加流媒体节点
        <a-button type="primary" @click="$emit('goBack')" style="float: right">返回</a-button>
      </div>
      <a-steps class="steps" :current="currentTab">
        <a-step title="基本信息"/>
        <a-step title="完善信息"/>
        <a-step title="预览提交"/>
      </a-steps>

      <div class="content">
        <step-base-form v-if="currentTab === 0" @nextStep="nextStep" :mediaServerConf="mediaServerConf"/>
        <step-detail-form v-if="currentTab === 1" @nextStep="nextStep" @prevStep="prevStep"
                          :mediaServerConf="mediaServerConf"/>
        <step-submit-form v-if="currentTab === 2" @prevStep="prevStep" @finish="finish"
                          :mediaServerConf="mediaServerConf"/>
      </div>

      <a-divider/>
      <div class="step-form-style-desc">
        <h3>说明</h3>
        <h4>基本信息</h4>
        <p>基本信息，包括流媒体节点的IP地址，HTTP端口号以及SECRET密码，请正确填写，填写完后点击测试，
          系统会检测当前地址端口的流媒体服务器是否在线，并且是否能够获取到必要信息，检测通过，则可以继续进行下一步操作。
        </p>
        <h4>完善信息</h4>
        <p>完善信息步骤中，可以根据自己的需求，进行详细的参数设置。</p>
        <h4>预览提交</h4>
        <p>前置步骤操作完成，在这里可以进行整个配置的预览确认，无误后点击提交，完成流媒体服务器节点添加配置。</p>
      </div>
    </a-card>
  </div>
</template>

<script>
import StepBaseForm from "@/views/mediaServer/stepForm/StepBaseForm";
import StepDetailForm from "@/views/mediaServer/stepForm/StepDetailForm";
import StepSubmitForm from "@/views/mediaServer/stepForm/StepSubmitForm";

export default {
  components: {
    StepBaseForm,
    StepDetailForm,
    StepSubmitForm
  },
  data() {
    return {
      currentTab: 0,
      mediaServerConf: {
        id: "",
        ip: "",
        autoConfig: true,
        hookIp: "",
        sdpIp: "",
        streamIp: "",
        streamNoneReaderDelayMS: "",
        secret: "035c73f7-bb6b-4889-a715-d9eb2d1925cc",
        httpPort: "",
        httpSSlPort: "",
        recordAssistPort: "",
        rtmpPort: "",
        rtmpSSlPort: "",
        rtpEnable: false,
        rtpPortRange: "",
        sendRtpPortRange: "",
        rtpProxyPort: "",
        rtspPort: "",
        rtspSSLPort: ""
      }
    }
  },
  created() {

  },
  methods: {
    nextStep(mediaServerConf) {
      this.mediaServerConf = mediaServerConf
      if (this.currentTab < 2) {
        this.currentTab += 1
      }
    },
    prevStep(mediaServerConf) {
      this.mediaServerConf = mediaServerConf
      if (this.currentTab > 0) {
        this.currentTab -= 1
      }
    },
    finish() {
      this.$emit('goBack')
    }
  },
}
</script>

<style lang="less" scoped>
.steps {
  max-width: 750px;
  margin: 16px auto;
}

.step-form-style-desc {
  padding: 0 56px;
  color: rgba(0, 0, 0, .45);

  h3 {
    margin: 0 0 12px;
    color: rgba(0, 0, 0, .45);
    font-size: 16px;
    line-height: 32px;
  }

  h4 {
    margin: 0 0 4px;
    color: rgba(0, 0, 0, .45);
    font-size: 14px;
    line-height: 22px;
  }

  p {
    margin-top: 0;
    margin-bottom: 12px;
    line-height: 22px;
  }

}
</style>