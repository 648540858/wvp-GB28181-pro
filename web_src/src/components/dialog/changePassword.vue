<template>
  <div id="changePassword" v-loading="isLoging">
    <el-dialog
      title="修改密码"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="passwordForm" :rules="rules" status-icon label-width="80px">
              <el-form-item label="旧密码" prop="oldPassword" >
                <el-input v-model="oldPassword" autocomplete="off"></el-input>
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword" >
                <el-input v-model="newPassword" autocomplete="off"></el-input>
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="confirmPassword" autocomplete="off"></el-input>
              </el-form-item>

              <el-form-item>
                <div style="float: right;">
                  <el-button type="primary" @click="onSubmit">保存</el-button>
                  <el-button @click="close">取消</el-button>
                </div>
              </el-form-item>
            </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import crypto from 'crypto'
import userService from "../service/UserService";
export default {
  name: "changePassword",
  props: {},
  computed: {},
  created() {},
  data() {
    let validatePass0 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入旧密码'));
      } else {
        callback();
      }
    };
    let validatePass1 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入新密码'));
      } else {
        if (this.confirmPassword !== '') {
          this.$refs.passwordForm.validateField('confirmPassword');
        }
        callback();
      }
    };
    let validatePass2 = (rule, value, callback) => {
      if (this.confirmPassword === '') {
        callback(new Error('请再次输入密码'));
      } else if (this.confirmPassword !== this.newPassword) {
        callback(new Error('两次输入密码不一致!'));
      } else {
        callback();
      }
    };
    return {
      oldPassword: null,
      newPassword: null,
      confirmPassword: null,
      showDialog: false,
      isLoging: false,
      rules: {
        oldPassword: [{ required: true, validator: validatePass0, trigger: "blur" }],
        newPassword: [{ required: true, validator: validatePass1, trigger: "blur" }, {
            pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,.\/]).{8,20}$/,
            message: "密码长度在8-20位之间,由字母+数字+特殊字符组成",
          },],
        confirmPassword: [{ required: true, validator: validatePass2, trigger: "blur" }],
      },
    };
  },
  methods: {
    openDialog: function () {
      this.showDialog = true;
    },
    onSubmit: function () {
      this.$axios({
        method: 'post',
        url:"/api/user/changePassword",
        params: {
          oldPassword: crypto.createHash('md5').update(this.oldPassword, "utf8").digest('hex'),
          password: this.newPassword
        }
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: '修改成功，请重新登录',
            type: 'success'
          });
          this.showDialog = false;
          setTimeout(()=>{
            // 删除cookie，回到登录页面
            userService.clearUserInfo();
            this.$router.push('/login');
            this.sseSource.close();
          },800)
        }else {
          this.$message({
            showClose: true,
            message: '修改密码失败，是否已登录（接口鉴权关闭无法修改密码）',
            type: 'error'
          });
        }
      }).catch((error)=> {
        console.error(error)
      });
    },
    close: function () {
      this.showDialog = false;
      this.oldPassword = null;
      this.newPassword = null;
      this.confirmPassword = null;
    },
  },
};
</script>
