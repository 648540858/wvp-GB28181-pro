<template>
  <div style="width: 100%;">
    <el-checkbox label="紧急报警" v-model="form.urgent" @change="change"></el-checkbox>
    <el-checkbox label="超速报警" v-model="form.alarmSpeeding" @change="change"></el-checkbox>
    <el-checkbox label="疲劳驾驶报警" v-model="form.alarmTired" @change="change"></el-checkbox>
    <el-checkbox label="危险驾驶行为报警" v-model="form.alarmDangerous" @change="change"></el-checkbox>
    <el-checkbox label="GNSS模块发生故障报警" v-model="form.alarmGnssFault" @change="change"></el-checkbox>
    <el-checkbox label="GNSS天线未接或被剪断报警" v-model="form.alarmGnssBreak" @change="change"></el-checkbox>
    <el-checkbox label="GNSS天线短路报警" v-model="form.alarmGnssShortCircuited" @change="change"></el-checkbox>
    <el-checkbox label="终端主电源欠压报警" v-model="form.alarmUnderVoltage" @change="change"></el-checkbox>
    <el-checkbox label="终端主电源掉电报警" v-model="form.alarmPowerOff" @change="change"></el-checkbox>
    <el-checkbox label="终端LCD或显示器故障报警" v-model="form.alarmLCD" @change="change"></el-checkbox>
    <el-checkbox label="TTS模块故障报警" v-model="form.alarmTtsFault" @change="change"></el-checkbox>
    <el-checkbox label="摄像头故障报警" v-model="form.alarmCameraFault" @change="change"></el-checkbox>
    <el-checkbox label="IC卡模块故障报警" v-model="form.alarmIcFault" @change="change"></el-checkbox>
    <el-checkbox label="超速预警" v-model="form.warningSpeeding" @change="change"></el-checkbox>
    <el-checkbox label="疲劳驾驶预警" v-model="form.warningTired" @change="change"></el-checkbox>
    <el-checkbox label="违规行驶报警" v-model="form.alarmWrong" @change="change"></el-checkbox>
    <el-checkbox label="胎压预警" v-model="form.warningTirePressure" @change="change"></el-checkbox>
    <el-checkbox label="右转盲区异常报警" v-model="form.alarmBlindZone" @change="change"></el-checkbox>
    <el-checkbox label="当天累计驾驶超时报警" v-model="form.alarmDrivingTimeout" @change="change"></el-checkbox>
    <el-checkbox label="超时停车报警" v-model="form.alarmParkingTimeout" @change="change"></el-checkbox>
    <el-checkbox label="进出区域报警" v-model="form.alarmRegion" @change="change"></el-checkbox>
    <el-checkbox label="进出路线报警" v-model="form.alarmRoute" @change="change"></el-checkbox>
    <el-checkbox label="路段行驶时间不足/过长报警" v-model="form.alarmTravelTime" @change="change"></el-checkbox>
    <el-checkbox label="路线偏离报警" v-model="form.alarmRouteDeviation" @change="change"></el-checkbox>
    <el-checkbox label="车辆VSS故障" v-model="form.alarmVSS" @change="change"></el-checkbox>
    <el-checkbox label="车辆油量异常报警" v-model="form.alarmOil" @change="change"></el-checkbox>
    <el-checkbox label="车辆被盗报警" v-model="form.alarmStolen" @change="change"></el-checkbox>
    <el-checkbox label="车辆非法点火报警" v-model="form.alarmIllegalIgnition" @change="change"></el-checkbox>
    <el-checkbox label="车辆非法位移报警" v-model="form.alarmIllegalDisplacement" @change="change"></el-checkbox>
    <el-checkbox label="碰撞侧翻报警" v-model="form.alarmRollover" @change="change"></el-checkbox>
    <el-checkbox label="侧翻预警" v-model="form.warningRollover" @change="change"></el-checkbox>
  </div>
</template>

<script>

