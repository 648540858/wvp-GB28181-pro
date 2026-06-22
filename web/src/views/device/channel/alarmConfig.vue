<template>
  <div id="dhAlarmConfigPage">
    <div class="alarm-config-body">
      <div class="card-list">
        <div class="alarm-section">
          <div class="section-header">报警设置</div>
          <el-form ref="alarmSettingForm">
            <el-form-item>
              <el-button type="primary" @click="handleSetGuard">布防</el-button>
              <el-button type="warning" @click="handleResetGuard">撤防</el-button>
              <el-button type="danger" @click="handleResetAlarm">复位</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AlarmConfigPage',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {

    }
  },
  mounted() {},
  methods: {
    handleSetGuard() {
      this.$confirm('确认对该通道执行布防操作？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/setGuard', this.deviceId).then(() => {
          this.$message.success('布防成功')
        })
      }).catch(() => {})
    },
    handleResetGuard() {
      this.$confirm('确认对该通道执行撤防操作？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/resetGuard', this.deviceId).then(() => {
          this.$message.success('撤防成功')
        })
      }).catch(() => {})
    },
    handleResetAlarm() {
      this.$confirm('确认对该通道执行复位操作？', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/resetAlarm', {
          deviceId: this.deviceId,
          channelId: this.channelDeviceId
        }).then(() => {
          this.$message.success('复位成功')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
#dhAlarmConfigPage {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.alarm-config-body {
  flex: 1;
  padding-top: 16px;
  overflow: auto;
}

.card-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-content: flex-start;
}

.alarm-section {
  width: 380px;
  flex-shrink: 0;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 16px;
}

.section-header {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.alarm-section .el-select,
.alarm-section .el-input-number {
  width: 100%;
}

.alarm-section .el-button + .el-button {
  margin-left: 12px;
}

</style>
