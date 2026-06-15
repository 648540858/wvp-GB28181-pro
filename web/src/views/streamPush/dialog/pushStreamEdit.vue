<template>
  <div id="addStreamProxy" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title=" 加入"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="streamProxy" :rules="rules" :model="proxyParam" label-width="140px">
          <el-form-item label="名称" prop="name">
            <el-input v-model="proxyParam.name" clearable />
          </el-form-item>
          <el-form-item label="流应用名" prop="app">
            <el-input v-model="proxyParam.app" clearable :disabled="edit" />
          </el-form-item>
          <el-form-item label="流ID" prop="stream">
            <el-input v-model="proxyParam.stream" clearable :disabled="edit" />
          </el-form-item>
          <el-form-item label="国标编码" prop="gbId">
            <el-input v-model="proxyParam.gbId" placeholder="设置国标编码可推送到国标" clearable />
          </el-form-item>
          <el-form-item v-if="proxyParam.gbId" label="经度" prop="longitude">
            <el-input v-model="proxyParam.longitude" placeholder="经度" clearable />
          </el-form-item>
          <el-form-item v-if="proxyParam.gbId" label="纬度" prop="latitude">
            <el-input v-model="proxyParam.latitude" placeholder="经度" clearable />
          </el-form-item>
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

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'PushStreamEdit',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      edit: false,
      proxyParam: {
        name: null,
        app: null,
        stream: null,
        gbId: null,
        longitude: null,
        latitude: null
      },
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        app: [{ required: true, message: '请输入应用名', trigger: 'blur' }],
        stream: [{ required: true, message: '请输入流ID', trigger: 'blur' }],
        gbId: [{ required: true, message: '请输入国标编码', trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(proxyParam, callback) {
      this.showDialog = true
      this.listChangeCallback = callback
      if (proxyParam != null) {
        this.proxyParam = proxyParam
        this.edit = true
      } else {
        this.proxyParam = {
          name: null,
          app: null,
          stream: null,
          gbId: null,
          longitude: null,
          latitude: null
        }
        this.edit = false
      }
    },
    onSubmit: function() {
      console.log('onSubmit')
      if (this.edit) {
        this.$store.dispatch('streamPush/saveToGb', this.proxyParam)
          .then((data) => {
            this.$message({
              showClose: true,
              message: '保存成功',
              type: 'success'
            })
            this.showDialog = false
            if (this.listChangeCallback != null) {
              this.listChangeCallback()
            }
          })
      } else {
        this.$store.dispatch('streamPush/add', this.proxyParam)
          .then((data) => {
            this.$message({
              showClose: true,
              message: '保存成功',
              type: 'success'
            })
            this.showDialog = false
            if (this.listChangeCallback != null) {
              this.listChangeCallback()
            }
          })
      }
    },
    close: function() {
      console.log('关闭加入GB')
      this.showDialog = false
      this.$refs.streamProxy.resetFields()
    }
  }
}
</script>
