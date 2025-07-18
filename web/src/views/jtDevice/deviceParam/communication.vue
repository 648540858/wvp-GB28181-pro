<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px);">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="心跳发送间隔(秒)" prop="keepaliveInterval">
          <el-input v-model="form.keepaliveInterval" clearable />
        </el-form-item>
        <el-form-item label="TCP消息应答超时(秒)" prop="tcpResponseTimeout">
          <el-input v-model="form.tcpResponseTimeout" clearable />
        </el-form-item>
        <el-form-item label="TCP消息重传次数" prop="tcpRetransmissionCount">
          <el-input v-model="form.tcpRetransmissionCount" clearable />
        </el-form-item>
        <el-form-item label="UDP消息应答超时时间(秒)" prop="udpResponseTimeout">
          <el-input v-model="form.udpResponseTimeout" clearable />
        </el-form-item>
        <el-form-item label="UDP消息重传次数" prop="udpRetransmissionCount">
          <el-input v-model="form.udpRetransmissionCount" clearable />
        </el-form-item>
        <el-form-item label="SMS 消息应答超时时间(秒)" prop="smsResponseTimeout">
          <el-input v-model="form.smsResponseTimeout" clearable />
        </el-form-item>
        <el-form-item label="SMS 消息重传次数" prop="smsRetransmissionCount">
          <el-input v-model="form.smsRetransmissionCount" clearable />
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
  name: 'communication',
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
