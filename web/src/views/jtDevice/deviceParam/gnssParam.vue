<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="波特率" prop="gnssBaudRate">
          <el-select
            v-model="form.gnssBaudRate"
            style="width: 100%"
            placeholder="请选择波特率"
          >
            <el-option label="4800" :value="0" />
            <el-option label="19200" :value="1" />
            <el-option label="38400" :value="2" />
            <el-option label="57600" :value="3" />
            <el-option label="115200" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="输出频率" prop="gnssOutputFrequency">
          <el-select
            v-model="form.gnssOutputFrequency"
            style="width: 100%"
            placeholder="请选择输出频率"
          >
            <el-option label="500ms" :value="0" />
            <el-option label="1000ms" :value="1" />
            <el-option label="2000ms" :value="2" />
            <el-option label="3000ms" :value="3" />
            <el-option label="4000ms" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="采集频率(秒)" prop="gnssCollectionFrequency">
          <el-input v-model="form.gnssCollectionFrequency" />
        </el-form-item>
        <el-form-item label="上传方式" prop="gnssDataUploadMethod">
          <el-select
            v-model="form.gnssDataUploadMethod"
            style="width: 100%"
            placeholder="请选择上传方式"
          >
            <el-option label="本地存储, 不上传" :value="0" />
            <el-option label="按时间间隔上传" :value="1" />
            <el-option label="按距离间隔上传" :value="2" />
            <el-option label="按累计时间上传, 达到传输时间后自动停止上传" :value="11" />
            <el-option label="按累计距离上传, 达到距离后自动停止上传" :value="12" />
            <el-option label="按累计条数上传, 达到上传条数后自动停止上传" :value="13" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.gnssDataUploadMethod > 0" :label="gnssDataUploadMethodUnitLable" prop="gnssDataUploadMethodUnit">
          <el-input v-model="form.gnssDataUploadMethodUnit" />
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
  components: {},
  computed: {
    gnssDataUploadMethodUnitLable(){
      switch (this.form.gnssDataUploadMethod) {
        case 1:
        case 11:
          return '上传设置（秒）'
        case 2:
        case 12:
          return '上传设置（米）'
        case 13:
          return '上传设置（条）'
        default:
          return '上传设置'
      }
    }
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
