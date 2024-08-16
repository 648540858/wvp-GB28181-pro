<template>
  <div id="app" style="width: 100%">
    <div v-if="!platform">
      <div class="page-header">
        <div class="page-title">上级平台列表</div>
        <div class="page-header-btn">
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addParentPlatform">添加</el-button>
          <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
        </div>
      </div>

      <!--设备列表-->
      <el-table size="medium"  :data="platformList" style="width: 100%" :height="winHeight">
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
              <el-tag size="medium">{{ scope.row.serverIp}}:{{scope.row.serverPort }}</el-tag>
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
            <el-button size="medium" icon="el-icon-share"  type="text"  @click="chooseChannel(scope.row)">通道共享</el-button>
            <el-button size="medium" icon="el-icon-delete"  type="text" style="color: #f56c6c" @click="deletePlatform(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        @size-change="handleSizeChange"
        @current-change="currentChange"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total">
      </el-pagination>
    </div>

    <platformEdit ref="platformEdit" v-if="platform" v-model="platform" :closeEdit="closeEdit" :device-ips="deviceIps" ></platformEdit>
    <shareChannel ref="shareChannel" ></shareChannel>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import shareChannel from './dialog/shareChannel.vue'
import platformEdit from './PlatformEdit.vue'
import streamProxyEdit from "./dialog/StreamProxyEdit.vue";
export default {
  name: 'app',
  components: {
    streamProxyEdit,
    uiHeader,
    shareChannel,
    platformEdit
  },
  data() {
    return {
      platformList: [], //设备列表
      deviceIps: [], //设备列表
      defaultPlatform: null,
      platform: null,
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
      this.platform = this.defaultPlatform;
    },
    editPlatform: function(platform) {
      this.platform = platform;
    },
    closeEdit: function() {
      this.platform = null;
      this.getPlatformList()
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
            if (res.data.code === 0) {
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
      this.$refs.shareChannel.openDialog(platform.id, this.initData)
    },
    initData: function() {
      this.$axios({
        method: 'get',
        url: `/api/platform/server_config`
      }).then((res)=> {
        if (res.data.code === 0) {
          this.deviceIps = res.data.data.deviceIp.split(',');
          this.defaultPlatform = {
            id: null,
            enable: true,
            ptz: true,
            rtcp: false,
            asMessageChannel: false,
            autoPushChannel: false,
            name: null,
            serverGBId: null,
            serverGBDomain: null,
            serverIp: null,
            serverPort: null,
            deviceGBId: res.data.data.username,
            deviceIp: this.deviceIps[0],
            devicePort: res.data.data.devicePort,
            username: res.data.data.username,
            password: res.data.data.password,
            expires: 3600,
            keepTimeout: 60,
            transport: "UDP",
            characterSet: "GB2312",
            startOfflinePush: false,
            customGroup: false,
            catalogWithPlatform: false,
            catalogWithGroup: false,
            catalogWithRegion: false,
            manufacturer: null,
            model: null,
            address: null,
            secrecy: 1,
            catalogGroup: 1,
            civilCode: null,
            sendStreamIp: res.data.data.sendStreamIp,
          }
        }
      }).catch(function (error) {
        console.log(error);
      });
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
        if (res.data.code === 0) {
          that.total = res.data.data.total;
          that.platformList = res.data.data.list;
        }

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
