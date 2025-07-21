<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px);">
      <el-form ref="form" :model="form" label-width="240px" style="width: 60%; margin: 0 auto">
        <el-form-item label="实时流编码模式" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.liveStreamCodeRateType"
            style="width: 100%"
            placeholder="请选择实时流编码模式"
          >
            <el-option label="CBR( 固定码率)" :value="0" />
            <el-option label="BR( 可变码率)" :value="1" />
            <el-option label="ABR( 平均码率)" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="实时流分辨率" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.liveStreamResolving"
            style="width: 100%"
            placeholder="请选择实时流分辨率"
          >
            <el-option label="QCIF( 164×144 )" :value="0" />
            <el-option label="CIF( 360×288 )" :value="1" />
            <el-option label="WCIF( 480×288 )" :value="2" />
            <el-option label="D1( 720x576 )" :value="2" />
            <el-option label="WD1( 960×576 )" :value="2" />
            <el-option label="720P( 1280×720 )" :value="2" />
            <el-option label="1080P( 1920×1080 )" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="实时流关键帧间隔" prop="chroma">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.liveStreamIInterval" show-input :min="1" :max="1000" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="实时流目标码率" prop="liveStreamFrameRate">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.liveStreamFrameRate" show-input :min="1" :max="120" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="实时流目标码率( kbps)" prop="liveStreamCodeRate">
          <el-input type="number" v-model="form.videoParam.liveStreamCodeRate" />
        </el-form-item>


        <el-form-item label="存储流编码模式" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.storageStreamCodeRateType"
            style="width: 100%"
            placeholder="请选择存储流编码模式"
          >
            <el-option label="CBR( 固定码率)" :value="0" />
            <el-option label="BR( 可变码率)" :value="1" />
            <el-option label="ABR( 平均码率)" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储流分辨率" prop="topSpeed" >
          <el-select
            v-model="form.videoParam.storageStreamResolving"
            style="width: 100%"
            placeholder="请选择存储流分辨率"
          >
            <el-option label="QCIF( 164×144 )" :value="0" />
            <el-option label="CIF( 360×288 )" :value="1" />
            <el-option label="WCIF( 480×288 )" :value="2" />
            <el-option label="D1( 720x576 )" :value="2" />
            <el-option label="WD1( 960×576 )" :value="2" />
            <el-option label="720P( 1280×720 )" :value="2" />
            <el-option label="1080P( 1920×1080 )" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储流关键帧间隔" prop="chroma">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.storageStreamIInterval" show-input :min="1" :max="1000" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="存储流目标帧率" prop="liveStreamFrameRate">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.videoParam.storageStreamFrameRate" show-input :min="1" :max="120" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="存储流目标码率(kbps)" prop="liveStreamCodeRate">
          <el-input type="number" v-model="form.videoParam.storageStreamCodeRate" />
        </el-form-item>
        <el-form-item label="特殊报警录像存储阈值(百分比)" prop="storageLimit">
          <div style="padding: 0 0 0 5px">
            <el-slider v-model="form.alarmRecordingParam.storageLimit" show-input :min="1" :max="99" :step="1"/>
          </div>
        </el-form-item>
        <el-form-item label="特殊报警录像持续时间(分钟)" prop="duration">
          <el-input type="number" v-model="form.videoParam.duration" />
        </el-form-item>
        <el-form-item label="特殊报警标识起始时间(分钟)" prop="startTime">
          <el-input type="number" v-model="form.videoParam.startTime" />
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
          if (!data.videoParam) {
            data.videoParam = {}
          }
          if (!data.alarmRecordingParam) {
            data.alarmRecordingParam = {}
          }
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
