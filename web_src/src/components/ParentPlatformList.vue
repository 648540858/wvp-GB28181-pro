<template>
  <div id="app">
    <el-container>
      <el-header>
        <uiHeader></uiHeader>
      </el-header>
      <el-main>
        <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;">
          <span style="font-size: 1rem; font-weight: bold;">上级平台列表</span>
        </div>
        <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
            <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addParentPlatform">添加</el-button>
        </div>
        <!--设备列表-->
        <el-table :data="platformList" border style="width: 100%" :height="winHeight">
          <el-table-column prop="name" label="名称" width="240" align="center"></el-table-column>
          <el-table-column prop="serverGBId" label="平台编号" width="180" align="center"></el-table-column>
          <el-table-column label="是否启用" width="120" align="center">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.enable">已启用</el-tag>
                <el-tag size="medium" v-if="!scope.row.enable">未启用</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.status">在线</el-tag>
                <el-tag size="medium" type="info" v-if="!scope.row.status">离线</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="地址" width="180" align="center">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium">{{ scope.row.serverIP}}:{{scope.row.serverPort }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="deviceGBId" label="设备国标编号" width="240" align="center"></el-table-column>
          <el-table-column prop="transport" label="信令传输模式" width="120" align="center"></el-table-column>
          <el-table-column prop="channelCount" label="通道数" align="center"></el-table-column>

          <el-table-column label="操作" width="300" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" icon="el-icon-edit" @click="editPlatform(scope.row)">编辑</el-button>
              <el-button size="mini" icon="el-icon-share"  type="primary"  @click="chooseChannel(scope.row)">选择通道</el-button>
              <el-button size="mini" icon="el-icon-delete"  type="danger" @click="deletePlatform(scope.row)">删除</el-button>
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
      </el-main>
    </el-container>
  </div>
</template>

<script>
import platformEdit from './platformEdit.vue'
import uiHeader from './UiHeader.vue'
import chooseChannelDialog from './gb28181/chooseChannel.vue'
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
        that.$axios.post(`/api/platforms/delete`, platform)
            .then(function (res) {
                if (res.data == "success") {
                    that.$message({
                        showClose: true,
                        message: '删除成功',
                        type: 'success'
                    });
                    that.initData()
                }
            })
            .catch(function (error) {
                console.log(error);
            });
    },
    chooseChannel: function(platform) {
       this.$refs.chooseChannelDialog.openDialog(platform.deviceGBId, ()=>{
         this.initData()
       })
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

      this.$axios.get(`/api/platforms/${that.count}/${that.currentPage - 1}`)
        .then(function (res) {
          that.total = res.data.total;
          that.platformList = res.data.list;
        })
        .catch(function (error) {
          console.log(error);
        });

    }

  }
};
</script>
