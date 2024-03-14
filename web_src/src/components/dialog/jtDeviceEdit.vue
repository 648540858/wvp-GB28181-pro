<template>
  <div id="deviceEdit" v-loading="isLoging">
    <el-dialog
      title="设备编辑"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="form" :rules="rules" :model="form" label-width="200px" >
          <el-form-item label="终端ID" prop="terminalId">
            <el-input v-model="form.terminalId" clearable></el-input>
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
  name: "deviceEdit",
  props: {},
  computed: {},
  created() {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      form: {},
      isEdit: false,
      rules: {
        deviceId: [{ required: true, message: "请输入设备编号", trigger: "blur" }]
      },
    };
  },
  methods: {
    openDialog: function (row, callback) {
      console.log(row)
      this.showDialog = true;
      this.isEdit = false;
      if (row) {
        this.isEdit = true;
      }
      this.form = {};
      this.listChangeCallback = callback;
      if (row != null) {
        this.form = row;
      }
    },
    onSubmit: function () {
      console.log("onSubmit");
      this.$axios({
        method: 'post',
        url:`/api/jt1078/device/${this.isEdit?'update':'add'}/`,
        params: this.form
      }).then((res) => {
        console.log(res.data)
        if (res.data.code === 0) {
          this.listChangeCallback()
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
