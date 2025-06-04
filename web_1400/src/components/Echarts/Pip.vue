<template>
  <div>
    <div style="height: 70%;" ref="chartpip"></div>
  </div>
</template>
   
<script>
import echarts from "echarts";

export default {
  name: "EchartsPip",
  props: {
    data: {
      type: Array
    },
    title: {
      type: String
    }
  },
  data() {
    return {
      chart: null,
      pieData: []
    };
  },
  watch: {
    data: {
      handler() {
        this.renderChart()
      },
      deep: true
    }
  },
  mounted() {
    // 在组件mounted时绑定resize事件，当窗口大小发生变化时自动调整图表大小
    window.addEventListener("resize", this.handleResize)
    // 创建Echarts实例并绘制饼状图
    this.creatCharts();
  },
  beforeDestroy() {
    // 在组件销毁前解绑resize事件
    window.removeEventListener("resize", this.handleResize)
  },
  methods: {
    async creatCharts() {
      this.renderChart()
    },
    // 处理resize事件，调整图表大小
    handleResize() {
      if (this.chart) {
        // 调用Echarts实例的resize方法，重新绘制图表
        this.chart.resize();
      }
    },
    renderChart() {
      const chart = echarts.init(this.$refs.chartpip)
      let option = null
      if (this.data.length > 0) {
        option = {
          title: {
            text: this.title,
            textStyle: {
              fontWeight: "normal",
              fontSize: 16
            },
            left: "center"
          },
          color: ["#6395F9", "#62DAAB", "#657798", "#F6C022", "#E96C5B", "#74CBED", "#9968BD", "#FF9D4E", "#299998"],
          tooltip: {
            trigger: "item"
          },
          legend: {
            orient: 'vertical',
            left: 'left',
            padding: [0, 30, 0, 30]
          },
          series: [
            {
              type: "pie",
              radius: '100%',
              top: '30%',
              bottom: '10%',
              left: "center",
              textStyle: {
                color: "#999",
                fontSize: "12px"
              },
              itemWidth: 6,
              itemHeight: 6,
              label: {
                show: true,
                formatter: "{b}: {d}%"
              },
              data: this.data,
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }
          ]
        };
      } else {
        option = {
          title: {
            text: this.title,
            textStyle: {
              fontWeight: "normal",
              fontSize: 16
            },
            left: "center"
          },
          tooltip: {
            trigger: "none"
          },
          color: ["#d3d3d3"],
          series: [
            {
              type: "pie",
              radius: ["60%", "90%"],
              left: "center",
              label: {
                show: true,
                formatter: "{b}"
              },
              data: [{ value: 1, name: "暂无数据" }]
            }
          ]
        };
      }

      chart.setOption(option)
      this.chart = chart
    }
  }
};
</script>