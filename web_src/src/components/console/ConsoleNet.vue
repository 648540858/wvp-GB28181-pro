<template>
  <div id="ConsoleNet" style="width: 100%; height: 100%; background: #FFFFFF; text-align: center">
    <ve-line ref="ConsoleNet" :data="chartData" :extend="extend" :settings="chartSettings" width="100%" height="100%" ></ve-line>
  </div>
</template>

<script>


import moment from "moment/moment";

export default {
  name: 'ConsoleNet',
  data() {
    return {
      chartData: {
        columns: ['time','out','in'],
        rows: []
      },
      chartSettings: {
        area: true,
        labelMap: {
          'in': '下载',
          'out': '上传'
        },
      },
      extend: {
        title: {
          show: true,
          text: "网络",
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
          },
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 1000,
          splitNumber: 6,
          position: "left",
          silent: true,
        },
        tooltip: {
          trigger: 'axis',
          formatter: (data)=>{
            return data[1].marker + "下载：" + parseFloat(data[1].data[1]).toFixed(2) + "Mbps" +  "</br> "+ data[0].marker +" 上传：" + parseFloat(data[0].data[1]).toFixed(2) + "Mbps";
          }
        },
        legend: {
          left: "center",
          bottom: "15px",
        }
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
    setData: function(data, total) {
      this.chartData .rows = data;
      this.extend.yAxis.max= total;
    }

  }
};
</script>
