<template>
  <div style="width: 100%;">
  <div style="height: calc(100vh - 260px); overflow: auto">
    <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto; ">
      <el-divider content-position="center">主服务器</el-divider>
      <el-form-item label="APN(主)" prop="apnMaster">
        <el-input v-model="form.apnMaster" clearable />
      </el-form-item>
      <el-form-item label="无线通信拨号用户名(主)" prop="dialingUsernameMaster">
        <el-input v-model="form.dialingUsernameMaster" clearable />
      </el-form-item>
      <el-form-item label="无线通信拨号密码(主)" prop="dialingPasswordMaster">
        <el-input v-model="form.dialingPasswordMaster" clearable />
      </el-form-item>
      <el-form-item label="IP或域名(主)" prop="addressMaster">
        <el-input v-model="form.addressMaster" clearable />
      </el-form-item>


      <el-divider content-position="center">备份服务器</el-divider>
      <el-form-item label="APN(备)" prop="apnBackup">
        <el-input v-model="form.apnBackup" clearable />
      </el-form-item>
      <el-form-item label="无线通信拨号用户名(备)" prop="dialingUsernameBackup">
        <el-input v-model="form.dialingUsernameBackup" clearable />
      </el-form-item>
      <el-form-item label="无线通信拨号密码(备)" prop="dialingPasswordBackup">
        <el-input v-model="form.dialingPasswordBackup" clearable />
      </el-form-item>
      <el-form-item label="IP或域名(备)" prop="addressBackup">
        <el-input v-model="form.addressBackup" clearable />
      </el-form-item>


      <el-divider content-position="center">从服务器</el-divider>
      <el-form-item label="APN(从)" prop="apnBackup">
        <el-input v-model="form.apnBackup" clearable />
      </el-form-item>
      <el-form-item label="无线通信拨号用户名(从)" prop="dialingUsernameSlave">
        <el-input v-model="form.dialingUsernameSlave" clearable />
      </el-form-item>
      <el-form-item label="无线通信拨号密码(从)" prop="dialingPasswordSlave">
        <el-input v-model="form.dialingPasswordSlave" clearable />
      </el-form-item>
      <el-form-item label="IP或域名(从)" prop="addressSlave">
        <el-input v-model="form.addressSlave" clearable />
      </el-form-item>

      <el-divider content-position="center">IC卡认证服务器</el-divider>
      <el-form-item label="IC卡认证服务器IP(主)" prop="addressIcMaster">
        <el-input v-model="form.addressIcMaster" clearable />
      </el-form-item>
      <el-form-item label="IC卡认证服务器IP(备)" prop="addressIcMaster">
        <el-input v-model="form.addressIcBackup" clearable />
      </el-form-item>
      <el-form-item label="IC卡认证服务器TCP端口" prop="tcpPortIcMaster">
        <el-input v-model="form.tcpPortIcMaster" clearable />
      </el-form-item>
      <el-form-item label="IC卡认证服务器UDP端口" prop="udpPortIcMaster">
        <el-input v-model="form.udpPortIcMaster" clearable />
      </el-form-item>

    </el-form>
  </div>
    <p style="text-align: right">
      <el-button type="primary" @click="onSubmit">确认</el-button>
      <el-button @click="showDevice">取消</el-button>
    </p>
  </div>
</template>

<script>

export default {
  name: 'server',
  components: {
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {},
      isLoading: false
    }
  },

  mounted() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.isLoading = true
      this.$store.dispatch('jtDevice/queryConfig', this.phoneNumber)
        .then((data) => {
          this.form = data
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    onSubmit: function() {
      this.$emit('submit', this.form)
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
