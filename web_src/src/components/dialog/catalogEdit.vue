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
<!--          <el-form-item >-->
<!--            建议的类型：-->
<!--            <br/>-->
<!--            &emsp;&emsp;行政区划（可选2位/4位/6位/8位/10位数字，例如：130432，表示河北省邯郸市广平县）-->
<!--            <br/>-->
<!--            &emsp;&emsp;业务分组（第11、12、13位215，例如：34020000002150000001）-->
<!--            <br/>-->
<!--            &emsp;&emsp;虚拟组织（第11、12、13位216，例如：34020000002160000001）-->
<!--          </el-form-item>-->
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
  props: ['platformId'],
  created() {},
  data() {
    return {
      submitCallback: null,
      showDialog: false,
      isLoging: false,
      isEdit: false,
      form: {
        id: null,
        name: null,
        platformId: null,
        parentId: null,
      },
      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        id: [{ required: true, message: "请输入ID", trigger: "blur" }]
      },
    };
  },
  methods: {
    openDialog: function (isEdit, id, name, parentId, callback) {
      console.log("parentId: " + parentId)
      console.log(this.form)
      this.isEdit = isEdit;
      this.form.id = id;
      this.form.name = name;
      this.form.platformId = this.platformId;
      this.form.parentId = parentId;
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
