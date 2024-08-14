<template>
  <div id="StreamProxyEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-button icon="el-icon-back" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        <el-divider direction="vertical"></el-divider>
        编辑拉流代理信息
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-button icon="el-icon-close" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        </div>
      </div>
    </div>
    <div id="shared" style="text-align: right; margin-top: 1rem">
      <el-row :gutter="24">
        <el-col :span="11">
          <el-form ref="platform1" :rules="rules" :model="platform" label-width="160px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="platform.name"></el-input>
            </el-form-item>
            <el-form-item label="SIP服务国标编码" prop="serverGBId">
              <el-input v-model="platform.serverGBId" clearable @input="serverGBIdChange"></el-input>
            </el-form-item>
            <el-form-item label="SIP服务国标域" prop="serverGBDomain">
              <el-input v-model="platform.serverGBDomain" clearable></el-input>
            </el-form-item>
            <el-form-item label="SIP服务IP" prop="serverIP">
              <el-input v-model="platform.serverIP" clearable></el-input>
            </el-form-item>
            <el-form-item label="SIP服务端口" prop="serverPort">
              <el-input v-model="platform.serverPort" clearable type="number"></el-input>
            </el-form-item>
            <el-form-item label="设备国标编号" prop="deviceGBId">
              <el-input v-model="platform.deviceGBId" clearable @input="deviceGBIdChange"></el-input>
            </el-form-item>
            <el-form-item label="本地IP" prop="deviceIp">
              <el-select v-model="platform.deviceIp" placeholder="请选择与上级相通的网卡" style="width: 100%">
                <el-option
                  v-for="ip in deviceIps"
                  :key="ip"
                  :label="ip"
                  :value="ip">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="本地端口" prop="devicePort">
              <el-input v-model="platform.devicePort" :disabled="true" type="number"></el-input>
            </el-form-item>
            <el-form-item label="SDP发流IP" prop="sendStreamIp">
              <el-input v-model="platform.sendStreamIp"></el-input>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <el-form ref="platform2" :rules="rules" :model="platform" label-width="160px">
            <el-form-item label="行政区划" prop="administrativeDivision">
              <el-input v-model="platform.administrativeDivision" clearable></el-input>
            </el-form-item>
            <el-form-item label="SIP认证用户名" prop="username">
              <el-input v-model="platform.username"></el-input>
            </el-form-item>
            <el-form-item label="SIP认证密码" prop="password">
              <el-input v-model="platform.password"></el-input>
            </el-form-item>
            <el-form-item label="注册周期(秒)" prop="expires">
              <el-input v-model="platform.expires"></el-input>
            </el-form-item>
            <el-form-item label="心跳周期(秒)" prop="keepTimeout">
              <el-input v-model="platform.keepTimeout"></el-input>
            </el-form-item>
            <el-form-item label="信令传输" prop="transport">
              <el-select
                v-model="platform.transport"
                style="width: 100%"
                placeholder="请选择信令传输方式"
              >
                <el-option label="UDP" value="UDP"></el-option>
                <el-option label="TCP" value="TCP"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="目录分组" prop="catalogGroup">
              <el-select
                v-model="platform.catalogGroup"
                style="width: 100%"
                placeholder="请选择目录分组"
              >
                <el-option label="1" value="1"></el-option>
                <el-option label="2" value="2"></el-option>
                <el-option label="4" value="4"></el-option>
                <el-option label="8" value="8"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="字符集" prop="characterSet">
              <el-select
                v-model="platform.characterSet"
                style="width: 100%"
                placeholder="请选择字符集"
              >
                <el-option label="GB2312" value="GB2312"></el-option>
                <el-option label="UTF-8" value="UTF-8"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="其他选项">
              <el-checkbox label="启用" v-model="platform.enable" @change="checkExpires"></el-checkbox>
              <!--                <el-checkbox label="云台控制" v-model="platform.ptz"></el-checkbox>-->
              <el-checkbox label="拉起推流" v-model="platform.startOfflinePush"></el-checkbox>
              <el-checkbox label="RTCP保活" v-model="platform.rtcp" @change="rtcpCheckBoxChange"></el-checkbox>
              <el-checkbox label="消息通道" v-model="platform.asMessageChannel"></el-checkbox>
              <el-checkbox label="推送通道" v-model="platform.autoPushChannel"></el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">{{
                  onSubmit_text
                }}
              </el-button>
              <el-button @click="close">取消</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>

