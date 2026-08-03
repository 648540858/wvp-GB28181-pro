<template>
  <div id="dashboardPage" class="app-container dashboard-page">
    <el-row :gutter="[16, 16]" class="dashboard-grid">
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleCPU ref="consoleCPU" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleResource ref="consoleResource" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleNet ref="consoleNet" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div class="control-cell">
          <div style="width:100%; height:100%; ">

            <consoleMem ref="consoleMem" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div class="control-cell">
          <div style="width:100%; height:100%; ">
            <consoleNodeLoad ref="consoleNodeLoad" />
          </div>
        </div>
      </el-col>
      <el-col :xl="{ span: 8 }" :lg="{ span: 8 }" :md="{ span: 12 }" :sm="{ span: 12 }" :xs="{ span: 24 }">
        <div class="control-cell">
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
  unmounted() {
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

<style scoped>
.dashboard-page {
  min-height: calc(100dvh - var(--wvp-shell-height));
}

.dashboard-grid {
  width: 100%;
}

.control-cell {
  height: 360px;
}

.control-cell > div {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--wvp-surface);
  border: 1px solid var(--wvp-border-light);
  border-radius: var(--wvp-radius-lg);
  box-shadow: var(--wvp-shadow-sm);
}

@media (max-width: 768px) {
  .control-cell {
    height: 320px;
  }
}
</style>
