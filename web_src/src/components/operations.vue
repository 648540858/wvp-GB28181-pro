<template>
  <div id="operations" >
    <el-container class="container-box">
      <el-aside width="200px" style="text-align: left" >
        <el-menu :default-active="activeIndex" :height="winHeight" @select="handleSelect">
          <el-menu-item index="systemInfo">
            <template slot="title"><i class="el-icon-s-home"></i>平台信息</template>
          </el-menu-item>
          <el-submenu index="log">
            <template slot="title"><i class="el-icon-document"></i>日志信息</template>
            <el-menu-item index="historyLog">历史日志</el-menu-item>
            <el-menu-item index="realTimeLog">运行日志</el-menu-item>
          </el-submenu>
          <el-submenu index="senior">
            <template slot="title"><i class="el-icon-setting"></i>高级维护</template>
            <el-menu-item disabled="disabled" index="tcpdump">网络抓包</el-menu-item>
          </el-submenu>
        </el-menu>
      </el-aside>
      <el-main style="background-color: #FFFFFF; margin: 0 20px 20px 20px">
        <operationsForRealLog v-if="activeIndex==='realTimeLog'"></operationsForRealLog>
        <operationsForHistoryLog v-if="activeIndex==='historyLog'"></operationsForHistoryLog>
        <operationsForSystemInfo v-if="activeIndex==='systemInfo'"></operationsForSystemInfo>
      </el-main>
    </el-container>
  </div>
</template>

<script>

import operationsForRealLog from './operationsForRealLog'
import operationsForHistoryLog from './operationsForHistoryLog.vue'
import operationsForSystemInfo from './operationsForSystemInfo.vue'


export default {
  name: 'log',
  components: {
    operationsForRealLog, operationsForHistoryLog, operationsForSystemInfo
  },
  data() {
    return {
      loading: false,
      winHeight: (window.innerHeight - 170) + "px",
      data: [],
      filter: "",
      activeIndex: "systemInfo"
    };
  },

  created() {
    console.log('created');
  },
  destroyed() {
  },
  methods: {
    handleSelect: function (index) {
      this.activeIndex = index;
    },
  }
};
</script>

<style>
.container-box {
  position: absolute;
  top: 80px;
  width: calc(100% - 20px);
  height: calc(100% - 80px);
}
</style>
