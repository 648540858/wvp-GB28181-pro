<template>
  <div id="region" style="width: 100%">
    <el-container v-loading="loading" >
      <el-aside width="400px" >
        <RegionTree ref="regionTree" :edit="true" :clickEvent="treeNodeClickEvent"></RegionTree>
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
            </div>
            <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
          </div>
        </div>
        <el-table ref="channelListTable" :data="channelList" :height="winHeight" style="width: 100%"
                  header-row-class-name="table-header">
          <el-table-column prop="gbName" label="名称" min-width="180">
          </el-table-column>
          <el-table-column prop="gbDeviceId" label="编号" min-width="180">
          </el-table-column>
          <el-table-column prop="gbManufacturer" label="厂家" min-width="100">
          </el-table-column>
          <el-table-column label="状态" min-width="100">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.gbStatus === 'ON'">在线</el-tag>
                <el-tag size="medium" type="info" v-if="scope.row.status !== 'ON'">离线</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="340" fixed="right">
            <template slot-scope="scope">
              <el-button size="medium" icon="el-icon-video-play" type="text" @click="add(scope.row)">
                添加
              </el-button>
              <el-divider direction="vertical"></el-divider>
              <el-button size="medium" type="text" icon="el-icon-edit" @click="remove(scope.row)">
                移除
              </el-button>
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
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
    };
  },

  mounted() {
    this.initData();

  },
  destroyed() {},
  methods: {
    initData: function () {
      if (typeof (this.parentChannelId) == "undefined" || this.parentChannelId == 0) {
        this.channelList();
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
      this.channelList();
    },
    channelList: function () {
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
            that.$set(e, "location", "");
            if (e.longitude && e.latitude) {
              that.$set(e, "location", e.longitude + "," + e.latitude);
            }
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
    add: function (row) {
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
      console.log(device)
      if (!!!data.channelId) {
        this.parentChannelId = device.deviceId;
      } else {
        this.parentChannelId = data.channelId;
      }
      this.initData();
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
