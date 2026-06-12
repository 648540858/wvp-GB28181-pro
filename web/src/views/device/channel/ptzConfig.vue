<template>
  <div id="dhPtzConfigPage">
    <el-page-header content="云台设置" @back="$emit('close')" />
    <div class="ptz-config-body">
      <div class="config-sidebar">
        <el-menu :default-active="activeTab" @select="handleMenuSelect">
          <el-menu-item index="preset">
            <i class="el-icon-map-location" style="margin-right: 6px" />
            <span>预置点</span>
          </el-menu-item>
          <el-menu-item index="cruise">
            <i class="el-icon-s-order" style="margin-right: 6px" />
            <span>巡航组</span>
          </el-menu-item>
          <el-menu-item index="scan">
            <i class="iconfont icon-slider-right" style="margin-right: 6px" />
            <span>线性扫描</span>
          </el-menu-item>
          <el-menu-item index="switch">
            <i class="el-icon-s-tools" style="margin-right: 6px" />
            <span>辅助开关</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="content-wrapper">
        <div class="player-panel">
          <playerPtzPanel :device-id="deviceId" :channel-device-id="channelDeviceId" />
        </div>
        <div class="tab-panel">
          <ptzPresetConfig v-if="activeTab === 'preset'" :device-id="deviceId" :channel-device-id="channelDeviceId" />
          <ptzCruiseConfig v-if="activeTab === 'cruise'" :device-id="deviceId" :channel-device-id="channelDeviceId" />
          <ptzScanConfig v-if="activeTab === 'scan'" :device-id="deviceId" :channel-device-id="channelDeviceId" />
          <ptzSwitchConfig v-if="activeTab === 'switch'" :device-id="deviceId" :channel-device-id="channelDeviceId" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import playerPtzPanel from '../common/playerPtzPanel.vue'
import ptzPresetConfig from '../common/ptzPresetConfig.vue'
import ptzCruiseConfig from '../common/ptzCruiseConfig.vue'
import ptzScanConfig from '../common/ptzScanConfig.vue'
import ptzSwitchConfig from '../common/ptzSwitchConfig.vue'

export default {
  name: 'PtzConfigPage',
  components: { playerPtzPanel, ptzPresetConfig, ptzCruiseConfig, ptzScanConfig, ptzSwitchConfig },
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      activeTab: 'preset'
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
#dhPtzConfigPage {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.ptz-config-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding-top: 16px;
}
.config-sidebar {
  width: 140px;
  flex: none;
  border-right: 1px solid #e6e6e6;
  overflow-y: auto;
}
.config-sidebar .el-menu {
  border-right: none;
}
.content-wrapper {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.player-panel {
  width: 600px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e6e6e6;
  padding: 0 12px;
}
.tab-panel {
  flex: 1;
  overflow: auto;
  padding: 0 12px;
}
</style>
