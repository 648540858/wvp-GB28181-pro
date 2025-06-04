export const chartTypes = [
  {
    name: '表格', value: 'table', icon: 'chart_table',
    status: true,
    component: 'ChartTable',
    rule: {
      text: '0个或多个 行维;0个或多个 列维;1个或多个 指标',
      check(rows, columns, measures) {
        return rows.length >= 0 && columns.length >= 0 && measures.length >= 1
      }
    }
  },
  {
    name: '折线图', value: 'line', icon: 'chart_line',
    status: true,
    component: 'ChartLine',
    rule: {
      text: '1个或多个 行维;0个或多个 列维;1个或多个 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length >= 0 && measures.length >= 1
      }
    }
  },
  {
    name: '柱状图', value: 'bar', icon: 'chart_bar',
    status: true,
    component: 'ChartBar',
    rule: {
      text: '1个或多个 行维;0个或多个 列维;1个或多个 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length >= 0 && measures.length >= 1
      }
    }
  },
  {
    name: '饼图', value: 'pie', icon: 'chart_pie',
    status: true,
    component: 'ChartPie',
    rule: {
      text: '1个或多个 行维;0个或多个 列维;1个或多个 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length >= 0 && measures.length >= 1
      }
    }
  },
  {
    name: '雷达图', value: 'radar', icon: 'chart_radar',
    status: true,
    component: 'ChartRadar',
    rule: {
      text: '1个或多个 行维;0个或多个 列维;1个或多个 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length >= 0 && measures.length >= 1
      }
    }
  },
  {
    name: '散点图', value: 'scatter', icon: 'chart_scatter',
    status: true,
    component: 'ChartScatter',
    rule: {
      text: '1个或多个 行维;0个或多个 列维;1个或多个 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length >= 0 && measures.length >= 1
      }
    }
  },
  {
    name: '漏斗图', value: 'funnel', icon: 'chart_funnel',
    status: true,
    component: 'ChartFunnel',
    rule: {
      text: '0个或多个 行维;0 列维;1个或多个 指标',
      check(rows, columns, measures) {
        return rows.length >= 0 && columns.length === 0 && measures.length >= 1
      }
    }
  },
  {
    name: '矩形树图', value: 'treemap', icon: 'chart_treemap',
    status: true,
    component: 'ChartTreemap',
    rule: {
      text: '1个或多个 行维;0 列维;1 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length === 0 && measures.length === 1
      }
    }
  },
  {
    name: '桑基图', value: 'sankey', icon: 'chart_sankey',
    status: true,
    component: 'ChartSankey',
    rule: {
      text: '1个或多个 行维;0个或多个 列维;1 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length >= 0 && measures.length === 1
      }
    }
  },
  {
    name: '地图', value: 'map', icon: 'chart_geo',
    status: true,
    component: 'ChartMap',
    rule: {
      text: '1 行维;0 列维;1 指标',
      check(rows, columns, measures) {
        return rows.length === 1 && columns.length === 0 && measures.length === 1
      }
    }
  },
  {
    name: '仪表盘', value: 'gauge', icon: 'chart_gauge',
    status: true,
    component: 'ChartGauge',
    rule: {
      text: '0 行维;0 列维;1 指标',
      check(rows, columns, measures) {
        return rows.length === 0 && columns.length === 0 && measures.length === 1
      }
    }
  },
  {
    name: '指标卡', value: 'kpi', icon: 'chart_kpi',
    status: true,
    component: 'ChartKpi',
    rule: {
      text: '0 行维;0 列维;1 指标',
      check(rows, columns, measures) {
        return rows.length === 0 && columns.length === 0 && measures.length === 1
      }
    }
  },
  {
    name: '水球图', value: 'liquidFill', icon: 'chart_liquidfill',
    status: true,
    component: 'ChartLiquidFill',
    rule: {
      text: '0 行维;0 列维;1 指标',
      check(rows, columns, measures) {
        return rows.length === 0 && columns.length === 0 && measures.length === 1
      }
    }
  },
  {
    name: '词云图', value: 'wordCloud', icon: 'chart_wordcloud',
    status: true,
    component: 'ChartWordCloud',
    rule: {
      text: '1个或多个 行维;0 列维;1 指标',
      check(rows, columns, measures) {
        return rows.length >= 1 && columns.length === 0 && measures.length === 1
      }
    }
  }
]

