<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="连接到指定的服务器"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div style="padding: 0 20px 0 10px">
        <el-form label-width="110px">
          <el-form-item label="服务类型">
            <el-radio-group v-model="form.switchOn">
              <el-radio :label="false" border>指定监管平台服务器</el-radio>
              <el-radio :label="true" border>原缺省监控平台服务器</el-radio>
            </el-radio-group>
          </el-form-item>
          <div v-if="form.switchOn != null && !form.switchOn">
            <el-form-item label="平台鉴权码">
              <el-input type="input" v-model="form.authentication" ></el-input>
            </el-form-item>
            <el-form-item label="拨号点名称">
              <el-input type="input" v-model="form.name" ></el-input>
            </el-form-item>
            <el-form-item label="拨号用户名">
              <el-input type="input" v-model="form.username" ></el-input>
            </el-form-item>
            <el-form-item label="拨号密码">
              <el-input type="input" v-model="form.password" ></el-input>
            </el-form-item>
            <el-form-item label="IP地址">
              <el-input type="input" v-model="form.address" ></el-input>
            </el-form-item>
            <el-form-item label="TCP端口">
              <el-input type="input" v-model="form.tcpPort" ></el-input>
            </el-form-item>
            <el-form-item label="UDP端口">
              <el-input type="input" v-model="form.udpPort" ></el-input>
            </el-form-item>
            <el-form-item label="时限">
              <el-input type="input" v-model="form.timeLimit" ></el-input>
            </el-form-item>
          </div>

          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">确认</el-button>
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
  name: 'ConnectionServer',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      phoneNumber: null,
      form: {
        switchOn: null,
        authentication: null,
        name: null,
        username: null,
        password: null,
        address: null,
        tcpPort: null,
        udpPort: null,
        timeLimit: null,
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
      this.phoneNumber = data
      this.form = {
        switchOn: null,
        authentication: null,
        name: null,
        username: null,
        password: null,
        address: null,
        tcpPort: null,
        udpPort: null,
        timeLimit: null,
        sign: 0, // 标志: 0:普通通话,1:监听
        destPhoneNumber: null // 回拨电话号码
      }
    },
    close: function() {
      this.showDialog = false
    },
    onSubmit: function() {
      this.$store.dispatch('jtDevice/connection', {
        phoneNumber: this.phoneNumber,
        control: this.form
      })
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
