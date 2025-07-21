<template>
  <div style="width: 100%;">
    <el-checkbox label="视频信号丢失报警" v-model="form.lossSignal" @change="change"></el-checkbox>
    <el-checkbox label="视频信号遮挡报警" v-model="form.occlusionSignal" @change="change"></el-checkbox>
    <el-checkbox label="存储单元故障报警" v-model="form.storageFault" @change="change"></el-checkbox>
    <el-checkbox label="其他视频设备故障报警" v-model="form.otherDeviceFailure" @change="change"></el-checkbox>
    <el-checkbox label="客车超员报警" v-model="form.overcrowding" @change="change"></el-checkbox>
    <el-checkbox label="异常驾驶行为报警" v-model="form.abnormalDriving" @change="change"></el-checkbox>
    <el-checkbox label="特殊报警录像达到存储阈值报警" v-model="form.storageLimit" @change="change"></el-checkbox>
  </div>
</template>

<script>

export default {
  name: 'communication',
  components: {
  },
  model: {
    prop: 'fatherValue',
    event: 'change'
  },
  props: {
    fatherValue: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      form: {
        lossSignal: false,
        occlusionSignal: false,
        storageFault: false,
        otherDeviceFailure: false,
        overcrowding: false,
        abnormalDriving: false,
        storageLimit: false
      },
      isLoading: false
    }
  },

  mounted() {
    if (this.fatherValue !== null) {
      console.log(this.fatherValue)
      this.form.lossSignal = this.fatherValue.lossSignal || false
      this.form.occlusionSignal = this.fatherValue.occlusionSignal || false
      this.form.storageFault = this.fatherValue.storageFault || false
      this.form.otherDeviceFailure = this.fatherValue.otherDeviceFailure || false
      this.form.overcrowding = this.fatherValue.overcrowding || false
      this.form.abnormalDriving = this.fatherValue.abnormalDriving || false
      this.form.storageLimit = this.fatherValue.storageLimit || false
    }
  },
  methods: {
    change() {
      this.$emit('change', this.form)
    }
  }
}
</script>
