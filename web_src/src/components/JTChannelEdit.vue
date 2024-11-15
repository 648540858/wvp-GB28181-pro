<template>
  <div id="channelList" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-button icon="el-icon-back" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        <el-divider direction="vertical"></el-divider>
        编辑推流信息
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-button icon="el-icon-close" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        </div>
      </div>
    </div>
    <el-tabs tab-position="left">
      <el-tab-pane label="推流信息编辑" style="background-color: #FFFFFF; padding: 1rem">
        <el-form ref="form" :rules="rules" :model="jtChannel" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr ">
          <el-form-item label="编号" prop="channelId">
            <el-input v-model="jtChannel.channelId" clearable></el-input>
          </el-form-item>
          <el-form-item label="名称" prop="name">
            <el-input v-model="jtChannel.name" clearable></el-input>
          </el-form-item>
        </el-form>
        <el-form style="text-align: right">
          <el-form-item >
            <el-button type="primary" @click="onSubmit">保存</el-button>
            <el-button @click="close">取消</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="国标通道配置" v-if="jtChannel.id">
        <CommonChannelEdit ref="commonChannelEdit" :dataForm="jtChannel" :cancel="close"></CommonChannelEdit>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from './common/CommonChannelEdit'

export default {
  name: 'channelList',
  props: [ 'jtChannel', 'closeEdit'],
  components: {
    CommonChannelEdit
  },
  data() {
    return {
      version: 3,
      rules: {
        deviceId: [{ required: true, message: "请输入设备编号", trigger: "blur" }]
      },
      winHeight: window.innerHeight - 200,
      isLoading: false,
      loadSnap: {},
    };
  },

  mounted() {},
  methods: {
    onSubmit: function () {
      console.log(this.jtChannel)
      let isEdit = typeof (this.jtChannel.id) !== "undefined"
      this.$axios({
        method: 'post',
        url:`/api/jt1078/terminal/channel/${isEdit?'update':'add'}/`,
        params: this.jtChannel
      }).then((res) => {
        console.log(res.data)
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: "保存成功",
            type: "success",
          });
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
      this.closeEdit()
    },
  }
};
</script>
