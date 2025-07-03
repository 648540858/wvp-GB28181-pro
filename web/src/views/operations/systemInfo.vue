<template>
  <div id="operationsForSystemInfo" class="app-container" style="margin: 100px 200px">
    <el-descriptions v-for="(value, key) in systemInfoList" :key="key" :title="key" :column="2" :loading="loading" style="margin-bottom: 30px">
      <el-descriptions-item v-for="(childValue, childKey) in value" :key="childKey">
        <template slot="label">
          <span>{{ childKey }}</span>
        </template>
        <span v-if="!childValue.startsWith('http')">{{ childValue }}</span>
        <a v-else target="_blank" :href="childValue">{{ childValue }}</a>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script>

export default {
  name: 'OperationsSystemInfo',
  data() {
    return {
      loading: false,
      winHeight: window.innerHeight - 220,
      systemInfoList: {}
    }
  },
  created() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.loading = true
      this.$store.dispatch('server/info')
        .then(data => {
          this.systemInfoList = data
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
