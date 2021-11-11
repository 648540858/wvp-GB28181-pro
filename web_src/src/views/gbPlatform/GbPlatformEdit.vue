<template>
  <a-modal
    title="平台信息"
    :width="1000"
    :visible="formVisible"
    :confirmLoading="loading"
    :ok-text="okText"
    :cancel-text="cancelText"
    @ok="onSubmit"
    @cancel="close"
  >
    <a-spin :spinning="loading">
      <a-row type="flex" :gutter="[0, 0]">
        <a-col :flex="1">
          <a-form-model ref="platform1" :rules="rules" :model="platform" :layout="form.layout" v-bind="formItemLayout">
            <a-form-model-item label="名称" prop="name">
              <a-input v-model="platform.name"></a-input>
            </a-form-model-item>
            <a-form-model-item label="SIP服务国标编码" prop="serverGBId">
              <a-input v-model="platform.serverGBId"></a-input>
            </a-form-model-item>
            <a-form-model-item label="SIP服务国标域" prop="serverGBDomain">
              <a-input v-model="platform.serverGBDomain"></a-input>
            </a-form-model-item>
            <a-form-model-item label="SIP服务IP" prop="serverIP">
              <a-input v-model="platform.serverIP"></a-input>
            </a-form-model-item>
            <a-form-model-item label="SIP服务端口" prop="serverPort">
              <a-input v-model="platform.serverPort"></a-input>
            </a-form-model-item>
            <a-form-model-item label="设备国标编号" prop="deviceGBId">
              <a-input v-model="platform.deviceGBId"></a-input>
            </a-form-model-item>
            <a-form-model-item label="本地IP" prop="deviceIp">
              <a-input v-model="platform.deviceIp" :disabled="true"></a-input>
            </a-form-model-item>
            <a-form-model-item label="本地端口" prop="devicePort">
              <a-input v-model="platform.devicePort" :disabled="true"></a-input>
            </a-form-model-item>
          </a-form-model>
        </a-col>
        <a-col>
          <a-divider type="vertical" style="height: 100%"/>
        </a-col>
        <a-col :flex="1">
          <a-form-model ref="platform2" :rules="rules" :model="platform" :layout="form.layout" v-bind="formItemLayout">
            <a-form-model-item label="SIP认证用户名" prop="username">
              <a-input v-model="platform.username"></a-input>
            </a-form-model-item>
            <a-form-model-item label="SIP认证密码" prop="password">
              <a-input v-model="platform.password"></a-input>
            </a-form-model-item>
            <a-form-model-item label="注册周期(秒)" prop="expires">
              <a-input v-model="platform.expires"></a-input>
            </a-form-model-item>
            <a-form-model-item label="心跳周期(秒)" prop="keepTimeout">
              <a-input v-model="platform.keepTimeout"></a-input>
            </a-form-model-item>
            <a-form-model-item label="信令传输" prop="transport">
              <a-select
                v-model="platform.transport"
                style="width: 100%"
                placeholder="请选择信令传输方式"
              >
                <a-select-option value="UDP">UDP</a-select-option>
                <a-select-option value="TCP">TCP</a-select-option>
              </a-select>
            </a-form-model-item>
            <a-form-model-item label="字符集" prop="characterSet">
              <a-select
                v-model="platform.characterSet"
                style="width: 100%"
                placeholder="请选择字符集"
              >
                <a-select-option value="GB2312">GB2312</a-select-option>
                <a-select-option value="UTF-8">UTF-8</a-select-option>
              </a-select>
            </a-form-model-item>
            <a-form-model-item label="其他选项">
              <a-checkbox v-model="platform.enable" @change="checkExpires">启用</a-checkbox>
              <a-checkbox v-model="platform.ptz">云台控制</a-checkbox>
              <a-checkbox v-model="platform.rtcp">RTCP保活</a-checkbox>
            </a-form-model-item>
          </a-form-model>
        </a-col>
      </a-row>
    </a-spin>
  </a-modal>
</template>

<script>
import {exitPlatform, getPlatformServerConf, savePlatform} from "@/api/gbPlatform";

export default {
  name: "platformEdit",
  data() {
    let deviceGBIdRules = async (rule, value, callback) => {
      if (value === "") {
        callback(new Error("请输入设备国标编号"));
      } else {
        let exit = await this.deviceGBIdExit(value);
        if (exit) {
          callback(new Error("设备国标编号已存在"));
        } else {
          callback();
        }
      }
    };
    return {
      formVisible: false,
      loading: false,
      form: {
        layout: 'horizontal'
      },
      okText: '确认',
      cancelText: '取消',

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
      },
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
      },
    };
  },
  computed: {
    formItemLayout() {
      const {layout} = this.form;
      return layout === 'horizontal'
        ? {
          labelCol: {
            xs: {span: 24},
            sm: {span: 7}
          },
          wrapperCol: {
            xs: {span: 24},
            sm: {span: 13}
          }
        }
        : {};
    },
  },
  methods: {
    openDialog: function (platform) {
      this.formVisible = true
      if (platform == null) {
        this.okText = "立即创建";
        this.loading = true
        getPlatformServerConf().then(res => {
          this.loading = false
          this.platform.deviceGBId = res.username;
          this.platform.deviceIp = res.deviceIp;
          this.platform.devicePort = res.devicePort;
          this.platform.username = res.username;
          this.platform.password = res.password;
        }).catch(error => {
          this.loading = false
          this.$message.error('获取信令服务配置信息失败')
          console.log(error);
        });
      } else {
        this.platform = platform;
        this.okText = "保存";
      }
    },

    onSubmit: function () {
      this.loading = true
      this.$refs.platform1.validate(valid => {
        if (!valid) {
          this.loading = false
          return
        }
        this.$refs.platform2.validate(valid => {
          if (!valid) {
            this.loading = false
            return
          }
          savePlatform(this.platform).then(res => {
            this.loading = false
            if (res === "success") {
              this.$message.success("保存成功")
              this.formVisible = false;
              this.$emit('refreshTable')
            } else {
              this.$message.error("保存失败")
            }
          }).catch(error => {
            this.$message.error("保存失败：" + error)
          });
        })
      })
    },

    close() {
      this.formVisible = false;
      this.$refs.platform1.resetFields();
      this.$refs.platform2.resetFields();
    },

    deviceGBIdExit: async function (deviceGbId) {
      let result = false
      await exitPlatform({deviceGbId: deviceGbId}).then(res => {
        result = res.data
      }).catch(function (error) {
        console.log(error)
      })
      return result
    },

    checkExpires() {
      if (this.platform.enable && this.platform.expires === 0) {
        this.platform.expires = 300;
      }
    }
  },
};
</script>

<style>
</style>
