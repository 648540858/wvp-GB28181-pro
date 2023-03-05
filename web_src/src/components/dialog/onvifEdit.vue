<template>
  <div id="onvif搜索" v-loading="isLoging">
    <el-dialog
      title="onvif搜索"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="form" :rules="rules" :model="form" label-width="140px" >
          <el-form-item label="地址" prop="hostName" >
            <el-select v-model="form.hostName" style="float: left; width: 100%" >
              <el-option
                v-for="item in hostNames"
                :key="item"
                :label="item.replace('http://', '')"
                :value="item">
              </el-option>
            </el-select>

          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" clearable></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" clearable></el-input>
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit" >确认</el-button>
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
  name: "onvifEdit",
  props: {},
  computed: {},
  created() {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      hostNames:[],
      form: {
        hostName: null,
        username: "admin",
        password: "admin123",
      },

      rules: {
        hostName: [{ required: true, message: "请选择", trigger: "blur" }],
        username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
        password: [{ required: true, message: "请输入密码", trigger: "blur" }],
      },
    };
  },
  methods: {
    openDialog: function (hostNamesParam, callback) {
      console.log(hostNamesParam)
      this.showDialog = true;
      this.listChangeCallback = callback;
      if (hostNamesParam != null) {
        this.hostNames = hostNamesParam;
      }

    },
    onSubmit: function () {
      console.log("onSubmit");
      console.log(this.form);
      this.$axios({
        method: 'get',
        url:`/api/onvif/rtsp`,
        params: {
          hostname: this.form.hostName,
          timeout: 3000,
          username: this.form.username,
          password: this.form.password,
        }
      }).then((res) => {
        console.log(res.data)
        if (res.data.code === 0) {
          if (res.data.data != null) {
            this.listChangeCallback(res.data.data)
          }else {
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: "error",
            });
          }

        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: "error",
          });
        }
      }).catch(function (error) {
        console.log(error);
      });
    },
    close: function () {
      this.showDialog = false;
      this.$refs.form.resetFields();
    },
  },
};
</script>
