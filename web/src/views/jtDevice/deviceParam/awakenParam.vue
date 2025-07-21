<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 80%; margin: 0 auto">
        <el-form-item label="休眠唤醒模式" prop="wakeUpModeByCondition">
          <el-checkbox label="条件唤醒" v-model="form.awakenParam.wakeUpModeByCondition" ></el-checkbox>
          <el-checkbox label="定时唤醒" v-model="form.awakenParam.wakeUpModeByTime" ></el-checkbox>
          <el-checkbox label="手动唤醒" v-model="form.awakenParam.wakeUpModeByManual" ></el-checkbox>
        </el-form-item>
        <el-form-item label="唤醒条件类型" prop="wakeUpConditionsByAlarm">
          <el-checkbox label="紧急报警" v-model="form.awakenParam.wakeUpConditionsByAlarm" ></el-checkbox>
          <el-checkbox label="碰撞侧翻报警" v-model="form.awakenParam.wakeUpConditionsByRollover" ></el-checkbox>
          <el-checkbox label="车辆开门" v-model="form.awakenParam.wakeUpConditionsByOpenTheDoor" ></el-checkbox>
        </el-form-item>
        <el-form-item label="定时唤醒日设置" prop="awakeningDayForMonday">
          <el-checkbox label="周一" v-model="form.awakenParam.awakeningDayForMonday" ></el-checkbox>
          <el-checkbox label="周二" v-model="form.awakenParam.awakeningDayForTuesday" ></el-checkbox>
          <el-checkbox label="周三" v-model="form.awakenParam.awakeningDayForWednesday" ></el-checkbox>
          <el-checkbox label="周四" v-model="form.awakenParam.awakeningDayForThursday" ></el-checkbox>
          <el-checkbox label="周五" v-model="form.awakenParam.awakeningDayForFriday" ></el-checkbox>
          <el-checkbox label="周六" v-model="form.awakenParam.awakeningDayForSaturday" ></el-checkbox>
          <el-checkbox label="周日" v-model="form.awakenParam.awakeningDayForSunday" ></el-checkbox>
        </el-form-item>
        <el-form-item label="日定时唤醒-时间段1" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="启用" v-model="form.awakenParam.time1Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time1Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time1StartTime" placeholder="请输入时间段1的开始时间" clearable size="small"/>
              <span style="text-align: center">至</span>
              <el-input v-model="form.awakenParam.time1EndTime" placeholder="请输入时间段1的结束时间" clearable size="small"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="日定时唤醒-时间段2" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="启用" v-model="form.awakenParam.time2Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time2Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time2StartTime" placeholder="请输入时间段2的开始时间" clearable size="small"/>
              <span style="text-align: center">至</span>
              <el-input v-model="form.awakenParam.time2EndTime" placeholder="请输入时间段2的结束时间" clearable size="small"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="日定时唤醒-时间段3" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="启用" v-model="form.awakenParam.time3Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time3Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time3StartTime" placeholder="请输入时间段3的开始时间" clearable size="small"/>
              <span style="text-align: center">至</span>
              <el-input v-model="form.awakenParam.time3EndTime" placeholder="请输入时间段3的结束时间" clearable size="small"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="日定时唤醒-时间段4" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="启用" v-model="form.awakenParam.time4Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time4Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time4StartTime" placeholder="请输入时间段4的开始时间" clearable size="small"/>
              <span style="text-align: center">至</span>
              <el-input v-model="form.awakenParam.time4EndTime" placeholder="请输入时间段4的结束时间" clearable size="small"/>
            </div>
          </div>
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
          if (!data.awakenParam) {
            data.awakenParam = {}
          }
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
