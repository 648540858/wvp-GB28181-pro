<template>
  <div id="region" style="width: 100%">
    <el-container v-loading="loading">
      <el-aside width="400px">
        <GroupTree ref="groupTree" :show-header="true" :edit="true" :clickEvent="treeNodeClickEvent"
                   :onChannelChange="onChannelChange" :enableAddChannel="true" :addChannelToGroup="addChannelToGroup"></GroupTree>
      </el-aside>
      <el-main style="padding: 0 0 0 5px;">
        <div class="page-header">
          <div class="page-title">
            <el-breadcrumb separator="/" v-if="regionParents.length > 0">
              <el-breadcrumb-item v-for="key in regionParents" key="key">{{ key }}</el-breadcrumb-item>
            </el-breadcrumb>
            <div v-else style="color: #00c6ff">未选择虚拟组织</div>
          </div>
          <div class="page-header-btn">
            <div style="display: inline;">
              搜索:
              <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                        prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>

              在线状态:
              <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="search" v-model="online"
                         placeholder="请选择"
                         default-first-option>
                <el-option label="全部" value=""></el-option>
                <el-option label="在线" value="true"></el-option>
                <el-option label="离线" value="false"></el-option>
              </el-select>
              类型:
              <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="getChannelList"
                         v-model="channelType" placeholder="请选择"
                         default-first-option>
                <el-option label="全部" value=""></el-option>
                <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
              <el-button size="mini" type="primary" @click="add()">
                添加通道
              </el-button>
              <el-button v-bind:disabled="multipleSelection.length === 0" size="mini" type="danger" @click="remove()">
                移除通道
              </el-button>
              <el-button plain size="mini" type="warning" @click="showUnusualChanel()">
                异常挂载通道
              </el-button>
              <el-button icon="el-icon-refresh-right" circle size="mini" @click="getChannelList()"></el-button>
            </div>
          </div>
        </div>
        <el-table size="medium" ref="channelListTable" :data="channelList" :height="$tableHeght" style="width: 100%"
                  header-row-class-name="table-header" @selection-change="handleSelectionChange"
                  @row-dblclick="rowDblclick">
          <el-table-column type="selection" width="55" >
          </el-table-column>
          <el-table-column prop="gbName" label="名称" min-width="180">
          </el-table-column>
          <el-table-column prop="gbDeviceId" label="编号" min-width="180">
          </el-table-column>
          <el-table-column prop="gbManufacturer" label="厂家" min-width="100">
          </el-table-column>
          <el-table-column label="类型" min-width="100">
            <template v-slot:default="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" effect="plain" type="success" :style="$channelTypeList[scope.row.dataType].style" >{{$channelTypeList[scope.row.dataType].name}}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template v-slot:default="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.gbStatus === 'ON'">在线</el-tag>
                <el-tag size="medium" type="info" v-if="scope.row.gbStatus !== 'ON'">离线</el-tag>
              </div>
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
      </el-main>
    </el-container>
    <GbChannelSelect ref="gbChannelSelect" dataType="group"></GbChannelSelect>
    <UnusualGroupChannelSelect ref="unusualGroupChannelSelect" ></UnusualGroupChannelSelect>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import DeviceService from "./service/DeviceService";
import GroupTree from "./common/GroupTree.vue";
import GbChannelSelect from "./dialog/GbChannelSelect.vue";
import UnusualGroupChannelSelect from "./dialog/UnusualGroupChannelSelect.vue";
import RegionTree from "./common/RegionTree.vue";

