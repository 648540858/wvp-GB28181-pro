<template>
  <div id="PlatformEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header @back="close" content="添加上级平台"></el-page-header>
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-button icon="el-icon-close" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        </div>
      </div>
    </div>
    <div id="shared" style="text-align: right; margin-top: 1rem; background-color: #FFFFFF; padding-top: 2rem;">
      <el-row :gutter="24">
        <el-col :span="11">
          <el-form ref="platform1" :rules="rules" :model="value" size="medium" label-width="160px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="value.name"></el-input>
            </el-form-item>
            <el-form-item label="SIP服务国标编码" prop="serverGBId">
              <el-input v-model="value.serverGBId" clearable @input="serverGBIdChange"></el-input>
            </el-form-item>
            <el-form-item label="SIP服务国标域" prop="serverGBDomain">
              <el-input v-model="value.serverGBDomain" clearable></el-input>
            </el-form-item>
            <el-form-item label="SIP服务IP" prop="serverIp">
              <el-input v-model="value.serverIp" clearable></el-input>
            </el-form-item>
            <el-form-item label="SIP服务端口" prop="serverPort">
              <el-input v-model="value.serverPort" clearable type="number"></el-input>
            </el-form-item>
            <el-form-item label="设备国标编号" prop="deviceGBId">
              <el-input v-model="value.deviceGBId" clearable @input="deviceGBIdChange"></el-input>
            </el-form-item>
            <el-form-item label="本地IP" prop="deviceIp">
              <el-select v-model="value.deviceIp" placeholder="请选择与上级相通的网卡" style="width: 100%">
                <el-option
                  v-for="ip in deviceIps"
                  :key="ip"
                  :label="ip"
                  :value="ip">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="本地端口" prop="devicePort">
              <el-input v-model="value.devicePort" :disabled="true" type="number"></el-input>
            </el-form-item>

            <el-form-item label="SIP认证用户名" prop="username">
              <el-input v-model="value.username"></el-input>
            </el-form-item>
            <el-form-item label="SIP认证密码" prop="password">
              <el-input v-model="value.password"></el-input>
            </el-form-item>
            <el-form-item label="注册周期(秒)" prop="expires">
              <el-input v-model="value.expires"></el-input>
            </el-form-item>
            <el-form-item label="心跳周期(秒)" prop="keepTimeout">
              <el-input v-model="value.keepTimeout"></el-input>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <el-form ref="platform2" :rules="rules" :model="value" size="medium" label-width="160px">
            <el-form-item label="SDP发流IP" prop="sendStreamIp">
              <el-input v-model="value.sendStreamIp"></el-input>
            </el-form-item>
            <el-form-item label="信令传输" prop="transport">
              <el-select
                v-model="value.transport"
                style="width: 100%"
                placeholder="请选择信令传输方式"
              >
                <el-option label="UDP" value="UDP"></el-option>
                <el-option label="TCP" value="TCP"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="保密属性" >
              <el-select v-model="value.secrecy" style="width: 100%" placeholder="请选择保密属性">
                <el-option label="不涉密" :value="0"></el-option>
                <el-option label="涉密" :value="1"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="目录分组" prop="catalogGroup">
              <el-select
                v-model="value.catalogGroup"
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
                v-model="value.characterSet"
                style="width: 100%"
                placeholder="请选择字符集"
              >
                <el-option label="GB2312" value="GB2312"></el-option>
                <el-option label="UTF-8" value="UTF-8"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="行政区划" prop="civilCode">
              <el-input v-model="value.civilCode" clearable></el-input>
            </el-form-item>
            <el-form-item label="平台厂商" prop="manufacturer">
              <el-input v-model="value.manufacturer" clearable></el-input>
            </el-form-item>
            <el-form-item label="平台型号" prop="model">
              <el-input v-model="value.model" clearable></el-input>
            </el-form-item>
            <el-form-item label="平台安装地址" prop="address">
              <el-input v-model="value.address" clearable></el-input>
            </el-form-item>
            <el-form-item label="其他选项" >
              <div style="text-align: left">
                <el-checkbox label="启用" v-model="value.enable" @change="checkExpires"></el-checkbox>
                <!--                <el-checkbox label="云台控制" v-model="value.ptz"></el-checkbox>-->
                <el-checkbox label="RTCP保活" v-model="value.rtcp" @change="rtcpCheckBoxChange"></el-checkbox>
                <el-checkbox label="消息通道" v-model="value.asMessageChannel"></el-checkbox>
                <el-checkbox label="主动推送通道" v-model="value.autoPushChannel"></el-checkbox>
                <el-checkbox label="推送平台信息" :true-label="1" :false-label="0" v-model="value.catalogWithPlatform"></el-checkbox>
                <el-checkbox label="推送分组信息" :true-label="1" :false-label="0" v-model="value.catalogWithGroup"></el-checkbox>
                <el-checkbox label="推送行政区划" :true-label="1" :false-label="0" v-model="value.catalogWithRegion"></el-checkbox>
              </div>


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
  props: [ 'value', 'closeEdit', 'deviceIps'],
  components: {
  },
  created() {

  },
  watch: {
    value(newValue, oldValue){
      this.streamProxy = newValue;
    }
  },
  data() {
    var deviceGBIdRules = async (rule, value, callback) => {
      console.log(value);
      if (value === "") {
        callback(new Error("请输入设备国标编号"));
      } else {
        var exit = await this.deviceGBIdExit(value);
        if (exit) {
          callback(new Error("设备国标编号格式错误或已存在"));
        } else {
          callback();
        }
      }
    }
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      onSubmit_text: "保存",

      rules: {
        name: [{required: true, message: "请输入平台名称", trigger: "blur"}],
        serverGBId: [
          {required: true, message: "请输入SIP服务国标编码", trigger: "blur"},
        ],
        serverGBDomain: [
          {required: true, message: "请输入SIP服务国标域", trigger: "blur"},
        ],
        serverIp: [{required: true, message: "请输入SIP服务IP", trigger: "blur"}],
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
      this.saveLoading = true;
      if (this.value.id) {
        this.$axios({
          method: 'post',
          url: "/api/platform/update",
          data: this.value
        }).then((res) => {
          this.saveLoading = false;
          if (res.data.code === 0) {
            this.$message({
              showClose: true,
              message: "保存成功",
              type: "success",
            });
            this.showDialog = false;
            if (this.closeEdit) {
              this.closeEdit();
            }
          } else {
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: "error",
            });
          }
        }).catch((error) => {
          this.saveLoading = false;
          console.log(error);
        });
      }else {
        this.$axios({
          method: 'post',
          url: "/api/platform/add",
          data: this.value
        }).then((res) => {
          this.saveLoading = false;
          if (res.data.code === 0) {
            this.$message({
              showClose: true,
              message: "保存成功",
              type: "success",
            });
            if (this.closeEdit) {
              this.closeEdit();
            }
          } else {
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: "error",
            });
          }
        }).catch((error) => {
          this.saveLoading = false;
          console.log(error);
        });
      }


    },
    serverGBIdChange: function () {
      if (this.value.serverGBId.length > 10) {
        this.value.serverGBDomain = this.value.serverGBId.substr(0, 10);
      }
    },
    deviceGBIdChange: function () {
      this.value.username = this.value.deviceGBId;
    },
    checkExpires: function () {
      if (this.value.enable && this.value.expires === "0") {
        this.value.expires = "3600";
      }
    },
    rtcpCheckBoxChange: function (result) {
      if (result) {
        this.$message({
          showClose: true,
          message: "开启RTCP保活需要上级平台支持，可以避免无效推流",
          type: "warning",
        });
      }
    },
    deviceGBIdExit: async function (deviceGbId) {
      let result = false;
      await this.$axios({
        method:"get",
        url:`/api/platform/exit/${deviceGbId}`
      }).then((res)=> {
        result = res.data;
      }).catch((error)=> {
        console.log(error);
      });
      return result;
    },
    close: function () {
      this.closeEdit()
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
