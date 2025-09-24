<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="监控平台电话号码" prop="platformPhoneNumber">
          <el-input v-model="form.platformPhoneNumber" clearable />
        </el-form-item>
        <el-form-item label="复位电话号码" prop="phoneNumberForFactoryReset">
          <el-input v-model="form.phoneNumberForFactoryReset" clearable />
        </el-form-item>
        <el-form-item label="监控平台SMS电话号码" prop="phoneNumberForSms">
          <el-input v-model="form.phoneNumberForSms" clearable />
        </el-form-item>
        <el-form-item label="接收终端SMS文本报警号码" prop="phoneNumberForReceiveTextAlarm">
          <el-input v-model="form.phoneNumberForReceiveTextAlarm" clearable />
        </el-form-item>
        <el-form-item label="终端电话接听策略" prop="locationReportingStrategy">
          <el-select v-model="form.locationReportingStrategy" style="float: left; width: 100%">
            <el-option label="自动接听" :value="0">定时汇报</el-option>
            <el-option label="ACC ON时自动接听 ,OFF时手动接听" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="每次最长通话时间(秒)" prop="longestCallTimeForPerSession">
          <el-input v-model="form.longestCallTimeForPerSession" clearable />
        </el-form-item>
        <el-form-item label="当月最长通话时间(秒)" prop="longestCallTimeInMonth">
          <el-input v-model="form.longestCallTimeInMonth" clearable />
        </el-form-item>
        <el-form-item label="监听电话号码" prop="phoneNumbersForListen">
          <el-input v-model="form.phoneNumbersForListen" clearable />
        </el-form-item>
        <el-form-item label="监管平台特权短信号码" prop="privilegedSMSNumber">
          <el-input v-model="form.privilegedSMSNumber" clearable />
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
