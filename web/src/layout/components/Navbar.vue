<template>
  <header class="navbar">
    <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
      <el-dropdown class="avatar-container" trigger="click">
        <button class="avatar-wrapper" type="button" aria-label="用户菜单">
          <span class="avatar-icon" aria-hidden="true">
            <svg-icon icon-class="user" />
          </span>
          <span class="user-name"><span class="welcome-prefix">欢迎，</span>{{ name }}</span>
          <ant-icon name="el-icon-arrow-down" class="el-icon-arrow-down" aria-hidden="true"  />
        </button>
        <el-dropdown-menu slot="dropdown" class="user-dropdown">
          <el-dropdown-item icon="el-icon-lock" @click="changePassword">修改密码</el-dropdown-item>
          <el-dropdown-item icon="el-icon-switch-button" divided @click="logout">注销</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <change-password-dialog ref="changePasswordDialog" />
  </header>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import ChangePasswordDialog from './dialog/changePassword.vue'

export default {
  components: {
    Breadcrumb,
    Hamburger,
    ChangePasswordDialog
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'name'
    ])
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    },
    changePassword() {
      this.$refs.changePasswordDialog.openDialog(this.logout)
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  height: var(--wvp-header-height);
  overflow: hidden;
  background: var(--wvp-surface);
  border-bottom: 1px solid var(--wvp-border-light);
  box-shadow: var(--wvp-shadow-sm);
}

.hamburger-container {
  flex: 0 0 56px;
  width: 56px;
  height: var(--wvp-header-height);
}

.breadcrumb-container {
  flex: 1 1 auto;
  min-width: 0;
}

.right-menu {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  height: 100%;
  padding: 0 16px;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  min-width: 40px;
  padding: 0 10px;
  color: var(--wvp-text-primary);
  font: inherit;
  background: transparent;
  border: 0;
  border-radius: var(--wvp-radius-md);
  cursor: pointer;
  transition: background-color var(--wvp-transition), color var(--wvp-transition);

  &:hover,
  &:focus-visible {
    color: var(--wvp-primary);
    background: var(--wvp-primary-soft);
  }
}

.avatar-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--wvp-primary);
  font-size: 16px;
  background: var(--wvp-primary-soft);
  border-radius: 50%;
}

.user-name {
  max-width: 180px;
  overflow: hidden;
  font-size: 14px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.el-icon-arrow-down {
  font-size: 12px;
}

@media (max-width: 768px) {
  .right-menu {
    padding: 0 8px;
  }

  .avatar-wrapper {
    min-width: 44px;
    height: 44px;
    padding: 0 8px;
  }

  .welcome-prefix {
    display: none;
  }
}

@media (max-width: 640px) {
  .breadcrumb-container {
    display: none;
  }

  .right-menu {
    margin-left: auto;
  }

  .user-name {
    display: none;
  }
}
</style>
