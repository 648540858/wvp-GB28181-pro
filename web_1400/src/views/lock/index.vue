<template>
  <div class="lock-container">
    <div class="lock-form">
      <el-input
        v-model="password"
        placeholder="请输入锁屏密码"
        type="password"
        class="input-with-select"
        @keyup.enter.native="handleLogin"
      >
        <el-button
          slot="append"
          icon="el-icon-unlock"
          @click="handleLogin"
        />
        <el-button
          slot="append"
          icon="el-icon-turn-off"
          @click="handleLogout"
        />
      </el-input>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'Lock',
  data() {
    return {
      password: ''
    }
  },
  computed: {
    ...mapGetters([
      'user_lock'
    ])
  },
  methods: {
    handleLogout() {
      this.$confirm('是否退出系统, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('user/logout').then(() => {
          this.$router.push({ path: '/login' })
        })
      }).catch(() => {
      })
    },
    handleLogin() {
      if (this.password !== this.user_lock) {
        this.password = ''
        this.$message({
          message: '解锁密码错误,请重新输入',
          type: 'error'
        })
        return
      }
      setTimeout(() => {
        this.$router.push({ path: '/' })
      }, 1000)
    }
  }
}
</script>

<style lang="scss" scoped>
.lock-container {
  height: calc(100vh);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  .lock-form {
    width: 300px;
  }
}
</style>
