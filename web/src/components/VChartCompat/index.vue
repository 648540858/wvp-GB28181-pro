<template>
  <div ref="container" :style="{ width, height, minHeight: height === '100%' ? '180px' : undefined }" />
</template>

<script>
import * as echarts from 'echarts'

function merge(target, source) {
  if (!source || typeof source !== 'object') return target
  Object.entries(source).forEach(([key, value]) => {
    if (value && typeof value === 'object' && !Array.isArray(value) && typeof value !== 'function') {
      target[key] = merge({ ...(target[key] || {}) }, value)
    } else {
      target[key] = value
    }
  })
  return target
}

export default {
  name: 'VChartCompat',
  props: {
    chartType: { type: String, default: 'line' },
    data: { type: Object, default: () => ({ columns: [], rows: [] }) },
    settings: { type: Object, default: () => ({}) },
    extend: { type: Object, default: () => ({}) },
    events: { type: Object, default: () => ({}) },
    legendVisible: { type: Boolean, default: true },
    width: { type: String, default: '100%' },
    height: { type: String, default: '400px' }
  },
  data() {
    return { echarts: null, resizeObserver: null }
  },
  watch: {
    data: { deep: true, handler: 'renderChart' },
    settings: { deep: true, handler: 'renderChart' },
    extend: { deep: true, handler: 'renderChart' }
  },
  mounted() {
    this.echarts = echarts.init(this.$refs.container)
    Object.entries(this.events).forEach(([name, handler]) => this.echarts.on(name, handler))
    this.resizeObserver = new ResizeObserver(() => this.echarts?.resize())
    this.resizeObserver.observe(this.$refs.container)
    this.renderChart()
  },
  beforeUnmount() {
    this.resizeObserver?.disconnect()
    this.echarts?.dispose()
  },
  methods: {
    renderChart() {
      if (!this.echarts) return
      const columns = this.data.columns || []
      const rows = this.data.rows || []
      const dimension = this.settings.dimension || columns[0]
      const metrics = this.settings.metrics || columns.filter(column => column !== dimension)
      const horizontal = this.chartType === 'bar'
      const stackMap = Object.entries(this.settings.stack || {}).reduce((result, [stack, names]) => {
        names.forEach(name => { result[name] = stack })
        return result
      }, {})
      const series = metrics.map(metric => {
        const item = {
          name: this.settings.labelMap?.[metric] || metric,
          type: this.chartType === 'line' ? 'line' : 'bar',
          stack: stackMap[metric],
          areaStyle: this.settings.area ? {} : undefined,
          data: horizontal
            ? rows.map(row => row[metric])
            : rows.map(row => [row[dimension], row[metric]])
        }
        return merge(item, this.extend.series && !Array.isArray(this.extend.series) ? this.extend.series : {})
      })
      const categories = rows.map(row => row[dimension])
      const option = {
        animationDuration: 200,
        grid: { left: 16, right: 16, top: 64, bottom: 48, containLabel: true },
        tooltip: { trigger: 'axis' },
        legend: { show: this.legendVisible },
        xAxis: horizontal ? { type: 'value' } : { type: 'category', data: categories },
        yAxis: horizontal ? { type: 'category', data: categories } : { type: 'value' },
        series
      }
      const extension = { ...this.extend }
      delete extension.series
      if (extension.label) {
        option.series.forEach(item => { item.label = extension.label })
        delete extension.label
      }
      merge(option, extension)
      option.legend = { ...option.legend, show: this.legendVisible }
      this.echarts.setOption(option, { notMerge: true })
    }
  }
}
</script>
