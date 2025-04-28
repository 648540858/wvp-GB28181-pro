<template>
  <div id="app" style="width: 100%">
    <el-dialog
      title="ApiKey列表"
      width="80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <el-form :inline="true" size="mini">
        <el-form-item>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addUserApiKey">
            添加ApiKey
          </el-button>
        </el-form-item>
      </el-form>
      <!--ApiKey列表-->
      <el-table
        size="small"
        :data="userList"
        style="width: 100%;font-size: 12px;"
        :height="winHeight"
        header-row-class-name="table-header"
      >
        <el-table-column prop="user.username" label="用户名" min-width="120" />
        <el-table-column prop="app" label="应用名" min-width="160" />
        <el-table-column label="ApiKey" :show-overflow-tooltip="true" min-width="300">
          <template #default="scope">
            <i v-clipboard="scope.row.apiKey" class="cpoy-btn el-icon-document-copy" title="点击拷贝" @success="$message({type:'success', message:'成功拷贝到粘贴板'})" />
            <span>{{ scope.row.apiKey }}</span>

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
        <el-table-column label="过期时间" width="160">
          <template #default="scope">
            {{ formatTime(scope.row.expiredAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注信息" min-width="160" />
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.enable"
              size="medium"
              icon="el-icon-circle-close"
              type="text"
              @click="disableUserApiKey(scope.row)"
            >
              停用
            </el-button>
            <el-button
              v-else
              size="medium"
              icon="el-icon-circle-check"
              type="text"
              @click="enableUserApiKey(scope.row)"
            >
              启用
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-refresh" type="text" @click="resetUserApiKey(scope.row)">
              重置
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-edit" type="text" @click="remarkUserApiKey(scope.row)">
              备注
            </el-button>
            <el-divider direction="vertical" />
            <el-button
              size="medium"
              icon="el-icon-delete"
              type="text"
              style="color: #f56c6c"
              @click="deleteUserApiKey(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </el-dialog>
    <addUserApiKey ref="addUserApiKey" />
    <remarkUserApiKey ref="remarkUserApiKey" />
  </div>
</template>

<script>
import addUserApiKey from '../dialog/addUserApiKey.vue'
import remarkUserApiKey from '../dialog/remarkUserApiKey.vue'
import moment from 'moment'

export default {
  name: 'UserApiKeyManager',
  components: {
    addUserApiKey,
    remarkUserApiKey
  },
  data() {
    return {
      userList: [], // 设备列表
      currentUser: {}, // 当前操作设备对象
      winHeight: window.innerHeight - 300,
      currentPage: 1,
      count: 15,
      total: 0,
      getUserApiKeyListLoading: false,
      showDialog: false
    }
  },
  mounted() {},
  methods: {
    openDialog: function(userId) {
      this.userId = userId
      this.showDialog = true
      this.initData()
    },
    initData() {
      this.getUserApiKeyList()
    },
    currentChange(val) {
      this.currentPage = val
      this.getUserApiKeyList()
    },
    handleSizeChange(val) {
      this.count = val
      this.getUserApiKeyList()
    },
    getUserApiKeyList() {
      this.getUserApiKeyListLoading = true
      this.$store.dispatch('userApiKeys/queryList', {
        page: this.currentPage,
        count: this.count
      })
        .then(data => {
          this.total = data.total
          this.userList = data.list
        })
        .finally(() => {
          this.getUserApiKeyListLoading = false
        })
    },
    addUserApiKey() {
      this.$refs.addUserApiKey.openDialog(this.userId, () => {
        this.$refs.addUserApiKey.close()
        this.$message({
          showClose: true,
          message: 'ApiKey添加成功',
          type: 'success'
        })
        setTimeout(this.getUserApiKeyList, 200)
      })
    },
    remarkUserApiKey(row) {
      this.$refs.remarkUserApiKey.openDialog(row.id, () => {
        this.$refs.remarkUserApiKey.close()
        this.$message({
          showClose: true,
          message: '备注修改成功',
          type: 'success'
        })
        setTimeout(this.getUserApiKeyList, 200)
      })
    },
    enableUserApiKey(row) {
      let msg = '确定启用此ApiKey？'
      if (row.online !== 0) {
        msg = '<strong>确定启用此ApiKey？</strong>'
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/enable', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: '启用成功',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }).catch(() => {
      })
    },
    disableUserApiKey(row) {
      let msg = '确定停用此ApiKey？'
      if (row.online !== 0) {
        msg = '<strong>确定停用此ApiKey？</strong>'
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/disable', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: '停用成功',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: '停用失败',
              type: 'error'
            })
            console.error(error)
          })
      }).catch(() => {
      })
    },
    resetUserApiKey(row) {
      let msg = '确定重置此ApiKey？'
      if (row.online !== 0) {
        msg = '<strong>确定重置此ApiKey？</strong>'
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/reset', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: '重置成功',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: '重置失败',
              type: 'error'
            })
            console.error(error)
          })
      }).catch(() => {
      })
    },
    deleteUserApiKey(row) {
      let msg = '确定删除此ApiKey？'
      if (row.online !== 0) {
        msg = '<strong>确定删除此ApiKey？</strong>'
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('userApiKeys/remove', row.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: '删除成功',
              type: 'success'
            })
            this.getUserApiKeyList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: '删除失败',
              type: 'error'
            })
            console.error(error)
          })
      }).catch(() => {
      })
    },
    close() {
      this.showDialog = false
    },
    formatTime(timestamp) {
      return moment(timestamp).format('YYYY-MM-DD HH:mm:ss')
    }
  }
}
</script>
<style>

</style>
