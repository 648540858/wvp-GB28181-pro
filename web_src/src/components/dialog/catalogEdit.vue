<template>
  <div id="catalogEdit" v-loading="isLoging">
    <el-dialog
      title="节点编辑"
      width="40%"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="form" :rules="rules" :model="form" label-width="140px" >
          <el-form-item label="节点编号" prop="id" >
            <el-input v-model="form.id" :disabled="isEdit" clearable></el-input>
          </el-form-item>
          <el-form-item label="节点名称" prop="name">
            <el-input v-model="form.name" clearable></el-input>
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
  name: "catalogEdit",
  computed: {},
  props: ['platformId', 'platformDeviceId'],
  created() {},
  data() {
    let checkId = (rule, value, callback) => {
      console.log("checkId")
      console.log(rule)
      console.log(value)
      console.log(value.length)
      console.log(this.level)
      if (!value) {
        return callback(new Error('编号不能为空'));
      }
      if (value.trim().length <= 8) {
        if (value.trim().length%2 !== 0) {
          return callback(new Error('行政区划编号必须为2/4/6/8位'));
        }
        if (this.form.parentId !== this.platformDeviceId && this.form.parentId.length >= value.trim().length) {
          if (this.form.parentId.length === 20) {
            return callback(new Error('业务分组/虚拟组织下不可创建行政区划'));
          }else {
            return callback(new Error('行政区划编号长度应该每次两位递增'));
          }
        }
      }else {
        if (value.trim().length !== 20) {
          return callback(new Error('编号必须为2/4/6/8位的行政区划或20位的虚拟组织/业务分组'));
        }
        let catalogType = value.substring(10, 13);
        console.log(catalogType)
        if (catalogType !== "215" && catalogType !== "216") {
          return callback(new Error('编号错误，业务分组11-13位为215，虚拟组织11-13位为216'));
        }
        if (catalogType === "216") {

          if (this.form.parentId !== this.platformDeviceId){
            if (this.form.parentId.length <= 8) {
              return callback(new Error('编号错误，建立虚拟组织前必须先建立业务分组（11-13位为215）'));
            }
          }
        }
        if (catalogType === "215") {
          if (this.form.parentId.length === "215") {
            return callback(new Error('编号错误，业务分组下只能建立虚拟组织（11-13位为216）'));
          }
        }
      }
      callback();
    }
    return {
      submitCallback: null,
      showDialog: false,
      isLoging: false,
      isEdit: false,
      level: 0,
      form: {
        id: null,
        name: null,
        platformId: null,
        parentId: null,
      },
      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        id: [{ required: true, trigger: "blur",validator: checkId  }]
      },
    };
  },
  methods: {
    openDialog: function (isEdit, id, name, parentId, level, callback) {
      console.log("parentId: " + parentId)
      console.log(this.form)
      this.isEdit = isEdit;
      this.form.id = id;
      this.form.name = name;
      this.form.platformId = this.platformId;
      this.form.parentId = parentId;
      this.showDialog = true;
      this.submitCallback = callback;
      this.level = level;
    },
    onSubmit: function () {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          this.$axios({
            method:"post",
            url:`/api/platform/catalog/${!this.isEdit? "add":"edit"}`,
            data: this.form
          }).then((res)=> {
            if (res.data.code === 0) {
              if (this.submitCallback)this.submitCallback(this.form)
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
        } else {
          return false;
        }
      });
    },
    close: function () {
      this.isEdit = false;
      this.form.id = null;
      this.form.name = null;
      this.form.platformId = null;
      this.form.parentId = null;
      this.callback = null;
      this.showDialog = false;
      console.log(this.form)
    },
  },
};
</script>
