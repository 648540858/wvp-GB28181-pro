<template>
  <div id="jtDeviceParam" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="终端参数" @back="showDevice" />
      </div>
    </div>
    <el-container v-loading="isLoading" style="height: 82vh; overflow: auto">
      <el-main style="padding: 5px; background-color: #ffffff;">
        <el-divider content-position="center">通讯参数</el-divider>
        <el-form ref="form" size="mini" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="心跳发送间隔(秒)" prop="keepaliveInterval">
            <el-input v-model="form.keepaliveInterval" clearable />
          </el-form-item>
          <el-form-item label="TCP消息应答超时(秒)" prop="tcpResponseTimeout">
            <el-input v-model="form.tcpResponseTimeout" clearable />
          </el-form-item>
          <el-form-item label="TCP消息重传次数" prop="tcpRetransmissionCount">
            <el-input v-model="form.tcpRetransmissionCount" clearable />
          </el-form-item>
          <el-form-item label="UDP消息应答超时时间(秒)" prop="udpResponseTimeout">
            <el-input v-model="form.udpResponseTimeout" clearable />
          </el-form-item>
          <el-form-item label="UDP消息重传次数" prop="udpRetransmissionCount">
            <el-input v-model="form.udpRetransmissionCount" clearable />
          </el-form-item>
          <el-form-item label="SMS 消息应答超时时间(秒)" prop="smsResponseTimeout">
            <el-input v-model="form.smsResponseTimeout" clearable />
          </el-form-item>
          <el-form-item label="SMS 消息重传次数" prop="smsRetransmissionCount">
            <el-input v-model="form.smsRetransmissionCount" clearable />
          </el-form-item>
        </el-form>
        <el-divider content-position="center">服务器参数</el-divider>
        <el-form ref="form" size="mini" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="APN(主)" prop="apnMaster">
            <el-input v-model="form.apnMaster" clearable />
          </el-form-item>
          <el-form-item label="无线通信拨号用户名(主)" prop="dialingUsernameMaster">
            <el-input v-model="form.dialingUsernameMaster" clearable />
          </el-form-item>
          <el-form-item label="无线通信拨号密码(主)" prop="dialingPasswordMaster">
            <el-input v-model="form.dialingPasswordMaster" clearable />
          </el-form-item>
          <el-form-item label="IP或域名(主)" prop="addressMaster">
            <el-input v-model="form.addressMaster" clearable />
          </el-form-item>
          <el-form-item label="APN(备)" prop="apnBackup">
            <el-input v-model="form.apnBackup" clearable />
          </el-form-item>
          <el-form-item label="无线通信拨号用户名(备)" prop="dialingUsernameBackup">
            <el-input v-model="form.dialingUsernameBackup" clearable />
          </el-form-item>
          <el-form-item label="无线通信拨号密码(备)" prop="dialingPasswordBackup">
            <el-input v-model="form.dialingPasswordBackup" clearable />
          </el-form-item>
          <el-form-item label="IP或域名(备)" prop="addressBackup">
            <el-input v-model="form.addressBackup" clearable />
          </el-form-item>

          <el-form-item label="APN(从)" prop="apnBackup">
            <el-input v-model="form.apnBackup" clearable />
          </el-form-item>
          <el-form-item label="无线通信拨号用户名(从)" prop="dialingUsernameSlave">
            <el-input v-model="form.dialingUsernameSlave" clearable />
          </el-form-item>
          <el-form-item label="无线通信拨号密码(从)" prop="dialingPasswordSlave">
            <el-input v-model="form.dialingPasswordSlave" clearable />
          </el-form-item>
          <el-form-item label="IP或域名(从)" prop="addressSlave">
            <el-input v-model="form.addressSlave" clearable />
          </el-form-item>

          <el-form-item label="IC卡认证服务器IP(主)" prop="addressIcMaster">
            <el-input v-model="form.addressIcMaster" clearable />
          </el-form-item>
          <el-form-item label="IC卡认证服务器IP(备)" prop="addressIcMaster">
            <el-input v-model="form.addressIcBackup" clearable />
          </el-form-item>
          <el-form-item label="IC卡认证服务器TCP端口" prop="tcpPortIcMaster">
            <el-input v-model="form.tcpPortIcMaster" clearable />
          </el-form-item>
          <el-form-item label="IC卡认证服务器UDP端口" prop="udpPortIcMaster">
            <el-input v-model="form.udpPortIcMaster" clearable />
          </el-form-item>
        </el-form>

        <el-divider content-position="center">位置汇报</el-divider>
        <el-form ref="form" size="mini" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="策略" prop="locationReportingStrategy">
            <el-select v-model="form.locationReportingStrategy" style="float: left; width: 100%">
              <el-option label="定时汇报" :value="0">定时汇报</el-option>
              <el-option label="定距汇报" :value="1" />
              <el-option label="定时和定距汇报" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="方案" prop="locationReportingPlan">
            <el-select v-model="form.locationReportingPlan" style="float: left; width: 100%">
              <el-option label="根据ACC状态" :value="0" />
              <el-option label="登录状态和ACC状态" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="驾驶员未登录汇报时间间隔(秒)" prop="reportingIntervalOffline">
            <el-input v-model="form.reportingIntervalOffline" clearable />
          </el-form-item>
          <el-form-item label="休眠时汇报时间间隔(秒)" prop="reportingIntervalDormancy">
            <el-input v-model="form.reportingIntervalDormancy" clearable />
          </el-form-item>
          <el-form-item label="紧急报警时汇报时间间隔(秒)" prop="reportingIntervalEmergencyAlarm">
            <el-input v-model="form.reportingIntervalEmergencyAlarm" clearable />
          </el-form-item>
          <el-form-item label="缺省时间汇报间隔(秒)" prop="reportingIntervalDefault">
            <el-input v-model="form.reportingIntervalDefault" clearable />
          </el-form-item>
          <el-form-item label="缺省距离汇报间隔(米)" prop="reportingDistanceDefault">
            <el-input v-model="form.reportingDistanceDefault" clearable />
          </el-form-item>
          <el-form-item label="驾驶员未登录汇报距离间隔(米)" prop="reportingDistanceOffline">
            <el-input v-model="form.reportingDistanceOffline" clearable />
          </el-form-item>
          <el-form-item label="休眠时汇报距离间隔(米)" prop="reportingDistanceDormancy">
            <el-input v-model="form.reportingDistanceDormancy" clearable />
          </el-form-item>
          <el-form-item label="紧急报警时汇报距离间隔(米)" prop="reportingDistanceEmergencyAlarm">
            <el-input v-model="form.reportingDistanceEmergencyAlarm" clearable />
          </el-form-item>
          <el-form-item label="拐点补传角度(度，小于180)" prop="inflectionPointAngle">
            <el-input v-model="form.inflectionPointAngle" clearable />
          </el-form-item>
        </el-form>
        <el-divider content-position="center">电话号码</el-divider>
        <el-form ref="form" size="mini" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="监控平台电话号码" prop="platformPhoneNumber">
            <el-input v-model="form.platformPhoneNumber" clearable />
          </el-form-item>
          <el-form-item label="复位电话号码" prop="phoneNumberForFactoryReset">
            <el-input v-model="form.phoneNumberForFactoryReset" clearable />
          </el-form-item>
          <el-form-item label="监控平台SMS电话号码" prop="phoneNumberForSms">
            <el-input v-model="form.phoneNumberForSms" clearable />
          </el-form-item>
          <el-form-item label="接收终端SMS文本报警号码" prop="phoneNumberForReceiveTextAlarm">
            <el-input v-model="form.phoneNumberForReceiveTextAlarm" clearable />
          </el-form-item>
          <el-form-item label="终端电话接听策略" prop="locationReportingStrategy">
            <el-select v-model="form.locationReportingStrategy" style="float: left; width: 100%">
              <el-option label="自动接听" :value="0">定时汇报</el-option>
              <el-option label="ACC ON时自动接听 ,OFF时手动接听" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="每次最长通话时间(秒)" prop="longestCallTimeForPerSession">
            <el-input v-model="form.longestCallTimeForPerSession" clearable />
          </el-form-item>
          <el-form-item label="当月最长通话时间(秒)" prop="longestCallTimeInMonth">
            <el-input v-model="form.longestCallTimeInMonth" clearable />
          </el-form-item>
          <el-form-item label="监听电话号码" prop="phoneNumbersForListen">
            <el-input v-model="form.phoneNumbersForListen" clearable />
          </el-form-item>
          <el-form-item label="监管平台特权短信号码" prop="privilegedSMSNumber">
            <el-input v-model="form.privilegedSMSNumber" clearable />
          </el-form-item>
        </el-form>

        <el-divider content-position="center">报警参数</el-divider>
        <el-form ref="form" size="mini" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="报警屏蔽字(TODO)" prop="alarmMaskingWord">
            <el-input v-model="form.alarmMaskingWord" clearable />
          </el-form-item>
          <el-form-item label="报警发送文本 SMS 开关(TODO)" prop="alarmSendsTextSmsSwitch">
            <el-input v-model="form.alarmSendsTextSmsSwitch" clearable />
          </el-form-item>
          <el-form-item label="报警拍摄开关(TODO)" prop="alarmShootingSwitch">
            <el-input v-model="form.alarmShootingSwitch" clearable />
          </el-form-item>
          <el-form-item label="报警拍摄存储标志(TODO)" prop="alarmShootingStorageFlags">
            <el-input v-model="form.alarmShootingStorageFlags" clearable />
          </el-form-item>
          <el-form-item label="关键标志(TODO)" prop="KeySign">
            <el-input v-model="form.KeySign" clearable />
          </el-form-item>
          <el-form-item label="电子围栏半径(米)" prop="fenceRadius">
            <el-input v-model="form.fenceRadius" clearable />
          </el-form-item>
        </el-form>

        <el-divider content-position="center">行驶参数</el-divider>
        <el-form ref="form" size="mini" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item v-if="form.illegalDrivingPeriods" label="违规行驶时段-开始时间(HH:mm)" prop="illegalDrivingPeriods">
            <el-input v-model="form.illegalDrivingPeriods.startTime" clearable />
          </el-form-item>
          <el-form-item v-if="form.illegalDrivingPeriods" label="违规行驶时段-结束时间(HH:mm)" prop="illegalDrivingPeriods">
            <el-input v-model="form.illegalDrivingPeriods.endTime" clearable />
          </el-form-item>
          <el-form-item label="最高速度(千米每小时)" prop="topSpeed">
            <el-input v-model="form.topSpeed" clearable />
          </el-form-item>
          <el-form-item label="超速持续时间(秒)" prop="overSpeedDuration">
            <el-input v-model="form.overSpeedDuration" clearable />
          </el-form-item>
          <el-form-item label="连续驾驶时间门限(秒)" prop="continuousDrivingTimeThreshold">
            <el-input v-model="form.continuousDrivingTimeThreshold" clearable />
          </el-form-item>
          <el-form-item label="当天累计驾驶时间门限(秒)" prop="cumulativeDrivingTimeThresholdForTheDay">
            <el-input v-model="form.cumulativeDrivingTimeThresholdForTheDay" clearable />
          </el-form-item>
          <el-form-item label="最小休息时间(秒)" prop="minimumBreakTime">
            <el-input v-model="form.minimumBreakTime" clearable />
          </el-form-item>
          <el-form-item label="最长停车时间(秒)" prop="maximumParkingTime">
            <el-input v-model="form.maximumParkingTime" clearable />
          </el-form-item>
          <el-form-item label="超速预警差值(1/10 千米每小时)" prop="overSpeedWarningDifference">
            <el-input v-model="form.overSpeedWarningDifference" clearable />
          </el-form-item>
          <el-form-item label="疲劳驾驶预警差值(秒)" prop="drowsyDrivingWarningDifference">
            <el-input v-model="form.drowsyDrivingWarningDifference" clearable />
          </el-form-item>
          <el-form-item label="碰撞报警-碰撞时间(毫秒)" prop="collisionAlarmParamsCollisionAlarmTime">
            <el-input v-model="form.collisionAlarmParams.collisionAlarmTime" clearable />
          </el-form-item>
          <el-form-item label="碰撞报警-碰撞加速度(0.1g)" prop="collisionAlarmParamsCollisionAcceleration">
            <el-input v-model="form.collisionAlarmParams.collisionAcceleration" clearable />
          </el-form-item>
          <el-form-item label="侧翻报警参数-侧翻角度(度)" prop="rolloverAlarm">
            <el-input v-model="form.rolloverAlarm" clearable />
          </el-form-item>
        </el-form>

        <el-divider content-position="center">定时拍照控制</el-divider>
        <el-form ref="form" size="mini" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="定时拍照开关-通道1" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel1" />
          </el-form-item>
          <el-form-item label="定时拍照开关-通道2" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel2" />
          </el-form-item>
          <el-form-item label="定时拍照开关-通道3" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel3" />
          </el-form-item>
          <el-form-item label="定时拍照开关-通道4" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel4" />
          </el-form-item>
          <el-form-item label="定时拍照开关-通道5" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel5" />
          </el-form-item>
          <el-form-item label="定时拍照存储-通道1" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel1" />
          </el-form-item>
          <el-form-item label="定时拍照存储-通道2" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel2" />
          </el-form-item>
          <el-form-item label="定时拍照存储-通道3" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel3" />
          </el-form-item>
          <el-form-item label="定时拍照存储-通道4" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel4" />
          </el-form-item>
          <el-form-item label="定时拍照存储-通道5" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel5" />
          </el-form-item>

          <el-form-item label="定时时间间隔" prop="timeInterval">
            <el-input v-model="form.timeInterval" clearable />
          </el-form-item>
          <el-form-item label="定时时间单位" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.timeUnit" active-text="分" inactive-text="秒" />
          </el-form-item>
        </el-form>
        <div style="float: right;">
          <el-button type="primary" @click="onSubmit">确认</el-button>
          <el-button @click="showDevice">取消</el-button>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>

export default {
  name: 'JTDeviceParam',
  components: {
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {
        collisionAlarmParams: {},
        illegalDrivingPeriods: {},
        cameraTimer: {}
      },
      rules: {
        deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }]
      },
      isLoading: false
    }
  },

  mounted() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.isLoading = true
      this.$store.dispatch('jtDevice/queryConfig', this.phoneNumber)
        .then((data) => {
          this.form = data
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    onSubmit: function() {
      this.$store.dispatch('jtDevice/setConfig', {
        phoneNumber: this.phoneNumber,
        config: this.form
      })
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
        })
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
