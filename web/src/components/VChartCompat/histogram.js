import { defineComponent, h } from 'vue'
import VChartCompat from './index.vue'

export default defineComponent({
  name: 'VeHistogram',
  inheritAttrs: false,
  setup(_, { attrs }) {
    return () => h(VChartCompat, { ...attrs, chartType: 'histogram' })
  }
})
