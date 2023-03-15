<template>
  <div id="importChannel" v-loading="isLoging">
    <el-dialog
      title="导入通道数据"
      width="30rem"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        <el-upload
          class="upload-box"
          drag
          :action="uploadUrl"
          name="file"
          :headers="headers"
          :on-success="successHook"
          :on-error="errorHook"
          >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <div class="el-upload__tip" slot="tip">只能上传 csv / xls / xlsx 文件</div>
        </el-upload>
      </div>
    </el-dialog>
    <ShowErrorData ref="showErrorData" :gbIds="errorGBIds" :streams="errorStreams" ></ShowErrorData>
  </div>
</template>

<script>

import ShowErrorData from './importChannelShowErrorData.vue'

import userService from "../service/UserService";

export default {
  name: "importChannel",
  components: {
    ShowErrorData,
  },
  created() {},
  data() {
    return {
      submitCallback: null,
      showDialog: false,
      isLoging: false,
      isEdit: false,
      errorStreams: [],
      errorGBIds: [],
      headers: {
        "access-token": userService.getToken()
      },
      uploadUrl: process.env.NODE_ENV === 'development'? `http://127.0.0.1:8080/debug/api/push/upload`: (window.baseUrl ? window.baseUrl : "") + `/api/push/upload`,
    };
  },
  methods: {
    openDialog: function (callback) {
      this.showDialog = true;
      this.submitCallback = callback;
    },
    onSubmit: function () {
      console.log("onSubmit");
      console.log(this.form);
      this.$axios({
        method:"post",
        url:`/api/platform/catalog/${!this.isEdit? "add":"edit"}`,
        data: this.form
      })
        .then((res)=> {
          if (res.data.code === 0) {
            console.log("添加/修改成功")
            if (this.submitCallback)this.submitCallback()
          }else {
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: "error",
            });
          }
          this.close();
        })
        .catch((error)=> {
          console.log(error);
        });
    },
    close: function () {
      this.showDialog = false;
    },
    successHook: function(response, file, fileList){
      if (response.code === 0) {
        this.$message({
          showClose: true,
          message: response.msg,
          type: "success",
        });
      }else if (response.code === 1) {
        this.errorGBIds = response.data.gbId
        this.errorStreams = response.data.stream
        console.log(this.$refs)
        console.log(this.$refs.showErrorData)
        this.$refs.showErrorData.openDialog()
      }else {
        this.$message({
          showClose: true,
          message: response.msg,
          type: "error",
        });
      }
    },
    errorHook: function (err, file, fileList) {
      this.$message({
        showClose: true,
        message: err,
        type: "error",
      });
    }
  },
};
</script>
<style>
.upload-box{
  text-align: center;
}
.errDataBox{
  max-height: 15rem;
  overflow: auto;
}
</style>
