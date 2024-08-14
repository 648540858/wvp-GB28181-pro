<template>
  <div id="app" style="width: 100%">
    <div class="page-header" style="margin-bottom: 0">
      <div class="page-title">
        <el-page-header @back="goBack" content="ApiKey列表"></el-page-header>
      </div>
      <div class="page-header-btn">
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addUserApiKey">
          添加ApiKey
        </el-button>
      </div>
    </div>
    <!--ApiKey列表-->
    <el-table size=mini :data="userList" style="width: 100%;font-size: 12px;" :height="winHeight"
              header-row-class-name="table-header">
      <el-table-column prop="user.username" label="用户名" min-width="120"/>
      <el-table-column prop="app" label="应用名" min-width="160"/>
      <el-table-column label="ApiKey" :show-overflow-tooltip="true" min-width="300">
        <template #default="scope">
<!--          <el-button style="float: right;" type="primary" size="mini" icon="el-icon-document-copy"  title="点击拷贝" v-clipboard="scope.row.apiKey" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>-->
          <i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="scope.row.apiKey" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
          <span>{{scope.row.apiKey}}</span>

        </template>
      </el-table-column>
      <el-table-column prop="enable" label="启用" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.enable">
            启用
          </el-tag>
          <el-tag v-else type="info">
            停用
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expiredAt" label="过期时间" width="160"/>
      <el-table-column prop="remark" label="备注信息" min-width="160"/>
      <el-table-column label="操作" min-width="260" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.enable"
                     size="medium" icon="el-icon-circle-close" type="text" @click="disableUserApiKey(scope.row)">
            停用
          </el-button>
          <el-button v-else
                     size="medium" icon="el-icon-circle-check" type="text" @click="enableUserApiKey(scope.row)">
            启用
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-refresh" type="text" @click="resetUserApiKey(scope.row)">
            重置
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-edit" type="text" @click="remarkUserApiKey(scope.row)">
            备注
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-delete" type="text" @click="deleteUserApiKey(scope.row)"
                     style="color: #f56c6c">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <addUserApiKey ref="addUserApiKey"></addUserApiKey>
    <remarkUserApiKey ref="remarkUserApiKey"></remarkUserApiKey>
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
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import addUserApiKey from "./dialog/addUserApiKey.vue";
import remarkUserApiKey from './dialog/remarkUserApiKey.vue'

export default {
  name: 'userApiKeyManager',
  components: {
    uiHeader,
    addUserApiKey,
    remarkUserApiKey
  },
  data() {
    return {
      userList: [], //设备列表
      currentUser: {}, //当前操作设备对象
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      getUserApiKeyListLoading: false
    };
  },
  mounted() {
    this.initParam();
    this.initData();
  },
  methods: {
    goBack() {
      this.$router.back()
    },
    initParam() {
      this.userId = this.$route.params.userId;
    },
    initData() {
      this.getUserApiKeyList();
    },
    currentChange(val) {
      this.currentPage = val;
      this.getUserApiKeyList();
    },
    handleSizeChange(val) {
      this.count = val;
      this.getUserApiKeyList();
    },
    getUserApiKeyList() {
      let that = this;
      this.getUserApiKeyListLoading = true;
      this.$axios({
        method: 'get',
        url: `/api/userApiKey/userApiKeys`,
        params: {
          page: that.currentPage,
          count: that.count
        }
      }).then((res) => {
        if (res.data.code === 0) {
          that.total = res.data.data.total;
          that.userList = res.data.data.list;
        }
        that.getUserApiKeyListLoading = false;
      }).catch((error) => {
        that.getUserApiKeyListLoading = false;
      });
    },
    addUserApiKey() {
      this.$refs.addUserApiKey.openDialog(this.userId, () => {
        this.$refs.addUserApiKey.close();
        this.$message({
          showClose: true,
          message: "ApiKey添加成功",
          type: "success",
        });
        setTimeout(this.getUserApiKeyList, 200)
      })
    },
    remarkUserApiKey(row) {
      this.$refs.remarkUserApiKey.openDialog(row.id, () => {
        this.$refs.remarkUserApiKey.close();
        this.$message({
          showClose: true,
          message: "备注修改成功",
          type: "success",
        });
        setTimeout(this.getUserApiKeyList, 200)
      })
    },
    enableUserApiKey(row) {
      let msg = "确定启用此ApiKey？"
      if (row.online !== 0) {
        msg = "<strong>确定启用此ApiKey？</strong>"
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$axios({
          method: 'post',
          url: `/api/userApiKey/enable?id=${row.id}`
        }).then((res) => {
          this.$message({
            showClose: true,
            message: '启用成功',
            type: 'success'
          });
          this.getUserApiKeyList();
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: '启用失败',
            type: 'error'
          });
          console.error(error);
        });
      }).catch(() => {
      });
    },
    disableUserApiKey(row) {
      let msg = "确定停用此ApiKey？"
      if (row.online !== 0) {
        msg = "<strong>确定停用此ApiKey？</strong>"
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$axios({
          method: 'post',
          url: `/api/userApiKey/disable?id=${row.id}`
        }).then((res) => {
          this.$message({
            showClose: true,
            message: '停用成功',
            type: 'success'
          });
          this.getUserApiKeyList();
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: '停用失败',
            type: 'error'
          });
          console.error(error);
        });
      }).catch(() => {
      });
    },
    resetUserApiKey(row) {
      let msg = "确定重置此ApiKey？"
      if (row.online !== 0) {
        msg = "<strong>确定重置此ApiKey？</strong>"
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$axios({
          method: 'post',
          url: `/api/userApiKey/reset?id=${row.id}`
        }).then((res) => {
          this.$message({
            showClose: true,
            message: '重置成功',
            type: 'success'
          });
          this.getUserApiKeyList();
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: '重置失败',
            type: 'error'
          });
          console.error(error);
        });
      }).catch(() => {
      });
    },
    deleteUserApiKey(row) {
      let msg = "确定删除此ApiKey？"
      if (row.online !== 0) {
        msg = "<strong>确定删除此ApiKey？</strong>"
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$axios({
          method: 'delete',
          url: `/api/userApiKey/delete?id=${row.id}`
        }).then((res) => {
          this.$message({
            showClose: true,
            message: '删除成功',
            type: 'success'
          });
          this.getUserApiKeyList();
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: '删除失败',
            type: 'error'
          });
          console.error(error);
        });
      }).catch(() => {
      });
    },
  }
}
</script>
<style>

</style>
