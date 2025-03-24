<template>
  <div id="gbChannelSelect" v-loading="getChannelListLoading">
    <el-dialog
      title="添加国标通道"
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
          <el-input @input="getChannelList" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                    prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>
          在线状态:
          <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="getChannelList" v-model="online" placeholder="请选择"
                     default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="在线" value="true"></el-option>
            <el-option label="离线" value="false"></el-option>
          </el-select>
          类型:
          <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="getChannelList" v-model="channelType" placeholder="请选择"
                     default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
          <el-button size="mini" :loading="getChannelListLoading"
                     @click="getChannelList()">刷新</el-button>
          <el-button type="primary" size="mini" style="float: right" @click="onSubmit">确 定</el-button>
        </div>
      </div>
      <!--通道列表-->
      <el-table size="small"  ref="channelListTable" :data="channelList" :height="winHeight" style="width: 100%;"
                header-row-class-name="table-header" @selection-change="handleSelectionChange" >
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
      <div style="display: grid; grid-template-columns: 1fr 1fr">
        <div style="text-align: left; line-height: 32px">
          <i class="el-icon-info"></i> 未找到通道，可在国标设备/通道中选择编辑按钮， 选择{{dataType === 'civilCode'?'行政区划':'父节点编码'}}
        </div>
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
      </div>

    </el-dialog>
  </div>
</template>

<script>


export default {
  name: "gbChannelSelect",
  props: ['dataType', "selected"],
  computed: {},
  data() {
    return {
      showDialog: false,
      channelList: [], //设备列表
      currentDevice: {}, //当前操作设备对象
      searchSrt: "",
      online: null,
      channelType: "",
      videoComponentList: [],
      updateLooper: 0, //数据刷新轮训标志
      currentDeviceChannelsLenth: 0,
      winHeight: 580,
      currentPage: 1,
      count: 10,
      total: 0,
      getChannelListLoading: false,
      multipleSelection: [],
    };
  },
  methods: {
    initData: function () {
      this.getChannelList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.getChannelList();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getChannelList();
    },
    handleSelectionChange: function (val){
      this.multipleSelection = val;
    },
    getChannelList: function () {
      this.getChannelListLoading = true;
      if (this.dataType === "civilCode") {
        this.$axios({
          method: 'get',
          url: `/api/common/channel/civilcode/list`,
          params: {
            page: this.currentPage,
            count: this.count,
            channelType: this.channelType,
            query: this.searchSrt,
            online: this.online,
          }
        }).then( (res)=> {
          if (res.data.code === 0) {
            this.total = res.data.data.total;
            this.channelList = res.data.data.list;
          }
          this.getChannelListLoading = false;
        }).catch( (error)=> {
          console.error(error);
          this.getChannelListLoading = false;
        });
      }else {
        this.$axios({
          method: 'get',
          url: `/api/common/channel/parent/list`,
          params: {
            page: this.currentPage,
            count: this.count,
            query: this.searchSrt,
            channelType: this.channelType,
            online: this.online,
          }
        }).then( (res)=> {
          if (res.data.code === 0) {
            this.total = res.data.data.total;
            this.channelList = res.data.data.list;
          }
          this.getChannelListLoading = false;
        }).catch( (error)=> {
          console.error(error);
          this.getChannelListLoading = false;
        });
      }

    },
    openDialog: function (callback) {
      this.listChangeCallback = callback;
      this.showDialog = true;
      this.initData();
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
