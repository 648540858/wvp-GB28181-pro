<template>
  <div class="app-container">
    <div style="height: var(--wvp-page-content-height);">
      <el-form :inline="true" size="mini">
        <el-form-item>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addUser">
            添加用户
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <!--        <el-button icon="el-icon-refresh-right" circle @click="refresh()" />-->
        </el-form-item>
      </el-form>
      <!--用户列表-->
      <el-table
        size="small"
        :data="userList"
        style="width: 100%;font-size: 12px;"
        height="calc(100% - 64px)"
        header-row-class-name="table-header"
      >
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="pushKey" label="推流密钥" min-width="160" />
        <el-table-column prop="role.name" label="类型" min-width="160" />
        <el-table-column label="操作" min-width="450" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">修改密码</el-button>
            <el-button size="medium" icon="el-icon-edit" type="text" @click="changePushKey(scope.row)">修改推流密钥</el-button>
            <el-button size="medium" icon="el-icon-edit" type="text" @click="showUserApiKeyManager(scope.row)">管理API密钥</el-button>
            <el-button
              size="medium"
              icon="el-icon-delete"
              type="text"
              style="color: #f56c6c"
              @click="deleteUser(scope.row)"
            >删除
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
    </div>
    <changePasswordForAdmin ref="changePasswordForAdmin" />
    <changePushKey ref="changePushKey" />
    <addUser ref="addUser" />
    <apiKeyManager ref="apiKeyManager" />
  </div>
</template>

<script>
import changePasswordForAdmin from './dialog/changePasswordForAdmin.vue'
import changePushKey from './dialog/changePushKey.vue'
import addUser from './dialog/addUser.vue'
import apiKeyManager from './apiKeyManager.vue'

export default {
  name: 'User',
  components: {
    changePasswordForAdmin,
    changePushKey,
    addUser,
    apiKeyManager
  },
  data() {
    return {
      userList: [], // 设备列表
      currentUser: {}, // 当前操作设备对象
      videoComponentList: [],
      currentUserLenth: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      getUserListLoading: false
    }
  },
  mounted() {
    this.initData()
  },
  unmounted() {},
  methods: {
    initData: function() {
      this.getUserList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getUserList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getUserList()
    },
    getUserList: function() {
      this.$store.dispatch('user/queryList', {
        page: this.currentPage,
        count: this.count
      })
        .then(data => {
          this.total = data.total
          this.userList = data.list
        })
        .catch(error => {
          console.log(error)
        })
        .finally(() => {
          this.getUserListLoading = false
        })
    },
    edit: function(row) {
      this.$refs.changePasswordForAdmin.openDialog(row, () => {
        this.$refs.changePasswordForAdmin.close()
        this.$message({
          showClose: true,
          message: '密码修改成功',
          type: 'success'
        })
        setTimeout(this.getUserList, 200)
      })
    },
    deleteUser: function(row) {
      let msg = '确定删除此用户？'
      if (row.online !== 0) {
        msg = '<strong>确定删除此用户？</strong>'
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('user/removeById', row.id)
          .then(() => {
            this.getUserList()
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

    changePushKey: function(row) {
      this.$refs.changePushKey.openDialog(row, () => {
        this.$refs.changePushKey.close()
        this.$message({
          showClose: true,
          message: '推流密钥修改成功',
          type: 'success'
        })
        setTimeout(this.getUserList, 200)
      })
    },
    addUser: function() {
      // this.$refs.addUser.openDialog()
      this.$refs.addUser.openDialog(() => {
        this.$refs.addUser.close()
        this.$message({
          showClose: true,
          message: '用户添加成功',
          type: 'success'
        })
        setTimeout(this.getUserList, 200)
      })
    },
    showUserApiKeyManager: function(row) {
      this.$refs.apiKeyManager.openDialog(row.id)
    }
  }
}
</script>
