<template>
  <div id="changepushKey" v-loading="isLoging">
    <el-dialog
      title="修改密码"
      width="42%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 18px;">
        <el-form ref="pushKeyForm" :rules="rules" status-icon label-width="86px">
              <el-form-item label="新pushKey" prop="newPushKey" >
                <el-input v-model="newPushKey" autocomplete="off"></el-input>
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
export default {
  name: "changePushKey",
  props: {},
  computed: {},
  created() {},
  data() {
    let validatePass1 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入新pushKey'));
      } else {
        callback();
      }
    };
    return {
      newPushKey: null,
      confirmpushKey: null,
      userId: null,
      showDialog: false,
      isLoging: false,
      listChangeCallback: null,
      form: {},
      rules: {
        newpushKey: [{ required: true, validator: validatePass1, trigger: "blur" }],
      },
    };
  },
  methods: {
    openDialog: function (row, callback) {
      console.log(row)
      this.showDialog = true;
      this.listChangeCallback = callback;
      if (row != null) {
        this.form = row;
      }
    },
    onSubmit: function () {
      this.$axios({
        method: 'post',
        url:"/api/user/changePushKey",
        params: {
          pushKey: this.newPushKey,
          userId: this.form.id,
        }
      }).then((res)=> {
        console.log(res.data)
        if (res.data.msg === "success"){
          this.$message({
            showClose: true,
            message: '修改成功',
            type: 'success'
          });
          this.showDialog = false;
          this.listChangeCallback();
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
      this.newpushKey = null;
      this.userId=null;
      this.adminId=null;
    },
  },
};
</script>
