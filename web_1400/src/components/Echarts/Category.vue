<template>
    <div>
        <div style="height: 70%;" ref="chartCategory"></div>
    </div>
</template>
     
<script>
import echarts from "echarts";

export default {
    name: "EchartsCategory",
    props: {
        data: {
            type: Object
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
            this.renderChart();
        },
        // 处理resize事件，调整图表大小
        handleResize() {
            if (this.chart) {
                // 调用Echarts实例的resize方法，重新绘制图表
                this.chart.resize();
            }
        },
        renderChart() {
            const chart = echarts.init(this.$refs.chartCategory)
            let option = null
            if (this.data) {
                option = {
                    title: {
                        left: 'center',
                        textStyle: {
                            fontWeight: "normal",
                            fontSize: 16
                        },
                        text: this.title
                    },
                    xAxis: {
                        type: 'category',
                        data: this.data.keys
                    },
                    yAxis: {
                        type: 'value'
                    },
                    series: [
                        {
                            data: this.data.values,
                            type: 'line',
                            smooth: true
                        }
                    ]
                }
            } else {
                option = {
                    title: {
                        left: 'center',
                        text: this.title || ''
                    },
                    xAxis: {
                        type: 'category',
                        data: []
                    },
                    yAxis: {
                        type: 'value'
                    },
                    series: [
                        {
                            data: [],
                            type: 'line',
                            smooth: true
                        }
                    ]
                }
            }
            chart.setOption(option)
            this.chart = chart
        }
    }
};
</script>