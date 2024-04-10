<template>
  <div id="remarkUserApiKey" v-loading="isLoading">
    <el-dialog
      title="ApiKey备注"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="form" :rules="rules" status-icon label-width="80px">
          <el-form-item label="备注" prop="oldPassword">
            <el-input type="textarea" v-model="form.remark" autocomplete="off" :autosize="{ minRows: 5}" maxlength="255" show-word-limit></el-input>
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
  name: "remarkUserApiKey",
  props: {},
  computed: {},
  created() {
  },
  data() {
    return {
      userApiKeyId: null,
      form: {
        remark: null
      },
      rules: {},
      listChangeCallback: null,
      showDialog: false,
      isLoading: false
    };
  },
  methods: {
    resetForm() {
      this.form = {
        remark: null
      }
    },
    openDialog(userApiKeyId, callback) {
      this.resetForm()
      this.userApiKeyId = userApiKeyId
      this.listChangeCallback = callback
      this.showDialog = true
    },
    onSubmit() {
      this.$axios({
        method: 'post',
        url: "/api/userApiKey/remark",
        params: {
          id: this.userApiKeyId,
          remark: this.form.remark
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: '备注修改成功!',
            type: 'success'
          });
          this.showDialog = false;
          this.listChangeCallback()
        } else {
          this.$message({
            showClose: true,
            message: '备注修改失败',
            type: 'error'
          });
        }
      }).catch((error) => {
        console.error(error)
      });
    },
    close() {
      this.showDialog = false
    },
  },
};
</script>
