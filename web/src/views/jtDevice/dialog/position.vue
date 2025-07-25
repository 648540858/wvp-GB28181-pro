<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="位置信息"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="height: 45rem; overflow: auto">
        <el-descriptions title="基本信息" :column="3" v-if="positionData" style="margin-bottom: 1rem;">
          <el-descriptions-item label="经度">{{ positionData.longitude }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ positionData.latitude }}</el-descriptions-item>
          <el-descriptions-item label="高程">{{ positionData.altitude }}</el-descriptions-item>
          <el-descriptions-item label="速度">{{ positionData.speed }}</el-descriptions-item>
          <el-descriptions-item label="方向">{{ positionData.direction }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ positionData.time }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="报警标志" :column="3" v-if="positionData.alarmSign" style="margin-bottom: 1rem;">
          <el-descriptions-item label="紧急报警">{{ positionData.alarmSign.urgent?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="超速报警">{{ positionData.alarmSign.alarmSpeeding?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="疲劳驾警报警">{{ positionData.alarmSign.alarmTired?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="危险驾驶行为报警">{{ positionData.alarmSign.alarmDangerous?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="GNSS模块故障报警">{{ positionData.alarmSign.alarmGnssFault?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="GNSS天线未接或被剪断报警">{{ positionData.alarmSign.alarmGnssBreak?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="GNSS天线短路报警">{{ positionData.alarmSign.alarmGnssShortCircuited?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="终端主电源欠压报警">{{ positionData.alarmSign.alarmUnderVoltage?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="终端主电源掉电报警">{{ positionData.alarmSign.alarmPowerOff?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="终端LCD或显示器故障报警">{{ positionData.alarmSign.alarmLCD?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="TTS模块故障报警">{{ positionData.alarmSign.alarmTtsFault?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="摄像头故障报警">{{ positionData.alarmSign.alarmCameraFault?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="IC卡模块故障报警">{{ positionData.alarmSign.alarmIcFault?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="超速预警">{{ positionData.alarmSign.warningSpeeding?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="疲劳驾驶预警">{{ positionData.alarmSign.warningTired?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="违规行驶报警">{{ positionData.alarmSign.alarmwrong?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="胎压预警">{{ positionData.alarmSign.warningTirePressure?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="右转盲区异常报警">{{ positionData.alarmSign.alarmBlindZone?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="当天累计驾驶超时报警">{{ positionData.alarmSign.alarmDrivingTimeout?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="超时停车报警">{{ positionData.alarmSign.alarmParkingTimeout?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="进出区域报警">{{ positionData.alarmSign.alarmRegion?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="进出路线报警">{{ positionData.alarmSign.alarmRoute?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="路段行驶时间不足/过长报警">{{ positionData.alarmSign.alarmTravelTime?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="路线偏离报警">{{ positionData.alarmSign.alarmRouteDeviation?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="车辆VSS故障">{{ positionData.alarmSign.alarmVSS?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="车辆油量异常报警">{{ positionData.alarmSign.alarmOil?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="车辆被盗报警">{{ positionData.alarmSign.alarmStolen?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="车辆非法点火报警">{{ positionData.alarmSign.alarmIllegalIgnition?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="车辆非法位移报警">{{ positionData.alarmSign.alarmIllegalDisplacement?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="碰撞侧翻报警">{{ positionData.alarmSign.alarmRollover?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="侧翻预警">{{ positionData.alarmSign.warningRollover?'是': '否' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="状态" :column="3" v-if="positionData.status" style="margin-bottom: 1rem;">
          <el-descriptions-item label="ACC">{{ positionData.status.acc?'开': '关' }}</el-descriptions-item>
          <el-descriptions-item label="定位">{{ positionData.status.positioning?'已定位': '未定位' }}</el-descriptions-item>
          <el-descriptions-item label="北纬/南纬">{{ positionData.status.southLatitude?'南纬': '北纬' }}</el-descriptions-item>
          <el-descriptions-item label="东经/西经">{{ positionData.status.wesLongitude?'西经': '东经' }}</el-descriptions-item>
          <el-descriptions-item label="运营状态">{{ positionData.status.outage?'运营': '停运' }}</el-descriptions-item>
          <el-descriptions-item label="经纬度保密插件加密">{{ positionData.status.positionEncryption?'未加密': '已加密' }}</el-descriptions-item>
          <el-descriptions-item label="紧急刹车系统采集的前撞预警">{{ positionData.status.warningFrontCrash?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="车道偏移预警">{{ positionData.status.warningShifting?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="载货">{{ getLoadStatus(positionData.status.load)}}</el-descriptions-item>
          <el-descriptions-item label="车辆油路">{{ positionData.status.oilWayBreak?'正常': '断开' }}</el-descriptions-item>
          <el-descriptions-item label="车辆电路">{{ positionData.status.circuitBreak?'正常': '断开' }}</el-descriptions-item>
          <el-descriptions-item label="车门锁定">{{ positionData.status.doorLocking?'加锁': '解锁' }}</el-descriptions-item>
          <el-descriptions-item label="门1（(前门)）">{{ positionData.status.door1Open?'关': '开' }}</el-descriptions-item>
          <el-descriptions-item label="门2（(中门)）">{{ positionData.status.door2Open?'关': '开' }}</el-descriptions-item>
          <el-descriptions-item label="门3（(后门)）">{{ positionData.status.door3Open?'关': '开' }}</el-descriptions-item>
          <el-descriptions-item label="门4（(驾驶席门)）">{{ positionData.status.door4Open?'关': '开' }}</el-descriptions-item>
          <el-descriptions-item label="门5">{{ positionData.status.door5Open?'关': '开' }}</el-descriptions-item>
          <el-descriptions-item label="GPS卫星定位">{{ positionData.status.gps?'使用': '未使用' }}</el-descriptions-item>
          <el-descriptions-item label="北斗卫星定位">{{ positionData.status.beidou?'使用': '未使用' }}</el-descriptions-item>
          <el-descriptions-item label="GLONASS卫星定位">{{ positionData.status.glonass?'使用': '未使用' }}</el-descriptions-item>
          <el-descriptions-item label="GaLiLeo卫星定位">{{ positionData.status.gaLiLeo?'使用': '未使用' }}</el-descriptions-item>
          <el-descriptions-item label="行驶状态">{{ positionData.status.driving?'行使': '停止' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="视频报警" :column="2" v-if="positionData.videoAlarm" style="margin-bottom: 1rem;">
          <el-descriptions-item label="视频信号丢失报警的通道">{{ positionData.videoAlarm.videoLossChannels?positionData.videoAlarm.videoLossChannels.join(','): '无' }}</el-descriptions-item>
          <el-descriptions-item label="视频信号遮挡报警的通道">{{ positionData.videoAlarm.videoOcclusionChannels?positionData.videoAlarm.videoOcclusionChannels.join(','): '无' }}</el-descriptions-item>
          <el-descriptions-item label="存储器故障报警状态">{{ positionData.videoAlarm.storageFaultAlarm?positionData.videoAlarm.storageFaultAlarm.join(','): '无' }}</el-descriptions-item>
          <el-descriptions-item label="异常驾驶行为-疲劳">{{ positionData.videoAlarm.drivingForFatigue?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="异常驾驶行为-打电话">{{ positionData.videoAlarm.drivingForCall?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="异常驾驶行为-抽烟">{{ positionData.videoAlarm.drivingSmoking?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="其他视频设备故障">{{ positionData.videoAlarm.otherDeviceFailure?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="客车超员报警">{{ positionData.videoAlarm.overcrowding?'是': '否' }}</el-descriptions-item>
          <el-descriptions-item label="特殊报警录像达到存储阈值报警">{{ positionData.videoAlarm.specialRecordFull?'是': '否' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      positionData: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.positionData = data
    },
    getLoadStatus: function(load) {
      switch (load) {
        case 0:
          return '空车'
        case 1:
          return '半载'
        case 2:
          return '保留'
        case 3:
          return '满载'
      }
    },

    close: function() {
      this.showDialog = false
    }
  }
}
</script>
