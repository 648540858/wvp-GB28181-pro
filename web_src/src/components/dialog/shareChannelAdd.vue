<template>
  <div id="shareChannelAdd" style="width: 100%;  background-color: #FFFFFF">
    <div class="page-header">
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
          类型:
          <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="search" v-model="channelType" placeholder="请选择"
                     default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="国标设备" value="gb"></el-option>
            <el-option label="推流设备" value="push"></el-option>
            <el-option label="拉流代理" value="proxy"></el-option>
          </el-select>
          添加状态:
          <el-select size="mini" style="width: 8rem; margin-right: 1rem;" @change="search" v-model="hasShare" placeholder="请选择"
                     default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="已共享" value="true"></el-option>
            <el-option label="未共享" value="false"></el-option>
          </el-select>
          <el-button v-if="hasShare !=='true'" size="mini" type="primary" @click="add()">
            添加
          </el-button>
          <el-button v-if="hasShare ==='true'" size="mini" type="danger" @click="remove()">
            移除
          </el-button>
          <el-button size="mini" @click="addByDevice()">按设备添加</el-button>
          <el-button size="mini" @click="removeByDevice()">按设备移除</el-button>
          <el-button size="mini" @click="addAll()">全部添加</el-button>
          <el-button size="mini" @click="removeAll()">全部移除</el-button>
          <el-button size="mini" @click="getChannelList()">刷新</el-button>
        </div>
      </div>
    </div>
    <el-table size="small"  ref="channelListTable" :data="channelList" :height="winHeight" style="width: 100%;"
              header-row-class-name="table-header" @selection-change="handleSelectionChange" >
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
            <el-tag size="medium" effect="plain" v-if="scope.row.gbDeviceDbId">国标设备</el-tag>
            <el-tag size="medium" effect="plain" type="success" v-if="scope.row.streamPushId">推流设备</el-tag>
            <el-tag size="medium" effect="plain" type="warning" v-if="scope.row.streamProxyId">拉流代理</el-tag>
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
      <el-table-column label="共享状态" min-width="100">
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" :title="scope.row.platformId" v-if="scope.row.platformId">已共享</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.platformId">未共享</el-tag>
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
    <gbDeviceSelect ref="gbDeviceSelect"></gbDeviceSelect>
  </div>
</template>

<script>

import gbDeviceSelect from "./GbDeviceSelect.vue";

export default {
  name: 'shareChannelAdd',
  components: {gbDeviceSelect},
  props: [ 'platformId'],
  data() {
    return {
      channelList: [],
      searchSrt: "",
      channelType: "",
      online: "",
      hasShare: "false",
      winHeight: window.innerHeight - 380,
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
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
        url: `/api/platform/channel/list`,
        params: {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
          online: this.online,
          channelType: this.channelType,
          platformId: this.platformId,
          hasShare: this.hasShare
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
      if (this.hasShare === "") {
        if (row.platformId) {
          return false
        }else {
          return true
        }
      }else {
        return true
      }
    },
    add: function (row) {
      let channels = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channels.push(this.multipleSelection[i].gbId)
      }
      if (channels.length === 0) {
        this.$message.info({
          showClose: true,
          message: "请选择右侧通道"
        })
        return;
      }
      this.loading = true

      this.$axios({
        method: 'post',
        url: `/api/platform/channel/add`,
        data: {
          platformId: this.platformId,
          channelIds: channels
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "保存成功"
          })
          this.getChannelList()
        }else {
          this.$message.error({
              showClose: true,
              message: res.data.msg
            })
        }
        this.loading = false
      }).catch((error)=> {
        this.$message.error({
            showClose: true,
            message: error
          })
        this.loading = false
      });
    },
    addAll: function (row) {
      this.$confirm("确定全部添加？", '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.loading = true

        this.$axios({
          method: 'post',
          url: `/api/platform/channel/add`,
          data: {
            platformId: this.platformId,
            all: true
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            this.$message.success({
            showClose: true,
            message: "保存成功"
          })
            this.getChannelList()
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
          this.loading = false
        }).catch((error)=> {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      }).catch(() => {
      });
    },

    addByDevice: function (row) {
      this.$refs.gbDeviceSelect.openDialog((rows)=>{
        let deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$axios({
          method: 'post',
          url: `/api/platform/channel/device/add`,
          data: {
            platformId: this.platformId,
            deviceIds: deviceIds,
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            this.$message.success({
              showClose: true,
              message: "保存成功"
            })
            this.initData()
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
        }).catch((error)=> {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      })
    },

    removeByDevice: function (row) {
      this.$refs.gbDeviceSelect.openDialog((rows)=>{
        let deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$axios({
          method: 'post',
          url: `/api/platform/channel/device/remove`,
          data: {
            platformId: this.platformId,
            deviceIds: deviceIds,
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            this.$message.success({
              showClose: true,
              message: "保存成功"
            })
            this.initData()
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
        }).catch((error)=> {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      })
    },
    remove: function (row) {
      let channels = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channels.push(this.multipleSelection[i].gbId)
      }
      if (channels.length === 0) {
        this.$message.info({
          showClose: true,
          message: "请选择右侧通道"
        })
        return;
      }
      this.loading = true

      this.$axios({
        method: 'delete',
        url: `/api/platform/channel/remove`,
        data: {
          platformId: this.platformId,
          channelIds: channels
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message.success({
            showClose: true,
            message: "保存成功"
          })
          this.getChannelList()
        }else {
          this.$message.error({
              showClose: true,
              message: res.data.msg
            })
        }
        this.loading = false
      }).catch((error)=> {
        this.$message.error({
            showClose: true,
            message: error
          })
        this.loading = false
      });
    },
    removeAll: function (row) {

      this.$confirm("确定全部移除？", '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.loading = true
        this.$axios({
          method: 'delete',
          url: `/api/platform/channel/remove`,
          data: {
            platformId: this.platformId,
            all: true
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            this.$message.success({
            showClose: true,
            message: "保存成功"
          })
            this.getChannelList()
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
          this.loading = false
        }).catch((error)=> {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      }).catch(() => {
      });

    },
    search: function () {
      this.currentPage = 1;
      this.total = 0;
      this.initData();
    },
    refresh: function () {
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
  background-image: url("../../assets/loading.png");
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
