<template>
  <div id="groupEdit" v-loading="loading">
    <el-dialog
      title="分组编辑"
      width="40%"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="form" :rules="rules" :model="group" label-width="140px" >
          <el-form-item label="节点编号" prop="id" >
            <el-input v-model="group.deviceId" clearable></el-input>
          </el-form-item>
          <el-form-item label="节点名称" prop="name">
            <el-input v-model="group.name" clearable></el-input>
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
  name: "groupEdit",
  computed: {},
  props: [],
  created() {},
  data() {
    return {
      submitCallback: null,
      showDialog: false,
      loading: false,
      level: 0,
      group: {},
    };
  },
  methods: {
    openDialog: function (group, callback) {
      console.log(group)
      this.group = group;
      this.showDialog = true;
      this.submitCallback = callback;
    },
    onSubmit: function () {

      this.$axios({
        method:"post",
        url: this.group.id ? '/api/group/add':'/api/group/update',
        data: this.group
      }).then((res)=> {
        if (res.data.code === 0) {
          this.$message.success("保存成功")
          if (this.submitCallback)this.submitCallback(this.group)
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
          this.$message({
            showClose: true,
            message: error,
            type: "error",
          });
        });
    },
    close: function () {
      this.showDialog = false;
      console.log(this.group)
    },
  },
};
</script>
