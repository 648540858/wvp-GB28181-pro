<template>
  <div id="channelEdit" style="width: 100%; height: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="编辑推流信息" @back="close" />
      </div>
    </div>
    <el-tabs tab-position="left" style="padding: 1rem; height: calc(100% - 24px)">
      <el-tab-pane label="部标通道编辑" style="background-color: #FFFFFF;">
        <el-form ref="form" :rules="rules" :model="form" label-width="60px" style="width: 40rem; margin: 0 auto">
          <el-form-item label="编号" prop="channelId">
            <el-input v-model="form.channelId" clearable />
          </el-form-item>
          <el-form-item label="名称" prop="name">
            <el-input v-model="form.name" clearable />
          </el-form-item>
          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">保存</el-button>
            <el-button @click="close">取消</el-button>
          </el-form-item>
        </el-form>

      </el-tab-pane>
      <el-tab-pane label="国标通道配置">
        <CommonChannelEdit :id="form.gbId" ref="commonChannelEdit" :data-form="form" @cancel="close" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from '../../common/CommonChannelEdit'

export default {
  name: 'ChannelEdit',
  components: {
    CommonChannelEdit
  },
  props: ['jtChannel', 'closeEdit'],
  data() {
    return {
      version: 3,
      form: { ...(this.jtChannel || {}) },
      rules: {
        deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }]
      },
      isLoading: false,
      loadSnap: {}
    }
  },

  watch: {
    jtChannel(value) {
      this.form = { ...(value || {}) }
    }
  },

  mounted() {},
  methods: {
    onSubmit: function() {
      console.log(this.form)
      const isEdit = typeof (this.form.id) !== 'undefined'
      if (isEdit) {
        this.$store.dispatch('jtDevice/updateChannel', this.form)
          .then(data => {
            this.$message({
              showClose: true,
              message: '保存成功',
              type: 'success'
            })
            this.form = { ...data }
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      } else {
        this.$store.dispatch('jtDevice/addChannel', this.form)
          .then(data => {
            this.$message({
              showClose: true,
              message: '保存成功',
              type: 'success'
            })
            this.form = { ...data }
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }
    },
    close: function() {
      this.closeEdit()
    }
  }
}
</script>
