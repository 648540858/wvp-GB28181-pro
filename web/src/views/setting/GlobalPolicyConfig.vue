<template>
  <div class="global-policy-panel">
    <user-setting-config ref="userSetting" />
  </div>
</template>

<script>
import UserSettingConfig from './UserSettingConfig.vue'

export default {
  name: 'GlobalPolicyConfig',
  components: { UserSettingConfig },
  methods: {
    // 供外层刷新按钮调用
    reload() {
      if (this.$refs.userSetting && typeof this.$refs.userSetting.reload === 'function') {
        this.$refs.userSetting.reload()
      }
    }
  }
}
</script>

<style scoped>
.global-policy-panel {
  width: 100%;
}
</style>
