<template>
  <div id="commonChannelEditDialog">
    <el-dialog
      v-el-drag-dialog
      title="通道编辑"
      width="90%"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <CommonChannelEdit style="overflow: auto !important;" ref="commonChannelEdit" :showCancel="true" :id="channelId" @cancel="close" @submitSuccess="close"></CommonChannelEdit>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import CommonChannelEdit from '../common/CommonChannelEdit.vue'

export default {
  name: 'CommonChannelEditDialog',
  components: { CommonChannelEdit },
  directives: { elDragDialog },
  props: ['platformId', 'platformDeviceId'],
  data() {
    return {
      submitCallback: null,
      showDialog: false,
      channelId: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(id) {
      console.log(id)
      this.channelId = id
      this.showDialog = true
      this.$refs.commonChannelEdit.getCommonChannel(this.channelId)
    },
    onSubmit: function() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          this.$axios({
            method: 'post',
            url: `/api/platform/catalog/${!this.isEdit ? 'add' : 'edit'}`,
            data: this.form
          }).then((res) => {
            if (res.data.code === 0) {
              if (this.submitCallback) this.submitCallback(this.form)
            } else {
              this.$message({
                showClose: true,
                message: res.data.msg,
                type: 'error'
              })
            }
            this.close()
          })
            .catch((error) => {
              console.log(error)
            })
        } else {
          return false
        }
      })
    },
    close: function() {
      this.channelId = null
      this.showDialog = false
    }
  }
}
</script>
