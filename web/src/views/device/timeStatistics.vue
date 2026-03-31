<template>
  <div id="timeStatistics" v-loading="loading">
    <el-dialog
      v-el-drag-dialog
      :title="title"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close"
    >
      <div style="margin-right: 20px;">
        <el-row type="flex" justify="space-between" align="middle" style="margin-bottom: 12px;">
          <div>
            <el-button-group>
              <el-button type="primary" :plain="viewMode !== 'table'" size="mini" @click="viewMode = 'table'">表格</el-button>
              <el-button type="primary" :plain="viewMode !== 'chart'" size="mini" @click="viewMode = 'chart'">折线图</el-button>
            </el-button-group>
            <el-button icon="el-icon-refresh" size="mini" @click="fetchData" style="margin-left: 8px;">刷新</el-button>
          </div>
          <el-form :inline="true" size="mini">
            <el-form-item label="数量">
              <el-input-number v-model="count" :min="1" :max="500" @change="fetchData" />
            </el-form-item>
          </el-form>
        </el-row>

        <el-table
          v-if="viewMode === 'table'"
          :data="tableData"
          border
          stripe
          size="mini"
          height="400px"
          style="width: 100%;"
        >
          <el-table-column prop="time" label="时间" min-width="180" />
          <el-table-column prop="timeDiff" label="间隔(秒)" min-width="120" />
        </el-table>

        <ve-line
          v-else
          :data="chartData"
          :extend="extend"
          height="400px"
          :legend-visible="false"
        />
      </div>
      <div style="margin-top: 12px; text-align: right;">
        <span>最大波动：{{ timeDiffDelta }} 秒</span>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import moment from 'moment/moment'
import veLine from 'v-charts/lib/line'
import request from '@/utils/request'
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'TimeStatistics',
  components: { veLine },
  directives: { elDragDialog },
  data() {
    return {
      title: null,
      url: null,
      deviceId: null,
      count: 50,
      showDialog: false,
      loading: false,
      viewMode: 'table',
      list: [],
      extend: {
        grid: { right: '30px', containLabel: true },
        xAxis: {
          boundaryGap: false,
          axisLabel: {
            formatter: (v) => moment(v).format('HH:mm:ss')
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          splitNumber: 6,
          axisLabel: { formatter: (v) => `${v} 秒` }
        },
        tooltip: {
          trigger: 'axis',
          formatter: (data) => {
            if (!data || !data.length) return ''
            const [item] = data
            return `${moment(item.data[0]).format('HH:mm:ss')}<br/>间隔：${item.data[1]} 秒`
          }
        },
        series: {
          itemStyle: { color: '#409EFF' }
        }
      }
    }
  },
  computed: {
    chartData() {
      return {
        columns: ['time', 'timeDiff'],
        rows: this.list
      }
    },
    tableData() {
      return this.list.slice().reverse();
    },
    timeDiffDelta() {
      if (!this.list.length) return 0
      const nums = this.list
        .map(item => Number(item.timeDiff))
        .filter(v => !Number.isNaN(v))
      if (!nums.length) return 0
      const max = Math.max(...nums)
      const min = Math.min(...nums)
      return (max - min).toFixed(2)
    }
  },
  methods: {
    openDialog(title, url, deviceId, count = 50) {
      this.title = title
      this.url = url
      this.deviceId = deviceId
      this.count = count
      this.showDialog = true
      this.viewMode = 'table'
      this.fetchData()
    },
    fetchData() {
      console.log(this.url)
      if (!this.url || !this.deviceId) return
      this.loading = true
      this.$store.dispatch(this.url, {
        deviceId: this.deviceId,
        count: this.count
      }).then(data => {
          this.list = data
      }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error.message
          })
      })
    },
    close() {
      this.title = null
      this.url = null
      this.deviceId = null
      this.list = []
      this.showDialog = false
      this.loading = false
    }
  }
}
</script>
