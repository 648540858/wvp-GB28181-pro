<template>
  <div id="addUser" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="添加用户"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="passwordForm" :model="form" :rules="rules" status-icon label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" autocomplete="off" />
          </el-form-item>
          <el-form-item label="用户类型" prop="roleId">
            <el-select v-model="form.roleId" placeholder="请选择" style="width: 100%">
              <el-option
                v-for="item in options"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" autocomplete="off" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" autocomplete="off" />
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
  name: 'AddUser',
  directives: { elDragDialog },
  props: {},
  data() {
    const validatePass1 = (rule, value, callback) => {
      const username = this.form.username || ''
      if (!value) {
        callback(new Error('请输入新密码'))
      } else if (value.length < 10) {
        callback(new Error('密码长度至少10位'))
      } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,./]).{10,}$/.test(value)) {
        callback(new Error('密码必须同时包含大写字母、小写字母、数字和特殊符号'))
      } else if (username && value.toLowerCase().includes(username.toLowerCase())) {
        callback(new Error('密码不能包含用户名'))
      } else {
        if (this.form.confirmPassword !== '') {
          this.$refs.passwordForm.validateField('confirmPassword')
        }
        callback()
      }
    }
    const validatePass2 = (rule, value, callback) => {
      if (this.form.confirmPassword === '') {
        callback(new Error('请再次输入密码'))
      } else if (this.form.confirmPassword !== this.form.password) {
        callback(new Error('两次输入密码不一致!'))
      } else {
        callback()
      }
    }
    return {
      options: [],
      loading: false,
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      form: {
        username: null,
        password: null,
        roleId: null,
        confirmPassword: null
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        roleId: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
        password: [{ required: true, validator: validatePass1, trigger: 'blur' }],
        confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {
    this.getAllRole()
  },
  methods: {
    openDialog: function(callback) {
      this.listChangeCallback = callback
      this.showDialog = true
    },
    onSubmit: function() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.$store.dispatch('user/add', {
          username: this.form.username,
          password: this.form.password,
          roleId: this.form.roleId
        })
          .then(data => {
            this.$message({
              showClose: true,
              message: '添加成功',
              type: 'success'
            })
            this.showDialog = false
            this.listChangeCallback()
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
      this.form.username = null
      this.form.password = null
      this.form.roleId = null
      this.form.confirmPassword = null
    },
    getAllRole: function() {
      this.loading = true
      this.$store.dispatch('role/getAll').then(data => {
        this.loading = false
        this.options = data
      })
    }
  }
}
</script>
