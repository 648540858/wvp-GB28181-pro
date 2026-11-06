<template>
  <el-dialog
    title="新建配置文件"
    :visible.sync="visible"
    width="520px"
    append-to-body
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-width="110px"
      @submit.native.prevent
    >
      <el-form-item label="配置文件名" prop="profileId">
        <el-input
          v-model="form.profileId"
          clearable
          placeholder="如 dev、prod、test"
          style="width: 220px"
        />
        <span class="file-tip">{{ fileNamePreview }}</span>
      </el-form-item>

      <el-form-item label="初始内容" prop="mode">
        <el-radio-group v-model="form.mode">
          <el-radio label="blank">空白配置</el-radio>
          <el-radio label="copy" :disabled="!activeProfileId">复制当前配置</el-radio>
        </el-radio-group>
        <div v-if="!activeProfileId" class="file-tip">当前没有可复制的生效配置</div>
      </el-form-item>
    </el-form>

    <div class="dialog-tip">
      <i class="el-icon-info" /> 文件将创建在配置目录，保存后不会自动切换，需要重启服务后生效。
    </div>

    <span slot="footer">
      <el-button size="small" @click="visible = false">取消</el-button>
      <el-button type="primary" size="small" :loading="saving" @click="handleSubmit">创建</el-button>
    </span>
  </el-dialog>
</template>

<script>
const PROFILE_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/

export default {
  name: 'NewProfileDialog',
  props: {
    // 当前生效的配置文件ID, 用于“复制当前配置”
    activeProfileId: {
      type: String,
      default: ''
    }
  },
  data() {
    const validateId = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入配置文件名'))
      } else if (!PROFILE_ID_PATTERN.test(value)) {
        callback(new Error('只能包含字母、数字、下划线和中划线，长度 1-64'))
      } else {
        callback()
      }
    }
    return {
      visible: false,
      saving: false,
      form: {
        profileId: '',
        mode: 'blank'
      },
      rules: {
        profileId: [{ validator: validateId, trigger: 'blur' }],
        mode: [{ required: true, message: '请选择初始内容', trigger: 'change' }]
      }
    }
  },
  computed: {
    fileNamePreview() {
      const id = (this.form.profileId || '').trim()
      return id ? `application-${id}.yml` : 'application-{id}.yml'
    }
  },
  methods: {
    open() {
      this.visible = true
      this.saving = false
      this.form = {
        profileId: '',
        mode: this.activeProfileId ? 'copy' : 'blank'
      }
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    handleClosed() {
      this.saving = false
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.saving = true
        const profileId = this.form.profileId.trim()
        const create = content => {
          this.$store.dispatch('setting/createProfile', { profileId, content })
            .then(() => {
              this.$message.success('配置文件已创建')
              this.visible = false
              this.$emit('created')
            })
            .catch(error => {
              this.$message.error(typeof error === 'string' ? error : '创建配置文件失败')
            })
            .finally(() => {
              this.saving = false
            })
        }
        if (this.form.mode === 'copy' && this.activeProfileId) {
          this.$store.dispatch('setting/getProfileDetail', this.activeProfileId)
            .then(data => {
              create((data && data.content) || '')
            })
            .catch(error => {
              this.saving = false
              this.$message.error(typeof error === 'string' ? error : '读取当前配置失败')
            })
        } else {
          create('')
        }
      })
    }
  }
}
</script>

<style scoped>
.file-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.dialog-tip {
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}
</style>
