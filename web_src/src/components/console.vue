<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">控制台</div>
      <div class="page-header-btn">
        <el-button icon="el-icon-info" size="mini" style="margin-right: 1rem;" type="primary" @click="showInfo">平台信息
        </el-button>
      </div>
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
            <consoleResource ref="consoleResource"></consoleResource>
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

            <consoleMem ref="consoleMem"></consoleMem>
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="WorkThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleNodeLoad ref="consoleNodeLoad"></consoleNodeLoad>
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }" >
        <div class="control-cell" id="WorkThreadsLoad" >
          <div style="width:100%; height:100%; ">
            <consoleDisk ref="consoleDisk"></consoleDisk>
          </div>
        </div>
      </el-col>
    </el-row>
    <configInfo ref="configInfo"></configInfo>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import consoleCPU from './console/ConsoleCPU.vue'
import consoleMem from './console/ConsoleMEM.vue'
import consoleNet from './console/ConsoleNet.vue'
import consoleNodeLoad from './console/ConsoleNodeLoad.vue'
import consoleDisk from './console/ConsoleDisk.vue'
import consoleResource from './console/ConsoleResource.vue'
import configInfo from './dialog/configInfo.vue'

import echarts from 'echarts';

export default {
  name: 'app',
  components: {
    echarts,
    uiHeader,
    consoleCPU,
    consoleMem,
    consoleNet,
    consoleNodeLoad,
    consoleDisk,
    consoleResource,
    configInfo,
  },
  data() {
    return {
      timer: null,
    };
  },
  created() {
    this.getSystemInfo();
    this.getLoad();
    this.getResourceInfo();
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
        if (this.$route.path === "/console") {
          this.getSystemInfo();
          this.getLoad();
          this.timer = null;
          this.loopForSystemInfo()
          this.getResourceInfo()
        }

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
          this.$refs.consoleNet.setData(res.data.data.net, res.data.data.netTotal)
          this.$refs.consoleDisk.setData(res.data.data.disk)
        }
      }).catch( (error)=> {
      });
    },
    getLoad: function (){
      this.$axios({
        method: 'get',
        url: `/api/server/media_server/load`,
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$refs.consoleNodeLoad.setData(res.data.data)
        }
      }).catch( (error)=> {
      });
    },
    getResourceInfo: function (){
      this.$axios({
        method: 'get',
        url: `/api/server/resource/info`,
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$refs.consoleResource.setData(res.data.data)
        }
      }).catch( (error)=> {
      });
    },
    showInfo: function (){

      this.$axios({
        method: 'get',
        url: `/api/server/system/configInfo`,
      }).then( (res)=> {
        console.log(res)
        if (res.data.code === 0) {
          console.log(2222)
          console.log(this.$refs.configInfo)
          this.$refs.configInfo.openDialog(res.data.data)
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
