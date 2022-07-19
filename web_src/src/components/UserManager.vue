<template>

  <div id="app" style="width: 100%">
    <div class="page-header">

      <div class="page-title">用户列表</div>
      <div class="page-header-btn">
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addUser">
          添加用户
        </el-button>

      </div>
    </div>
    <!--用户列表-->
    <el-table :data="userList" style="width: 100%;font-size: 12px;" :height="winHeight"
              header-row-class-name="table-header">
      <el-table-column prop="username" label="用户名" min-width="160"/>
      <el-table-column prop="pushKey" label="pushkey" min-width="160"/>
      <el-table-column prop="role.name" label="类型" min-width="160"/>
      <el-table-column label="操作" min-width="450" fixed="right">
        <template slot-scope="scope">
          <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">修改密码</el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-edit" type="text" @click="changePushKey(scope.row)">修改pushkey</el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-delete" type="text" @click="deleteUser(scope.row)"
                     style="color: #f56c6c">删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <changePasswordForAdmin ref="changePasswordForAdmin"></changePasswordForAdmin>
    <changePushKey ref="changePushKey"></changePushKey>
    <addUser ref="addUser"></addUser>
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
import changePasswordForAdmin from './dialog/changePasswordForAdmin.vue'
import changePushKey from './dialog/changePushKey.vue'
import addUser from '../components/dialog/addUser.vue'

export default {
  name: 'userManager',
  components: {
    uiHeader,
    changePasswordForAdmin,
    changePushKey,
    addUser
  },
  data() {
    return {
      userList: [], //设备列表
      currentUser: {}, //当前操作设备对象

      videoComponentList: [],
      updateLooper: 0, //数据刷新轮训标志
      currentUserLenth: 0,
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      getUserListLoading: false
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
      this.getUserList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.getUserList();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getUserList();
    },
    getUserList: function () {
      let that = this;
      this.getUserListLoading = true;
      this.$axios({
        method: 'get',
        url: `/api/user/users`,
        params: {
          page: that.currentPage,
          count: that.count
        }
      }).then(function (res) {
        that.total = res.data.total;
        that.userList = res.data.list;
        that.getUserListLoading = false;
      }).catch(function (error) {
        that.getUserListLoading = false;
      });

    },
    edit: function (row) {
      this.$refs.changePasswordForAdmin.openDialog(row, () => {
        this.$refs.changePasswordForAdmin.close();
        this.$message({
          showClose: true,
          message: "密码修改成功",
          type: "success",
        });
        setTimeout(this.getUserList, 200)

      })
    },
    deleteUser: function (row) {
      let msg = "确定删除此用户？"
      if (row.online !== 0) {
        msg = "<strong>确定删除此用户？</strong>"
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
          url: `/api/user/delete?id=${row.id}`
        }).then((res) => {
          this.getUserList();
        }).catch((error) => {
          console.error(error);
        });
      }).catch(() => {

      });


    },

    changePushKey: function (row) {
      this.$refs.changePushKey.openDialog(row, () => {
        this.$refs.changePushKey.close();
        this.$message({
          showClose: true,
          message: "pushKey修改成功",
          type: "success",
        });
        setTimeout(this.getUserList, 200)

      })
    },
    addUser: function () {
      // this.$refs.addUser.openDialog()
      this.$refs.addUser.openDialog( () => {
        this.$refs.addUser.close();
        this.$message({
          showClose: true,
          message: "用户添加成功",
          type: "success",
        });
        setTimeout(this.getUserList, 200)

      })
    }
  }
}
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