export default {
  name: 'channelList',
  components: {
    RegionTree,
    GbChannelSelect,
    UnusualGroupChannelSelect,
    uiHeader,
    GroupTree,
  },
  data() {
    return {
      channelList: [],
      searchSrt: "",
      channelType: "",
      online: "",
      hasGroup: "false",
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
      groupDeviceId: "",
      groupId: "",
      businessGroup: "",
      regionParents: [],
      multipleSelection: []
    };
  },

  created() {
    this.initData();
  },
  destroyed() {
  },
  methods: {
    initData: function () {
      this.getChannelList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.initData();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getChannelList();
    },
    getChannelList: function () {
      this.$axios({
        method: 'get',
        url: `/api/common/channel/parent/list`,
        params: {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
          online: this.online,
          channelType: this.channelType,
          groupDeviceId: this.groupDeviceId
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.total = res.data.data.total;
          this.channelList = res.data.data.list;
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout();
          })
        }

      }).catch((error) => {
        console.log(error);
      });
    },
    handleSelectionChange: function (val) {
      this.multipleSelection = val;
    },
    rowDblclick: function (row, rowIndex) {

    },
    add: function (row) {
      if (this.regionDeviceId === "") {
        this.$message.info({
          showClose: true,
          message: "请选择左侧虚拟组织节点"
        })
        return;
      }
      this.$refs.gbChannelSelect.openDialog((data) => {
        console.log("选择的数据")
        console.log(data)
        this.addChannelToGroup(this.groupDeviceId, this.businessGroup, data)
      })
    },
    addChannelToGroup: function (groupDeviceId, businessGroup,  data) {
        if (data.length === 0) {
          return;
        }
        let channels = []
        for (let i = 0; i < data.length; i++) {
          channels.push(data[i].gbId)
        }
      this.loading = true

      this.$axios({
        method: 'post',
        url: `/api/common/channel/group/add`,
        data: {
          parentId: groupDeviceId,
          businessGroup: businessGroup,
          channelIds: channels
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "保存成功"
          })
          this.getChannelList()
        } else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
        this.loading = false
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error
        })
        this.loading = false
      });
    },
    remove: function (row) {

      let channels = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channels.push(this.multipleSelection[i].gbId)
      }
      if (channels.length === 0) {
        this.$message.info({
          showClose: true,
          message: "请选择通道"
        })
        return;
      }
      this.loading = true

      this.$axios({
        method: 'post',
        url: `/api/common/channel/group/delete`,
        data: {
          channelIds: channels
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "保存成功"
          })
          this.getChannelList()
          // 刷新树节点
          this.$refs.groupTree.refresh(this.groupDeviceId)
        } else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
        this.loading = false
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error
        })
        this.loading = false
      });
    },
    getSnap: function (row) {
      let baseUrl = window.baseUrl ? window.baseUrl : "";
      return ((process.env.NODE_ENV === 'development') ? process.env.BASE_API : baseUrl) + '/api/device/query/snap/' + this.deviceId + '/' + row.deviceId;
    },
    search: function () {
      this.currentPage = 1;
      this.total = 0;
      this.initData();
    },
    refresh: function () {
      this.initData();
    },
    treeNodeClickEvent: function (group) {
      if (group.deviceId === "" || group.deviceId === group.businessGroup) {
        this.channelList = []
        this.regionParents = [];
        this.$message.info({
          showClose: true,
          message: "当前为业务分组，挂载通道请选择其下的虚拟组织，如不存在可右键新建"
        })
        return
      }
      this.groupDeviceId = group.deviceId;
      this.businessGroup = group.businessGroup;
      this.initData();
      // 获取regionDeviceId对应的节点信息
      this.$axios({
        method: 'get',
        url: `/api/group/path`,
        params: {
          deviceId: this.groupDeviceId,
          businessGroup: this.businessGroup,
        }
      }).then((res) => {
        if (res.data.code === 0) {
          let path = []
          for (let i = 0; i < res.data.data.length; i++) {
            path.push(res.data.data[i].name)
          }
          this.regionParents = path;
        }

      }).catch((error) => {
        console.log(error);
      });
    },
    onChannelChange: function (deviceId) {
      //
    },
    showUnusualChanel: function () {
      this.$refs.unusualGroupChannelSelect.openDialog()
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
