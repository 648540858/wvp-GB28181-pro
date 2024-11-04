<template>
  <div id="operationsForSystemInfo" style="margin: 40px">
    <el-descriptions v-for="(value, key) in systemInfoList" :key="key" :column="2" :loading="loading">
      <template slot="title">
        <span>{{key}}</span>
      </template>
      <el-descriptions-item v-for="(childValue, childKey) in value" :key="childKey" >
        <template slot="label">
          <span>{{childKey}}</span>
        </template>
        {{ childValue }}
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script>

export default {
  name: 'operationsForSystemInfo',
  data() {
    return {
      loading: false,
      winHeight: window.innerHeight - 220,
      systemInfoList: {
        "测试": {
          "qwqew": "1111"
        }
      },
    };
  },
  created() {
    this.initData()
  },
  methods: {
    initData: function () {
      this.loading = true;
      this.$axios({
        method: 'get',
        url: `/api/server/info`,
      }).then((res) => {
        console.log(res)
        if (res.data.code === 0) {
          this.systemInfoList = res.data.data;
        }
        this.loading = false;
      }).catch((error) => {
        console.log(error);
        this.loading = false;
      });
    },
  }
};
</script>
