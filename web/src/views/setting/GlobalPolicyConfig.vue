<template>
  <div class="global-policy-panel">
    <el-tabs v-model="activeInner" class="policy-tabs">
      <el-tab-pane label="流媒体" name="media">
        <media-server-config ref="media" />
      </el-tab-pane>
      <el-tab-pane label="平台策略" name="userSetting">
        <user-setting-config ref="userSetting" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import MediaServerConfig from './MediaServerConfig.vue'
import UserSettingConfig from './UserSettingConfig.vue'

export default {
  name: 'GlobalPolicyConfig',
  components: { MediaServerConfig, UserSettingConfig },
  data() {
    return {
      activeInner: 'media'
    }
  },
  methods: {
    // 供外层刷新按钮调用, 刷新当前子页签
    reload() {
      const ref = this.activeInner === 'media' ? this.$refs.media : this.$refs.userSetting
      if (ref && typeof ref.reload === 'function') {
        ref.reload()
      }
    }
  }
}
</script>

<style scoped>
.policy-tabs >>> .el-tabs__header {
  margin-bottom: 16px;
}
</style>
