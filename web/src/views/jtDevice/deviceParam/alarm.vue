<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 90%; margin: 0 auto; ">
        <el-form-item label="报警屏蔽字" prop="alarmMaskingWord">
          <alarmSign v-model="form.alarmMaskingWord"></alarmSign>
        </el-form-item>
        <el-form-item label="报警发送文本SMS开关" prop="alarmSendsTextSmsSwitch">
          <alarmSign v-model="form.alarmSendsTextSmsSwitch"></alarmSign>
        </el-form-item>
        <el-form-item label="报警拍摄开关" prop="alarmShootingSwitch">
          <alarmSign v-model="form.alarmShootingSwitch"></alarmSign>
        </el-form-item>
        <el-form-item label="报警拍摄存储标志" prop="alarmShootingStorageFlags">
          <alarmSign v-model="form.alarmShootingStorageFlags"></alarmSign>
        </el-form-item>
        <el-form-item label="关键标志" prop="KeySign">
          <alarmSign v-model="form.KeySign"></alarmSign>
        </el-form-item>
        <el-form-item label="电子围栏半径(米)" prop="fenceRadius">
          <el-input v-model="form.fenceRadius" clearable />
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
import alarmSign from './alarmSign.vue'

export default {
  name: 'communication',
  components: {
    alarmSign
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {
        alarmMaskingWord: {},
        alarmSendsTextSmsSwitch: {},
        alarmShootingSwitch: {},
        alarmShootingStorageFlags: {},
        KeySign: {}
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
