<template>
  <div id="ChannelEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="编辑推流信息" @back="close" />
      </div>
    </div>
    <el-tabs tab-position="top" style="padding-top: 1rem;">
      <el-tab-pane label="推流信息编辑" style="background-color: #FFFFFF; padding: 1rem; height: calc(-218px + 100vh);
    overflow: auto;">
        <el-divider content-position="center">基础信息</el-divider>
        <el-form ref="streamPushForm" v-loading="locading" status-icon label-width="160px" class="channel-form">
          <el-form-item label="应用名">
            <el-input v-model="form.app" placeholder="请输入应用名" />
          </el-form-item>
          <el-form-item label="流ID">
            <el-input v-model="form.stream" placeholder="请输入流ID" />
          </el-form-item>
        </el-form>
        <el-divider content-position="center">策略</el-divider>
        <el-form ref="streamPushForm" v-loading="locading" status-icon label-width="160px">
          <el-form-item style="text-align: left">
            <el-checkbox v-model="form.startOfflinePush">拉起离线推流</el-checkbox>
          </el-form-item>

        </el-form>
        <el-form style="text-align: right">
          <el-form-item>
            <el-button type="primary" @click="onSubmit">保存</el-button>
            <el-button @click="close">取消</el-button>
          </el-form-item>
        </el-form>

      </el-tab-pane>
      <el-tab-pane v-if="form.id" label="国标通道配置">
        <CommonChannelEdit ref="commonChannelEdit" :showCancel="true" :data-form="form" @cancel="close" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from '../common/CommonChannelEdit'

export default {
  name: 'ChannelEdit',
  components: {
    CommonChannelEdit
  },
  props: ['streamPush', 'closeEdit'],
  data() {
    return {
      locading: false,
      form: { ...(this.streamPush || {}) }
    }
  },
  watch: {
    streamPush(value) {
      this.form = { ...(value || {}) }
    }
  },
  created() {
    console.log(this.form)
  },
  methods: {
    onSubmit: function() {
      console.log(this.form)
      this.locading = true
      if (this.form.id) {
        this.$store.dispatch('streamPush/update', this.form)
          .then(data => {
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
          })
          .finally(() => {
            this.locading = false
          })
      } else {
        this.$store.dispatch('streamPush/add', this.form)
          .then(data => {
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
          })
          .finally(() => {
            this.locading = false
          })
      }
    },
    close: function() {
      this.closeEdit()
    }
  }
}
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
