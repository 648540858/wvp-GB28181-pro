<template>
  <div class="login-container" :style="{ backgroundImage: 'url(/static/images/bg19.webp)' }">
    <main class="login-shell">
      <el-form
        ref="loginForm"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        auto-complete="on"
        label-position="top"
        @keydown.enter.prevent="handleLogin"
      >
        <div class="brand-header">
          <span class="brand-icon" aria-hidden="true">
            <svg-icon icon-class="live" />
          </span>
          <div>
            <h1 class="title">WVP视频平台</h1>
            <p class="subtitle">视频监控管理与运维平台</p>
          </div>
        </div>

        <el-form-item prop="username">
          <label class="field-label" for="login-username">用户名</label>
          <el-input
            id="login-username"
            ref="username"
            v-model="loginForm.username"
            placeholder="请输入用户名"
            name="username"
            type="text"
            tabindex="1"
            auto-complete="username"
          >
            <svg-icon slot="prefix" icon-class="user" class-name="input-icon" />
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <label class="field-label" for="login-password">密码</label>
          <el-input
            :key="passwordType"
            id="login-password"
            ref="password"
            v-model="loginForm.password"
            :type="passwordType"
            placeholder="请输入密码"
            name="password"
            tabindex="2"
            auto-complete="current-password"
          >
            <svg-icon slot="prefix" icon-class="password" class-name="input-icon" />
            <button
              slot="suffix"
              class="password-toggle"
              type="button"
              :aria-label="passwordType === 'password' ? '显示密码' : '隐藏密码'"
              @click="showPwd"
            >
              <svg-icon :icon-class="passwordType === 'password' ? 'eye' : 'eye-open'" />
            </button>
          </el-input>
        </el-form-item>

        <el-button class="login-button" :loading="loading" type="primary" native-type="button" @click="handleLogin">登录</el-button>
      </el-form>
    </main>
  </div>
</template>

<script>
import { validUsername } from '@/utils/validate'

export default {
  name: 'Login',
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!value || !validUsername(value)) {
        callback(new Error('请输入用户名'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      value ? callback() : callback(new Error('请输入密码'))
    }
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', validator: validateUsername }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      loading: false,
      passwordType: 'password',
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  methods: {
    showPwd() {
      if (this.passwordType === 'password') {
        this.passwordType = ''
      } else {
        this.passwordType = 'password'
      }
      this.$nextTick(() => {
        this.$refs.password.focus()
      })
    },
    handleLogin() {
      if (this.loading) return
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          this.$store.dispatch('user/login', this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || '/' })
          }).catch(error => {
            this.$message({
              showClose: true,
              message: this.getLoginErrorMessage(error),
              type: 'error'
            })
          }).finally(() => {
            this.loading = false
          })
        } else {
          this.$message({
            showClose: true,
            message: '请输入用户名和密码',
            type: 'warning'
          })
          return false
        }
      })
    },
    getLoginErrorMessage(error) {
      if (typeof error === 'string') {
        return error
      }
      const responseData = error && error.response && error.response.data
      if (responseData && (responseData.msg || responseData.message)) {
        return responseData.msg || responseData.message
      }
      if (error && error.message === 'Network Error') {
        return '无法连接到服务器，请检查网络或服务状态'
      }
      if (error && error.code === 'ECONNABORTED') {
        return '登录请求超时，请稍后重试'
      }
      return (error && error.message) || '登录失败，请检查用户名和密码'
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  position: relative;
  min-height: 100vh;
  width: 100%;
  background-color: #172b3f;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  overflow: hidden;
  user-select: none;

  &::before {
    position: absolute;
    inset: 0;
    content: '';
    background: rgba(15, 35, 54, 0.62);
  }
}

.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32px;
}

.login-form {
  width: 420px;
  max-width: 100%;
  padding: 40px;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(223, 227, 232, 0.9);
  border-radius: var(--wvp-radius-lg);
  box-shadow: 0 16px 40px rgba(15, 35, 54, 0.24);

  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  :deep(.el-input__inner) {
    height: 44px;
    padding-left: 40px;
    padding-right: 44px;
    border-radius: 4px;
  }

  :deep(.el-input__prefix) {
    left: 12px;
    display: flex;
    align-items: center;
  }

  :deep(.el-input__suffix) {
    right: 4px;
    display: flex;
    align-items: center;
  }
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.brand-icon {
  display: inline-flex;
  flex: 0 0 44px;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #fff;
  font-size: 24px;
  background: var(--wvp-primary);
  border-radius: var(--wvp-radius-md);
}

.title {
  margin: 0;
  color: var(--wvp-text-primary);
  font-size: 22px;
  font-weight: 600;
  line-height: 1.4;
}

.subtitle {
  margin: 2px 0 0;
  color: var(--wvp-text-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  color: var(--wvp-text-primary);
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
}

.input-icon {
  width: 18px;
  height: 18px;
  color: var(--wvp-text-placeholder);
}

.password-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 40px;
  padding: 0;
  color: var(--wvp-text-regular);
  font-size: 17px;
  background: transparent;
  border: 0;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: var(--wvp-primary);
  }
}

.login-button {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  font-size: 15px;
  border-radius: var(--wvp-radius-md);
}

@media (max-width: 480px) {
  .login-shell {
    padding: 16px;
  }

  .login-form {
    padding: 32px 24px;
  }
}
</style>
