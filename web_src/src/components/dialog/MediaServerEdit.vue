<template>
  <div id="mediaServerEdit" v-loading="isLoging">
    <el-dialog
      title="媒体节点"
      :width="dialogWidth"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="formStep" style="margin-top: 1rem; margin-right: 20px;">
        <el-form v-if="currentStep == 1" ref="mediaServerForm" :rules="rules" :model="mediaServerForm" label-width="140px" >
          <el-form-item label="IP" prop="ip">
            <el-input v-model="mediaServerForm.ip"  placeholder="媒体服务IP" clearable></el-input>
          </el-form-item>
          <el-form-item label="HTTP端口" prop="port">
            <el-input v-model="mediaServerForm.httpPort" placeholder="媒体服务HTTP端口"  clearable></el-input>
          </el-form-item>
          <el-form-item label="SECRET" prop="secret">
            <el-input v-model="mediaServerForm.secret" placeholder="媒体服务SECRET"  clearable></el-input>
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" v-if="currentStep === 1 && serverCheck === 1" @click="next" >下一步</el-button>
              <el-button @click="close">取消</el-button>
              <el-button type="primary" @click="checkServer" >测试</el-button>
              <i v-if="serverCheck === 1" class="el-icon-success" style="color: #3caf36"></i>
              <i v-if="serverCheck === -1" class="el-icon-error" style="color: #c80000"></i>
            </div>
          </el-form-item>
        </el-form>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form v-if="currentStep === 2 || currentStep === 3" ref="mediaServerForm1" :rules="rules" :model="mediaServerForm" label-width="140px" >
              <el-form-item label="IP" prop="ip">
                <el-input  v-if="currentStep === 2" v-model="mediaServerForm.ip" disabled></el-input>
                <el-input  v-if="currentStep === 3"  v-model="mediaServerForm.ip"></el-input>
              </el-form-item>
              <el-form-item label="HTTP端口" prop="port">
                <el-input  v-if="currentStep === 2"  v-model="mediaServerForm.httpPort" disabled></el-input>
                <el-input  v-if="currentStep === 3"  v-model="mediaServerForm.httpPort"></el-input>
              </el-form-item>
              <el-form-item label="SECRET" prop="secret">
                <el-input v-if="currentStep === 2"  v-model="mediaServerForm.secret" disabled></el-input>
                <el-input v-if="currentStep === 3"  v-model="mediaServerForm.secret"></el-input>
              </el-form-item>
              <el-form-item label="HOOK IP" prop="ip">
                <el-input v-model="mediaServerForm.hookIp" placeholder="媒体服务HOOK_IP" clearable></el-input>
              </el-form-item>
              <el-form-item label="SDP IP" prop="ip">
                <el-input v-model="mediaServerForm.sdpIp" placeholder="媒体服务SDP_IP" clearable></el-input>
              </el-form-item>
              <el-form-item label="流IP" prop="ip">
                <el-input v-model="mediaServerForm.streamIp" placeholder="媒体服务流IP" clearable></el-input>
              </el-form-item>
              <el-form-item label="HTTPS PORT" prop="port">
                <el-input v-model="mediaServerForm.httpSSlPort" placeholder="媒体服务HTTPS_PORT" clearable></el-input>
              </el-form-item>
              <el-form-item label="RTSP PORT" prop="port">
                <el-input v-model="mediaServerForm.rtspPort" placeholder="媒体服务RTSP_PORT" clearable></el-input>
              </el-form-item>
              <el-form-item label="RTSPS PORT" prop="port">
                <el-input v-model="mediaServerForm.rtspSSLPort" placeholder="媒体服务RTSPS_PORT" clearable></el-input>
              </el-form-item>

            </el-form>
          </el-col>
          <el-col :span="12">
            <el-form v-if="currentStep === 2 || currentStep === 3"  ref="mediaServerForm2" :rules="rules" :model="mediaServerForm" label-width="180px" >
              <el-form-item label="RTMP PORT" prop="port">
                <el-input v-model="mediaServerForm.rtmpPort" placeholder="媒体服务RTMP_PORT" clearable></el-input>
              </el-form-item>
              <el-form-item label="RTMPS PORT" prop="port">
                <el-input v-model="mediaServerForm.rtmpSSlPort" placeholder="媒体服务RTMPS_PORT" clearable></el-input>
              </el-form-item>
              <el-form-item label="自动配置媒体服务" >
                <el-switch v-model="mediaServerForm.autoConfig"></el-switch>
              </el-form-item>
              <el-form-item label="收流端口模式" >
                <el-switch  active-text="多端口" inactive-text="单端口" v-model="mediaServerForm.rtpEnable"></el-switch>
              </el-form-item>

              <el-form-item v-if="!mediaServerForm.rtpEnable" label="收流端口" prop="port">
                <el-input v-model.number="mediaServerForm.rtpProxyPort" clearable></el-input>
              </el-form-item>
              <el-form-item v-if="mediaServerForm.rtpEnable" label="收流端口" prop="port">
                <el-input v-model="mediaServerForm.rtpPortRange1" placeholder="起始" clearable style="width: 100px" prop="port"></el-input>
                -
                <el-input v-model="mediaServerForm.rtpPortRange2" placeholder="终止"  clearable style="width: 100px" prop="port"></el-input>
              </el-form-item>
              <el-form-item label="推流端口" prop="port">
                <el-input v-model="mediaServerForm.sendRtpPortRange1" placeholder="起始" clearable style="width: 100px" prop="port"></el-input>
                -
                <el-input v-model="mediaServerForm.sendRtpPortRange2" placeholder="终止"  clearable style="width: 100px" prop="port"></el-input>
              </el-form-item>
              <el-form-item label="无人观看多久后停止拉流" >
                <el-input v-model.number="mediaServerForm.streamNoneReaderDelayMS" clearable></el-input>
              </el-form-item>
              <el-form-item label="录像管理服务端口" prop="port">
                <el-input v-model.number="mediaServerForm.recordAssistPort">
