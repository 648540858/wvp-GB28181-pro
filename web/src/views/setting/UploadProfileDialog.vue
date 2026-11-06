<template>
  <el-dialog
    title="上传配置文件"
    :visible.sync="visible"
    width="540px"
    append-to-body
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-upload
      drag
      action="#"
      :auto-upload="false"
      :show-file-list="false"
      :limit="1"
      accept=".yml,.yaml"
      :on-change="handleChange"
      :on-exceed="handleExceed"
    >
      <i class="el-icon-upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
      <div slot="tip" class="el-upload__tip">
        仅支持 application-{id}.yml / application-{id}.yaml，文件不超过 2MB
      </div>
    </el-upload>

    <div v-if="fileName" class="file-info">
      <i class="el-icon-document" />
      <span class="file-name">{{ fileName }}</span>
      <span class="file-size">{{ sizeText }}</span>
    </div>

    <div class="dialog-tip">
      <i class="el-icon-info" /> 同名配置文件不会被覆盖，上传后不会自动切换，需要重启服务后生效。
    </div>

    <span slot="footer">
      <el-button size="small" @click="visible = false">取消</el-button>
      <el-button type="primary" size="small" :loading="uploading" :disabled="!file" @click="handleSubmit">上传</el-button>
    </span>
  </el-dialog>
</template>

<script>
const PROFILE_FILE_PATTERN = /^application-[A-Za-z0-9_-]{1,64}\.(yml|yaml)$/i
const MAX_SIZE = 2 * 1024 * 1024

export default {
  name: 'UploadProfileDialog',
  data() {
    return {
      visible: false,
      uploading: false,
      file: null,
      fileName: '',
      fileSize: 0
    }
  },
  computed: {
    sizeText() {
      if (!this.fileSize) {
        return ''
      }
      if (this.fileSize < 1024) {
        return `${this.fileSize} B`
      }
      return `${(this.fileSize / 1024).toFixed(1)} KB`
    }
  },
  methods: {
    open() {
      this.visible = true
      this.uploading = false
      this.resetFile()
    },
    resetFile() {
      this.file = null
      this.fileName = ''
      this.fileSize = 0
    },
    handleChange(file) {
      const raw = file.raw || file
      const name = raw.name || ''
      if (!PROFILE_FILE_PATTERN.test(name)) {
        this.$message.error('文件名必须为 application-{id}.yml 或 application-{id}.yaml')
        this.resetFile()
        return
      }
      if (raw.size > MAX_SIZE) {
        this.$message.error('配置文件不能超过 2MB')
        this.resetFile()
        return
      }
      this.file = raw
      this.fileName = name
      this.fileSize = raw.size
    },
    handleExceed() {
      this.$message.warning('一次只能上传一个配置文件')
    },
    handleSubmit() {
      if (!this.file) {
        return
      }
      const formData = new FormData()
      formData.append('file', this.file)
      this.uploading = true
      this.$store.dispatch('setting/uploadProfile', formData)
        .then(() => {
          this.$message.success('配置文件已上传')
          this.visible = false
          this.$emit('uploaded')
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '上传配置文件失败')
        })
        .finally(() => {
          this.uploading = false
        })
    },
    handleClosed() {
      this.uploading = false
      this.resetFile()
    }
  }
}
</script>

<style scoped>
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}

.file-name {
  word-break: break-all;
}

.file-size {
  color: #909399;
}

.dialog-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fdf6ec;
  border-radius: 4px;
  font-size: 12px;
  color: #b88230;
}
</style>
