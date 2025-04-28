<template>
  <div id="PlatformEdit" style="width: 100%">
    <div id="shared" style="text-align: right; margin-top: 1rem; background-color: #FFFFFF; padding-top: 2rem;">
      <el-row :gutter="24">
        <el-col :span="11">
          <el-form ref="platform1" :rules="rules" :model="value" size="medium" label-width="160px">
            <el-form-item label="名称" prop="name">
              <el-input v-model="value.name" />
            </el-form-item>
            <el-form-item label="SIP服务国标编码" prop="serverGBId">
              <el-input v-model="value.serverGBId" clearable @input="serverGBIdChange" />
            </el-form-item>
            <el-form-item label="SIP服务国标域" prop="serverGBDomain">
              <el-input v-model="value.serverGBDomain" clearable />
            </el-form-item>
            <el-form-item label="SIP服务IP" prop="serverIp">
              <el-input v-model="value.serverIp" clearable />
            </el-form-item>
            <el-form-item label="SIP服务端口" prop="serverPort">
              <el-input v-model="value.serverPort" clearable type="number" />
            </el-form-item>
            <el-form-item label="设备国标编号" prop="deviceGBId">
              <el-input v-model="value.deviceGBId" clearable @input="deviceGBIdChange" />
            </el-form-item>
            <el-form-item label="本地IP" prop="deviceIp">
              <el-select v-model="value.deviceIp" placeholder="请选择与上级相通的网卡" style="width: 100%">
                <el-option
                  v-for="ip in deviceIps"
                  :key="ip"
                  :label="ip"
                  :value="ip"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="本地端口" prop="devicePort">
              <el-input v-model="value.devicePort" :disabled="true" type="number" />
            </el-form-item>

            <el-form-item label="SIP认证用户名" prop="username">
              <el-input v-model="value.username" />
            </el-form-item>
            <el-form-item label="SIP认证密码" prop="password">
              <el-input v-model="value.password" />
            </el-form-item>
            <el-form-item label="注册周期(秒)" prop="expires">
              <el-input v-model="value.expires" />
            </el-form-item>
            <el-form-item label="心跳周期(秒)" prop="keepTimeout">
              <el-input v-model="value.keepTimeout" />
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <el-form ref="platform2" :rules="rules" :model="value" size="medium" label-width="160px">
            <el-form-item label="SDP发流IP" prop="sendStreamIp">
              <el-input v-model="value.sendStreamIp" />
            </el-form-item>
            <el-form-item label="信令传输" prop="transport">
              <el-select
                v-model="value.transport"
                style="width: 100%"
                placeholder="请选择信令传输方式"
              >
                <el-option label="UDP" value="UDP" />
                <el-option label="TCP" value="TCP" />
              </el-select>
            </el-form-item>
            <el-form-item label="保密属性">
              <el-select v-model="value.secrecy" style="width: 100%" placeholder="请选择保密属性">
                <el-option label="不涉密" :value="0" />
                <el-option label="涉密" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="目录分组" prop="catalogGroup">
              <el-select
                v-model="value.catalogGroup"
                style="width: 100%"
                placeholder="请选择目录分组"
              >
                <el-option label="1" value="1" />
                <el-option label="2" value="2" />
                <el-option label="4" value="4" />
                <el-option label="8" value="8" />
              </el-select>
            </el-form-item>
            <el-form-item label="字符集" prop="characterSet">
              <el-select
                v-model="value.characterSet"
                style="width: 100%"
                placeholder="请选择字符集"
              >
                <el-option label="GB2312" value="GB2312" />
                <el-option label="UTF-8" value="UTF-8" />
              </el-select>
            </el-form-item>
            <el-form-item label="行政区划" prop="civilCode">
              <el-input v-model="value.civilCode" clearable />
            </el-form-item>
            <el-form-item label="平台厂商" prop="manufacturer">
              <el-input v-model="value.manufacturer" clearable />
            </el-form-item>
            <el-form-item label="平台型号" prop="model">
              <el-input v-model="value.model" clearable />
            </el-form-item>
            <el-form-item label="平台安装地址" prop="address">
              <el-input v-model="value.address" clearable />
            </el-form-item>
            <el-form-item label="其他选项">
              <div style="text-align: left">
                <el-checkbox v-model="value.enable" label="启用" @change="checkExpires" />
                <!--                <el-checkbox label="云台控制" v-model="value.ptz"></el-checkbox>-->
                <el-checkbox v-model="value.rtcp" label="RTCP保活" @change="rtcpCheckBoxChange" />
                <el-checkbox v-model="value.asMessageChannel" label="消息通道" />
                <el-checkbox v-model="value.autoPushChannel" label="主动推送通道" />
                <el-checkbox
                  v-model="value.catalogWithPlatform"
                  label="推送平台信息"
                  :true-label="1"
                  :false-label="0"
                />
                <el-checkbox
                  v-model="value.catalogWithGroup"
                  label="推送分组信息"
                  :true-label="1"
                  :false-label="0"
                />
                <el-checkbox
                  v-model="value.catalogWithRegion"
                  label="推送行政区划"
                  :true-label="1"
                  :false-label="0"
                />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">{{ onSubmit_text }} </el-button>
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
  name: 'PlatformEdit',
  components: {},
  props: ['value', 'closeEdit', 'deviceIps'],
  data() {
    var deviceGBIdRules = async(rule, value, callback) => {
      console.log(value)
      if (value === '') {
        callback(new Error('请输入设备国标编号'))
      } else {
        var exit = await this.deviceGBIdExit(value)
        if (exit) {
          callback(new Error('设备国标编号格式错误或已存在'))
        } else {
          callback()
        }
      }
    }
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      onSubmit_text: '保存',

      rules: {
        name: [{ required: true, message: '请输入平台名称', trigger: 'blur' }],
        serverGBId: [
          { required: true, message: '请输入SIP服务国标编码', trigger: 'blur' }
        ],
        serverGBDomain: [
          { required: true, message: '请输入SIP服务国标域', trigger: 'blur' }
        ],
        serverIp: [{ required: true, message: '请输入SIP服务IP', trigger: 'blur' }],
        serverPort: [{ required: true, message: '请输入SIP服务端口', trigger: 'blur' }],
        deviceGBId: [{ validator: deviceGBIdRules, trigger: 'blur' }],
        username: [{ required: false, message: '请输入SIP认证用户名', trigger: 'blur' }],
        password: [{ required: false, message: '请输入SIP认证密码', trigger: 'blur' }],
        expires: [{ required: true, message: '请输入注册周期', trigger: 'blur' }],
        keepTimeout: [{ required: true, message: '请输入心跳周期', trigger: 'blur' }],
        transport: [{ required: true, message: '请选择信令传输', trigger: 'blur' }],
        characterSet: [{ required: true, message: '请选择编码字符集', trigger: 'blur' }],
        deviceIp: [{ required: true, message: '请选择本地IP', trigger: 'blur' }]
      },

      saveLoading: false
    }
  },
  watch: {
    value(newValue, oldValue) {
      this.streamProxy = newValue
    }
  },
  created() {

  },
  methods: {
    onSubmit: function() {
      this.saveLoading = true
      if (this.value.id) {
        this.$store.dispatch('platform/update', this.value)
          .then(data => {
            this.$message({
              showClose: true,
              message: '保存成功',
              type: 'success'
            })
            if (this.closeEdit) {
              this.closeEdit()
            }
          })
          .catch(error => {
            console.log(error)
          })
          .finally(() => {
            this.saveLoading = false
          })
      } else {
        this.$store.dispatch('platform/add', this.value)
          .then(data => {
            this.$message({
              showClose: true,
              message: '保存成功',
              type: 'success'
            })
            if (this.closeEdit) {
              this.closeEdit()
            }
          })
          .catch(error => {
            console.log(error)
          })
          .finally(() => {
            this.saveLoading = false
          })
      }
    },
    serverGBIdChange: function() {
      if (this.value.serverGBId.length > 10) {
        this.value.serverGBDomain = this.value.serverGBId.substr(0, 10)
      }
    },
    deviceGBIdChange: function() {
      this.value.username = this.value.deviceGBId
    },
    checkExpires: function() {
      if (this.value.enable && this.value.expires === '0') {
        this.value.expires = '3600'
      }
    },
    rtcpCheckBoxChange: function(result) {
      if (result) {
        this.$message({
          showClose: true,
          message: '开启RTCP保活需要上级平台支持，可以避免无效推流',
          type: 'warning'
        })
      }
    },
    deviceGBIdExit: async function(deviceGbId) {
      let result = false
      await this.$store.dispatch('platform/exit', deviceGbId)
        .then((data) => {
          result = data
        }).catch((error) => {
          console.log(error)
        })
      return result
    },
    close: function() {
      this.closeEdit()
    }
  }
}
</script>
