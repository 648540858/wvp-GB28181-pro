<template>
  <div id="editRecordPlan" v-loading="loading" style="text-align: left;">
    <el-dialog
      title="录制计划"
      width="700px"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <ByteWeektimePicker v-model="byteTime" name="name"/>
        <el-form status-icon label-width="80px">
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
import { ByteWeektimePicker } from 'byte-weektime-picker'


export default {
  name: "editRecordPlan",
  props: {},
  components: {ByteWeektimePicker},
  created() {
  },
  data() {
    return {
      value:"",
      options: [],
      loading: false,
      showDialog: false,
      channel: "",
      deviceDbId: "",
      endCallback: "",
      byteTime: "",
    };
  },
  methods: {
    openDialog: function (channel, deviceDbId, endCallback) {
      this.channel = channel;
      this.deviceDbId = deviceDbId;
      this.endCallback = endCallback;
      this.showDialog = true;
    },
    onSubmit: function () {
      this.$axios({
        method: 'post',
        url: "/api/user/add",
        params: {
          username: this.username,
          password: this.password,
          roleId: this.roleId
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: '添加成功',
            type: 'success',

          });
          this.showDialog = false;
          this.listChangeCallback()

        } else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error) => {
        console.error(error)
      });
    },
    close: function () {
      console.log(this.byteTime)
      this.channel = "";
      this.deviceDbId = "";
      this.showDialog = false;
      if(this.endCallback) {
        this.endCallback();
      }
    },
  },
};
</script>
