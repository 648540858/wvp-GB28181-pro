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
        <el-form-item>
          <el-button
            size="mini"
            type="danger"
            plain
            icon="el-icon-delete-solid"
            @click="clearByCondition"
          >
            清空
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
        <el-table-column label="快照" width="100">
          <template v-slot:default="scope">
            <el-image
              v-if="scope.row.snapPath"
              :src="getSnapUrl(scope.row.id)"
              :preview-src-list="[getSnapUrl(scope.row.id)]"
              fit="cover"
              style="width: 64px; height: 48px; cursor: pointer;"
              lazy
            >
              <div slot="error" style="width: 64px; height: 48px; line-height: 48px; text-align: center; color: #c0c4cc; font-size: 12px;">
                <i class="el-icon-picture-outline" />
              </div>
            </el-image>
            <span v-else style="color: #c0c4cc; font-size: 12px;">无</span>
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
        <el-table-column label="操作" width="150" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              icon="el-icon-video-play"
              type="text"
              @click="openPlayback(scope.row)"
            >回放</el-button>
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

    <!-- 录像回放对话框 -->
    <el-dialog
      :title="playbackTitle"
      :visible.sync="playbackDialogVisible"
      width="800px"
      :before-close="closePlayback"
      destroy-on-close
    >
      <div v-if="playbackLoading" style="text-align: center; padding: 40px 0;">
        <i class="el-icon-loading" style="font-size: 32px;" />
        <div style="margin-top: 10px; color: #606266;">正在加载回放...</div>
      </div>
      <div v-else-if="playbackError" style="text-align: center; padding: 40px 0; color: #f56c6c;">
        <i class="el-icon-warning-outline" style="font-size: 32px;" />
        <div style="margin-top: 10px;">{{ playbackError }}</div>
      </div>
      <div v-else-if="playbackStreamInfo">
        <h265web
          ref="playbackPlayer"
          :video-url="playbackVideoUrl"
          :height="'400px'"
          :show-button="false"
          :has-audio="true"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button size="mini" @click="closePlayback">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import h265web from '../common/h265web.vue'

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

function formatDatetime(ts) {
  if (!ts) return null
  const date = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
         `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export default {
  name: 'AlarmManage',
  components: { h265web },
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
      selectedRows: [],
      // 回放相关
      playbackDialogVisible: false,
      playbackLoading: false,
      playbackError: null,
      playbackStreamInfo: null,
      playbackVideoUrl: null,
      playbackTitle: '录像回放',
      currentPlaybackChannelId: null
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
    getSnapUrl(id) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      return ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + `/api/alarm/snap/${id}`
    },
    openPlayback(row) {
      if (!row.channelId) {
        this.$message({ showClose: true, message: '该报警无关联通道，无法回放', type: 'warning' })
        return
      }
      this.playbackTitle = `录像回放 - ${row.channelName || row.channelDeviceId} (${this.formatTime(row.alarmTime)})`
      this.playbackDialogVisible = true
      this.playbackLoading = true
      this.playbackError = null
      this.playbackStreamInfo = null
      this.playbackVideoUrl = null
      this.currentPlaybackChannelId = row.channelId

      // 开始时间：报警时间前10秒，结束时间：报警时间后10秒（共20秒）
      const alarmTs = row.alarmTime
      const startTime = formatDatetime(alarmTs - 10 * 1000)
      const endTime = formatDatetime(alarmTs + 10 * 1000)

      this.$store.dispatch('commonChanel/playback', {
        channelId: row.channelId,
        startTime: startTime,
        endTime: endTime
      }).then(data => {
        this.playbackStreamInfo = data
        if (location.protocol === 'https:') {
          this.playbackVideoUrl = data['wss_flv']
        } else {
          this.playbackVideoUrl = data['ws_flv']
        }
        this.playbackLoading = false
      }).catch(err => {
        this.playbackLoading = false
        this.playbackError = (err && err.msg) ? err.msg : '回放请求失败，请检查通道是否有该时段录像'
        console.log(err)
      })
    },
    closePlayback() {
      if (this.playbackStreamInfo && this.currentPlaybackChannelId) {
        this.$store.dispatch('commonChanel/stopPlayback', {
          channelId: this.currentPlaybackChannelId,
          stream: this.playbackStreamInfo.stream
        }).catch(err => {
          console.log(err)
        })
      }
      this.playbackDialogVisible = false
      this.playbackStreamInfo = null
      this.playbackVideoUrl = null
      this.playbackError = null
      this.currentPlaybackChannelId = null
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
    clearByCondition() {
      const hasFilter = this.beginTime || this.endTime || this.selectedAlarmTypes.length > 0
      const filterDesc = hasFilter
        ? [
          this.beginTime ? `开始时间：${this.beginTime}` : null,
          this.endTime ? `结束时间：${this.endTime}` : null,
          this.selectedAlarmTypes.length > 0 ? `报警类型：${this.selectedAlarmTypes.map(v => {
            const opt = this.alarmTypeOptions.find(o => o.value === v)
            return opt ? opt.label : v
          }).join('、')}` : null
        ].filter(Boolean).join('；')
        : '全部'
      this.$confirm(
        `将删除符合当前筛选条件的所有报警记录（${filterDesc}），此操作不可恢复，确定继续？`,
        '清空报警',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        this.$store.dispatch('alarm/clearAlarms', {
          alarmType: this.selectedAlarmTypes.length > 0 ? this.selectedAlarmTypes : undefined,
          beginTime: this.beginTime || undefined,
          endTime: this.endTime || undefined
        }).then(count => {
          this.$message({ showClose: true, message: `已清空 ${count != null ? count : ''} 条报警记录`, type: 'success' })
          this.currentPage = 1
          this.getAlarmList()
        }).catch(error => {
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
      return formatDatetime(timestamp)
    }
  }
}
</script>
