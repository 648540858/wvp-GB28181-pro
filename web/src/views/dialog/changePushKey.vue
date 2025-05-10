<template>
  <div id="changepushKey" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="修改pushKey"
      width="42%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 18px;">
        <el-form ref="pushKeyForm" :rules="rules" status-icon label-width="86px">
          <el-form-item label="新pushKey" prop="newPushKey">
            <el-input v-model="newPushKey" autocomplete="off" />
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
  name: 'ChangePushKey',
  directives: { elDragDialog },
  props: {},
  data() {
    const validatePass1 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入新pushKey'))
      } else {
        callback()
      }
    }
    return {
      newPushKey: null,
      confirmpushKey: null,
      userId: null,
      showDialog: false,
      isLoging: false,
      listChangeCallback: null,
      form: {},
      rules: {
        newpushKey: [{ required: true, validator: validatePass1, trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(row, callback) {
      console.log(row)
      this.showDialog = true
      this.listChangeCallback = callback
      if (row != null) {
        this.form = row
      }
    },
    onSubmit: function() {
      this.$store.dispatch('user/changePushKey', {
        pushKey: this.newPushKey,
        userId: this.form.id
      })
        .then(data => {
          this.$message({
            showClose: true,
            message: '修改成功',
            type: 'success'
          })
          this.listChangeCallback()
          this.showDialog = false
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    close: function() {
      this.showDialog = false
      this.newpushKey = null
      this.userId = null
      this.adminId = null
    }
  }
}
</script>
