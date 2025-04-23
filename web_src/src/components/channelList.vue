<template>
  <div id="channelList" style="width: 100%">
    <div v-if="!editId">
      <div class="page-header">
        <div class="page-title">
          <el-page-header @back="showDevice" content="通道列表"></el-page-header>
        </div>
        <div class="page-header-btn">
          <div v-if="!showTree" style="display: inline;">
            搜索:
            <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                      prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>

            通道类型:
            <el-select size="mini" @change="search" style="width: 8rem; margin-right: 1rem;" v-model="channelType" placeholder="请选择"
                       default-first-option>
              <el-option label="全部" value=""></el-option>
              <el-option label="设备" value="false"></el-option>
              <el-option label="子目录" value="true"></el-option>
            </el-select>
            在线状态:
            <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="search" v-model="online" placeholder="请选择"
                       default-first-option>
              <el-option label="全部" value=""></el-option>
              <el-option label="在线" value="true"></el-option>
              <el-option label="离线" value="false"></el-option>
            </el-select>
            码流类型重置:
            <el-select size="mini" style="width: 16rem; margin-right: 1rem;" @change="subStreamChange" v-model="subStream"
                       placeholder="请选择码流类型" default-first-option >
              <el-option label="stream:0(主码流)" value="stream:0"></el-option>
              <el-option label="stream:1(子码流)" value="stream:1"></el-option>
              <el-option label="streamnumber:0(主码流-2022)" value="streamnumber:0"></el-option>
              <el-option label="streamnumber:1(子码流-2022)" value="streamnumber:1"></el-option>
              <el-option label="streamprofile:0(主码流-大华)" value="streamprofile:0"></el-option>
              <el-option label="streamprofile:1(子码流-大华)" value="streamprofile:1"></el-option>
              <el-option label="streamMode:main(主码流-水星+TP-LINK)" value="streamMode:main"></el-option>
              <el-option label="streamMode:sub(子码流-水星+TP-LINK)" value="streamMode:sub"></el-option>
            </el-select>
          </div>
          <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
        </div>
      </div>
      <el-table size="medium"  ref="channelListTable" :data="deviceChannelList" :height="$tableHeght"
                header-row-class-name="table-header">
        <el-table-column prop="name" label="名称" min-width="180">
        </el-table-column>
        <el-table-column prop="deviceId" label="编号" min-width="180">
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
        <!--          <el-table-column prop="subCount" label="子节点数" min-width="100">-->
        <!--          </el-table-column>-->
        <el-table-column prop="manufacturer" label="厂家" min-width="100">
        </el-table-column>
        <el-table-column label="位置信息" min-width="150">
          <template v-slot:default="scope">
            <span size="medium" v-if="scope.row.longitude && scope.row.latitude">{{scope.row.longitude}}<br/>{{scope.row.latitude}}</span>
            <span size="medium" v-if="!scope.row.longitude || !scope.row.latitude">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="ptzType" label="云台类型" min-width="100">
          <template v-slot:default="scope">
            <div >{{ scope.row.ptzTypeText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="开启音频" min-width="100">
          <template v-slot:default="scope">
            <el-switch @change="updateChannel(scope.row)" v-model="scope.row.hasAudio" active-color="#409EFF">
            </el-switch>
          </template>
        </el-table-column>
        <el-table-column label="码流类型" min-width="180">
          <template v-slot:default="scope">
            <el-select size="mini" style="margin-right: 1rem;" @change="channelSubStreamChange(scope.row)" v-model="scope.row.streamIdentification"
                       placeholder="请选择码流类型" default-first-option >
              <el-option label="stream:0(主码流)" value="stream:0"></el-option>
              <el-option label="stream:1(子码流)" value="stream:1"></el-option>
              <el-option label="streamnumber:0(主码流-2022)" value="streamnumber:0"></el-option>
              <el-option label="streamnumber:1(子码流-2022)" value="streamnumber:1"></el-option>
              <el-option label="streamprofile:0(主码流-大华)" value="streamprofile:0"></el-option>
              <el-option label="streamprofile:1(子码流-大华)" value="streamprofile:1"></el-option>
              <el-option label="streamMode:main(主码流-水星+TP-LINK)" value="streamMode:main"></el-option>
              <el-option label="streamMode:sub(子码流-水星+TP-LINK)" value="streamMode:sub"></el-option>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" v-if="scope.row.status === 'ON'">在线</el-tag>
              <el-tag size="medium" type="info" v-if="scope.row.status !== 'ON'">离线</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="340" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" v-bind:disabled="device == null || device.online === 0" icon="el-icon-video-play"
                       type="text" :loading="scope.row.playLoading" @click="sendDevicePush(scope.row)">播放
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
                       v-if="scope.row.subCount > 0 || scope.row.parental === 1 || scope.row.deviceId.length <= 8"
                       @click="changeSubchannel(scope.row)">查看
            </el-button>
            <el-divider v-if="scope.row.subCount > 0 || scope.row.parental === 1 || scope.row.deviceId.length <= 8" direction="vertical"></el-divider>
            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
              <el-button size="medium" type="text" >
                更多<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu>
                <el-dropdown-item command="records" v-bind:disabled="device == null || device.online === 0">
                  设备录像</el-dropdown-item>
                <el-dropdown-item command="cloudRecords" v-bind:disabled="device == null || device.online === 0" >
                  云端录像</el-dropdown-item>
                <el-dropdown-item command="record" v-bind:disabled="device == null || device.online === 0" >
                  设备录像控制-开始</el-dropdown-item>
                <el-dropdown-item command="stopRecord" v-bind:disabled="device == null || device.online === 0" >
                  设备录像控制-停止</el-dropdown-item>
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
    </div>

    <devicePlayer ref="devicePlayer"></devicePlayer>
    <channel-edit v-if="editId" :id="editId" :closeEdit="closeEdit"></channel-edit>

  </div>
</template>

<script>
import devicePlayer from './dialog/devicePlayer.vue'
import uiHeader from '../layout/UiHeader.vue'
import DeviceService from "./service/DeviceService";
import DeviceTree from "./common/DeviceTree";
import ChannelEdit from "./ChannelEdit";

export default {
  name: 'channelList',
  components: {
    devicePlayer,
    uiHeader,
    DeviceTree,
    ChannelEdit,
  },
  data() {
    return {
      deviceService: new DeviceService(),
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
      beforeUrl: "/deviceList",
      showTree: false,
      editId: null,
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
      if (typeof (this.parentChannelId) == "undefined" || this.parentChannelId == 0) {
        this.getDeviceChannelList();
      } else {
        this.showSubchannels();
      }
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
        url: `/api/device/query/devices/${this.$route.params.deviceId}/channels`,
        params: {
          page: that.currentPage,
          count: that.count,
          query: that.searchSrt,
          online: that.online,
          channelType: that.channelType
        }
      }).then(function (res) {
        if (res.data.code === 0) {
          that.total = res.data.data.total;
          that.deviceChannelList = res.data.data.list;
          that.deviceChannelList.forEach(e => {
            e.ptzType = e.ptzType + "";
            that.$set(e, "playLoading", false);
          });
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
      let channelId = itemData.deviceId;
      itemData.playLoading = true;
      console.log("通知设备推流1：" + deviceId + " : " + channelId);
      this.$axios({
        method: 'get',
        url: '/api/play/start/' + deviceId + '/' + channelId,
        params: {
          isSubStream: this.isSubStream
        }
      }).then((res) =>{
        console.log(res)
        if (res.data.code === 0) {

          setTimeout(() => {
            let snapId = deviceId + "_" + channelId;
            this.loadSnap[deviceId + channelId] = 0;
            this.getSnapErrorEvent(snapId)
          }, 5000)
          itemData.streamId = res.data.data.stream;
          this.$refs.devicePlayer.openDialog("media", deviceId, channelId, {
            streamInfo: res.data.data,
            hasAudio: itemData.hasAudio
          });
          setTimeout(() => {
            this.initData();
          }, 1000)

        } else {
          this.$message.error(res.data.msg);
        }
      }).catch(function (e) {
        console.error(e)
        // that.$message.error("请求超时");
      }).finally(()=>{
        itemData.playLoading = false;
      })
    },
    moreClick: function (command, itemData) {
      if (command === "records") {
        this.queryRecords(itemData)
      }else if (command === "cloudRecords") {
        this.queryCloudRecords(itemData)
      }else if (command === "record") {
        this.startRecord(itemData)
      }else if (command === "stopRecord") {
        this.stopRecord(itemData)
      }
    },
    queryRecords: function (itemData) {
      let deviceId = this.deviceId;
      let channelId = itemData.deviceId;

      this.$router.push(`/gbRecordDetail/${deviceId}/${channelId}`)
    },
    queryCloudRecords: function (itemData) {
      let deviceId = this.deviceId;
      let channelId = itemData.deviceId;

      this.$router.push(`/cloudRecordDetail/rtp/${deviceId}_${channelId}`)
    },
    startRecord: function (itemData) {
      this.$axios({
        method: 'get',
        url: `/api/device/control/record`,
        params: {
          deviceId: this.deviceId,
          channelId: itemData.deviceId,
          recordCmdStr: "Record"
        }
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "开始录像成功"
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
    stopRecord: function (itemData) {
      this.$axios({
        method: 'get',
        url: `/api/device/control/record`,
        params: {
          deviceId: this.deviceId,
          channelId: itemData.deviceId,
          recordCmdStr: "StopRecord"
        }
      }).then( (res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "停止录像成功"
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
    stopDevicePush: function (itemData) {
      var that = this;
      this.$axios({
        method: 'get',
        url: '/api/play/stop/' + this.deviceId + "/" + itemData.deviceId,
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
      return ((process.env.NODE_ENV === 'development') ? process.env.BASE_API : baseUrl) + '/api/device/query/snap/' + this.deviceId + '/' + row.deviceId;
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

      var url = `/${this.$router.currentRoute.name}/${this.$router.currentRoute.params.deviceId}/${itemData.deviceId}`
      this.$router.push(url).then(() => {
        this.searchSrt = "";
        this.channelType = "";
        this.online = "";
        this.initParam();
        this.initData();
      })
    },
    showSubchannels: function (channelId) {
      if (!this.showTree) {
        this.$axios({
          method: 'get',
          url: `/api/device/query/sub_channels/${this.deviceId}/${this.parentChannelId}/channels`,
          params: {
            page: this.currentPage,
            count: this.count,
            query: this.searchSrt,
            online: this.online,
            channelType: this.channelType
          }
        }).then((res) => {
          if (res.data.code === 0) {
            this.total = res.data.data.total;
            this.deviceChannelList = res.data.data.list;
            this.deviceChannelList.forEach(e => {
              e.ptzType = e.ptzType + "";
            });
            // 防止出现表格错位
            this.$nextTick(() => {
              this.$refs.channelListTable.doLayout();
            })
          }

        }).catch(function (error) {
          console.log(error);
        });
      } else {
        this.$axios({
          method: 'get',
          url: `/api/device/query/tree/channel/${this.deviceId}`,
          params: {
            parentId: this.parentChannelId,
            page: this.currentPage,
            count: this.count,
          }
        }).then((res) => {
          if (res.data.code === 0) {
            this.total = res.data.total;
            this.deviceChannelList = res.data.list;
            // 防止出现表格错位
            this.$nextTick(() => {
              this.$refs.channelListTable.doLayout();
            })
          }
        }).catch(function (error) {
          console.log(error);
        });
      }

    },
    search: function () {
      this.currentPage = 1;
      this.total = 0;
      this.initData();
    },
    updateChannel: function (row) {
      this.$axios({
        method: 'post',
        url: `/api/device/query/channel/audio`,
        params: {
          channelId: row.id,
          audio: row.hasAudio
        }
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
            deviceDbId: this.device.id,
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
          deviceDbId: row.deviceDbId,
          id: row.id,
          streamIdentification: row.streamIdentification
        }
      }).then(function (res) {
        console.log(JSON.stringify(res));
      });
    },
    refresh: function () {
      this.initData();
    },
    switchTree: function () {
      this.showTree = true;
      this.deviceChannelList = [];
      this.parentChannelId = 0;
      this.currentPage = 1;

    },
    switchList: function () {
      this.showTree = false;
      this.deviceChannelList = [];
      this.parentChannelId = 0;
      this.currentPage = 1;
      this.initData();
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
    // 编辑
    handleEdit(row) {
      this.editId = row.id
    },
    // 结束编辑
    closeEdit: function (){
      this.editId = null
      this.getDeviceChannelList()
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
