<template>
  <div id="addUser" v-loading="getDeviceListLoading">
    <el-dialog
      title="添加国标设备通道"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      append-to-body
      @close="close()"
    >
      <div class="page-header" style="width: 100%">
        <div class="page-header-btn" style="width: 100%; text-align: left">
          搜索:
          <el-input @input="getDeviceList" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                    prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>
          在线状态:
          <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="getDeviceList" v-model="online" placeholder="请选择"
                     default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="在线" value="true"></el-option>
            <el-option label="离线" value="false"></el-option>
          </el-select>
          <el-button size="mini" :loading="getDeviceListLoading"
                     @click="getDeviceList()">刷新</el-button>
          <el-button type="primary" size="mini" style="float: right" @click="onSubmit">确 定</el-button>
        </div>
      </div>
      <!--设备列表-->
      <el-table size="medium"  :data="deviceList" style="width: 100%;font-size: 12px;" :height="winHeight" header-row-class-name="table-header" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" >
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160">
        </el-table-column>
        <el-table-column prop="deviceId" label="设备编号" min-width="200" >
        </el-table-column>
        <el-table-column prop="channelCount" label="通道数" min-width="120" >
        </el-table-column>
        <el-table-column prop="manufacturer" label="厂家" min-width="120" >
        </el-table-column>
        <el-table-column label="地址" min-width="160" >
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.hostAddress" size="medium">{{ scope.row.hostAddress }}</el-tag>
              <el-tag v-if="!scope.row.hostAddress" size="medium">未知</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" v-if="scope.row.onLine">在线</el-tag>
              <el-tag size="medium" type="info" v-if="!scope.row.onLine">离线</el-tag>
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
        :page-sizes="[10, 25, 35, 50, 200, 1000, 50000]"
        layout="total, sizes, prev, pager, next"
        :total="total">
      </el-pagination>
    </el-dialog>
  </div>
</template>

<script>

export default {
  name: "gbDeviceSelect",
  props: {},
  computed: {},
  data() {
    return {
      showDialog: false,
      deviceList: [], //设备列表
      currentDevice: {}, //当前操作设备对象
      searchSrt: "",
      online: null,
      videoComponentList: [],
      updateLooper: 0, //数据刷新轮训标志
      currentDeviceChannelsLenth: 0,
      winHeight: 580,
      currentPage: 1,
      count: 10,
      total: 0,
      getDeviceListLoading: false,
      multipleSelection: [],
    };
  },
  mounted() {
    this.initData();
  },
  methods: {
    initData: function () {
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
    handleSelectionChange: function (val){
      this.multipleSelection = val;
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
    openDialog: function (callback) {
      this.listChangeCallback = callback;
      this.showDialog = true;
    },
    onSubmit: function () {
      if (this.listChangeCallback ) {
        this.listChangeCallback(this.multipleSelection)
      }
      this.showDialog = false;
    },
    close: function () {
      this.showDialog = false;
    },

  }
};
</script>
