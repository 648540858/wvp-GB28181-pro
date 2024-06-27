<template>
  <div id="channelList" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-button icon="el-icon-back" size="mini" style="font-size: 20px; color: #000;" type="text" @click="showDevice" ></el-button>
        <el-divider direction="vertical"></el-divider>
        通道列表
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
          搜索:
          <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                    prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">添加通道</el-button>
        <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
        </div>
      </div>
    </div>
    <devicePlayer ref="devicePlayer"></devicePlayer>
    <el-container v-loading="isLoging" style="height: 82vh;">
      <el-main style="padding: 5px;">
        <el-table ref="channelListTable" :data="deviceChannelList" :height="winHeight" style="width: 100%"
                  header-row-class-name="table-header">
          <el-table-column prop="channelId" label="通道编号" min-width="180">
          </el-table-column>
          <el-table-column prop="name" label="名称" min-width="180">
          </el-table-column>
          <el-table-column label="快照" min-width="100">
            <template v-slot:default="scope">
              <el-image
                :src="getSnap(scope.row)"
                :preview-src-list="getBigSnap(scope.row)"
                @error="getSnapErrorEvent(scope.row.deviceId, scope.row.channelId)"
                :fit="'contain'"
                style="width: 60px">
                <div slot="error" class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column label="开启音频" min-width="100">
            <template slot-scope="scope">
              <el-switch @change="updateChannel(scope.row)" v-model="scope.row.hasAudio" active-color="#409EFF">
              </el-switch>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="340" fixed="right">
            <template slot-scope="scope">
              <el-button size="medium" v-bind:disabled="device == null || device.online === 0" icon="el-icon-video-play"
                         type="text" @click="sendDevicePush(scope.row)">播放
              </el-button>
              <el-button size="medium" v-bind:disabled="device == null || device.online === 0"
                         icon="el-icon-switch-button"
                         type="text" style="color: #f56c6c" v-if="!!scope.row.stream"
                         @click="stopDevicePush(scope.row)">停止
              </el-button>
              <el-divider direction="vertical"></el-divider>
              <el-button
                size="medium"
                type="text"
                icon="el-icon-edit"
                @click="handleEdit(scope.row)"
              >
                编辑
              </el-button>
              <el-divider direction="vertical"></el-divider>
              <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
                <el-button size="medium" type="text" >
                  更多功能<i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="records" v-bind:disabled="device == null || device.online === 0">
                    设备录像</el-dropdown-item>
                  <el-dropdown-item command="cloudRecords" v-bind:disabled="device == null || device.online === 0" >
                    云端录像</el-dropdown-item>
<!--                  <el-dropdown-item command="shooting" v-bind:disabled="device == null || device.online === 0" >-->
<!--                    立即拍摄</el-dropdown-item>-->
                </el-dropdown-menu>
              </el-dropdown>
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
      </el-main>
    </el-container>
    <channelEdit ref="channelEdit"></channelEdit>
    <!--设备列表-->

  </div>
</template>

<script>
import devicePlayer from './dialog/jtDevicePlayer.vue'
import uiHeader from '../layout/UiHeader.vue'
import DeviceTree from "./common/DeviceTree";
import channelEdit from "./dialog/jtChannelEdit.vue";
import JTDeviceService from "./service/JTDeviceService";

