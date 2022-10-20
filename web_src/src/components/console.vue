<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">控制台</div>
    </div>
    <el-row style="width: 100%">
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="ThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleCPU ref="consoleCPU"></consoleCPU>
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="WorkThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleMem ref="consoleMem"></consoleMem>
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="WorkThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleNet ref="consoleNet"></consoleNet>
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="WorkThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleCPU></consoleCPU>
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="WorkThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleCPU></consoleCPU>
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="WorkThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleCPU></consoleCPU>
          </div>
        </div>
      </el-col>


    </el-row>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import consoleCPU from './console/ConsoleCPU.vue'
import consoleMem from './console/ConsoleMEM.vue'
import consoleNet from './console/ConsoleNet.vue'

import echarts from 'echarts';

export default {
  name: 'app',
  components: {
    echarts,
    uiHeader,
    consoleCPU,
    consoleMem,
    consoleNet
  },
  data() {
    return {
      timer: null
    };
  },
  created() {
    this.getSystemInfo();
    this.loopForSystemInfo();
  },
  destroyed() {
  },
  methods: {
    loopForSystemInfo: function (){
      if (this.timer != null) {
        window.clearTimeout(this.timer);
      }
      this.timer = setTimeout(()=>{
        this.getSystemInfo();
        this.timer = null;
        this.loopForSystemInfo()
      }, 2000)
    },
    getSystemInfo: function (){
      this.$axios({
        method: 'get',
        url: `/api/server/system/info`,
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$refs.consoleCPU.setData(res.data.data.cpu)
          this.$refs.consoleMem.setData(res.data.data.mem)
          this.$refs.consoleNet.setData(res.data.data.net)
        }
      }).catch( (error)=> {
      });
    }
  }
};
</script>

<style>
#app {
  height: 100%;
}
.control-cell {
  padding-top: 10px;
  padding-left: 5px;
  padding-right: 10px;
  height: 360px;
}
</style>
