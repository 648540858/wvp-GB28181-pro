<template>
  <div id="addlatform" v-loading="isLoging">
    <el-dialog
      title="添加平台"
      width="70%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
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
                <el-input v-model="platform.deviceIp" :disabled="true"></el-input>
              </el-form-item>
              <el-form-item label="本地端口" prop="devicePort">
                <el-input v-model="platform.devicePort" :disabled="true" type="number"></el-input>
              </el-form-item>
            </el-form>
          </el-col>
          <el-col :span="12">
            <el-form ref="platform2" :rules="rules" :model="platform" label-width="160px">
              <el-form-item label="SIP认证用户名" prop="username">
                <el-input v-model="platform.username"></el-input>
              </el-form-item>
              <el-form-item label="行政区划" prop="administrativeDivision">
                <el-input v-model="platform.administrativeDivision" clearable></el-input>
              </el-form-item>
              <el-form-item label="SIP认证密码" prop="password">
                <el-input v-model="platform.password" ></el-input>
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
                <el-checkbox label="云台控制" v-model="platform.ptz"></el-checkbox>
                <el-checkbox label="共享所有直播流" v-model="platform.shareAllLiveStream"></el-checkbox>
                <el-checkbox label="拉起离线推流" v-model="platform.startOfflinePush"></el-checkbox>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onSubmit">{{
                  onSubmit_text
                }}</el-button>
                <el-button @click="close">取消</el-button>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "platformEdit",
  props: {},
  computed: {},
  data() {
    var deviceGBIdRules = async (rule, value, callback) => {
      console.log(value);
      if (value === "") {
        callback(new Error("请输入设备国标编号"));
      } else {
        var exit = await this.deviceGBIdExit(value);
        if (exit) {
          callback(new Error("设备国标编号已存在"));
        } else {
          callback();
        }
      }
    };
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
        expires: 300,
        keepTimeout: 60,
        transport: "UDP",
        characterSet: "GB2312",
        shareAllLiveStream: false,
        startOfflinePush: false,
        catalogGroup: 1,
        administrativeDivision: null,
      },
      rules: {
        name: [{ required: true, message: "请输入平台名称", trigger: "blur" }],
        serverGBId: [
          { required: true, message: "请输入SIP服务国标编码", trigger: "blur" },
        ],
        serverGBDomain: [
          { required: true, message: "请输入SIP服务国标域", trigger: "blur" },
        ],
        serverIP: [{ required: true, message: "请输入SIP服务IP", trigger: "blur" }],
        serverPort: [{ required: true, message: "请输入SIP服务端口", trigger: "blur" }],
        deviceGBId: [{ validator: deviceGBIdRules, trigger: "blur" }],
        username: [{ required: false, message: "请输入SIP认证用户名", trigger: "blur" }],
        password: [{ required: false, message: "请输入SIP认证密码", trigger: "blur" }],
        expires: [{ required: true, message: "请输入注册周期", trigger: "blur" }],
        keepTimeout: [{ required: true, message: "请输入心跳周期", trigger: "blur" }],
        transport: [{ required: true, message: "请选择信令传输", trigger: "blur" }],
        characterSet: [{ required: true, message: "请选择编码字符集", trigger: "blur" }],
      },
    };
  },
  methods: {
    openDialog: function (platform, callback) {
      var that = this;
      if (platform == null) {
        this.onSubmit_text = "立即创建";
        this.saveUrl = "/api/platform/add";
        this.$axios({
          method: 'get',
          url:`/api/platform/server_config`
        }).then(function (res) {
          console.log(res);
          that.platform.deviceGBId = res.data.username;
          that.platform.deviceIp = res.data.deviceIp;
          that.platform.devicePort = res.data.devicePort;
          that.platform.username = res.data.username;
          that.platform.password = res.data.password;
          that.platform.administrativeDivision = res.data.username.substr(0, 6);
        }).catch(function (error) {
          console.log(error);
        });
      }else {
        this.platform.id = platform.id;
        this.platform.enable = platform.enable;
        this.platform.ptz = platform.ptz;
        this.platform.rtcp = platform.rtcp;
        this.platform.name = platform.name;
        this.platform.serverGBId = platform.serverGBId;
        this.platform.serverGBDomain = platform.serverGBDomain;
        this.platform.serverIP = platform.serverIP;
        this.platform.serverPort = platform.serverPort;
        this.platform.deviceGBId = platform.deviceGBId;
        this.platform.deviceIp = platform.deviceIp;
        this.platform.devicePort = platform.devicePort;
        this.platform.username = platform.username;
        this.platform.password = platform.password;
        this.platform.expires = platform.expires;
        this.platform.keepTimeout = platform.keepTimeout;
        this.platform.transport = platform.transport;
        this.platform.characterSet = platform.characterSet;
        this.platform.shareAllLiveStream = platform.shareAllLiveStream;
        this.platform.catalogId = platform.catalogId;
        this.platform.startOfflinePush = platform.startOfflinePush;
        this.platform.catalogGroup = platform.catalogGroup;
        this.platform.administrativeDivision = platform.administrativeDivision;
        this.onSubmit_text = "保存";
        this.saveUrl = "/api/platform/save";
      }
      this.showDialog = true;
      this.listChangeCallback = callback;
    },
    serverGBIdChange: function () {
      if (this.platform.serverGBId.length > 10) {
        this.platform.serverGBDomain = this.platform.serverGBId.substr(0, 10);
      }
    },
    deviceGBIdChange: function () {

      this.platform.username = this.platform.deviceGBId ;
      if (this.platform.administrativeDivision == null) {
        this.platform.administrativeDivision = this.platform.deviceGBId.substr(0, 6);
      }

    },
    onSubmit: function () {
      var that = this;
      that.$axios({
        method: 'post',
        url: this.saveUrl,
        data: that.platform
      }).then(function (res) {
          if (res.data.code === 0) {
            that.$message({
              showClose: true,
              message: "保存成功",
              type: "success",
            });
            that.showDialog = false;
            if (that.listChangeCallback != null) {
              that.listChangeCallback();
            }
          }else {
            that.$message({
              showClose: true,
              message: res.data.msg,
              type: "error",
            });
          }
        }).catch(function (error) {
          console.log(error);
        });
    },
    close: function () {
      this.showDialog = false;
      this.$refs.platform1.resetFields();
      this.$refs.platform2.resetFields();
      this.platform = {
        id: null,
        enable: true,
        ptz: true,
        rtcp: false,
        name: null,
        serverGBId: null,
        administrativeDivision: null,
        serverGBDomain: null,
        serverIP: null,
        serverPort: null,
        deviceGBId: null,
        deviceIp: null,
        devicePort: null,
        username: null,
        password: null,
        expires: 300,
        keepTimeout: 60,
        transport: "UDP",
        characterSet: "GB2312",
        shareAllLiveStream: false,
        startOfflinePush: false,
        catalogGroup: 1,
      }
    },
    deviceGBIdExit: async function (deviceGbId) {
      var result = false;
      var that = this;
      await that.$axios({
                method: 'post',
                url:`/api/platform/exit/${deviceGbId}`})
        .then(function (res) {
          result = res.data;
        })
        .catch(function (error) {
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

<style>
/* 谷歌 */
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  appearance: none;
  margin: 0;
}
/* 火狐 */
input{
  -moz-appearance:textfield;
}
.control-wrapper-not-used {
  position: relative;
  width: 6.25rem;
  height: 6.25rem;
  max-width: 6.25rem;
  max-height: 6.25rem;
  border-radius: 100%;
  margin-top: 2.5rem;
  margin-left: 0.5rem;
  float: left;
}

.control-panel {
  position: relative;
  top: 0;
  left: 5rem;
  height: 11rem;
  max-height: 11rem;
}

.control-btn {
  display: flex;
  justify-content: center;
  position: absolute;
  width: 44%;
  height: 44%;
  border-radius: 5px;
  border: 1px solid #78aee4;
  box-sizing: border-box;
  transition: all 0.3s linear;
}

.control-btn i {
  font-size: 20px;
  color: #78aee4;
  display: flex;
  justify-content: center;
  align-items: center;
}

.control-round {
  position: absolute;
  top: 21%;
  left: 21%;
  width: 58%;
  height: 58%;
  background: #fff;
  border-radius: 100%;
}

.control-round-inner {
  position: absolute;
  left: 13%;
  top: 13%;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 70%;
  height: 70%;
  font-size: 40px;
  color: #78aee4;
  border: 1px solid #78aee4;
  border-radius: 100%;
  transition: all 0.3s linear;
}

.control-inner-btn {
  position: absolute;
  width: 60%;
  height: 60%;
  background: #fafafa;
}

.control-top {
  top: -8%;
  left: 27%;
  transform: rotate(-45deg);
  border-radius: 5px 100% 5px 0;
}

.control-top i {
  transform: rotate(45deg);
  border-radius: 5px 100% 5px 0;
}

.control-top .control-inner {
  left: -1px;
  bottom: 0;
  border-top: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 100% 0 0;
}

.control-top .fa {
  transform: rotate(45deg) translateY(-7px);
}

.control-left {
  top: 27%;
  left: -8%;
  transform: rotate(45deg);
  border-radius: 5px 0 5px 100%;
}

.control-left i {
  transform: rotate(-45deg);
}

.control-left .control-inner {
  right: -1px;
  top: -1px;
  border-bottom: 1px solid #78aee4;
  border-left: 1px solid #78aee4;
  border-radius: 0 0 0 100%;
}

.control-left .fa {
  transform: rotate(-45deg) translateX(-7px);
}

.control-right {
  top: 27%;
  right: -8%;
  transform: rotate(45deg);
  border-radius: 5px 100% 5px 0;
}

.control-right i {
  transform: rotate(-45deg);
}

.control-right .control-inner {
  left: -1px;
  bottom: -1px;
  border-top: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 100% 0 0;
}

.control-right .fa {
  transform: rotate(-45deg) translateX(7px);
}

.control-bottom {
  left: 27%;
  bottom: -8%;
  transform: rotate(45deg);
  border-radius: 0 5px 100% 5px;
}

.control-bottom i {
  transform: rotate(-45deg);
}

.control-bottom .control-inner {
  top: -1px;
  left: -1px;
  border-bottom: 1px solid #78aee4;
  border-right: 1px solid #78aee4;
  border-radius: 0 0 100% 0;
}

.control-bottom .fa {
  transform: rotate(-45deg) translateY(7px);
}
</style>
