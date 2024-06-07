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
                         type="text" style="color: #f56c6c" v-if="!!scope.row.streamId"
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
              <el-button size="medium" icon="el-icon-s-open" type="text"
                         v-if="scope.row.subCount > 0 || scope.row.parental === 1"
                         @click="changeSubchannel(scope.row)">查看
              </el-button>
              <el-divider v-if="scope.row.subCount > 0 || scope.row.parental === 1" direction="vertical"></el-divider>
              <!--              <el-button size="medium" v-bind:disabled="device == null || device.online === 0"-->
              <!--                         icon="el-icon-video-camera"-->
              <!--                         type="text" @click="queryRecords(scope.row)">设备录像-->
              <!--              </el-button>-->
              <!--              <el-button size="medium" v-bind:disabled="device == null || device.online === 0" icon="el-icon-cloudy"-->
              <!--                         type="text" @click="queryCloudRecords(scope.row)">云端录像-->
              <!--              </el-button>-->
              <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
                <el-button size="medium" type="text" >
                  更多功能<i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="records" v-bind:disabled="device == null || device.online === 0">
                    设备录像</el-dropdown-item>
                  <el-dropdown-item command="cloudRecords" v-bind:disabled="device == null || device.online === 0" >
                    云端录像</el-dropdown-item>
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
import devicePlayer from './dialog/devicePlayer.vue'
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
      parentChannelId: this.$route.params.parentChannelId,
      deviceChannelList: [],
      videoComponentList: [],
      currentPlayerInfo: {}, //当前播放对象
      updateLooper: 0, //数据刷新轮训标志
      searchSrt: "",
      channelType: "",
      online: "",
      subStream: "",
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      beforeUrl: "/jtDeviceList",
      isLoging: false,
      loadSnap: {},
      ptzTypes: {
        0: "未知",
        1: "球机",
        2: "半球",
        3: "固定枪机",
        4: "遥控枪机"
      }
    };
  },

  mounted() {
    if (this.deviceId) {
      this.deviceService.getDevice(this.deviceId, (result) => {
        this.device = result;

      }, (error) => {
        console.log("获取设备信息失败")
        console.error(error)
      })
    }
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
      this.parentChannelId = this.$route.params.parentChannelId;
      this.currentPage = 1;
      this.count = 15;
      if (this.parentChannelId == "" || this.parentChannelId == 0) {
        this.beforeUrl = "/deviceList"
      }

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
      let that = this;
      if (typeof (this.$route.params.deviceId) == "undefined") return;
      this.$axios({
        method: 'get',
        url: `/api/jt1078/terminal/channel/list`,
        params: {
          page: that.currentPage,
          count: that.count,
          query: that.searchSrt,
          deviceId: this.$route.params.deviceId,
        }
      }).then(function (res) {
        if (res.data.code === 0) {
          that.total = res.data.data.total;
          that.deviceChannelList = res.data.data.list;
          // 防止出现表格错位
          that.$nextTick(() => {
            that.$refs.channelListTable.doLayout();
          })
        }

      }).catch(function (error) {
        console.log(error);
      });
    },

    //通知设备上传媒体流
    sendDevicePush: function (itemData) {
      let deviceId = this.deviceId;
      this.isLoging = true;
      let channelId = itemData.channelId;
      console.log("通知设备推流1：" + deviceId + " : " + channelId);
      let that = this;
      this.$axios({
        method: 'get',
        url: '/api/jt1078/live/start' + deviceId + '/' + channelId,
        params: {
          phoneNumber: deviceId,
          channelId: channelId,
          type: 0,
        }
      }).then(function (res) {
        console.log(res)
        that.isLoging = false;
        if (res.data.code === 0) {

          setTimeout(() => {

            let snapId = deviceId + "_" + channelId;
            that.loadSnap[deviceId + channelId] = 0;
            that.getSnapErrorEvent(snapId)
          }, 5000)
          itemData.streamId = res.data.data.stream;
          that.$refs.devicePlayer.openDialog("media", deviceId, channelId, {
            streamInfo: res.data.data,
            hasAudio: itemData.hasAudio
          });
          setTimeout(() => {
            that.initData();
          }, 1000)

        } else {
          that.$message.error(res.data.msg);
        }
      }).catch(function (e) {
        console.error(e)
        that.isLoging = false;
        // that.$message.error("请求超时");
      });
    },
    moreClick: function (command, itemData) {
      if (command === "records") {
        this.queryRecords(itemData)
      }else if (command === "cloudRecords") {
        this.queryCloudRecords(itemData)
      }
    },
    queryRecords: function (itemData) {
      let deviceId = this.deviceId;
      let channelId = itemData.channelId;

      this.$router.push(`/gbRecordDetail/${deviceId}/${channelId}`)
    },
    queryCloudRecords: function (itemData) {
      let deviceId = this.deviceId;
      let channelId = itemData.channelId;

      this.$router.push(`/cloudRecordDetail/rtp/${deviceId}_${channelId}`)
    },
    stopDevicePush: function (itemData) {
      var that = this;
      this.$axios({
        method: 'get',
        url: '/api/play/stop/' + this.deviceId + "/" + itemData.channelId,
        params: {
          isSubStream: this.isSubStream
        }
      }).then(function (res) {
        that.initData();
      }).catch(function (error) {
        if (error.response.status === 402) { // 已经停止过
          that.initData();
        } else {
          console.log(error)
        }
      });
    },
    getSnap: function (row) {
      let baseUrl = window.baseUrl ? window.baseUrl : "";
      return ((process.env.NODE_ENV === 'development') ? process.env.BASE_API : baseUrl) + '/api/device/query/snap/' + row.deviceId + '/' + row.channelId;
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
    changeSubchannel(itemData) {
      this.beforeUrl = this.$router.currentRoute.path;

      var url = `/${this.$router.currentRoute.name}/${this.$router.currentRoute.params.deviceId}/${itemData.channelId}`
      this.$router.push(url).then(() => {
        this.searchSrt = "";
        this.channelType = "";
        this.online = "";
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
    subStreamChange: function () {
      this.$confirm('确定重置所有通道的码流类型?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$axios({
          method: 'post',
          url: `/api/device/query/channel/stream/identification/update/`,
          params: {
            deviceId: this.deviceId,
            streamIdentification: this.subStream
          }
        }).then((res)=> {
          console.log(JSON.stringify(res));
          this.initData()
        }).finally(()=>{
          this.subStream = ""
        })
      }).catch(() => {
        this.subStream = ""
      });

    },
    channelSubStreamChange: function (row) {
      this.$axios({
        method: 'post',
        url: `/api/device/query/channel/stream/identification/update/`,
        params: {
          deviceId: this.deviceId,
          channelId: row.channelId,
          streamIdentification: row.streamIdentification
        }
      }).then(function (res) {
        console.log(JSON.stringify(res));
      });
    },
    refresh: function () {
      this.initData();
    },
    add: function () {
      this.$refs.channelEdit.openDialog(null, this.deviceId, () => {
        this.$refs.channelEdit.close();
        this.$message({
          showClose: true,
          message: "添加成功",
          type: "success",
        });
        setTimeout(this.getList, 200)
      })
    },
    treeNodeClickEvent: function (device, data, isCatalog) {
      console.log(device)
      if (!!!data.channelId) {
        this.parentChannelId = device.deviceId;
      } else {
        this.parentChannelId = data.channelId;
      }
      this.initData();
    },
    // 保存
    handleSave(row) {
      if (row.location) {
        const segements = row.location.split(",");
        if (segements.length !== 2) {
          this.$message.warning("位置信息格式有误，例：117.234,36.378");
          return;
        } else {
          row.customLongitude = parseFloat(segements[0]);
          row.custom_latitude = parseFloat(segements[1]);
          if (!(row.longitude && row.latitude)) {
            this.$message.warning("位置信息格式有误，例：117.234,36.378");
            return;
          }
        }
      } else {
        delete row.longitude;
        delete row.latitude;
      }
      Object.keys(row).forEach(key => {
        const value = row[key];
        if (value === null || value === undefined || (typeof value === "string" && value.trim() === "")) {
          delete row[key];
        }
      });
      this.$axios({
        method: 'post',
        url: `/api/device/query/channel/update/${this.deviceId}`,
        params: row
      }).then(response => {
        if (response.data.code === 0) {
          this.$message.success("修改成功！");
          this.initData();
        } else {
          this.$message.error("修改失败！");
        }
      }).catch(_ => {
        this.$message.error("修改失败！");
      })
    },
    // 是否正在编辑
    isEdit() {
      let editing = false;
      this.deviceChannelList.forEach(e => {
        if (e.edit) {
          editing = true;
        }
      });

      return editing;
    },
    // 编辑
    handleEdit(row) {
      if (this.isEdit()) {
        this.$message.warning('请保存当前编辑项！');
      } else {
        row.edit = true;
      }
    }
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
