<template>
  <div id="app" class="app-container">
    <div style="height: calc(100vh - 124px);">
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
        <el-table-column prop="pushKey" label="推流鉴权密钥" min-width="160">
          <template v-slot:default="scope">
            <div class="push-key-cell">
              <template v-if="editingId === scope.row.id">
                <el-input
                  v-model="scope.row.pushKey"
                  ref="editInput"
                  size="mini"
                  class="edit-input"
                  @keyup.enter.native="savePushKey(scope.row)"
                  @blur="savePushKey(scope.row)"
                />
                <div class="edit-actions">
                  <el-button type="text" icon="el-icon-check" class="save-btn" @click="savePushKey(scope.row)"></el-button>
                  <el-button type="text" icon="el-icon-close" class="cancel-btn" @click="cancelEdit(scope.row)"></el-button>
                </div>
              </template>
              <template v-else>
                <el-tooltip
                  :content="scope.row.pushKey ? '点击修改' : '暂无推流鉴权密钥'"
                  placement="top"
                >
                  <span class="push-key-text" @click="startEdit(scope.row)">
                    {{ scope.row.pushKey || '未设置' }}
                  </span>
                </el-tooltip>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role.name" label="类型" min-width="160" />
        <el-table-column label="操作" min-width="450" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">修改密码</el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-edit" type="text" @click="showUserApiKeyManager(scope.row)">管理ApiKey</el-button>
            <el-divider direction="vertical" />
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
    <addUser ref="addUser" />
    <apiKeyManager ref="apiKeyManager" />
  </div>
</template>

<script>
import changePasswordForAdmin from './dialog/changePasswordForAdmin.vue'
import addUser from './dialog/addUser.vue'
import apiKeyManager from './apiKeyManager.vue'

export default {
  name: 'User',
  components: {
    changePasswordForAdmin,
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
      getUserListLoading: false,
      editingId: null,
      originalPushKey: null
    }
  },
  mounted() {
    this.initData()
  },
  destroyed() {},
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
    },
    startEdit: function(row) {
      if (!row.pushKey) {
        row.pushKey = ''
      }
      this.editingId = row.id
      this.originalPushKey = row.pushKey
      this.$nextTick(() => {
        const input = this.$refs.editInput
        if (input) {
          input.focus()
        }
      })
    },
    cancelEdit: function(row) {
      row.pushKey = this.originalPushKey
      this.editingId = null
      this.originalPushKey = null
    },
    savePushKey: function(row) {
      this.$store.dispatch('user/changePushKey', {
        pushKey: row.pushKey,
        userId: row.id
      })
        .then(() => {
          this.$message({
            showClose: true,
            message: 'pushKey修改成功',
            type: 'success'
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          this.editingId = null
          this.originalPushKey = null
        })
    }
  }
}
</script>

<style scoped>
.push-key-cell {
  display: flex;
  align-items: center;
  position: relative;
  min-height: 20px;
}

.push-key-text {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 3px;
  transition: background-color 0.2s;
}

.push-key-text:hover {
  background-color: #ecf5ff;
}

.edit-input {
  width: 100%;
  margin-bottom: 4px;
}

.edit-actions {
  display: flex;
  gap: 4px;
}

.save-btn {
  margin-left: 4px;
  color: #67c23a;
}

.cancel-btn {
  margin-left: 4px;
  color: #f56c6c;
}
</style>
