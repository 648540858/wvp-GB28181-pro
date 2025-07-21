<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px);">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item  label="通道1采集时间间隔(毫秒)" prop="canCollectionTimeForChannel1">
          <el-input v-model="form.canCollectionTimeForChannel1" placeholder="通道1采集时间间隔, 单位为毫秒(ms), 0表示不采集"/>
        </el-form-item>
        <el-form-item  label="通道1上传时间间隔(秒)" prop="canUploadIntervalForChannel1">
          <el-input v-model="form.canUploadIntervalForChannel1" placeholder="通道1 上传时间间隔, 单位为秒(s), 0表示不上传"/>
        </el-form-item>
        <el-form-item  label="通道2采集时间间隔(毫秒)" prop="canCollectionTimeForChannel2">
          <el-input v-model="form.canCollectionTimeForChannel2" placeholder="通道2采集时间间隔, 单位为毫秒(ms), 0表示不采集"/>
        </el-form-item>
        <el-form-item  label="通道2上传时间间隔(秒)" prop="canUploadIntervalForChannel2">
          <el-input v-model="form.canUploadIntervalForChannel2" placeholder="通道2 上传时间间隔, 单位为秒(s), 0表示不上传"/>
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