export default {
  name: 'communication',
  components: {
  },
  model: {
    prop: 'fatherValue',
    event: 'change'
  },
  props: {
    fatherValue: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      form: {
        urgent: false,
        alarmSpeeding: false,
        alarmTired: false,
        alarmDangerous: false,
        alarmGnssFault: false,
        alarmGnssBreak: false,
        alarmGnssShortCircuited: false,
        alarmUnderVoltage: false,
        alarmPowerOff: false,
        alarmLCD: false,
        alarmTtsFault: false,
        alarmCameraFault: false,
        alarmIcFault: false,
        warningSpeeding: false,
        warningTired: false,
        alarmWrong: false,
        warningTirePressure: false,
        alarmBlindZone: false,
        alarmDrivingTimeout: false,
        alarmParkingTimeout: false,
        alarmRegion: false,
        alarmRoute: false,
        alarmTravelTime: false,
        alarmRouteDeviation: false,
        alarmVSS: false,
        alarmOil: false,
        alarmStolen: false,
        alarmIllegalIgnition: false,
        alarmIllegalDisplacement: false,
        alarmRollover: false,
        warningRollover: false
      },
      isLoading: false
    }
  },

  mounted() {
    if (this.fatherValue !== null) {
      console.log(this.fatherValue)
      this.form.urgent = this.fatherValue.urgent || false
      this.form.alarmSpeeding = this.fatherValue.alarmSpeeding || false
      this.form.alarmTired = this.fatherValue.alarmTired || false
      this.form.alarmDangerous = this.fatherValue.alarmDangerous || false
      this.form.alarmGnssFault = this.fatherValue.alarmGnssFault || false
      this.form.alarmGnssBreak = this.fatherValue.alarmGnssBreak || false
      this.form.alarmGnssShortCircuited = this.fatherValue.alarmGnssShortCircuited || false
      this.form.alarmUnderVoltage = this.fatherValue.alarmUnderVoltage || false
      this.form.alarmPowerOff = this.fatherValue.alarmPowerOff || false
      this.form.alarmLCD = this.fatherValue.alarmLCD || false
      this.form.alarmTtsFault = this.fatherValue.alarmTtsFault || false
      this.form.alarmCameraFault = this.fatherValue.alarmCameraFault || false
      this.form.alarmIcFault = this.fatherValue.alarmIcFault || false
      this.form.warningSpeeding = this.fatherValue.warningSpeeding || false
      this.form.warningTired = this.fatherValue.warningTired || false
      this.form.alarmWrong = this.fatherValue.alarmWrong || false
      this.form.warningTirePressure = this.fatherValue.warningTirePressure || false
      this.form.alarmBlindZone = this.fatherValue.alarmBlindZone || false
      this.form.alarmDrivingTimeout = this.fatherValue.alarmDrivingTimeout || false
      this.form.alarmParkingTimeout = this.fatherValue.alarmParkingTimeout || false
      this.form.alarmRegion = this.fatherValue.alarmRegion || false
      this.form.alarmRoute = this.fatherValue.alarmRoute || false
      this.form.alarmTravelTime = this.fatherValue.alarmTravelTime || false
      this.form.alarmRouteDeviation = this.fatherValue.alarmRouteDeviation || false
      this.form.alarmVSS = this.fatherValue.alarmVSS || false
      this.form.alarmOil = this.fatherValue.alarmOil || false
      this.form.alarmStolen = this.fatherValue.alarmStolen || false
      this.form.alarmIllegalIgnition = this.fatherValue.alarmIllegalIgnition || false
      this.form.alarmIllegalDisplacement = this.fatherValue.alarmIllegalDisplacement || false
      this.form.alarmRollover = this.fatherValue.alarmRollover || false
      this.form.warningRollover = this.fatherValue.warningRollover || false
    }
  },
  methods: {
    change() {
      this.$emit('change', this.form)
    }
  }
}
</script>
