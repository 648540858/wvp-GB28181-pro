<template>
  <div id="alarmManage" class="app-container">
    <div style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="beginTime"
            type="datetime"
            placeholder="开始时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 180px;"
            clearable
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="endTime"
            type="datetime"
            placeholder="结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 180px;"
            clearable
          />
        </el-form-item>
        <el-form-item label="报警类型">
          <el-select
            v-model="selectedAlarmTypes"
            multiple
            collapse-tags
            placeholder="全部类型"
            style="width: 200px;"
            clearable
          >
            <el-option
              v-for="item in alarmTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="mini" type="primary" icon="el-icon-search" @click="search">
            查询
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            size="mini"
            type="danger"
            icon="el-icon-delete"
            :disabled="selectedRows.length === 0"
            @click="deleteSelected"
          >
            删除选中
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle size="mini" @click="getAlarmList()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="alarmTable"
        size="small"
        :data="alarmList"
        height="calc(100% - 64px)"
        style="width: 100%"
        header-row-class-name="table-header"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="alarmType" label="报警类型" width="160">
          <template v-slot:default="scope">
            <el-tag size="mini" :type="getAlarmTypeTagType(scope.row.alarmType)">
              {{ getAlarmTypeLabel(scope.row.alarmType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="报警描述" show-overflow-tooltip />
        <el-table-column prop="channelName" label="通道名称" width="150" />
        <el-table-column prop="channelDeviceId" label="通道编号" width="180" />
        <el-table-column prop="longitude" label="经度" width="110" />
        <el-table-column prop="latitude" label="纬度" width="110" />
        <el-table-column label="报警时间" width="170">
          <template v-slot:default="scope">
            {{ formatTime(scope.row.alarmTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              icon="el-icon-delete"
              style="color: #f56c6c"
              type="text"
              @click="deleteSingle(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </div>
  </div>
</template>

<script>
const ALARM_TYPE_OPTIONS = [
  { value: 'VideoLoss', label: '视频丢失报警' },
  { value: 'DeviceTamper', label: '设备防拆报警' },
  { value: 'StorageFull', label: '存储设备磁盘满报警' },
  { value: 'DeviceHighTemperature', label: '设备高温报警' },
  { value: 'DeviceLowTemperature', label: '设备低温报警' },
  { value: 'ManualVideo', label: '人工视频报警' },
  { value: 'MotionDetection', label: '运动目标检测报警' },
  { value: 'LeftObjectDetection', label: '遗留物检测报警' },
  { value: 'ObjectRemovalDetection', label: '物体移除检测报警' },
  { value: 'TripwireDetection', label: '绊线检测报警' },
  { value: 'IntrusionDetection', label: '入侵检测报警' },
  { value: 'MobileDetection', label: '移动侦测报警' },
  { value: 'VideoOcclusion', label: '视频遮挡报警' },
  { value: 'ReverseDetection', label: '逆行检测报警' },
  { value: 'LoiteringDetection', label: '徘徊检测报警' },
  { value: 'FlowStatistics', label: '流量统计报警' },
  { value: 'DensityDetection', label: '密度检测报警' },
  { value: 'VideoAbnormal', label: '视频异常检测报警' },
  { value: 'RapidMovement', label: '快速移动报警' },
  { value: 'StorageFault', label: '存储设备磁盘故障报警' },
  { value: 'StorageFanFault', label: '存储设备风扇故障报警' },
  { value: 'SoundAbnormal', label: '声音异常报警' },
  { value: 'SignalAbnormal', label: '信号量异常报警' },
  { value: 'IllegalAccess', label: '非法访问报警' },
  { value: 'Defocus', label: '虚焦报警' },
  { value: 'SceneChange', label: '场景变更报警' },
  { value: 'CrowdGathering', label: '人员聚集报警' },
  { value: 'ParkingDetection', label: '停车侦测报警' },
  { value: 'Other', label: '其他报警' }
]

export default {
  name: 'AlarmManage',
  data() {
    return {
      alarmList: [],
      beginTime: null,
      endTime: null,
      selectedAlarmTypes: [],
      alarmTypeOptions: ALARM_TYPE_OPTIONS,
      currentPage: 1,
      count: 15,
      total: 0,
      selectedRows: []
    }
  },
  created() {
    this.getAlarmList()
  },
  methods: {
    currentChange(val) {
      this.currentPage = val
      this.getAlarmList()
    },
    handleSizeChange(val) {
      this.count = val
      this.getAlarmList()
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows
    },
    search() {
      this.currentPage = 1
      this.total = 0
      this.getAlarmList()
    },
    getAlarmList() {
      this.$store.dispatch('alarm/getAlarmList', {
        page: this.currentPage,
        count: this.count,
        alarmType: this.selectedAlarmTypes.length > 0 ? this.selectedAlarmTypes : undefined,
        beginTime: this.beginTime || undefined,
        endTime: this.endTime || undefined
      }).then(data => {
        this.total = data.total
        this.alarmList = data.list
        this.$nextTick(() => {
          this.$refs.alarmTable.doLayout()
        })
      }).catch(error => {
        console.log(error)
      })
    },
    deleteSingle(row) {
      this.$confirm('确定删除该报警记录?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('alarm/deleteAlarms', [row.id])
          .then(() => {
            this.$message({ showClose: true, message: '删除成功', type: 'success' })
            this.getAlarmList()
          })
          .catch(error => {
            this.$message({ showClose: true, message: error, type: 'error' })
          })
      }).catch(() => {})
    },
    deleteSelected() {
      this.$confirm(`确定删除选中的 ${this.selectedRows.length} 条报警记录?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const ids = this.selectedRows.map(r => r.id)
        this.$store.dispatch('alarm/deleteAlarms', ids)
          .then(() => {
            this.$message({ showClose: true, message: '删除成功', type: 'success' })
            this.getAlarmList()
          })
          .catch(error => {
            this.$message({ showClose: true, message: error, type: 'error' })
          })
      }).catch(() => {})
    },
    getAlarmTypeLabel(value) {
      const option = ALARM_TYPE_OPTIONS.find(o => o.value === value)
      return option ? option.label : value
    },
    getAlarmTypeTagType(value) {
      const dangerTypes = ['VideoLoss', 'IntrusionDetection', 'IllegalAccess', 'ManualVideo', 'TripwireDetection']
      const warningTypes = ['MotionDetection', 'MobileDetection', 'ReverseDetection', 'CrowdGathering', 'RapidMovement']
      if (dangerTypes.includes(value)) return 'danger'
      if (warningTypes.includes(value)) return 'warning'
      return 'info'
    },
    formatTime(timestamp) {
      if (!timestamp) return '-'
      const date = new Date(timestamp)
      const pad = n => String(n).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
             `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    }
  }
}
</script>
