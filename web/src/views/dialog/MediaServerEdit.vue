<template>
  <div id="mediaServerEdit" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="媒体节点"
      :width="dialogWidth"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="formStep" style="margin-top: 1rem; margin-right: 20px;">
        <el-form v-if="currentStep === 1" ref="mediaServerForm" :rules="rules" :model="mediaServerForm" label-width="140px">
          <el-form-item label="IP" prop="ip">
            <el-input v-model="mediaServerForm.ip" placeholder="媒体服务IP" clearable :disabled="mediaServerForm.defaultServer" />
          </el-form-item>
          <el-form-item label="HTTP端口" prop="httpPort">
            <el-input v-model="mediaServerForm.httpPort" placeholder="媒体服务HTTP端口" clearable :disabled="mediaServerForm.defaultServer" />
          </el-form-item>
          <el-form-item label="SECRET" prop="secret">
            <el-input v-model="mediaServerForm.secret" placeholder="媒体服务SECRET" clearable :disabled="mediaServerForm.defaultServer" />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="mediaServerForm.type" style="float: left; width: 100%">
              <el-option key="zlm" label="ZLMediaKit" value="zlm" />
              <el-option key="abl" label="ABLMediaServer" value="abl" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button v-if="currentStep === 1 && serverCheck === 1" type="primary" @click="next">下一步</el-button>
              <el-button @click="close">取消</el-button>
              <el-button type="primary" @click="checkServer">测试</el-button>
              <i v-if="serverCheck === 1" class="el-icon-success" style="color: #3caf36" />
              <i v-if="serverCheck === -1" class="el-icon-error" style="color: #c80000" />
            </div>
          </el-form-item>
        </el-form>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form v-if="currentStep === 2 || currentStep === 3" ref="mediaServerForm1" :rules="rules" :model="mediaServerForm" label-width="140px">
              <el-form-item label="IP" prop="ip">
                <el-input v-if="currentStep === 2" v-model="mediaServerForm.ip" :disabled="mediaServerForm.defaultServer" />
                <el-input v-if="currentStep === 3" v-model="mediaServerForm.ip" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="HTTP端口" prop="httpPort">
                <el-input v-if="currentStep === 2" v-model="mediaServerForm.httpPort" :disabled="mediaServerForm.defaultServer" />
                <el-input v-if="currentStep === 3" v-model="mediaServerForm.httpPort" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="HOOK IP" prop="ip">
                <el-input v-model="mediaServerForm.hookIp" placeholder="媒体服务HOOK_IP" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="SDP IP" prop="ip">
                <el-input v-model="mediaServerForm.sdpIp" placeholder="媒体服务SDP_IP" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="流IP" prop="ip">
                <el-input v-model="mediaServerForm.streamIp" placeholder="媒体服务流IP" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="HTTPS PORT" prop="httpSSlPort">
                <el-input v-model="mediaServerForm.httpSSlPort" placeholder="媒体服务HTTPS_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="RTSP PORT" prop="rtspPort">
                <el-input v-model="mediaServerForm.rtspPort" placeholder="媒体服务RTSP_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="RTSPS PORT" prop="rtspSSLPort">
                <el-input v-model="mediaServerForm.rtspSSLPort" placeholder="媒体服务RTSPS_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>

            </el-form>
          </el-col>
          <el-col :span="12">
            <el-form v-if="currentStep === 2 || currentStep === 3" ref="mediaServerForm2" :rules="rules" :model="mediaServerForm" label-width="180px">
              <el-form-item label="RTMP PORT" prop="rtmpPort">
                <el-input v-model="mediaServerForm.rtmpPort" placeholder="媒体服务RTMP_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="RTMPS PORT" prop="rtmpSSlPort">
                <el-input v-model="mediaServerForm.rtmpSSlPort" placeholder="媒体服务RTMPS_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="SECRET" prop="secret">
                <el-input v-if="currentStep === 2" v-model="mediaServerForm.secret" :disabled="mediaServerForm.defaultServer" />
                <el-input v-if="currentStep === 3" v-model="mediaServerForm.secret" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="自动配置媒体服务">
                <el-switch v-model="mediaServerForm.autoConfig" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="收流端口模式">
                <el-switch v-model="mediaServerForm.rtpEnable" active-text="多端口" inactive-text="单端口" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
              </el-form-item>

              <el-form-item v-if="!mediaServerForm.rtpEnable" label="收流端口" prop="rtpProxyPort">
                <el-input v-model.number="mediaServerForm.rtpProxyPort" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item v-if="mediaServerForm.rtpEnable" label="收流端口">
                <el-input v-model="rtpPortRange1" placeholder="起始" clearable style="width: 100px" prop="rtpPortRange1" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
                -
                <el-input v-model="rtpPortRange2" placeholder="终止" clearable style="width: 100px" prop="rtpPortRange2" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
              </el-form-item>
              <el-form-item v-if="mediaServerForm.sendRtpEnable" label="发流端口">
                <el-input v-model="sendRtpPortRange1" placeholder="起始" clearable style="width: 100px" prop="rtpPortRange1" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
                -
                <el-input v-model="sendRtpPortRange2" placeholder="终止" clearable style="width: 100px" prop="rtpPortRange2" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
              </el-form-item>
              <el-form-item>
                <div style="float: right;">
                  <el-button v-if="!mediaServerForm.defaultServer" type="primary" @click="onSubmit">提交</el-button>
                  <el-button v-if="!mediaServerForm.defaultServer" @click="close">取消</el-button>
                  <el-button v-if="mediaServerForm.defaultServer" @click="close">关闭</el-button>
                </div>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>

      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'MediaServerEdit',
  directives: { elDragDialog },
  props: {},
  data() {
    const isValidIp = (rule, value, callback) => { // 校验IP是否符合规则
      var reg = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
      console.log(this.mediaServerForm.ip)
      if (!reg.test(this.mediaServerForm.ip)) {
        return callback(new Error('请输入有效的IP地址'))
      } else {
        callback()
      }
      return true
    }
    const isValidPort = (rule, value, callback) => { // 校验IP是否符合规则
      var reg = /^(([0-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-5]{2}[0-3][0-5]))$/
      if (!reg.test(this.mediaServerForm.httpPort)) {
        return callback(new Error('请输入有效的端口号'))
      } else {
        callback()
      }
      return true
    }
    return {
      dialogWidth: 0,
      defaultWidth: 1000,
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      dialogLoading: false,

      currentStep: 1,
      platformList: [],
      serverCheck: 0,
      recordServerCheck: 0,
      mediaServerForm: {
        id: '',
        ip: '',
        autoConfig: true,
        hookIp: '',
        sdpIp: '',
        streamIp: '',
        secret: '',
        httpPort: '',
        httpSSlPort: '',
        recordAssistPort: '',
        rtmpPort: '',
        rtmpSSlPort: '',
        rtpEnable: false,
        rtpPortRange: '',
        sendRtpPortRange: '',
        rtpProxyPort: '',
        rtspPort: '',
        rtspSSLPort: '',
        type: 'zlm'
      },
      rtpPortRange1: 30000,
      rtpPortRange2: 30500,

      sendRtpPortRange1: 50000,
      sendRtpPortRange2: 60000,

      rules: {
        ip: [{ required: true, validator: isValidIp, message: '请输入有效的IP地址', trigger: 'blur' }],
        httpPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        httpSSlPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        recordAssistPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        rtmpPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        rtmpSSlPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        rtpPortRange1: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        rtpPortRange2: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        rtpProxyPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        rtspPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        rtspSSLPort: [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        secret: [{ required: true, message: '请输入secret', trigger: 'blur' }],
        timeout_ms: [{ required: true, message: '请输入FFmpeg推流成功超时时间', trigger: 'blur' }],
        ffmpeg_cmd_key: [{ required: false, message: '请输入FFmpeg命令参数模板（可选）', trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {
    this.setDialogWidth()
  },
  methods: {
    setDialogWidth() {
      const val = document.body.clientWidth
      if (val < this.defaultWidth) {
        this.dialogWidth = '100%'
      } else {
        this.dialogWidth = this.defaultWidth + 'px'
      }
    },
    openDialog: function(param, callback) {
      this.showDialog = true
      this.listChangeCallback = callback
      if (param != null) {
        this.mediaServerForm = param
        this.currentStep = 3
        if (param.rtpPortRange) {
          const rtpPortRange = this.mediaServerForm.rtpPortRange.split(',')
          const sendRtpPortRange = this.mediaServerForm.sendRtpPortRange.split(',')
          if (rtpPortRange.length > 0) {
            this.rtpPortRange1 = rtpPortRange[0]
            this.rtpPortRange2 = rtpPortRange[1]
          }
          if (sendRtpPortRange.length > 0) {
            this.sendRtpPortRange1 = sendRtpPortRange[0]
            this.sendRtpPortRange2 = sendRtpPortRange[1]
          }
        }
      }
    },
    checkServer: function() {
      this.serverCheck = 0
      this.$store.dispatch('server/checkMediaServer', this.mediaServerForm)
        .then(data => {
          if (parseInt(this.mediaServerForm.httpPort) !== parseInt(data.httpPort)) {
            this.$message({
              showClose: true,
              message: '如果你正在使用docker部署你的媒体服务，请注意的端口映射。',
              type: 'warning',
              duration: 0
            })
          }
          const httpPort = this.mediaServerForm.httpPort
          this.mediaServerForm = data
          this.mediaServerForm.httpPort = httpPort
          this.mediaServerForm.autoConfig = true
          this.rtpPortRange1 = 30000
          this.rtpPortRange2 = 30500
          this.sendRtpPortRange1 = 50000
          this.sendRtpPortRange2 = 60000
          this.serverCheck = 1
        })
    },
    next: function() {
      this.currentStep = 2
      this.defaultWidth = 900
      this.setDialogWidth()
    },
    onSubmit: function() {
      this.dialogLoading = true
      this.$store.dispatch('server/saveMediaServer', this.mediaServerForm)
        .then(data => {
          this.$message({
            showClose: true,
            message: '保存成功',
            type: 'success'
          })
          if (this.listChangeCallback) this.listChangeCallback()
          this.close()
        })
    },
    close: function() {
      this.showDialog = false
      this.dialogLoading = false
      this.mediaServerForm = {
        id: '',
        ip: '',
        autoConfig: true,
        hookIp: '',
        sdpIp: '',
        streamIp: '',
        secret: '',
        httpPort: '',
        httpSSlPort: '',
        recordAssistPort: '',
        rtmpPort: '',
        rtmpSSlPort: '',
        rtpEnable: false,
        rtpPortRange: '',
        sendRtpPortRange: '',
        rtpProxyPort: '',
        rtspPort: '',
        rtspSSLPort: ''
      }
      this.rtpPortRange1 = 30500
      this.rtpPortRange2 = 30500
      this.sendRtpPortRange1 = 50000
      this.sendRtpPortRange2 = 60000
      this.listChangeCallback = null
      this.currentStep = 1
    },
    portRangeChange: function() {
      if (this.mediaServerForm.rtpEnable) {
        this.mediaServerForm.rtpPortRange = this.rtpPortRange1 + ',' + this.rtpPortRange2
        this.mediaServerForm.sendRtpPortRange = this.sendRtpPortRange1 + ',' + this.sendRtpPortRange2
      }
    }
  }
}
</script>
