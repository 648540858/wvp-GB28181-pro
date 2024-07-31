<template>
  <div id="region" style="width: 100%">
    <el-container v-loading="loading" >
      <el-aside width="400px" >
        <RegionTree ref="regionTree" :edit="true" :clickEvent="treeNodeClickEvent" :chooseIdChange="chooseIdChange" :onChannelChange="getChannelList"></RegionTree>
      </el-aside>
      <el-main style="padding: 5px;">
        <div class="page-header">
          <div class="page-title">通道列表</div>
          <div class="page-header-btn">
            <div  style="display: inline;">
              搜索:
              <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                        prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>

              在线状态:
              <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="search" v-model="online" placeholder="请选择"
                         default-first-option>
                <el-option label="全部" value=""></el-option>
                <el-option label="在线" value="true"></el-option>
                <el-option label="离线" value="false"></el-option>
              </el-select>
              添加状态:
              <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="search" v-model="hasCivilCode" placeholder="请选择"
                         default-first-option>
                <el-option label="全部" value=""></el-option>
                <el-option label="已添加" value="true"></el-option>
                <el-option label="未添加" value="false"></el-option>
              </el-select>
              <el-button size="mini" type="primary" @click="add()">
                添加
              </el-button>
              <el-button icon="el-icon-refresh-right" circle size="mini" @click="getChannelList()"></el-button>
            </div>
          </div>
        </div>
        <el-table ref="channelListTable" :data="channelList" :height="winHeight" style="width: 100%"
                  header-row-class-name="table-header" @selection-change="handleSelectionChange" @row-dblclick="rowDblclick">
          <el-table-column type="selection" width="55" :selectable="selectable">
          </el-table-column>
          <el-table-column prop="gbName" label="名称" min-width="180">
          </el-table-column>
          <el-table-column prop="gbDeviceId" label="编号" min-width="180">
          </el-table-column>
          <el-table-column prop="gbManufacturer" label="厂家" min-width="100">
          </el-table-column>
          <el-table-column label="类型" min-width="100">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.gbDeviceDbId">国标设备</el-tag>
                <el-tag size="medium" v-if="scope.row.streamPushId">推流设备</el-tag>
                <el-tag size="medium" v-if="scope.row.streamProxyId">拉流代理</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.gbStatus === 'ON'">在线</el-tag>
                <el-tag size="medium" type="info" v-if="scope.row.gbStatus !== 'ON'">离线</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="添加状态" min-width="100">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" :title="scope.row.gbCivilCode" v-if="scope.row.gbCivilCode">已添加</el-tag>
                <el-tag size="medium" type="info" v-if="!scope.row.gbCivilCode">未添加</el-tag>
              </div>
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

  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import DeviceService from "./service/DeviceService";
import RegionTree from "./common/RegionTree.vue";

export default {
  name: 'channelList',
  components: {
    uiHeader,
    RegionTree,
  },
  data() {
    return {
      channelList: [],
      searchSrt: "",
      channelType: "",
      online: "",
      hasCivilCode: "false",
      winHeight: window.innerHeight - 180,
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
      regionId: "",
      multipleSelection: []
    };
  },

  created() {
    this.initData();
  },
  destroyed() {},
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
        url: `/api/common/channel/list`,
        params: {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
          online: this.online,
          hasCivilCode: this.hasCivilCode
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.total = res.data.data.total;
          this.channelList = res.data.data.list;
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout();
          })
        }

      }).catch((error)=> {
        console.log(error);
      });
    },
    handleSelectionChange: function (val){
      this.multipleSelection = val;
    },
    selectable: function (row, rowIndex) {
      if (row.gbCivilCode) {
        return false
      }else {
        return true
      }
    },
    rowDblclick: function (row, rowIndex) {
      if (row.gbCivilCode) {
        this.$refs.regionTree.refresh(row.gbCivilCode)
      }
    },
    add: function (row) {
      if (!this.regionId) {
        this.$message.info("请选择左侧行政区划节点")
        return;
      }
      let channels = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channels.push(this.multipleSelection[i].gbId)
      }
      if (channels.length === 0) {
        this.$message.info("请选择右侧通道")
        return;
      }
      this.loading = true

      this.$axios({
        method: 'post',
        url: `/api/common/channel/region/add`,
        data: {
          civilCode: this.regionId,
          channelIds: channels
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message.success("保存成功")
          this.getChannelList()
          // 刷新树节点
          this.$refs.regionTree.refresh(this.regionId)
        }else {
          this.$message.error(res.data.msg)
        }
        this.loading = false
      }).catch((error)=> {
        this.$message.error(error)
        this.loading = false
      });
    },
    remove: function (row) {
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
    treeNodeClickEvent: function (device, data, isCatalog) {

    },
    chooseIdChange: function (id) {
      this.regionId = id;
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
