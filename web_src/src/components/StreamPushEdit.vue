<template>
  <div id="ChannelEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header @back="close" content="编辑推流信息"></el-page-header>
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-button icon="el-icon-close" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        </div>
      </div>
    </div>
    <el-tabs tab-position="left">
      <el-tab-pane label="推流信息编辑" style="background-color: #FFFFFF; padding: 1rem">
        <el-divider content-position="center">基础信息</el-divider>
        <el-form ref="streamPushForm" status-icon label-width="160px" class="channel-form" v-loading="locading">
          <el-form-item label="应用名" >
            <el-input v-model="streamPush.app" placeholder="请输入应用名"></el-input>
          </el-form-item>
          <el-form-item label="流ID" >
            <el-input v-model="streamPush.stream" placeholder="请输入流ID"></el-input>
          </el-form-item>
        </el-form>
        <el-divider content-position="center">策略</el-divider>
        <el-form ref="streamPushForm" status-icon label-width="160px" v-loading="locading">
          <el-form-item style="text-align: left">
            <el-checkbox v-model="streamPush.startOfflinePush">拉起离线推流</el-checkbox>
          </el-form-item>

        </el-form>
        <el-form style="text-align: right">
          <el-form-item >
            <el-button type="primary" @click="onSubmit">保存</el-button>
            <el-button @click="close">取消</el-button>
          </el-form-item>
        </el-form>

      </el-tab-pane>
      <el-tab-pane label="国标通道配置" v-if="streamPush.id">
        <CommonChannelEdit ref="commonChannelEdit" :dataForm="streamPush" :cancel="close"></CommonChannelEdit>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from './common/CommonChannelEdit'

export default {
  name: "channelEdit",
  props: [ 'streamPush', 'closeEdit'],
  components: {
    CommonChannelEdit,
  },
  created() {
    console.log(this.streamPush)
  },
  data() {
    return {
      locading: false,
    };
  },
  methods: {
    onSubmit: function () {
      console.log(this.streamPush)
      this.locading = true
      if (this.streamPush.id) {
        this.$axios({
          method: 'post',
          url: "/api/push/update",
          data: this.streamPush
        }).then((res) => {
          if (res.data.code === 0) {
            this.$message.success({
              showClose: true,
              message: '保存成功',
            });
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
        }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        }).finally(()=>{
          this.locading = false
        })
      }else {
        this.$axios({
          method: 'post',
          url: "/api/push/add",
          data: this.streamPush
        }).then((res) => {
          if (res.data.code === 0) {
            this.$message.success({
              showClose: true,
              message: '保存成功',
            });

            this.streamPush = res.data.data
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
        }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        }).finally(()=>{
          this.locading = false
        })
      }

    },
    close: function () {
      this.closeEdit()
    },
  },
};
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
