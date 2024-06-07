<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">设备列表</div>
      <div class="page-header-btn">
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">接入新设备
        </el-button>
        <el-button icon="el-icon-refresh-right" circle size="mini" :loading="getListLoading"
                   @click="getList()"></el-button>
      </div>
    </div>
    <!--设备列表-->
    <el-table :data="deviceList" style="width: 100%;font-size: 12px;" :height="winHeight"
              header-row-class-name="table-header">
      <el-table-column prop="phoneNumber" label="终端手机号" min-width="160">
      </el-table-column>
      <el-table-column prop="terminalId" label="终端ID" min-width="160">
      </el-table-column>
      <el-table-column prop="provinceText" label="省域" min-width="160">
      </el-table-column>
      <el-table-column prop="cityText" label="市县域" min-width="160">
      </el-table-column>
      <el-table-column prop="makerId" label="制造商" min-width="160">
      </el-table-column>
      <el-table-column prop="model" label="型号" min-width="160">
      </el-table-column>
      <el-table-column label="车牌颜色" min-width="160">
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <span v-if="scope.row.plateColor === 1">蓝色</span>
            <span v-else-if="scope.row.plateColor === 2">黄色</span>
            <span v-else-if="scope.row.plateColor === 3">黑色</span>
            <span v-else-if="scope.row.plateColor === 4">白色</span>
            <span v-else-if="scope.row.plateColor === 5">绿色</span>
            <span v-else-if="scope.row.plateColor === 91">农黄色</span>
            <span v-else-if="scope.row.plateColor === 92">农绿色</span>
            <span v-else-if="scope.row.plateColor === 93">黄绿色</span>
            <span v-else-if="scope.row.plateColor === 94">渐变绿</span>
            <span v-else>未上牌</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="plateNo" label="车牌" min-width="160">
      </el-table-column>
      <el-table-column prop="registerTime" label="注册时间" min-width="160">
      </el-table-column>
      <el-table-column label="状态" min-width="160">
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.status">在线</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.status">离线</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="280" fixed="right">
        <template slot-scope="scope">
          <el-divider direction="vertical"></el-divider>
          <el-button type="text" size="medium" icon="el-icon-video-camera"
                     @click="showChannelList(scope.row)">通道
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">编辑</el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-delete" type="text" @click="deleteDevice(scope.row)"
                     style="color: #f56c6c">删除
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
    <deviceEdit ref="deviceEdit"></deviceEdit>
    <syncChannelProgress ref="syncChannelProgress"></syncChannelProgress>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import deviceEdit from './dialog/jtDeviceEdit.vue'
import syncChannelProgress from './dialog/SyncChannelProgress.vue'
import JTDeviceService from "./service/JTDeviceService";

export default {
  name: 'app',
  components: {
    uiHeader,
    deviceEdit,
    syncChannelProgress,
  },
  data() {
    return {
      deviceService: new JTDeviceService(),
      deviceList: [], //设备列表
      updateLooper: 0, //数据刷新轮训标志
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      getListLoading: false,
    };
  },
  mounted() {
    this.initData();
    this.updateLooper = setInterval(this.initData, 10000);
  },
  destroyed() {
    this.$destroy('videojs');
    clearTimeout(this.updateLooper);
  },
  methods: {
    initData: function () {
      this.getList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.getList();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getList();
    },
    getList: function () {
      this.getListLoading = true;
      this.deviceService.getDeviceList(this.currentPage, this.count, (data) => {
          if (data.code === 0) {
            this.total = data.data.total;
            this.deviceList = data.data.list;
          }
          this.getListLoading = false;
        }, () => {
          this.getListLoading = false;
        }
      )
    },
    deleteDevice: function (row) {
      this.$confirm("确定删除此设备？", '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.deviceService.deleteDevice(row.id, (data) => {
          this.getList();
        })
      }).catch(() => {

      });
    },
    edit: function (row) {
      this.$refs.deviceEdit.openDialog(row, () => {
        this.$refs.deviceEdit.close();
        this.$message({
          showClose: true,
          message: "设备修改成功，通道字符集将在下次更新生效",
          type: "success",
        });
        setTimeout(this.getList, 200)

      })
    },
    showChannelList: function (row) {
      this.$router.push(`/jtChannelList/${row.id}`);
    },
    add: function () {
      this.$refs.deviceEdit.openDialog(null, () => {
        this.$refs.deviceEdit.close();
        this.$message({
          showClose: true,
          message: "添加成功",
          type: "success",
        });
        setTimeout(this.getList, 200)

      })
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
