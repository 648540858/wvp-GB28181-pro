import { defineComponent, h } from 'vue'
import VChartCompat from './index.vue'

export default defineComponent({
  name: 'VeLine',
  inheritAttrs: false,
  setup(_, { attrs }) {
    return () => h(VChartCompat, { ...attrs, chartType: 'line' })
  }
})
