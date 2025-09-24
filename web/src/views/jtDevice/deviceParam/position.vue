<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="汇报策略" prop="locationReportingStrategy">
          <el-select v-model="form.locationReportingStrategy" style="float: left; width: 100%">
            <el-option label="定时汇报" :value="0">定时汇报</el-option>
            <el-option label="定距汇报" :value="1" />
            <el-option label="定时和定距汇报" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="汇报方案" prop="locationReportingPlan">
          <el-select v-model="form.locationReportingPlan" style="float: left; width: 100%">
            <el-option label="根据ACC状态" :value="0" />
            <el-option label="登录状态和ACC状态" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="驾驶员未登录汇报时间间隔(秒)" prop="reportingIntervalOffline">
          <el-input type="number" v-model="form.reportingIntervalOffline" placeholder="请输入驾驶员未登录汇报时间间隔" />
        </el-form-item>
        <el-form-item label="休眠时汇报时间间隔(秒)" prop="reportingIntervalDormancy">
          <el-input type="number" v-model="form.reportingIntervalDormancy" placeholder="请输入休眠时汇报时间间隔" />
        </el-form-item>
        <el-form-item label="紧急报警时汇报时间间隔(秒)" prop="reportingIntervalEmergencyAlarm">
          <el-input type="number" v-model="form.reportingIntervalEmergencyAlarm" placeholder="请输入紧急报警时汇报时间间隔" />
        </el-form-item>
        <el-form-item label="缺省时间汇报间隔(秒)" prop="reportingIntervalDefault">
          <el-input type="number" v-model="form.reportingIntervalDefault" placeholder="请输入缺省时间汇报间隔" />
        </el-form-item>
        <el-form-item label="缺省距离汇报间隔(米)" prop="reportingDistanceDefault">
          <el-input type="number" v-model="form.reportingDistanceDefault" placeholder="请输入缺省距离汇报间隔" />
        </el-form-item>
        <el-form-item label="驾驶员未登录汇报距离间隔(米)" prop="reportingDistanceOffline">
          <el-input type="number" v-model="form.reportingDistanceOffline" placeholder="请输入驾驶员未登录汇报距离间隔" />
        </el-form-item>
        <el-form-item label="休眠时汇报距离间隔(米)" prop="reportingDistanceDormancy">
          <el-input type="number" v-model="form.reportingDistanceDormancy" placeholder="请输入休眠时汇报距离间隔" />
        </el-form-item>
        <el-form-item label="紧急报警时汇报距离间隔(米)" prop="reportingDistanceEmergencyAlarm">
          <el-input type="number" v-model="form.reportingDistanceEmergencyAlarm" placeholder="请输入紧急报警时汇报距离间隔" />
        </el-form-item>
        <el-form-item label="拐点补传角度(度，小于180)" prop="inflectionPointAngle">
          <el-input type="number" v-model="form.inflectionPointAngle" placeholder="请输入拐点补传角度" />
        </el-form-item>
        <el-form-item label="电子围栏半径(米)" prop="fenceRadius">
          <el-input type="number" v-model="form.fenceRadius" placeholder="请输入电子围栏半径" />
        </el-form-item>
        <el-form-item label="违规行驶时段-开始时间(HH:mm)" prop="illegalDrivingPeriods">
          <el-input v-model="form.illegalDrivingPeriods.startTime" clearable />
        </el-form-item>
        <el-form-item label="违规行驶时段-结束时间(HH:mm)" prop="illegalDrivingPeriods">
          <el-input v-model="form.illegalDrivingPeriods.endTime" clearable />
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
      illegalDrivingPeriods: [new Date(), new Date()],
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
          if (!data.illegalDrivingPeriods) {
            data.illegalDrivingPeriods = {
              startTime: null,
              endTime: null
            }
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
