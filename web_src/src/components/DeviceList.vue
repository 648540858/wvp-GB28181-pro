<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">设备列表</div>
      <div class="page-header-btn">
        搜索:
        <el-input @input="initData" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                  prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>
        在线状态:
        <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="initData" v-model="online" placeholder="请选择"
                   default-first-option>
          <el-option label="全部" value=""></el-option>
          <el-option label="在线" value="true"></el-option>
          <el-option label="离线" value="false"></el-option>
        </el-select>
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">添加设备
        </el-button>
        <el-button icon="el-icon-info" size="mini" style="margin-right: 1rem;" type="primary" @click="showInfo()">平台信息
        </el-button>
        <el-button icon="el-icon-refresh-right" circle size="mini" :loading="getDeviceListLoading"
                   @click="getDeviceList()"></el-button>
      </div>
    </div>
    <!--设备列表-->
    <el-table size="medium" :data="deviceList" style="width: 100%;font-size: 12px;" :height="$tableHeght" header-row-class-name="table-header">
      <el-table-column prop="name" label="名称" min-width="160">
      </el-table-column>
      <el-table-column prop="deviceId" label="设备编号" min-width="160" >
      </el-table-column>
      <el-table-column label="地址" min-width="160" >
        <template v-slot:default="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag v-if="scope.row.hostAddress" size="medium">{{ scope.row.hostAddress }}</el-tag>
            <el-tag v-if="!scope.row.hostAddress" size="medium">未知</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="manufacturer" label="厂家" min-width="100" >
      </el-table-column>
      <el-table-column prop="transport" label="信令传输模式" min-width="100" >
      </el-table-column>
      <el-table-column label="流传输模式"  min-width="160" >
        <template v-slot:default="scope">
          <el-select size="mini" @change="transportChange(scope.row)" v-model="scope.row.streamMode" placeholder="请选择" style="width: 120px">
            <el-option key="UDP" label="UDP" value="UDP"></el-option>
            <el-option key="TCP-ACTIVE" label="TCP主动模式"  value="TCP-ACTIVE"></el-option>
            <el-option key="TCP-PASSIVE" label="TCP被动模式" value="TCP-PASSIVE"></el-option>
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="通道数" min-width="100" >
        <template v-slot:default="scope">
          <span style="font-size: 1rem">{{scope.row.channelCount}}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="100">
        <template v-slot:default="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.onLine && Vue.prototype.$myServerId !== scope.row.serverId" style="border-color: #ecf1af">在线</el-tag>
            <el-tag size="medium" v-if="scope.row.onLine && Vue.prototype.$myServerId === scope.row.serverId">在线</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.onLine">离线</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="订阅"  min-width="260" >
        <template v-slot:default="scope">
          <el-checkbox label="目录" :checked="scope.row.subscribeCycleForCatalog > 0" @change="(e)=>subscribeForCatalog(scope.row.id, e)"></el-checkbox>
          <el-checkbox label="位置" :checked="scope.row.subscribeCycleForMobilePosition > 0" @change="(e)=>subscribeForMobilePosition(scope.row.id, e)"></el-checkbox>
          <el-checkbox label="报警" disabled :checked="scope.row.subscribeCycleForAlarm > 0"></el-checkbox>
        </template>
      </el-table-column>
      <el-table-column prop="keepaliveTime" label="最近心跳" min-width="140" >
      </el-table-column>
      <el-table-column prop="registerTime" label="最近注册"  min-width="140">
      </el-table-column>
      <el-table-column label="操作" min-width="300" fixed="right">
        <template v-slot:default="scope">
          <el-button type="text" size="medium" v-bind:disabled="scope.row.online===0" icon="el-icon-refresh" @click="refDevice(scope.row)"
                     @mouseover="getTooltipContent(scope.row.deviceId)">刷新
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button type="text" size="medium" icon="el-icon-video-camera"
                     @click="showChannelList(scope.row)">通道
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">编辑</el-button>
          <el-divider direction="vertical"></el-divider>
          <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
            <el-button size="medium" type="text" >
              操作<i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
            <el-dropdown-menu>
              <el-dropdown-item command="delete" style="color: #f56c6c">
                删除</el-dropdown-item>
              <el-dropdown-item command="setGuard" v-bind:disabled="!scope.row.onLine">
                布防</el-dropdown-item>
              <el-dropdown-item command="resetGuard" v-bind:disabled="!scope.row.onLine">
                撤防</el-dropdown-item>
              <el-dropdown-item command="syncBasicParam" v-bind:disabled="!scope.row.onLine">
                基础配置同步</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
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
    <deviceEdit ref="deviceEdit"></deviceEdit>
    <syncChannelProgress ref="syncChannelProgress"></syncChannelProgress>
    <configInfo ref="configInfo"></configInfo>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import deviceEdit from './dialog/deviceEdit.vue'
