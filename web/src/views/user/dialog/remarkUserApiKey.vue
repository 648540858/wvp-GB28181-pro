<template>
  <div id="remarkUserApiKey" v-loading="isLoading">
    <el-dialog
      v-el-drag-dialog
      title="ApiKey备注"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="form" :rules="rules" status-icon label-width="80px">
          <el-form-item label="备注" prop="oldPassword">
            <el-input v-model="form.remark" type="textarea" autocomplete="off" :autosize="{ minRows: 5}" maxlength="255" show-word-limit />
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
  name: 'RemarkUserApiKey',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      userApiKeyId: null,
      form: {
        remark: null
      },
      rules: {},
      listChangeCallback: null,
      showDialog: false,
      isLoading: false
    }
  },
  computed: {},
  created() {
  },
  methods: {
    resetForm() {
      this.form = {
        remark: null
      }
    },
    openDialog(userApiKeyId, callback) {
      this.resetForm()
      this.userApiKeyId = userApiKeyId
      this.listChangeCallback = callback
      this.showDialog = true
    },
    onSubmit() {
      this.$store.dispatch('userApiKeys/remark', {
        id: this.userApiKeyId,
        remark: this.form.remark
      })
        .then(data => {
          this.$message({
            showClose: true,
            message: '备注修改成功!',
            type: 'success'
          })
          this.listChangeCallback()
        })
        .catch(err => {
          this.$message({
            showClose: true,
            message: err,
            type: 'error'
          })
        })
        .finally(() => {
          this.showDialog = false
        })
    },
    close() {
      this.showDialog = false
    }
  }
}
</script>
