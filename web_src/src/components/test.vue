<template>
<div id="test">
  <div class="timeQuery" id="timeQuery">
    <el-row >
      <el-col :span="24">
        <div class="timeQuery-background"  @mousemove="hoveEvent"></div>
        <div class="timeQuery-pointer">
          <el-tooltip class="item" effect="dark" content="Top Center 提示文字" value="true" manual="true" hide-after="0" placement="top">
            <div class="timeQuery-pointer-content"></div>
          </el-tooltip>
        </div>

        <div class="timeQuery-data" >

          <div class="timeQuery-data-cell" v-for="item of recordData" :style="'width:'  +  getDataWidth(item) + '%; left:' + getDataLeft(item) + '%'"  ></div>
          <!--          <div class="timeQuery-data-cell" style="width: 30%; left: 20%" @click="timeChoose"></div>-->
          <!--          <div class="timeQuery-data-cell" style="width: 60%; left: 20%" @click="timeChoose"></div>-->
        </div>

        <div class="timeQuery-label" >
          <div class="timeQuery-label-cell" style="left: 0%">
            <div class="timeQuery-label-cell-label">0</div>
          </div>
          <div v-for="index of timeNode" class="timeQuery-label-cell" :style="'left:' + (100.0/timeNode*index).toFixed(4) + '%'">
            <div class="timeQuery-label-cell-label">{{24/timeNode * index}}</div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</div>
</template>

<script>
export default {
  name: "test",
  data() {
    return {
      timeNode: 24,
      recordData:[
        {
          startTime: "2021-04-18 00:00:00",
          endTime: "2021-04-18 00:00:09",
        },
        {
          startTime: "2021-04-18 00:00:09",
          endTime: "2021-04-18 01:00:05",
        },
        {
          startTime: "2021-04-18 02:00:01",
          endTime: "2021-04-18 04:25:05",
        },
        {
          startTime: "2021-04-18 05:00:01",
          endTime: "2021-04-18 20:00:05",
        },
      ]
    };
  },
  mounted() {
    for (let i = 1; i <= 24; i++) {
      console.log("<div class=\"timeQuery-label-cell\" style=\"left: " + (100.0/24*i).toFixed(4) + "%\"></div>")
    }
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
