<template>
  <div id="operations" style="width: 100%; height: 100%">
    <el-container >
      <el-aside width="200px" style="text-align: left">
        <el-menu :default-active="activeIndex" :height="winHeight" @select="handleSelect">
          <el-menu-item index="systemInfo">
            <template slot="title"><i class="el-icon-message"></i>平台信息</template>
          </el-menu-item>
          <el-submenu index="log">
            <template slot="title"><i class="el-icon-message"></i>日志信息</template>
            <el-menu-item index="historyLog">历史日志</el-menu-item>
            <el-menu-item index="realTimeLog">实时日志</el-menu-item>
          </el-submenu>
          <el-submenu index="senior">
            <template slot="title"><i class="el-icon-setting"></i>高级维护</template>
            <el-menu-item disabled="disabled" index="tcpdump">网络抓包</el-menu-item>
            <el-menu-item disabled="disabled" index="networkCard">网卡信息</el-menu-item>
          </el-submenu>
        </el-menu>
      </el-aside>
      <el-main style="background-color: #FFFFFF; margin: 20px">
        <operationsForRealLog v-if="activeIndex==='realTimeLog'"></operationsForRealLog>
        <operationsForHistoryLog v-if="activeIndex==='historyLog'"></operationsForHistoryLog>
      </el-main>
    </el-container>
  </div>
</template>

<script>

import operationsForRealLog from './operationsForRealLog'
import operationsForHistoryLog from './operationsForHistoryLog.vue'


export default {
  name: 'log',
  components: {
    operationsForRealLog, operationsForHistoryLog
  },
  data() {
    return {
      loading: false,
      winHeight: (window.innerHeight - 170) + "px",
      data: [],
      filter: "",
      activeIndex: "historyLog"
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
.videoList {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
}

.video-item {
  position: relative;
  width: 15rem;
  height: 10rem;
  margin-right: 1rem;
  background-color: #000000;
}

.video-item-img {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 100%;
  height: 100%;
}

.video-item-img:after {
  content: "";
  display: inline-block;
  position: absolute;
  z-index: 2;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 3rem;
  height: 3rem;
  background-image: url("../assets/loading.png");
  background-size: cover;
  background-color: #000000;
}

.video-item-title {
  position: absolute;
  bottom: 0;
  color: #000000;
  background-color: #ffffff;
  line-height: 1.5rem;
  padding: 0.3rem;
  width: 14.4rem;
}
</style>
