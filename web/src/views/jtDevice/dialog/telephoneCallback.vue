<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="电话回拨"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        <el-form >
          <el-form-item label="标志">
            <el-radio-group v-model="form.sign">
              <el-radio :label="0">普通通话</el-radio>
              <el-radio :label="1">监听</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="回拨电话号码">
            <el-input type="input" v-model="form.destPhoneNumber" ></el-input>
          </el-form-item>
          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">回拨</el-button>
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
        sign: 0, // 标志: 0:普通通话,1:监听
        destPhoneNumber: null // 回拨电话号码
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
      this.$store.dispatch('jtDevice/telephoneCallback', this.form)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: '发送成功'
          })
          this.close()
        })
    }
  }
}
</script>