export const chartOptions = {
  // 标题组件
  title: {
    // 是否显示
    show: false,
    // 主标题文本
    text: '',
    // 副标题文本
    subtext: '',
    // 离左侧的距离
    left: '0%',
    leftVal: 0,
    // 离上侧的距离
    top: '0%',
    topVal: 0,
    // 主标题样式
    textStyle: {
      // 主标题字体大小
      fontSize: 18,
      // 主标题文字的颜色
      color: '#333'
    },
    // 副标题样式
    subtextStyle: {
      // 副标题字体大小
      fontSize: 12,
      // 副标题文字的颜色
      color: '#aaa'
    }
  },
  // 图例组件
  legend: {
    // 是否显示
    show: true,
    // 图例的类型 'plain':普通图例  'scroll':可滚动翻页的图例
    type: 'plain',
    // 离左侧的距离
    left: '0%',
    leftVal: 0,
    // 离上侧的距离
    top: '0%',
    topVal: 0,
    // 列表的布局朝向(横竖,可选：'horizontal','vertical')
    orient: 'horizontal'
  }
}

export const chartThemes = ['default', 'vintage', 'westeros', 'wonderland',
  'chalk', 'macarons', 'shine', 'dark', 'essos', 'walden', 'infographic', 'roma', 'purple-passion']

export const chartSeriesTypes = {
  'line': [
    { name: '基础折线图', value: 'line' },
    { name: '区域折线图', value: 'arealine' },
    { name: '堆叠折线图', value: 'stackline' },
    { name: '堆叠区域图', value: 'stackarealine' },
    { name: '百分比堆叠区域图', value: 'percentagestackarealine' }
  ],
  'bar': [
    { name: '基础柱状图', value: 'bar' },
    { name: '堆叠柱状图', value: 'stackbar' },
    { name: '百分比堆叠柱状图', value: 'percentagestackbar' },
    { name: '条形图', value: 'barchart' },
    { name: '堆叠条形图', value: 'stackbarchart' },
    { name: '百分比堆叠条形图', value: 'percentagestackbarchart' }
  ],
  'pie': [
    { name: '基础饼图', value: 'pie' },
    { name: '南丁格尔玫瑰图（半径模式）', value: 'roseradiuspie' },
    { name: '南丁格尔玫瑰图（面积模式）', value: 'roseareapie' },
    { name: '环形图', value: 'donutpie' }
  ],
  'radar': [
    { name: '基础雷达图', value: 'radar' },
    { name: '区域雷达图', value: 'arearadar' }
  ],
  'funnel': [
    { name: '基础漏斗图', value: 'funnel' },
    { name: '金字塔漏斗图', value: 'pyramidfunnel' },
    { name: '对比漏斗图', value: 'contrastfunnel' }
  ],
  'liquidFill': [
    { name: '圆形', value: 'circle' },
    { name: '矩形', value: 'rect' },
    { name: '圆矩形', value: 'roundRect' },
    { name: '三角形', value: 'triangle' },
    { name: '菱形', value: 'diamond' },
    { name: '大头针形', value: 'pin' },
    { name: '箭头形', value: 'arrow' }
  ],
  'map': [
    { name: '基础地图', value: 'map' },
    { name: '视觉映射', value: 'visualMap' }
  ]
}

export const CELL_MIN_HEIGHT = 38

export const SEPARATOR = ':'

export function convertPathToMap(paths, keys) {
  return paths.map(path => {
    const pathArr = path.split(SEPARATOR)
    const obj = {}
    keys.forEach((key, index) => {
      if (pathArr[index]) {
        obj[key] = pathArr[index]
      }
    })
    return obj
  })
}

export function mergeBaseInfo(info = {}) {
  const _baseCellInfo = {
    value: '',
    x: 0,
    y: 0,
    colspan: 1,
    rowspan: 1
  }
  return Object.assign({}, _baseCellInfo, info)
}

export function getHeightByCount(count) {
  return count * CELL_MIN_HEIGHT + 'px'
}
