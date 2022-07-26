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
    let checkId = (rule, value, callback) => {
      console.log("checkId")
      console.log(this.treeType)
      console.log(rule)
      console.log(value)
      console.log(value.length)
      console.log(this.level)
      if (!value) {
        return callback(new Error('编号不能为空'));
      }
      if (this.treeType === "BusinessGroup" && value.length !== 20) {
        return callback(new Error('编号必须由20位数字组成'));
      }
      if (this.treeType === "CivilCode" && value.length <= 8 && value.length%2 !== 0) {
        return callback(new Error('行政区划必须是八位以下的偶数个数字组成'));
      }
      if (this.treeType === "BusinessGroup") {
        let catalogType = value.substring(10, 13);
        console.log(catalogType)
        // 216 为虚拟组织 215 为业务分组；目录第一级必须为业务分组， 业务分组下为虚拟组织，虚拟组织下可以有其他虚拟组织
        if (this.level === 1 && catalogType !== "215") {
          return callback(new Error('业务分组模式下第一层目录的编号11到13位必须为215'));
        }
        if (this.level > 1 && catalogType !== "216") {
          return callback(new Error('业务分组模式下第一层以下目录的编号11到13位必须为216'));
        }
      }
      callback();
    }
    return {
      submitCallback: null,
      showDialog: false,
      isLoging: false,
      isEdit: false,
      treeType: null,
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
    openDialog: function (isEdit, id, name, parentId, treeType, level, callback) {
      console.log("parentId: " + parentId)
      console.log(this.form)
      this.isEdit = isEdit;
      this.form.id = id;
      this.form.name = name;
      this.form.platformId = this.platformId;
      this.form.parentId = parentId;
      this.showDialog = true;
      this.submitCallback = callback;
      this.treeType = treeType;
      this.level = level;
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
