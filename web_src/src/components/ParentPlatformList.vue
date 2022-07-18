<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">上级平台列表</div>
      <div class="page-header-btn">
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addParentPlatform">添加</el-button>
        <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
      </div>
    </div>

    <!--设备列表-->
    <el-table :data="platformList" style="width: 100%" :height="winHeight">
      <el-table-column prop="name" label="名称" ></el-table-column>
      <el-table-column prop="serverGBId" label="平台编号" min-width="200"></el-table-column>
      <el-table-column label="是否启用" min-width="80" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.enable">已启用</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.enable">未启用</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="80" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.status">在线</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.status">离线</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="地址" min-width="160" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium">{{ scope.row.serverIP}}:{{scope.row.serverPort }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="deviceGBId" label="设备国标编号" min-width="200" ></el-table-column>
      <el-table-column prop="transport" label="信令传输模式" min-width="120" ></el-table-column>
      <el-table-column prop="channelCount" label="通道数" min-width="120" ></el-table-column>
      <el-table-column label="订阅信息" min-width="120"  fixed="right">
        <template slot-scope="scope">
          <i v-if="scope.row.alarmSubscribe" style="font-size: 20px" title="报警订阅" class="iconfont icon-gbaojings subscribe-on " ></i>
          <i v-if="!scope.row.alarmSubscribe" style="font-size: 20px" title="报警订阅" class="iconfont icon-gbaojings subscribe-off " ></i>
          <i v-if="scope.row.catalogSubscribe" title="目录订阅"  class="iconfont icon-gjichus subscribe-on" ></i>
          <i v-if="!scope.row.catalogSubscribe" title="目录订阅" class="iconfont icon-gjichus subscribe-off" ></i>
          <i v-if="scope.row.mobilePositionSubscribe" title="位置订阅" class="iconfont icon-gxunjians subscribe-on" ></i>
          <i v-if="!scope.row.mobilePositionSubscribe" title="位置订阅" class="iconfont icon-gxunjians subscribe-off" ></i>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="240" fixed="right">
        <template slot-scope="scope">
          <el-button size="medium" icon="el-icon-edit" type="text" @click="editPlatform(scope.row)">编辑</el-button>
          <el-button size="medium" icon="el-icon-share"  type="text"  @click="chooseChannel(scope.row)">选择通道</el-button>
          <el-button size="medium" icon="el-icon-delete"  type="text" style="color: #f56c6c" @click="deletePlatform(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="float: right"
      @size-change="handleSizeChange"
      @current-change="currentChange"
      :current-page="currentPage"
      :page-size="count"
      :page-sizes="[15, 25, 35, 50]"
      layout="total, sizes, prev, pager, next"
      :total="total">
    </el-pagination>
    <platformEdit ref="platformEdit" ></platformEdit>
    <chooseChannelDialog ref="chooseChannelDialog" ></chooseChannelDialog>
  </div>
</template>

<script>
import platformEdit from './dialog/platformEdit.vue'
import uiHeader from '../layout/UiHeader.vue'
import chooseChannelDialog from './dialog/chooseChannel.vue'
export default {
  name: 'app',
  components: {
    platformEdit,
    uiHeader,
    chooseChannelDialog
  },
  data() {
    return {
      platformList: [], //设备列表

      winHeight: window.innerHeight - 260,
      currentPage:1,
      count:15,
      total:0
    };
  },
  computed: {

    getcurrentDeviceChannels: function() {

    }
  },
  mounted() {
    this.initData();
    this.updateLooper = setInterval(this.initData, 10000);
  },
  destroyed() {
    clearTimeout(this.updateLooper);
  },
  methods: {
    addParentPlatform: function() {
      this.$refs.platformEdit.openDialog(null, this.initData)
    },
    editPlatform: function(platform) {
      console.log(platform)
      this.$refs.platformEdit.openDialog(platform, this.initData)
    },
    deletePlatform: function(platform) {
        var that = this;
        that.$confirm('确认删除?', '提示', {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }).then(() => {
                that.deletePlatformCommit(platform)
            })
    },
    deletePlatformCommit: function(platform) {
        var that = this;
        that.$axios({
		method: 'delete',
		url:`/api/platform/delete/${platform.serverGBId}`
        }).then(function (res) {
            if (res.data == "success") {
                that.$message({
                    showClose: true,
                    message: '删除成功',
                    type: 'success'
                });
                that.initData()
            }
        }).catch(function (error) {
            console.log(error);
        });
    },
    chooseChannel: function(platform) {
       this.$refs.chooseChannelDialog.openDialog(platform.serverGBId, platform.name, platform.catalogId, platform.treeType, this.initData)
    },
    initData: function() {
      this.getPlatformList();
    },
    currentChange: function(val){
      this.currentPage = val;
      this.getPlatformList();
    },
    handleSizeChange: function(val){
      this.count = val;
      this.getPlatformList();
    },
    getPlatformList: function() {
      let that = this;

      this.$axios({
      	method: 'get',
        url:`/api/platform/query/${that.count}/${that.currentPage}`
      }).then(function (res) {
        that.total = res.data.total;
        that.platformList = res.data.list;
      }).catch(function (error) {
        console.log(error);
      });

    },
    refresh: function (){
      this.initData();
    }

  }
};
</script>
<style>
.subscribe-on{
  color: #409EFF;
  font-size: 18px;
}
.subscribe-off{
  color: #afafb3;
  font-size: 18px;
}
</style>
