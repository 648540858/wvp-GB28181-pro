<template>
  <div id="dhCameraConfigPage">
    <el-page-header content="相机配置" @back="$emit('close')" />
    <div class="camera-config-body">
      <div class="config-sidebar">
        <el-menu :default-active="activeTab" @select="handleMenuSelect">
          <el-menu-item index="base">
            <i class="iconfont icon-wxbzhuye" style="margin-right: 10px" />
            <span>基础属性</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="config-content">
        <basicPropertyConfig v-if="activeTab === 'base'" :device-id="deviceId" :channel-device-id="channelDeviceId" />
      </div>
    </div>
  </div>
</template>

<script>
import basicPropertyConfig from './basicPropertyConfig.vue'

export default {
  name: 'CameraConfigPage',
  components: { basicPropertyConfig },
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      activeTab: 'base'
    }
  },
  methods: {
    handleMenuSelect(index) {
      this.activeTab = index
    }
  }
}
</script>

<style scoped>
#dhCameraConfigPage {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.camera-config-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding-top: 16px;
}
.config-sidebar {
  width: 160px;
  flex: none;
  border-right: 1px solid #e6e6e6;
}
.config-sidebar .el-menu {
  border-right: none;
}
.config-content {
  flex: 1;
  overflow: auto;
  padding: 0 16px;
}
.placeholder-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
  font-size: 16px;
}
</style>
