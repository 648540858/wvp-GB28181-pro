<template>
  <div id="addStreamProxy" v-loading="isLoging">
    <el-dialog
      title=" 加入"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="streamProxy" :rules="rules" :model="proxyParam" label-width="140px">
              <el-form-item label="名称" prop="name">
                <el-input v-model="proxyParam.name" clearable></el-input>
              </el-form-item>
              <el-form-item label="流应用名" prop="app">
                <el-input v-model="proxyParam.app" clearable :disabled="edit"></el-input>
              </el-form-item>
              <el-form-item label="流ID" prop="stream">
                <el-input v-model="proxyParam.stream" clearable :disabled="edit"></el-input>
              </el-form-item>
              <el-form-item label="国标编码" prop="gbId">
                <el-input v-model="proxyParam.gbId" placeholder="设置国标编码可推送到国标" clearable></el-input>
              </el-form-item>
              <el-form-item label="经度" prop="longitude" v-if="proxyParam.gbId">
                <el-input v-model="proxyParam.longitude" placeholder="经度" clearable></el-input>
              </el-form-item>
              <el-form-item label="纬度" prop="latitude" v-if="proxyParam.gbId">
                <el-input v-model="proxyParam.latitude" placeholder="经度" clearable></el-input>
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
  name: "pushStreamEdit",
  props: {},
  computed: {},
  created() {},
  data() {
    // var deviceGBIdRules = async (rule, value, callback) => {
    //   console.log(value);
    //   if (value === "") {
    //     callback(new Error("请输入设备国标编号"));
    //   } else {
    //     var exit = await this.deviceGBIdExit(value);
    //     console.log(exit);
    //     console.log(exit == "true");
    //     console.log(exit === "true");
    //     if (exit) {
    //       callback(new Error("设备国标编号已存在"));
    //     } else {
    //       callback();
    //     }
    //   }
    // };
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      edit: false,
      proxyParam: {
          name: null,
          app: null,
          stream: null,
          gbId: null,
          longitude: null,
          latitude: null,
      },
      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        app: [{ required: true, message: "请输入应用名", trigger: "blur" }],
        stream: [{ required: true, message: "请输入流ID", trigger: "blur" }],
        gbId: [{ required: true, message: "请输入国标编码", trigger: "blur" }],
      },
    };
  },
  methods: {
    openDialog: function (proxyParam, callback) {
      this.showDialog = true;
      this.listChangeCallback = callback;
      if (proxyParam != null) {
        this.proxyParam = proxyParam;
        this.edit = true
      }else{
        this.proxyParam= {
          name: null,
          app: null,
          stream: null,
          gbId: null,
          longitude: null,
          latitude: null,
        }
        this.edit = false
      }
    },
    onSubmit: function () {
      console.log("onSubmit");
      if (this.edit) {
        this.$axios({
          method:"post",
          url:`/api/push/save_to_gb`,
          data: this.proxyParam
        }).then( (res) => {
          if (res.data.code === 0) {
            this.$message({
              showClose: true,
              message: "保存成功",
              type: "success",
            });
            this.showDialog = false;
            if (this.listChangeCallback != null) {
              this.listChangeCallback();
            }
          }
        }).catch((error)=> {
          console.log(error);
        });
      }else {
        this.$axios({
          method:"post",
          url:`/api/push/add`,
          data: this.proxyParam
        }).then( (res) => {
          if (res.data.code === 0) {
            this.$message({
              showClose: true,
              message: "保存成功",
              type: "success",
            });
            this.showDialog = false;
            if (this.listChangeCallback != null) {
              this.listChangeCallback();
            }
          }
        }).catch((error)=> {
          console.log(error);
        });
      }

    },
    close: function () {
      console.log("关闭加入GB");
      this.showDialog = false;
      this.$refs.streamProxy.resetFields();
    },
    deviceGBIdExit: async function (deviceGbId) {
      var result = false;
      var that = this;
      await that.$axios({
        method:"get",
        url:`/api/platform/exit/${deviceGbId}`
      }).then(function (res) {
        result = res.data;
      }).catch(function (error) {
        console.log(error);
      });
      return result;
    },
    checkExpires: function() {
      if (this.platform.enable && this.platform.expires == "0") {
        this.platform.expires = "300";
      }
    },
    handleNodeClick: function (node){

    }
  },
};
</script>
