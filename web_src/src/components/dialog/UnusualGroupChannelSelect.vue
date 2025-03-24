<template>
  <div id="gbChannelSelect" v-loading="getChannelListLoading">
    <el-dialog
      title="异常挂载通道"
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
          <el-button size="mini" type="primary" :loading="getChannelListLoading" :disabled="multipleSelection.length ===0"
                     @click="clearUnusualRegion()">清除</el-button>
          <el-button size="mini" :loading="getChannelListLoading"
                     @click="clearUnusualRegion(true)">全部清除</el-button>
          <el-button size="mini" :loading="getChannelListLoading"
                     @click="getChannelList()">刷新</el-button>

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
        <el-table-column prop="gbCivilCode" label="行政区划" min-width="100">
        </el-table-column>
        <el-table-column label="类型" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" effect="plain" type="success" :style="$channelTypeList[scope.row.dataType].style">{{$channelTypeList[scope.row.dataType].name}}</el-tag>
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
          <i class="el-icon-info"></i> 清除后通道可正常添加到分组节点。
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
  name: "UnusualGroupChannelSelect",
  props: [],
  computed: {},
  data() {
    return {
      showDialog: false,
      channelList: [], //设备列表
      searchSrt: "",
      online: null,
      channelType: "",
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
      this.$axios({
        method: 'get',
        url: `/api/common/channel/parent/unusual/list`,
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
          for (let i = 0; i < res.data.data.list.length; i++) {
            res.data.data.list[i]["addRegionLoading"] = false
          }
          this.channelList = res.data.data.list;
        }
      }).catch( (error)=> {
        console.error(error);
      }).finally(()=>{
        this.getChannelListLoading = false;
      })

    },
    openDialog: function () {
      this.showDialog = true;
      this.initData();
    },
    close: function () {
      this.showDialog = false;
    },
    clearUnusualRegion: function (all) {
      let channels = null
      if (all || this.multipleSelection.length > 0 ) {
        channels = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          channels.push(this.multipleSelection[i].gbId)
        }
      }
      this.$axios({
        method: 'post',
        url: `/api/common/channel/parent/unusual/clear`,
        data: {
          all: all,
          channelIds: channels
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "清除成功"
          })
          this.getChannelList()
        } else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error
        })
      }).finally(()=>{
        this.loading = false
      })

    },
    addRegion: function (row) {
      row.addRegionLoading = true;
      this.$axios({
        method: 'get',
        url: `/api/region/description`,
        params: {
          civilCode: row.gbCivilCode,
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.$confirm(`确定添加： ${res.data.data}`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }).then(() => {
            this.$axios({
              method: 'get',
              url: `/api/region/addByCivilCode`,
              params: {
                civilCode: row.gbCivilCode,
              }
            }).then((res) => {
              if (res.data.code === 0) {
                this.$message.success({
                  showClose: true,
                  message: "添加成功"
                })
                this.initData()
              }else {
                this.$message.error({
                  showClose: true,
                  message: res.data.msg
                })
              }

            }).catch((error) => {
              console.error(error);
            });
          }).catch(() => {

          });
        } else {
          this.$message.error({
            showClose: true,
            message: res.data.msg
          })
        }
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error
        })
      }).finally(()=>{
        row.addRegionLoading = false;
      })
    },

  }
};
</script>
