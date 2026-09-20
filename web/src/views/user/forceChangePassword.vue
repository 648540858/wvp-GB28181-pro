<template>
  <div class="force-change-container">
    <div
      style="justify-content: center;
      align-items: center;
      width: 100%;
      height: 100vh;
      display: flex;
      background-image: url(/static/images/bg19.webp);
      background-position: center center;
      background-repeat: no-repeat;
      background-size: cover;"
    >
      <el-form ref="passwordForm" :model="passwordForm" :rules="rules" class="force-form" label-position="left">
        <div class="title-container">
          <h3 class="title">首次登录请修改默认密码</h3>
          <p class="sub-title">出于安全考虑，使用默认密码登录后必须修改密码才能继续使用系统</p>
        </div>

        <el-form-item prop="oldPassword" label="旧密码">
          <el-input
            :key="oldPasswordType"
            v-model="passwordForm.oldPassword"
            :type="oldPasswordType"
            placeholder="请输入原密码"
            autocomplete="off"
          />
          <span class="show-pwd" @click="showOldPwd">
            <svg-icon :icon-class="oldPasswordType === 'password' ? 'eye' : 'eye-open'" />
          </span>
        </el-form-item>

        <el-form-item prop="password" label="新密码">
          <el-input
            :key="passwordType"
            v-model="passwordForm.password"
            :type="passwordType"
            placeholder="至少10位，含大小写字母、数字及特殊符号"
            autocomplete="off"
          />
          <span class="show-pwd" @click="showPwd">
            <svg-icon :icon-class="passwordType === 'password' ? 'eye' : 'eye-open'" />
          </span>
        </el-form-item>

        <el-form-item prop="confirmPassword" label="确认密码">
          <el-input
            :key="confirmPasswordType"
            v-model="passwordForm.confirmPassword"
            :type="confirmPasswordType"
            placeholder="请再次输入新密码"
            autocomplete="off"
            @keyup.enter.native="handleSubmit"
          />
          <span class="show-pwd" @click="showConfirmPwd">
            <svg-icon :icon-class="confirmPasswordType === 'password' ? 'eye' : 'eye-open'" />
          </span>
        </el-form-item>

        <el-button :loading="loading" type="primary" style="width:100%;margin-top:10px;" @click.native.prevent="handleSubmit">修改密码</el-button>
        <el-button type="text" style="width:100%; margin-top:10px; margin-left: 0" @click.native.prevent="handleLogout">退出登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script>
import crypto from 'crypto'

export default {
  name: 'ForceChangePassword',
  data() {
    const validateOldPassword = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入原密码'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      const username = this.$store.state.user.name || ''
      if (!value) {
        callback(new Error('请输入新密码'))
      } else if (value.length < 10) {
        callback(new Error('密码长度至少10位'))
      } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,.\/]).{10,}$/.test(value)) {
        callback(new Error('密码必须同时包含大写字母、小写字母、数字和特殊符号'))
      } else if (username && value.toLowerCase().includes(username.toLowerCase())) {
        callback(new Error('密码不能包含用户名'))
      } else {
        if (this.passwordForm.confirmPassword !== '') {
          this.$refs.passwordForm.validateField('confirmPassword')
        }
        callback()
      }
    }
    const validateConfirm = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请再次输入新密码'))
      } else if (value !== this.passwordForm.password) {
        callback(new Error('两次输入密码不一致!'))
      } else {
        callback()
      }
    }
    return {
      passwordForm: {
        oldPassword: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        oldPassword: [{ required: true, trigger: 'blur', validator: validateOldPassword }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }],
        confirmPassword: [{ required: true, trigger: 'blur', validator: validateConfirm }]
      },
      loading: false,
      oldPasswordType: 'password',
      passwordType: 'password',
      confirmPasswordType: 'password'
    }
  },
  methods: {
    showOldPwd() {
      this.oldPasswordType = this.oldPasswordType === 'password' ? '' : 'password'
    },
    showPwd() {
      this.passwordType = this.passwordType === 'password' ? '' : 'password'
    },
    showConfirmPwd() {
      this.confirmPasswordType = this.confirmPasswordType === 'password' ? '' : 'password'
    },
    handleSubmit() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.loading = true
        this.$store.dispatch('user/changePassword', {
          oldPassword: crypto.createHash('md5').update(this.passwordForm.oldPassword, 'utf8').digest('hex'),
          password: this.passwordForm.password
        }).then(() => {
          this.$message({
            showClose: true,
            message: '密码修改成功，请重新登录',
            type: 'success'
          })
          this.loading = false
          this.$store.dispatch('user/logout').then(() => {
            this.$router.push('/login')
          })
        }).catch((error) => {
          this.loading = false
          this.$message({
            showClose: true,
            message: error || '密码修改失败',
            type: 'error'
          })
        })
      })
    },
    handleLogout() {
      this.$store.dispatch('user/logout').then(() => {
        this.$router.push('/login')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.force-change-container {
  min-height: 100%;
  width: 100%;
  background-color: #162e46;
  overflow: hidden;

  .force-form {
    position: relative;
    width: 520px;
    max-width: 100%;
    padding: 120px 35px 0;
    margin: 0 auto;
    overflow: hidden;
    border-radius: 24px;
    border: 1px solid rgba(160, 174, 192, 0.25);
    -webkit-backdrop-filter: blur(30px);
    backdrop-filter: blur(30px);
  }

  .title-container {
    position: relative;
    margin-bottom: 30px;

    .title {
      font-size: 22px;
      color: #eee;
      margin: 0 0 10px 0;
      text-align: center;
      font-weight: bold;
    }

    .sub-title {
      font-size: 13px;
      color: #aaa;
      margin: 0;
      text-align: center;
    }
  }

  .show-pwd {
    position: absolute;
    right: 10px;
    top: 7px;
    font-size: 16px;
    color: #eee;
    cursor: pointer;
    user-select: none;
  }

  ::v-deep .el-form-item__label {
    color: #eee !important;
  }
}
</style>