export default {
  name: 'channelList',
  components: {
    channelEdit,
    devicePlayer,
    uiHeader,
    DeviceTree
  },
  data() {
    return {
      deviceService: new JTDeviceService(),
      device: null,
      deviceId: this.$route.params.deviceId,
      deviceChannelList: [],
      updateLooper: 0, //数据刷新轮训标志
      searchSrt: "",
      channelType: "",
      online: "",
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      beforeUrl: "/jtDeviceList",
      isLoging: false,
      loadSnap: {},
    };
  },

  mounted() {
    this.initParam();
    this.initData();

  },
  destroyed() {
    this.$destroy('videojs');
    clearTimeout(this.updateLooper);
  },
  methods: {
    initData: function () {
      this.getDeviceChannelList();
    },
    initParam: function () {
      this.deviceId = this.$route.params.deviceId;
      this.currentPage = 1;
      this.count = 15;
      this.deviceService.getDevice(this.deviceId, (result) => {
        if (result.code === 0) {
          this.device = result.data;
        }
      }, (error) => {
        console.log("获取设备信息失败")
        console.error(error)
      })
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.initData();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getDeviceChannelList();
    },
    getDeviceChannelList: function () {
      if (typeof (this.deviceId) == "undefined") return;
      this.deviceService.getAllChannel(this.currentPage, this.count, this.searchSrt, this.deviceId, (data)=>{
        console.log(data)
        if (data.code === 0) {
          this.total = data.data.total;
          this.deviceChannelList = data.data.list;
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout();
          })
        }
      })
    },

    //通知设备上传媒体流
    sendDevicePush: function (itemData) {
      this.isLoging = true;
      let channelId = itemData.channelId;
      console.log("通知设备推流1：" + this.device.phoneNumber + " : " + channelId);
      this.$axios({
        method: 'get',
        url: '/api/jt1078/live/start',
        params: {
          phoneNumber: this.device.phoneNumber,
          channelId: channelId,
          type: 0,
        }
      }).then((res)=> {
        this.isLoging = false;
        if (res.data.code === 0) {
          setTimeout(() => {
            let snapId = this.device.phoneNumber + "_" + channelId;
            this.loadSnap[this.device.phoneNumber + channelId] = 0;
            this.getSnapErrorEvent(snapId)
          }, 5000)
          itemData.streamId = res.data.data.stream;
          this.$refs.devicePlayer.openDialog("media", this.device.phoneNumber, channelId, {
            streamInfo: res.data.data,
            hasAudio: itemData.hasAudio
          });
          setTimeout(() => {
            this.initData();
          }, 1000)

        } else {
          this.$message.error(res.data.msg);
        }
      }).catch((e)=> {
        console.error(e)
        this.isLoging = false;
        // that.$message.error("请求超时");
      });
    },
    moreClick: function (command, itemData) {
      if (command === "records") {
        this.queryRecords(itemData)
      }else if (command === "cloudRecords") {
        this.queryCloudRecords(itemData)
      }else {
        this.$message.info("尚不支持");
      }
    },
    queryRecords: function (itemData) {
      this.$router.push(`/jtRecordDetail/${this.device.phoneNumber}/${itemData.channelId}`)
    },
    queryCloudRecords: function (itemData) {
      let deviceId = this.deviceId;
      let channelId = itemData.channelId;

      this.$router.push(`/cloudRecordDetail/rtp/${deviceId}_${channelId}`)
    },
    stopDevicePush: function (itemData) {
      this.$axios({
        method: 'get',
        url: '/api/jt1078/live/stop',
        params: {
          phoneNumber: this.device.phoneNumber,
          channelId: itemData.channelId,
        }
      }).then((res)=> {
        console.log(res)
        if (res.data.code === 0) {
          this.initData();
        }else {
          this.$message.error(res.data.msg);
        }
      }).catch(function (error) {
        console.error(error)
      });
    },
    getSnap: function (row) {
      let baseUrl = window.baseUrl ? window.baseUrl : "";
      return ((process.env.NODE_ENV === 'development') ? process.env.BASE_API : baseUrl) + '/api/device/query/snap/' + this.device.phoneNumber + '/' + row.channelId;
    },
    getBigSnap: function (row) {
      return [this.getSnap(row)]
    },
    getSnapErrorEvent: function (deviceId, channelId) {

      if (typeof (this.loadSnap[deviceId + channelId]) != "undefined") {
        console.log("下载截图" + this.loadSnap[deviceId + channelId])
        if (this.loadSnap[deviceId + channelId] > 5) {
          delete this.loadSnap[deviceId + channelId];
          return;
        }
        setTimeout(() => {
          let url = (process.env.NODE_ENV === 'development' ? "debug" : "") + '/api/device/query/snap/' + deviceId + '/' + channelId
          this.loadSnap[deviceId + channelId]++
          document.getElementById(deviceId + channelId).setAttribute("src", url + '?' + new Date().getTime())
        }, 1000)

      }
    },
    showDevice: function () {
      this.$router.push(this.beforeUrl).then(() => {
        this.initParam();
        this.initData();
      })
    },
    search: function () {
      this.currentPage = 1;
      this.total = 0;
      this.initData();
    },
    updateChannel: function (row) {
      this.$axios({
        method: 'post',
        url: `/api/jt1078/terminal/channel/update`,
        params: row
      }).then(function (res) {
        console.log(JSON.stringify(res));
      });
    },
    refresh: function () {
      this.initData();
    },
    add: function () {
      // this.$refs.channelEdit.openDialog(null, this.deviceId, () => {
      //   this.$refs.channelEdit.close();
      //   this.$message({
      //     showClose: true,
      //     message: "添加成功",
      //     type: "success",
      //   });
      //   setTimeout(this.getList, 200)
      // })
      this.$router.push(`/jtChannelEdit/${this.device.id}`);
    },
    // 编辑
    handleEdit(row) {
      // this.$refs.channelEdit.openDialog(row, this.deviceId, () => {
      //   this.$refs.channelEdit.close();
      //   this.$message({
      //     showClose: true,
      //     message: "修改成功",
      //     type: "success",
      //   });
      //   setTimeout(this.getList, 200)
      // })
      this.$router.push(`/jtChannelEdit/${this.device.id}/${row.id}`);
    }
  }
};
</script>