export default {
  name: "platformEdit",
  props: [ 'value', 'closeEdit'],
  components: {
  },
  created() {},
  watch: {
    value(newValue, oldValue){
      this.streamProxy = newValue;
    }
  },
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      onSubmit_text: "立即创建",
      saveUrl: "/api/platform/save",

      platform: {
        id: null,
        enable: true,
        ptz: true,
        rtcp: false,
        asMessageChannel: false,
        autoPushChannel: false,
        name: null,
        serverGBId: null,
        serverGBDomain: null,
        serverIP: null,
        serverPort: null,
        deviceGBId: null,
        deviceIp: null,
        devicePort: null,
        username: null,
        password: null,
        expires: 3600,
        keepTimeout: 60,
        transport: "UDP",
        characterSet: "GB2312",
        startOfflinePush: false,
        catalogGroup: 1,
        administrativeDivision: "",
        sendStreamIp: null,
      },
      deviceIps: [], // 存储用户设备IP数组
      rules: {
        name: [{required: true, message: "请输入平台名称", trigger: "blur"}],
        serverGBId: [
          {required: true, message: "请输入SIP服务国标编码", trigger: "blur"},
        ],
        serverGBDomain: [
          {required: true, message: "请输入SIP服务国标域", trigger: "blur"},
        ],
        serverIP: [{required: true, message: "请输入SIP服务IP", trigger: "blur"}],
        serverPort: [{required: true, message: "请输入SIP服务端口", trigger: "blur"}],
        deviceGBId: [{validator: deviceGBIdRules, trigger: "blur"}],
        username: [{required: false, message: "请输入SIP认证用户名", trigger: "blur"}],
        password: [{required: false, message: "请输入SIP认证密码", trigger: "blur"}],
        expires: [{required: true, message: "请输入注册周期", trigger: "blur"}],
        keepTimeout: [{required: true, message: "请输入心跳周期", trigger: "blur"}],
        transport: [{required: true, message: "请选择信令传输", trigger: "blur"}],
        characterSet: [{required: true, message: "请选择编码字符集", trigger: "blur"}],
        deviceIp: [{required: true, message: "请选择本地IP", trigger: "blur"}],
      },

      saveLoading: false,
    };
  },
  methods: {
    onSubmit: function () {
      console.log(typeof this.streamProxy.noneReader)
      this.saveLoading = true;

      this.noneReaderHandler();
      if (this.streamProxy.id) {
        this.$axios({
          method: 'post',
          url:`/api/proxy/update`,
          data: this.streamProxy
        }).then((res)=> {
          this.saveLoading = false;
          if (typeof (res.data.code) != "undefined" && res.data.code === 0) {
            this.$message.success("保存成功");
            console.log(res.data.data)
            this.streamProxy = res.data.data
          }else {
            this.$message.error(res.data.msg);
          }
          this.saveLoading = false;
        }).catch((error) =>{
          this.$message.error(error);
          this.saveLoading = false;
        }).finally(()=>{
          console.log("finally==finally")
          this.saveLoading = false;
        })
      }else {
        this.$axios({
          method: 'post',
          url:`/api/proxy/add`,
          data: this.streamProxy
        }).then((res)=> {
          this.saveLoading = false;
          if (typeof (res.data.code) != "undefined" && res.data.code === 0) {
            this.$message.success("保存成功");
            this.streamProxy = res.data.data
          }else {
            this.$message.error(res.data.msg);
          }
        }).catch((error) =>{
          this.$message.error(res.data.error);
          this.saveLoading = false;
        }).finally(()=>{
          this.saveLoading = false;
        })
      }

    },
    close: function () {
      this.closeEdit()
    },
    mediaServerIdChange:function (){
      if (this.streamProxy.mediaServerId !== "auto"){
        this.$axios({
          method: 'get',
          url:`/api/proxy/ffmpeg_cmd/list`,
          params: {
            mediaServerId: this.streamProxy.mediaServerId
          }
        }).then((res)=> {
          this.ffmpegCmdList = res.data.data;
          this.streamProxy.ffmpegCmdKey = Object.keys(res.data.data)[0];
        }).catch(function (error) {
          console.log(error);
        });
      }

    },
    noneReaderHandler: function() {
      console.log(this.streamProxy)
      if (!this.streamProxy.noneReader || this.streamProxy.noneReader === 0 ) {
        this.streamProxy.enableDisableNoneReader = false;
        this.streamProxy.enableRemoveNoneReader = false;
      }else if (this.streamProxy.noneReader === 1){
        this.streamProxy.enableDisableNoneReader = true;
        this.streamProxy.enableRemoveNoneReader = false;
      }else if (this.streamProxy.noneReader ===2){
        this.streamProxy.enableDisableNoneReader = false;
        this.streamProxy.enableRemoveNoneReader = true;
      }
    },
  },
};
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
