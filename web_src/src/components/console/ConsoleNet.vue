<template>
  <div id="ConsoleNet" style="width: 100%; height: 100%; background: #FFFFFF; text-align: center">
    <ve-line :data="chartData" :extend="extend" :settings="chartSettings" width="100%" height="100%" ></ve-line>
  </div>
</template>

<script>


import moment from "moment/moment";

export default {
  name: 'ConsoleNet',
  data() {
    return {
      chartData: {
        columns: ['time', 'in', 'out'],
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
        tooltip: {
          trigger: 'axis',
          formatter: (data)=>{
            console.log(parseFloat(data[0].data[1]).toFixed(2))
            console.log(parseFloat(data[1].data[1]).toFixed(2))
            console.log("############")
            return "下载：" + parseFloat(data[0].data[1]).toFixed(2) + "Mbps" +  "</br> 上传：" + parseFloat(data[1].data[1]).toFixed(2) + "Mbps";
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
    // setInterval(()=>{
    //   // console.log(111111)
    //   for (let i = 0; i < this.chartData.rows.length; i++) {
    //     this.chartData.rows[i].销售额 += 1000;
    //   }
    // },1000)
  },
  destroyed() {
  },
  methods: {
    setData: function(data) {
      console.log(data)
      this.chartData .rows = data;
    }

  }
};
</script>
