<template>
  <div id="changePassword" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="修改密码"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="passwordForm" :model="passwordForm" :rules="rules" status-icon label-width="80px">
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" autocomplete="off" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" autocomplete="off" />
          </el-form-item>

          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit">保存</el-button>
              <el-button @click="close">取消</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ChangePasswordForAdmin',
  directives: { elDragDialog },
  props: {},
  data() {
    const validatePass1 = (rule, value, callback) => {
      const username = (this.form && this.form.username) || ''
      if (!value) {
        callback(new Error('请输入新密码'))
      } else if (value.length < 10) {
        callback(new Error('密码长度至少10位'))
      } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{10,}$/.test(value)) {
        callback(new Error('密码必须同时包含大写字母、小写字母、数字和特殊符号'))
      } else if (username && value.toLowerCase().includes(username.toLowerCase())) {
        callback(new Error('密码不能包含用户名'))
      } else {
        if (this.confirmPassword !== '') {
          this.$refs.passwordForm.validateField('confirmPassword')
        }
        callback()
      }
    }
    const validatePass2 = (rule, value, callback) => {
      if (this.confirmPassword === '') {
        callback(new Error('请再次输入密码'))
      } else if (this.confirmPassword !== this.newPassword) {
        callback(new Error('两次输入密码不一致!'))
      } else {
        callback()
      }
    }
    return {
      passwordForm: {
        newPassword: null,
        confirmPassword: null
      },
      userId: null,
      showDialog: false,
      isLoging: false,
      listChangeCallback: null,
      form: {},
      rules: {
        newPassword: [{ required: true, validator: validatePass1, trigger: 'blur' }],
        confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }]
      }
    }
  },
  computed: {
    newPassword: function() {
      return this.passwordForm.newPassword
    },
    confirmPassword: function() {
      return this.passwordForm.confirmPassword
    }
  },
  created() {},
  methods: {
    openDialog: function(row, callback) {
      console.log(row)
      this.showDialog = true
      this.listChangeCallback = callback
      if (row != null) {
        this.form = row
      }
    },
    onSubmit: function() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.$store.dispatch('user/changePasswordForAdmin', {
          password: this.passwordForm.newPassword,
          userId: this.form.id
        })
          .then(data => {
            this.$message({
              showClose: true,
              message: '修改成功',
              type: 'success'
            })
            this.showDialog = false
            if (this.listChangeCallback) {
              this.listChangeCallback()
            }
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      })
    },
    close: function() {
      this.showDialog = false
      this.passwordForm.newPassword = null
      this.passwordForm.confirmPassword = null
      this.userId = null
      this.adminId = null
    }
  }
}
</script>
