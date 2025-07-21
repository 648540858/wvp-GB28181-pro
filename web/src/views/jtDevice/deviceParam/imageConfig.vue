<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px);">
      <el-form ref="form" :model="form" label-width="240px" style="width: 60%; margin: 0 auto">
        <el-form-item label="图像/视频质量" prop="topSpeed" >
          <div style="padding: 0 14px">
            <el-slider v-model="form.qualityForVideo" show-input :height="1" :marks="qualityMarks" :min="1" :max="10" :step="1" style="width: calc(100% - 40px);"/>
          </div>
        </el-form-item>
        <el-form-item label="亮度" prop="brightness">
          <div style="padding: 0 14px">
            <el-slider v-model="form.brightness" show-input :height="1" :min="0" :max="255" :step="1" style="width: calc(100% - 40px);"/>
          </div>
        </el-form-item>
        <el-form-item label="对比度" prop="contrastRatio">
          <div style="padding: 0 14px">
            <el-slider v-model="form.contrastRatio" show-input :height="1" :min="0" :max="127" :step="1" style="width: calc(100% - 40px);"/>
          </div>
        </el-form-item>
        <el-form-item label="饱和度" prop="saturation">
          <div style="padding: 0 14px">
            <el-slider v-model="form.saturation" show-input :height="1" :min="0" :max="127" :step="1" style="width: calc(100% - 40px);"/>
          </div>
        </el-form-item>
        <el-form-item label="色度" prop="chroma">
          <div style="padding: 0 14px">
            <el-slider v-model="form.chroma" show-input :height="1" :min="0" :max="255" :step="1" style="width: calc(100% - 40px);"/>
          </div>
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
      qualityMarks: {
        1: '最优',
        10: '最差'
      },
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
<style scoped>
  >>> .el-slider__marks-text {
    margin-top: 6px;
    font-size: 12px;
    width: 2rem !important;
}
</style>
