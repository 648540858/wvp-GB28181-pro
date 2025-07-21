<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px);">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="里程表读数(1/10km)" prop="mileage">
          <el-input type="number" v-model="form.mileage" />
        </el-form-item>
        <el-form-item label="省域ID" prop="provincialId">
          <el-input v-model="form.provincialId" />
        </el-form-item>
        <el-form-item label="市域ID" prop="cityId">
          <el-input v-model="form.cityId" />
        </el-form-item>
        <el-form-item label="机动车号牌" prop="licensePlate">
          <el-input v-model="form.licensePlate" />
        </el-form-item>
        <el-form-item label="车牌颜色" prop="licensePlateColor">
          <el-select
            v-model="form.licensePlateColor"
            style="width: 100%"
            placeholder="请选择车牌颜色"
          >
            <el-option label="未上牌" :value="0" />
            <el-option label="蓝色" :value="1" />
            <el-option label="黄色" :value="2" />
            <el-option label="黑色" :value="3" />
            <el-option label="白色" :value="4" />
            <el-option label="绿色" :value="5" />
            <el-option label="农黄色" :value="91" />
            <el-option label="农绿色" :value="92" />
            <el-option label="黄绿色" :value="93" />
            <el-option label="渐变绿" :value="94" />
          </el-select>
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
