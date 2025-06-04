<template>
  <div class="app-container">
    <el-row>
      <el-col :span="8">
        <echart-pip class="yinying" title="设备状态统计" :data="deviceStatusData" />
      </el-col>
      <el-col :span="8">
        <echart-pip class="yinying" title="视图库状态统计" :data="serverStatusData" />
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <echart-category class="yinying" :data="devicePushCounteData" title="设备上推数据频率" />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import EchartPip from '@/components/Echarts/Pip.vue'
import EchartCategory from '@/components/Echarts/Category.vue'
import { viidMetrics } from '@/api/datawork/platform'

export default {
  name: 'DataWorkHome',
  components: { EchartPip, EchartCategory },
  data() {
    return {
      deviceStatusData: [],
      serverStatusData: [],
      devicePushCounteData: {}
    }
  },
  mounted() {
    this.refreshData()
  },
  methods: {
    refreshData() {
      viidMetrics().then(response => {
        const data = response.data
        this.deviceStatusData = data.device
        this.serverStatusData = data.server
        this.devicePushCounteData = data.metric
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.yinying {
  height: 400px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04)
}
</style>
