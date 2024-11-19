<template>
  <div id="recordPLan" style="width: 100%">
    <div class="page-header">
        <div class="page-title">
          <div >录像计划</div>
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
            录制计划:
            <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="search" v-model="hasRecordPlan"
                       placeholder="请选择"
                       default-first-option>
              <el-option label="全部" value=""></el-option>
              <el-option label="已设置" value="true"></el-option>
              <el-option label="未设置" value="false"></el-option>
            </el-select>
            类型:
            <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="getChannelList"
                       v-model="channelType" placeholder="请选择"
                       default-first-option>
              <el-option label="全部" value=""></el-option>
              <el-option label="国标设备" :value="0"></el-option>
              <el-option label="推流设备" :value="1"></el-option>
              <el-option label="拉流代理" :value="2"></el-option>
            </el-select>
            <el-button size="mini" type="primary" @click="add()">
              按国标设备添加
            </el-button>
            <el-button size="mini" type="danger" @click="remove()">
              按国标设备移除
            </el-button>
            <el-button icon="el-icon-refresh-right" circle size="mini" @click="getChannelList()"></el-button>
          </div>
        </div>
      </div>
      <el-table size="medium" ref="channelListTable" :data="channelList" :height="winHeight" style="width: 100%"
                header-row-class-name="table-header" >
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
              <el-tag size="medium" effect="plain" v-if="scope.row.gbDeviceDbId">国标设备</el-tag>
              <el-tag size="medium" effect="plain" type="success" v-if="scope.row.streamPushId">推流设备</el-tag>
              <el-tag size="medium" effect="plain" type="warning" v-if="scope.row.streamProxyId">拉流代理</el-tag>
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
        <el-table-column label="录制计划" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" effect="dark" v-if="scope.row.recordPlan">已设置</el-tag>
              <el-tag size="medium" effect="dark" v-else>未设置</el-tag>
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
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'

export default {
  name: 'recordPLan',
  components: {
    uiHeader,
  },
  data() {
    return {
      channelList: [],
      searchSrt: "",
      channelType: "",
      online: "",
      hasRecordPlan: "",
      hasGroup: "false",
      winHeight: window.innerHeight - 180,
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
      groupId: "",
      businessGroup: "",
      regionParents: ["请选择虚拟组织"],
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
        url: `/api/common/channel/list`,
        params: {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
          online: this.online,
          hasRecordPlan: this.hasRecordPlan,
          channelType: this.channelType,
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
    onChannelChange: function (deviceId) {
      //
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
