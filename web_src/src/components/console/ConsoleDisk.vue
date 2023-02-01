<template>
  <div id="ConsoleNet" style="width: 100%; height: 100%; background: #FFFFFF; text-align: center">
    <ve-bar ref="ConsoleNet" :data="chartData" :extend="extend" :settings="chartSettings" width="100%" height="100%" ></ve-bar>
  </div>
</template>

<script>


import moment from "moment/moment";

export default {
  name: 'ConsoleNet',
  data() {
    return {
      chartData: {
        columns: ['path','free','use'],
        rows: []
      },
      chartSettings: {
        stack: {
          'xxx': ['free', 'use']
        },
        labelMap: {
          'free': '剩余',
          'use': '已使用'
        },
      },
      extend: {
        title: {
          show: true,
          text: "磁盘",
          left: "center",
          top: 20,
        },
        grid: {
          show: true,
          right: "30px",
          containLabel: true,
        },
        series: {
          barWidth: 30
        },
        legend: {
          left: "center",
          bottom: "15px",
        },
        tooltip: {
          trigger: 'axis',
          formatter: (data)=>{
            console.log(data)
            let relVal = "";
            for (let i = 0; i < data.length; i++) {
              relVal +=  data[i].marker + data[i].seriesName + ":" + data[i].value.toFixed(2) + "GB"
              if (i < data.length - 1) {
                relVal += "</br>";
              }
            }
            return relVal;
          }
        },

      }
    };
  },
  mounted() {
    this.$nextTick(_ => {
      setTimeout(()=>{
        this.$refs.ConsoleNet.echarts.resize()
      }, 100)
    })
  },
  destroyed() {
  },
  methods: {
    setData: function(data) {
      this.chartData.rows = data;
    }
  }
};
</script>
