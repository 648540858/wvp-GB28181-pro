<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="最高速度(千米每小时)" prop="topSpeed">
          <el-input type="number" v-model="form.topSpeed" />
        </el-form-item>
        <el-form-item label="超速持续时间(秒)" prop="overSpeedDuration">
          <el-input type="number" v-model="form.overSpeedDuration" />
        </el-form-item>
        <el-form-item label="连续驾驶时间门限(秒)" prop="continuousDrivingTimeThreshold">
          <el-input type="number" v-model="form.continuousDrivingTimeThreshold" />
        </el-form-item>
        <el-form-item label="当天累计驾驶时间门限(秒)" prop="cumulativeDrivingTimeThresholdForTheDay">
          <el-input type="number" v-model="form.cumulativeDrivingTimeThresholdForTheDay" />
        </el-form-item>
        <el-form-item label="最小休息时间(秒)" prop="minimumBreakTime">
          <el-input type="number" v-model="form.minimumBreakTime" />
        </el-form-item>
        <el-form-item label="最长停车时间(秒)" prop="maximumParkingTime">
          <el-input type="number" v-model="form.maximumParkingTime" />
        </el-form-item>
        <el-form-item label="超速预警差值(1/10 千米每小时)" prop="overSpeedWarningDifference">
          <el-input type="number" v-model="form.overSpeedWarningDifference" />
        </el-form-item>
        <el-form-item label="疲劳驾驶预警差值(秒)" prop="drowsyDrivingWarningDifference">
          <el-input type="number" v-model="form.drowsyDrivingWarningDifference" />
        </el-form-item>
        <div v-if="form.collisionAlarmParams">
          <el-form-item label="碰撞报警-碰撞时间(毫秒)" prop="collisionAlarmParamsCollisionAlarmTime">
            <el-input type="number" v-model="form.collisionAlarmParams.collisionAlarmTime" />
          </el-form-item>
          <el-form-item label="碰撞报警-碰撞加速度(0.1g)" prop="collisionAlarmParamsCollisionAcceleration">
            <el-input type="number" v-model="form.collisionAlarmParams.collisionAcceleration" />
          </el-form-item>
        </div>

        <el-form-item label="侧翻报警参数-侧翻角度(度)" prop="rolloverAlarm">
          <el-input v-model="form.rolloverAlarm" clearable />
        </el-form-item>
      </el-form>
    </div>
    <p style="text-align: right">
      <el-button type="primary" @click="onSubmit">确认</el-button>
      <el-button @click="showDevice">取消</el-button>
    </p>

  </div>
</template>

<script>

export default {
  name: 'communication',
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
      form: {},
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
      this.$emit('submit', this.form)
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
