<template>
<div id="test2">
  <div class="timeQuery" style="width: 100%; height: 300px" id="timeQuery">
  </div>
</div>
</template>

<script>

import * as echarts from 'echarts';

export default {
  name: "test2",
  data() {
    return {
    };
  },
  mounted() {
    var base = +new Date("2021-02-02 00:00:00");
    var oneDay = 24 * 3600 * 1000;

    var data = [[base, 10]];

    for (var i = 1; i < 24; i++) {
      var now = new Date(base += oneDay);
      data.push([
        new Date("2021-02-02 " + i+":00:00"), 10
      ]);
    }
    // 基于准备好的dom，初始化echarts实例
    var myChart = echarts.init(document.getElementById('timeQuery'));
    let option = {

      toolbox: {
        feature: {
          dataZoom: {
            yAxisIndex: 'none'
          },
          restore: {},
          saveAsImage: {}
        }
      },
      xAxis: {
        type: 'time',
        boundaryGap: false
      },
      yAxis: {
        type: 'value',
        show: false,
        splitLine:{show: false},   //去除网格线
        boundaryGap: [0, '100%']
      },
      dataZoom: [{
        type: 'inside',
        start: 0,
        end: 20
      }, {
        start: 0,
        end: 20
      }],
      series: [
        {
          name: '模拟数据',
          type: 'line',
          smooth: false,
          symbol: 'none',
          areaStyle: {},
          data: data
        }
      ]
    };
    // 绘制图表
      myChart.setOption(option);
  },
  methods:{
    getTimeNode(){
      let mine = 20
      let width = document.getElementById("timeQuery").offsetWidth
      if (width/20 > 24){
        return 24
      }else if (width/20 > 12) {
        return 12
      }else if (width/20 > 6) {
        return 6
      }
    },
    hoveEvent(event){
      console.log(2222222)
      console.log(event)
    },
    timeChoose(event){
      console.log(event)
    },
    getDataWidth(item){
      let startTime = new Date(item.startTime);
      let endTime = new Date(item.endTime);
      let result = parseFloat((endTime.getTime() - startTime.getTime())/(24*60*60*10))
      // console.log(result)
      return parseFloat((endTime.getTime() - startTime.getTime())/(24*60*60*10))
    },
    getDataLeft(item){
      let startTime = new Date(item.startTime);
      let differenceTime = startTime.getTime() - new Date(item.startTime.substr(0,10) + " 00:00:00").getTime()
      let result = differenceTime/(24*60*60*10)
      console.log(differenceTime)
      console.log(result)
      return parseFloat(differenceTime/(24*60*60*10));
    }
  }
}
</script>

<style scoped>
  .timeQuery{
    width: 96%;
    margin-left: 2%;
    margin-right: 2%;
    margin-top: 20%;
    position: absolute;
  }
  .timeQuery-background{
    height: 16px;
    width: 100%;
    background-color: #ececec;
    position: absolute;
    left: 0;
    top: 0;
    z-index: 10;
    box-shadow: #9d9d9d 0px 0px 10px inset;
  }
  .timeQuery-data{
    height: 16px;
    width: 100%;
    position: absolute;
    left: 0;
    top: 0;
    z-index: 11;
  }
  .timeQuery-data-cell{
    height: 10px;
    background-color: #888787;
    position: absolute;
    z-index: 11;
    -webkit-box-shadow: #9d9d9d 0px 0px 10px inset;
    margin-top: 3px;
  }
  .timeQuery-label{
    height: 16px;
    width: 100%;
    position: absolute;
    pointer-events: none;
    left: 0;
    top: 0;
    z-index: 11;
  }
  .timeQuery-label-cell{
    height: 16px;
    position: absolute;
    z-index: 12;
    width: 0px;
    border-right: 1px solid #b7b7b7;
  }
  .timeQuery-label-cell-label {
    width: 23px;
    text-align: center;
    height: 18px;
    margin-left: -10px;
    margin-top: -30px;
    color: #444;
  }
  .timeQuery-pointer{
    width: 0px;
    height: 18px;
    position: absolute;
    left: 0;
  }
  .timeQuery-pointer-content{
    width: 0px;
    height: 16px;
    position: absolute;
    border-right: 3px solid #f60303;
    z-index: 14;
  }
  /*.timeQuery-cell:after{*/
  /*  content: "";*/
  /*  height: 14px;*/
  /*  border: 1px solid #e70303;*/
  /*  position: absolute;*/
  /*}*/
</style>