<!--                  <el-button v-if="mediaServerForm.recordAssistPort > 0" slot="append" type="primary" @click="checkRecordServer">测试</el-button>-->
                  <el-button v-if="mediaServerForm.recordAssistPort > 0" class="el-icon-check" slot="append" type="primary" @click="checkRecordServer"></el-button>
                </el-input>
                <i v-if="recordServerCheck == 1" class="el-icon-success" style="color: #3caf36; position: absolute;top: 14px;"></i>
                <i v-if="recordServerCheck == 2" class="el-icon-loading" style="color: #3caf36; position: absolute;top: 14px;"></i>
                <i v-if="recordServerCheck === -1" class="el-icon-error" style="color: #c80000; position: absolute;top: 14px;"></i>
              </el-form-item>
              <el-form-item>
                <div style="float: right;">
                  <el-button type="primary"  @click="onSubmit" >提交</el-button>
                  <el-button @click="close">取消</el-button>
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
import MediaServer from './../service/MediaServer'

export default {
  name: "streamProxyEdit",
  props: {},
  computed: {},
  created() {
    this.setDialogWidth()
  },
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
      mediaServer: new MediaServer(),
      serverCheck: 0,
      recordServerCheck: 0,
      mediaServerForm: {
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
        rtpPortRange1: "",
        rtpPortRange2: "",
        sendRtpPortRange1: "",
        sendRtpPortRange2: "",
        rtpProxyPort: "",
        rtspPort: "",
        rtspSSLPort: "",
      },

      rules: {
        ip:  [{ required: true, validator: isValidIp, message: '请输入有效的IP地址', trigger: 'blur' }],
        port:  [{ required: true, validator: isValidPort, message: '请输入有效的端口号', trigger: 'blur' }],
        secret: [{ required: true, message: "请输入secret", trigger: "blur" }],
        timeout_ms: [{ required: true, message: "请输入FFmpeg推流成功超时时间", trigger: "blur" }],
        ffmpeg_cmd_key: [{ required: false, message: "请输入FFmpeg命令参数模板（可选）", trigger: "blur" }],
      },
    };
  },
  methods: {
    setDialogWidth() {
      let val = document.body.clientWidth
      if (val < this.defaultWidth) {
        this.dialogWidth = '100%'
      } else {
        this.dialogWidth = this.defaultWidth + 'px'
      }
    },
    openDialog: function (param, callback) {
      this.showDialog = true;
      this.listChangeCallback = callback;
      if (param != null) {
        this.mediaServerForm = param;
        this.currentStep = 3;
        if (param.rtpPortRange) {
          let rtpPortRange = this.mediaServerForm.rtpPortRange.split(",");
          if (rtpPortRange.length > 0) {
            this.mediaServerForm["rtpPortRange1"] = rtpPortRange[0]
            this.mediaServerForm["rtpPortRange2"] = rtpPortRange[1]
          }
        }
        let sendRtpPortRange = this.mediaServerForm.sendRtpPortRange.split(",");
        this.mediaServerForm["sendRtpPortRange1"] = sendRtpPortRange[0]
        this.mediaServerForm["sendRtpPortRange2"] = sendRtpPortRange[1]
      }
    },
    checkServer: function() {
      let that = this;
      that.serverCheck = 0;
      that.mediaServer.checkServer(that.mediaServerForm, data =>{
        if (data.code === 0) {
          if (parseInt(that.mediaServerForm.httpPort) !== parseInt(data.data.httpPort)) {
            that.$message({
              showClose: true,
              message: '如果你正在使用docker部署你的媒体服务，请注意的端口映射。',
              type: 'warning',
              duration: 0
            });
          }
          let httpPort = that.mediaServerForm.httpPort;
          that.mediaServerForm = data.data;
          that.mediaServerForm.httpPort = httpPort;
          that.mediaServerForm.autoConfig = true;
          that.mediaServerForm.sendRtpPortRange1 = 30000
          that.mediaServerForm.sendRtpPortRange2 = 30500
          that.mediaServerForm.rtpPortRange1 = 30000
          that.mediaServerForm.rtpPortRange2 = 30500
          that.serverCheck = 1;
        }else {
          that.serverCheck = -1;
          that.$message({
            showClose: true,
            message: data.msg,
            type: "error",
          });
        }

      })
    },
    next: function (){
      this.currentStep = 2;
      this.defaultWidth = 900;
      this.setDialogWidth();
    },
    checkRecordServer: function (){
      let that = this;
      that.recordServerCheck = 2;
      if (that.mediaServerForm.recordAssistPort <= 0 || that.mediaServerForm.recordAssistPort > 65535 ) {
        that.recordServerCheck = -1;
        that.$message({
          showClose: true,
          message: "端口号应该在-65535之间",
          type: "error",
        });
        return;
      }
      that.mediaServer.checkRecordServer(that.mediaServerForm, data =>{
        if (data.code === 0) {
          that.recordServerCheck = 1;
        }else {
          that.recordServerCheck = -1;
          that.$message({
            showClose: true,
            message: data.msg,
            type: "error",
          });
        }
      })
    },
    onSubmit: function () {
      this.dialogLoading = true;
      let that = this;
      if (this.mediaServerForm.rtpEnable) {
        this.mediaServerForm.rtpPortRange = this.mediaServerForm.rtpPortRange1 + "," + this.mediaServerForm.rtpPortRange2;
      }
      this.mediaServerForm.sendRtpPortRange = this.mediaServerForm.sendRtpPortRange1 + "," + this.mediaServerForm.sendRtpPortRange2;
      that.mediaServer.addServer(this.mediaServerForm, data => {
        if (data.code === 0) {
          that.$message({
            showClose: true,
            message: "保存成功",
            type: "success",
          });
          if (this.listChangeCallback) this.listChangeCallback();
          that.close()
        }else {
          that.$message({
            showClose: true,
            message: data.msg,
            type: "error",
          });
        }
      })
    },
    close: function () {
      this.showDialog = false;
      this.dialogLoading = false;
      this.mediaServerForm = {
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
        rtpPortRange1: "",
        rtpPortRange2: "",
        sendRtpPortRange1: "",
        sendRtpPortRange2: "",
        rtpProxyPort: "",
        rtspPort: "",
        rtspSSLPort: "",
      };
      this.listChangeCallback = null
      this.currentStep = 1;
    },
    deviceGBIdExit: async function (deviceGbId) {
      var result = false;
      var that = this;
      await that.$axios({
        method: 'post',
        url:`/api/platform/exit/${deviceGbId}`
      }).then(function (res) {
        result = res.data;
      }).catch(function (error) {
        console.log(error);
      });
      return result;
    },
    checkExpires: function() {
      if (this.platform.enable && this.platform.expires == "0") {
        this.platform.expires = "300";
      }
    }
  },
};
</script>
