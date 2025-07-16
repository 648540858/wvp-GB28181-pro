<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="文本信息下发"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        <el-form >
          <el-divider content-position="center">标志</el-divider>
          <el-form-item label="类型">
            <el-radio-group v-model="form.sign.type">
              <el-radio :label="1">紧急</el-radio>
              <el-radio :label="2">服务</el-radio>
              <el-radio :label="3">通知</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="终端显示器显示">
            <el-checkbox v-model="form.sign.terminalDisplay"></el-checkbox>
          </el-form-item>
          <el-form-item label="终端TTS播读">
            <el-checkbox v-model="form.sign.tts"></el-checkbox>
          </el-form-item>
          <el-form-item label="广告屏显示">
            <el-checkbox v-model="form.sign.adScreen"></el-checkbox>
          </el-form-item>
          <el-form-item label="信息类型">
            <el-radio-group v-model="form.sign.source">
              <el-radio :label="false">中心导航信息</el-radio>
              <el-radio :label="true">CAN故障码信息</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-divider content-position="center">属性</el-divider>
          <el-form-item label="文本类型">
            <el-radio-group v-model="form.textType">
              <el-radio :label="1">通知</el-radio>
              <el-radio :label="2">服务</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="消息内容">
            <el-input type="textarea" v-model="form.content" maxlength="1024" show-word-limit></el-input>
          </el-form-item>
          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">下发</el-button>
            <el-button @click="close" >取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      form: {
        phoneNumber: null,
        sign: {
          type: 3, // 1紧急,2服务,3通知
          terminalDisplay: true, // 1终端显示器显示
          tts: true, // 从选区创建新的临时文件
          adScreen: true, // 广告屏显示
          source: false // false: 中心导航信息 true CAN故障码信息
        },
        textType: 1, // 文本类型,1 = 通知 ，2 = 服务
        content: '' // 消息内容，最长为1024字节
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {

      this.showDialog = true
      this.form = {
        phoneNumber: null,
          sign: {
          type: 3, // 1紧急,2服务,3通知
            terminalDisplay: true, // 1终端显示器显示
            tts: true, // 从选区创建新的临时文件
            adScreen: true, // 广告屏显示
            source: false // false: 中心导航信息 true CAN故障码信息
        },
        textType: 1, // 文本类型,1 = 通知 ，2 = 服务
          content: '' // 消息内容，最长为1024字节
      }
      this.form.phoneNumber = data.phoneNumber
    },
    close: function() {
      this.showDialog = false
    },
    onSubmit: function() {
      this.$store.dispatch('jtDevice/sendTextMessage', this.form)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: '发送成功'
          })
          // this.close()
        })
    }
  }
}
</script>
