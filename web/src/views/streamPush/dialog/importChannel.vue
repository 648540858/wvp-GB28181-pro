<template>
  <div id="importChannel" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="导入通道数据"
      width="30rem"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        <el-upload
          class="upload-box"
          drag
          :action="uploadUrl"
          name="file"
          :headers="headers"
          :on-success="successHook"
          :on-error="errorHook"
        >
          <i class="el-icon-upload" />
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <div slot="tip" class="el-upload__tip">只能上传 csv / xls / xlsx 文件</div>
        </el-upload>
      </div>
    </el-dialog>
    <ShowErrorData ref="showErrorData" :gb-ids="errorGBIds" :streams="errorStreams" />
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import ShowErrorData from './importChannelShowErrorData.vue'

export default {
  name: 'ImportChannel',
  directives: { elDragDialog },
  components: {
    ShowErrorData
  },
  data() {
    return {
      submitCallback: null,
      showDialog: false,
      isLoging: false,
      isEdit: false,
      errorStreams: [],
      errorGBIds: [],
      headers: {
        'access-token': this.$store.getters.token
      },
      uploadUrl: process.env.NODE_ENV === 'development' ? `${process.env.VUE_APP_BASE_API}/api/push/upload` : (window.baseUrl ? window.baseUrl : '') + `/api/push/upload`
    }
  },
  created() {},
  methods: {
    openDialog: function(callback) {
      this.showDialog = true
      this.submitCallback = callback
    },
    close: function() {
      this.showDialog = false
    },
    successHook: function(response, file, fileList) {
      if (response.code === 0) {
        this.$message({
          showClose: true,
          message: response.msg,
          type: 'success'
        })
      } else if (response.code === 1) {
        this.errorGBIds = response.data.gbId
        this.errorStreams = response.data.stream
        console.log(this.$refs)
        console.log(this.$refs.showErrorData)
        this.$refs.showErrorData.openDialog()
      } else {
        this.$message({
          showClose: true,
          message: response.msg,
          type: 'error'
        })
      }
    },
    errorHook: function(err, file, fileList) {
      this.$message({
        showClose: true,
        message: err,
        type: 'error'
      })
    }
  }
}
</script>
<style>
.upload-box{
  text-align: center;
}
.errDataBox{
  max-height: 15rem;
  overflow: auto;
}
</style>
