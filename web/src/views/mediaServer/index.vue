<template>
  <div id="mediaServerManger" class="app-container" style="height: calc(100vh - 124px);">
    <el-form :inline="true" size="mini" style="margin-bottom: 1rem">
      <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">添加节点</el-button>
    </el-form>
    <el-row :gutter="12">
      <el-col v-for="item in mediaServerList" :key="item.id" :span="getNumberByWidth()">
        <el-card shadow="hover" :body-style="{ padding: '0px'}" class="server-card">
          <div v-if="item.type === 'zlm'" class="card-img-zlm" />
          <div v-if="item.type === 'abl'" class="card-img-abl" />
          <div style="padding: 14px;text-align: left">
            <span style="font-size: 16px">{{ item.id }}</span>
            <el-button v-if="!item.defaultServer" icon="el-icon-edit" style="padding: 0;float: right;" type="text" @click="edit(item)">编辑</el-button>
            <el-button v-if="item.defaultServer" icon="el-icon-edit" style="padding: 0;float: right;" type="text" @click="edit(item)">查看</el-button>
            <el-button v-if="!item.defaultServer" icon="el-icon-delete" style="margin-right: 10px;padding: 0;float: right;" type="text" @click="del(item)">移除</el-button>
            <div style="margin-top: 13px; line-height: 12px; ">
              <span style="font-size: 14px; color: #999; margin-top: 5px; ">{{ item.ip }}</span>
              <span style="font-size: 14px; color: #999; margin-top: 5px; float: right;">{{ item.createTime }}</span>
            </div>
          </div>
          <i v-if="item.status" class="iconfont icon-online server-card-status-online" title="在线" />
          <i v-if="!item.status" class="iconfont icon-online server-card-status-offline" title="离线" />
          <i v-if="item.defaultServer" class="server-card-default">默认</i>
        </el-card>
      </el-col>
    </el-row>
    <mediaServerEdit ref="mediaServerEdit" />
  </div>
</template>

<script>
import mediaServerEdit from '../dialog/MediaServerEdit'
export default {
  name: 'MediaServer',
  components: {
    mediaServerEdit
  },
  data() {
    return {
      mediaServerList: [], // 设备列表
      updateLooper: false,
      currentPage: 1,
      count: 15,
      total: 0
    }
  },
  computed: {

  },
  mounted() {
    this.initData()
  },
  destroyed() {},
  methods: {
    initData: function() {
      this.getServerList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getServerList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getServerList()
    },
    getServerList: function() {
      this.$store.dispatch('server/getMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
    },
    add: function() {
      this.$refs.mediaServerEdit.openDialog(null, this.initData)
    },
    edit: function(row) {
      this.$refs.mediaServerEdit.openDialog(row, this.initData)
    },
    del: function(row) {
      this.$confirm('确认删除此节点？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('server/deleteMediaServer', row.id)
          .then((data) => {
            this.$message({
              type: 'success',
              message: '删除成功!'
            })
          })
      }).catch(() => {
      })
    },
    getNumberByWidth() {
      const candidateNums = [1, 2, 3, 4, 6, 8, 12, 24]
      const clientWidth = window.innerWidth - 30
      const interval = 20
      const itemWidth = 360
      const num = (clientWidth + interval) / (itemWidth + interval)
      const result = Math.ceil(24 / num)
      const resultVal = 24
      for (let i = 0; i < candidateNums.length; i++) {
        const value = candidateNums[i]
        if (i + 1 >= candidateNums.length) {
          return 24
        }
        if (value <= result && candidateNums[i + 1] > result) {
          return value
        }
      }

      return resultVal
    }
  }
}
</script>

<style>
  .server-card{
    position: relative;
    margin-bottom: 20px;
  }
  .card-img-zlm{
    width: 200px; height: 200px;
    background: url('../../assets/zlm-logo.png') no-repeat center;
    background-position: center;
    background-size: contain;
    margin: 0 auto;
  }
  .card-img-abl{
    width: 200px; height: 200px;
    background: url('../../assets/abl-logo.jpg') no-repeat center;
    background-position: center;
    background-size: contain;
    margin: 0 auto;
  }
  .server-card-status-online{
    position: absolute;
    right: 20px;
    top: 20px;
    color: #3caf36;
    font-size: 18px;
  }
  .server-card-status-offline{
    position: absolute;
    right: 20px;
    top: 20px;
    color: #808080;
    font-size: 18px;
  }
  .server-card-default{
    position: absolute;
    left: 20px;
    top: 20px;
    color: #808080;
    font-size: 18px;
  }
	.server-card:hover {
    border: 1px solid #adadad;
  }
</style>
