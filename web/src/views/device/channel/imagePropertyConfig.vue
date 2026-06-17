<template>
  <div id="dhImagePropertyPage">
    <div class="image-property-body">
      <div class="player-section">
        <playerPtzPanel :device-id="deviceId" :channel-device-id="channelDeviceId" />
      </div>
      <div class="config-section">
        <el-tabs v-model="activeTab" tab-position="left" class="config-tabs">
          <el-tab-pane label="OSD配置" name="osd">
            <div class="placeholder-tab">OSD配置（开发中）</div>
          </el-tab-pane>
          <el-tab-pane label="视角" name="control">
            <div class="placeholder-tab">视角（开发中）</div>
          </el-tab-pane>
          <el-tab-pane label="画面遮挡" name="pictureMask">
            <div class="placeholder-tab">画面遮挡（开发中）</div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script>
import playerPtzPanel from '../common/playerPtzPanel.vue'

export default {
  name: 'ImagePropertyPage',
  components: { playerPtzPanel },
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      activeTab: 'osd'
    }
  }
}
</script>

<style scoped>
#dhImagePropertyPage {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.image-property-body {
  display: flex;
  gap: 16px;
  min-height: 30vw;
  flex: 1;
  padding-top: 16px;
}

.player-section {
  width: 520px;
  flex: none;
  display: flex;
  flex-direction: column;
}

.config-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.config-tabs {
  flex: 1;
  min-height: 0;
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