import syncChannelProgress from './dialog/SyncChannelProgress.vue'
import configInfo from "./dialog/configInfo.vue";
import Vue from "vue";

export default {
  name: 'app',
  components: {
    configInfo,
    uiHeader,
    deviceEdit,
    syncChannelProgress,
  },
  data() {
    return {
      deviceList: [], //设备列表
      currentDevice: {}, //当前操作设备对象
      searchSrt: "",
      online: null,
      videoComponentList: [],
      updateLooper: 0, //数据刷新轮训标志
      currentDeviceChannelsLength: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      getDeviceListLoading: false,
    };
  },
  computed: {
    Vue() {
      return Vue
    },
    getcurrentDeviceChannels: function () {
      let data = this.currentDevice['channelMap'];
      let channels = null;
      if (data) {
        channels = Object.keys(data).map(key => {
          return data[key];
        });
        this.currentDeviceChannelsLength = channels.length;
      }
      return channels;
    }
  },
  mounted() {
    this.initData();
    this.updateLooper = setInterval(this.getDeviceList, 10000);
  },
  destroyed() {
    this.$destroy('videojs');
    clearTimeout(this.updateLooper);
  },
  methods: {
    initData: function () {
      this.currentPage = 1;
      this.total= 0;
      this.getDeviceList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.getDeviceList();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getDeviceList();
    },
    getDeviceList: function () {
      this.getDeviceListLoading = true;
      this.$axios({
        method: 'get',
        url: `/api/device/query/devices`,
        params: {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
          status: this.online,
        }
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.total = res.data.data.total;
          this.deviceList = res.data.data.list;
        }
        this.getDeviceListLoading = false;
      }).catch( (error)=> {
        console.error(error);
        this.getDeviceListLoading = false;
      });
    },
    deleteDevice: function (row) {
      let msg = "确定删除此设备？"
      if (row.online !== 0) {
        msg = "在线设备删除后仍可通过注册再次上线。<br/>如需彻底删除请先将设备离线。<br/><strong>确定删除此设备？</strong>"
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$axios({
          method: 'delete',
          url: `/api/device/query/devices/${row.deviceId}/delete`
        }).then((res) => {
          this.getDeviceList();
        }).catch((error) => {
          console.error(error);
        });
      }).catch(() => {

      });


    },
    showChannelList: function (row) {
      this.$router.push(`/channelList/${row.deviceId}/0`);
    },
    showDevicePosition: function (row) {
      this.$router.push(`/map?deviceId=${row.deviceId}`);
    },

    //gb28181平台对接
    //刷新设备信息
    refDevice: function (itemData) {
      console.log("刷新对应设备:" + itemData.deviceId);
      let that = this;
      this.$axios({
        method: 'get',
        url: '/api/device/query/devices/' + itemData.deviceId + '/sync'
      }).then((res) => {
        console.log("刷新设备结果：" + JSON.stringify(res));
        if (res.data.code !== 0) {
          that.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        } else {
          if (res.data.data && res.data.data.errorMsg) {
            that.$message({
              showClose: true,
              message: res.data.data.errorMsg,
              type: 'error'
            });
            return;
          }

          this.$refs.syncChannelProgress.openDialog(itemData.deviceId, ()=>{
            console.log(32322)
            this.initData()
          })
        }
        that.initData()
      }).catch((e) => {
        console.error(e)
        that.$message({
          showClose: true,
          message: e,
          type: 'error'
        });
      });

    },

    getTooltipContent: async function (deviceId) {
      let result = "";
      await this.$axios({
        method: 'get',
        async: false,
        url: `/api/device/query/${deviceId}/sync_status/`,
      }).then((res) => {
        if (res.data.code == 0) {
          if (res.data.data.errorMsg !== null) {
            result = res.data.data.errorMsg
          } else if (res.data.msg !== null) {
            result = res.data.msg
          } else {
            result = `同步中...[${res.data.data.current}/${res.data.data.total}]`;
          }
        }
      })
      return result;
    },
    transportChange: function (row) {
      console.log(`修改传输方式为 ${row.streamMode}：${row.deviceId} `);
      let that = this;
      this.$axios({
        method: 'post',
        url: '/api/device/query/transport/' + row.deviceId + '/' + row.streamMode
      }).then(function (res) {

      }).catch(function (e) {
      });
    },
    edit: function (row) {
      this.$refs.deviceEdit.openDialog(row, () => {
        this.$refs.deviceEdit.close();
        this.$message({
          showClose: true,
          message: "设备修改成功，通道字符集将在下次更新生效",
          type: "success",
        });
        setTimeout(this.getDeviceList, 200)

      })
    },
    add: function () {
      this.$refs.deviceEdit.openDialog(null, () => {
        this.$refs.deviceEdit.close();
        this.$message({
          showClose: true,
          message: "添加成功",
          type: "success",
        });
        setTimeout(this.getDeviceList, 200)

      })
    },
    showInfo: function (){
      this.$axios({
        method: 'get',
        url: `/api/server/system/configInfo`,
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.serverId = res.data.data.addOn.serverId;
          this.$refs.configInfo.openDialog(res.data.data)
        }
      }).catch( (error)=> {
      });
    },
    moreClick: function (command, itemData) {
      if (command === "setGuard") {
        this.setGuard(itemData)
      }else if (command === "resetGuard") {
        this.resetGuard(itemData)
      }else if (command === "delete") {
        this.deleteDevice(itemData)
      }else if (command === "syncBasicParam") {
        this.syncBasicParam(itemData)
      }
    },
    setGuard: function (itemData) {
      this.$axios({
        method: 'get',
        url: `/api/device/control/guard/${itemData.deviceId}/SetGuard`,
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "布防成功"
          })
        }else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
      }).catch( (error)=> {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      });
    },
    resetGuard: function (itemData) {
      this.$axios({
        method: 'get',
        url: `/api/device/control/guard/${itemData.deviceId}/ResetGuard`,
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "撤防成功"
          })
        }else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
      }).catch( (error)=> {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      });
    },
    subscribeForCatalog: function (data, value) {
      console.log(data)
      console.log(value)
      this.$axios({
        method: 'get',
        url: `/api/device/query/subscribe/catalog`,
        params: {
          id: data,
          cycle: value?60:0
        }
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: value?"订阅成功":"取消订阅成功"
          })
        }else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
      }).catch( (error)=> {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      });

    },
    subscribeForMobilePosition: function (data, value) {
      console.log(data)
      console.log(value)
      this.$axios({
        method: 'get',
        url: `/api/device/query/subscribe/mobile-position`,
        params: {
          id: data,
          cycle: value?60:0,
          interval: value?5:0
        }
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: value?"订阅成功":"取消订阅成功"
          })
        }else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
      }).catch( (error)=> {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      });
    },
    syncBasicParam: function (data) {
      console.log(data)
      this.$axios({
        method: 'get',
        url: `/api/device/config/query/${data.deviceId}/BasicParam`,
        params: {
          // channelId: data.deviceId
        }
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: `配置已同步，当前心跳间隔： ${res.data.data.BasicParam.HeartBeatInterval} 心跳间隔:${res.data.data.BasicParam.HeartBeatCount}`
          })
        }else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
      }).catch( (error)=> {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      });

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
