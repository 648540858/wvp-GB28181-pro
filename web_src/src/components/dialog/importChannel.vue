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
          >
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <div class="el-upload__tip" slot="tip">只能上传 csv / xls / xlsx 文件</div>
        </el-upload>
      </div>
    </el-dialog>
  </div>
</template>

<script>

export default {
  name: "importChannel",
  computed: {},
  created() {},
  data() {
    return {
      submitCallback: null,
      showDialog: false,
      isLoging: false,
      isEdit: false,
      uploadUrl: "debug/api/push/upload",
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
      this.$refs.form.resetFields();
    },
  },
};
</script>
<style>
.upload-box{
  text-align: center;
}
</style>
