<template>
  <div id="app" class="app-container" style="height: calc(100vh - 118px); background-color: rgba(242,242,242,0.50)">
    <el-row style="width: 100%;height: 100%;">
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="ThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleCPU ref="consoleCPU" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleResource ref="consoleResource" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleNet ref="consoleNet" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">

            <consoleMem ref="consoleMem" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleNodeLoad ref="consoleNodeLoad" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div id="WorkThreadsLoad" class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleDisk ref="consoleDisk" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import consoleCPU from './console/ConsoleCPU.vue'
import consoleMem from './console/ConsoleMEM.vue'
import consoleNet from './console/ConsoleNet.vue'
import consoleNodeLoad from './console/ConsoleNodeLoad.vue'
import consoleDisk from './console/ConsoleDisk.vue'
import consoleResource from './console/ConsoleResource.vue'

export default {
  name: 'Dashboard',
  components: {
    consoleCPU,
    consoleMem,
    consoleNet,
    consoleNodeLoad,
    consoleDisk,
    consoleResource
  },
  data() {
    return {
      timer: null
    }
  },
  created() {
    this.getSystemInfo()
    this.getLoad()
    this.getResourceInfo()
    this.loopForSystemInfo()
  },
  destroyed() {
    window.clearImmediate(this.timer)
  },
  methods: {
    loopForSystemInfo: function() {
      if (this.timer != null) {
        window.clearTimeout(this.timer)
      }
      this.timer = setTimeout(() => {
        console.log(this.$route.name)
        if (this.$route.name === '控制台') {
          this.getSystemInfo()
          this.getLoad()
          this.timer = null
          this.loopForSystemInfo()
          this.getResourceInfo()
        }
      }, 2000)
    },
    getSystemInfo: function() {
      this.$store.dispatch('server/getSystemInfo')
        .then(data => {
          this.$refs.consoleCPU.setData(data.cpu)
          this.$refs.consoleMem.setData(data.mem)
          this.$refs.consoleNet.setData(data.net, data.netTotal)
          this.$refs.consoleDisk.setData(data.disk)
        })
    },
    getLoad: function() {
      this.$store.dispatch('server/getMediaServerLoad')
        .then(data => {
          this.$refs.consoleNodeLoad.setData(data)
        })
    },
    getResourceInfo: function() {
      this.$store.dispatch('server/getResourceInfo')
        .then(data => {
          this.$refs.consoleResource.setData(data)
        })
    }
  }
}
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
