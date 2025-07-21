<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form v-loading="isLoading" ref="form" :model="form" label-width="240px" style="width: 90%; margin: 0 auto; ">
        <el-form-item label="报警屏蔽字" prop="alarmMaskingWord">
          <alarmSign v-if="form.alarmMaskingWord" :fatherValue="form.alarmMaskingWord" @change="(data)=>{form.alarmMaskingWord = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="报警发送文本SMS开关" prop="alarmSendsTextSmsSwitch">
          <alarmSign v-if="form.alarmSendsTextSmsSwitch" :fatherValue="form.alarmSendsTextSmsSwitch" @change="(data)=>{form.alarmSendsTextSmsSwitch = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="报警拍摄开关" prop="alarmShootingSwitch">
          <alarmSign v-if="form.alarmShootingSwitch" :fatherValue="form.alarmShootingSwitch" @change="(data)=>{form.alarmShootingSwitch = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="报警拍摄存储标志" prop="alarmShootingStorageFlags">
          <alarmSign v-if="form.alarmShootingStorageFlags" :fatherValue="form.alarmShootingStorageFlags" @change="(data)=>{form.alarmShootingStorageFlags = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="关键标志" prop="keySign">
          <alarmSign v-if="form.keySign" :fatherValue="form.keySign" @change="(data)=>{form.keySign = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="电子围栏半径(米)" prop="fenceRadius">
          <el-input type="number" v-if="form.fenceRadius" v-model="form.fenceRadius" placeholder="请输入电子围栏半径"  clearable />
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
        alarmMaskingWord: null,
        alarmSendsTextSmsSwitch: null,
        alarmShootingSwitch: null,
        alarmShootingStorageFlags: null,
        keySign: null,
        fenceRadius: null
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
          // this.form.alarmMaskingWord = data.alarmMaskingWord
          // this.form.alarmSendsTextSmsSwitch = data.alarmSendsTextSmsSwitch
          // this.form.alarmShootingSwitch = data.alarmShootingSwitch
          // this.form.alarmShootingStorageFlags = data.alarmShootingStorageFlags
          // this.form.keySign = data.keySign
          // this.$forceUpdate()
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    formChange: function(data) {
      this.form = data
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
