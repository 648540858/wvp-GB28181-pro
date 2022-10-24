<template>
  <div id="ConsoleMEM" style="width: 100%; height: 100%; background: #FFFFFF; text-align: center">
    <ve-line ref="ConsoleMEM" :data="chartData" :extend="extend"  width="100%" height="100%" :legend-visible="false"></ve-line>
  </div>
</template>

<script>


import moment from "moment/moment";

export default {
  name: 'ConsoleMEM',
  data() {
    return {
      chartData: {
        columns: ['time', 'data'],
        rows: []
      },

      extend: {
        title: {
          show: true,
          text: "内存",
          left: "center",
          top: 20,

        },
        grid: {
          show: true,
          right: "30px",
          containLabel: true,
        },
        xAxis: {
          time: "time",
          max: 'dataMax',
          boundaryGap: ['20%', '20%'],
          axisLabel: {
            formatter:(v)=>{
              return moment(v).format("HH:mm:ss");
            },
            showMaxLabel: true,
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 1,
          splitNumber: 6,
          position: "left",
          silent: true,
          axisLabel: {
            formatter: (v)=>{
              return v*100 + "%";
            },
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: (data)=>{
            console.log(data)
            return moment(data[0].data[0]).format("HH:mm:ss") +  "</br>"+ data[0].marker +" 使用：" + (data[0].data[1]*100).toFixed(2) + "%";
          }
        },
        series: {
          itemStyle: {
            color: "#409EFF"
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: '#50a3f8' // 0% 处的颜色
              }, {
                offset: 1, color: '#69b0fa' // 100% 处的颜色
              }],
              global: false // 缺省为 false
            }
          }
        }
      }
    };
  },
  mounted() {
    this.$nextTick(_ => {
      setTimeout(()=>{
        this.$refs.ConsoleMEM.echarts.resize()
      }, 100)
    })
  },
  destroyed() {
  },
  methods: {
    setData: function(data) {
      this.chartData .rows = data;
    }
  }
};
</script>
