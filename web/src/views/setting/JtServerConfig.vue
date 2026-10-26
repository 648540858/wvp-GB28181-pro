<template>
  <div v-loading="loading" class="jt-server-panel">
    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span class="card-title">部标服务配置</span>
        <span v-if="!writable" class="card-tip">
          <i class="el-icon-warning-outline" /> 当前配置文件不可写，仅可查看
        </span>
        <div class="header-actions">
          <restart-tag-toggle v-model="restartTagVisible" :disabled="restartFieldKeys.length === 0" />
        </div>
      </div>

      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="170px"
        class="config-form"
        @submit.native.prevent
      >
        <el-divider content-position="left">FTP 服务</el-divider>

        <el-form-item label="开启FTP服务" prop="ftpEnable">
          <template slot="label">
            <restart-field-label
              text="开启FTP服务"
              :required="isRestartField('ftpEnable')"
              :show="showRestartTags"
            />
          </template>
          <el-switch v-model="form.ftpEnable" :disabled="!writable" />
          <span class="field-tip">运行中: {{ runtime.ftpEnable ? '已开启' : '未开启' }}</span>
        </el-form-item>

        <el-form-item label="FTP服务端口" prop="ftpPort">
          <template slot="label">
            <restart-field-label
              text="FTP服务端口"
              :required="isRestartField('ftpPort')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model.number="form.ftpPort"
            type="number"
            :disabled="!writable || !form.ftpEnable"
            clearable
            placeholder="1-65535"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="被动模式端口范围" prop="ftpPassivePorts">
          <template slot="label">
            <restart-field-label
              text="被动模式端口范围"
              :required="isRestartField('ftpPassivePorts')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model="form.ftpPassivePorts"
            :disabled="!writable || !form.ftpEnable"
            clearable
            placeholder="如 10000-10500"
            style="width: 260px"
          />
        </el-form-item>

        <el-divider content-position="left">JT1078 服务</el-divider>

        <el-form-item label="开启JT1078服务" prop="jt1078Enable">
          <template slot="label">
            <restart-field-label
              text="开启JT1078服务"
              :required="isRestartField('jt1078Enable')"
              :show="showRestartTags"
            />
          </template>
          <el-switch v-model="form.jt1078Enable" :disabled="!writable" />
          <span class="field-tip">运行中: {{ runtime.jt1078Enable ? '已开启' : '未开启' }}</span>
        </el-form-item>

        <el-form-item label="设备接入端口" prop="jt1078Port">
          <template slot="label">
            <restart-field-label
              text="设备接入端口"
              :required="isRestartField('jt1078Port')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model.number="form.jt1078Port"
            type="number"
            :disabled="!writable || !form.jt1078Enable"
            clearable
            placeholder="1-65535"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="设备鉴权密码" prop="jt1078Password">
          <template slot="label">
            <restart-field-label
              text="设备鉴权密码"
              :required="isRestartField('jt1078Password')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model="form.jt1078Password"
            :disabled="!writable || !form.jt1078Enable"
            clearable
            style="width: 260px"
          />
        </el-form-item>

        <el-form-item label="IDLE超时时间" prop="jt1078ReaderIdleTime">
          <template slot="label">
            <restart-field-label
              text="IDLE超时时间"
              :required="isRestartField('jt1078ReaderIdleTime')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model.number="form.jt1078ReaderIdleTime"
            type="number"
            :disabled="!writable || !form.jt1078Enable"
            clearable
            placeholder="0 表示不启用"
            style="width: 200px"
          >
            <template slot="append">秒</template>
          </el-input>
        </el-form-item>

        <el-form-item label="是否录像" prop="jt1078Record">
          <template slot="label">
            <restart-field-label
              text="是否录像"
              :required="isRestartField('jt1078Record')"
              :show="showRestartTags"
            />
          </template>
          <el-switch v-model="form.jt1078Record" :disabled="!writable || !form.jt1078Enable" />
        </el-form-item>
      </el-form>

      <div class="save-bar">
        <span class="save-hint">
          <template v-if="dirty">配置已修改，保存后需重启服务生效</template>
          <template v-else-if="pendingRestart">配置已保存，重启服务后生效</template>
          <template v-else>当前配置未变更</template>
        </span>
        <div>
          <el-button size="mini" icon="el-icon-refresh" :disabled="loading" @click="reload">重新加载</el-button>
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-check"
            :loading="saving"
            :disabled="!writable || !dirty"
            @click="handleSave"
          >
            保存
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import configPanelMixin from './configPanelMixin'
import RestartFieldLabel from './components/RestartFieldLabel.vue'
import RestartTagToggle from './components/RestartTagToggle.vue'

function emptyForm() {
  return {
    ftpEnable: false,
    ftpPort: null,
    ftpPassivePorts: '',
    jt1078Enable: false,
    jt1078Port: null,
    jt1078Password: '',
    jt1078Record: false,
    jt1078ReaderIdleTime: null
  }
}

export default {
  name: 'JtServerConfig',
  components: { RestartFieldLabel, RestartTagToggle },
  mixins: [configPanelMixin],
  data() {
    const validPort = (label) => (rule, value, callback) => {
      if (value === null || value === undefined || value === '') {
        callback()
      } else if (Number(value) < 1 || Number(value) > 65535) {
        callback(new Error(`${label}需在 1-65535 之间`))
      } else {
        callback()
      }
    }
    const validPassivePorts = (rule, value, callback) => {
      if (value && !/^\d+(-\d+)?(,\d+(-\d+)?)*$/.test(value.trim())) {
        callback(new Error('格式不正确, 应为 10000-10500'))
      } else {
        callback()
      }
    }
    const validIdleTime = (rule, value, callback) => {
      if (value !== null && value !== '' && Number(value) < 0) {
        callback(new Error('超时时间不能小于0'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      saving: false,
      pendingRestart: false,
      writable: false,
      profileId: '',
      filePath: '',
      form: emptyForm(),
      saved: emptyForm(),
      runtime: {
        ftpEnable: false,
        jt1078Enable: false
      },
      // 后端标识的“需重启才能生效”字段
      restartFieldKeys: [],
      restartTagVisible: false,
      rules: {
        ftpPort: [{ validator: validPort('FTP服务端口'), trigger: 'blur' }],
        jt1078Port: [{ validator: validPort('JT1078服务端口'), trigger: 'blur' }],
        ftpPassivePorts: [{ validator: validPassivePorts, trigger: 'blur' }],
        jt1078ReaderIdleTime: [{ validator: validIdleTime, trigger: 'blur' }]
      }
    }
  },
  computed: {
    profileLabel() {
      return this.profileId ? `application-${this.profileId}.yml` : '主配置文件'
    },
    dirty() {
      return JSON.stringify(this.form) !== JSON.stringify(this.saved)
    },
    // 后端未返回字段列表或不显示重启标识时不展示重启标签
    showRestartTags() {
      return this.restartFieldKeys.length > 0 && this.restartTagVisible
    }
  },
  created() {
    this.reload()
  },
  methods: {
    // 是否属于后端标识的“需重启才能生效”字段
    isRestartField(key) {
      return this.restartFieldKeys.indexOf(key) >= 0
    },
    reload() {
      this.loading = true
      this.$store.dispatch('setting/getJtServerConfig')
        .then(data => {
          if (!data) {
            return
          }
          this.writable = !!data.writable
          this.profileId = data.profileId || ''
          this.filePath = data.filePath || ''
          this.runtime = {
            ftpEnable: !!data.runtimeFtpEnable,
            jt1078Enable: !!data.runtimeJt1078Enable
          }
          this.restartFieldKeys = Array.isArray(data.restartRequiredFields) ? data.restartRequiredFields : []
          const form = {
            ftpEnable: !!data.ftpEnable,
            ftpPort: typeof data.ftpPort === 'number' ? data.ftpPort : null,
            ftpPassivePorts: data.ftpPassivePorts || '',
            jt1078Enable: !!data.jt1078Enable,
            jt1078Port: typeof data.jt1078Port === 'number' ? data.jt1078Port : null,
            jt1078Password: data.jt1078Password || '',
            jt1078Record: !!data.jt1078Record,
            jt1078ReaderIdleTime: typeof data.jt1078ReaderIdleTime === 'number' ? data.jt1078ReaderIdleTime : null
          }
          this.form = { ...form }
          this.saved = { ...form }
          this.pendingRestart = false
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '读取部标服务配置失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSave() {
      if (!this.dirty) {
        return
      }
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.$confirm(
          '确认保存部标服务配置吗？保存后需要重启服务才能生效。',
          '保存部标服务配置',
          {
            confirmButtonText: '保存并重启',
            cancelButtonText: '仅保存',
            distinguishCancelAndClose: true,
            type: 'warning'
          }
        ).then(() => {
          this.doSave(true)
        }).catch(action => {
          if (action === 'cancel') {
            this.doSave(false)
          }
        })
      })
    },
    doSave(needRestart) {
      this.saving = true
      this.$store.dispatch('setting/saveJtServerConfig', {
        ftpEnable: this.form.ftpEnable,
        ftpPort: this.form.ftpPort === '' ? null : this.form.ftpPort,
        ftpPassivePorts: this.form.ftpPassivePorts,
        jt1078Enable: this.form.jt1078Enable,
        jt1078Port: this.form.jt1078Port === '' ? null : this.form.jt1078Port,
        jt1078Password: this.form.jt1078Password,
        jt1078Record: this.form.jt1078Record,
        jt1078ReaderIdleTime: this.form.jt1078ReaderIdleTime === '' ? null : this.form.jt1078ReaderIdleTime
      }).then(() => {
        this.$message.success('部标服务配置已保存')
        this.saved = { ...this.form }
        this.pendingRestart = true
        if (needRestart) {
          this.confirmRestart(() => this.reload())
        } else {
          this.$message.info('配置将在下次服务重启后生效')
        }
      }).catch(error => {
        this.$message.error(typeof error === 'string' ? error : '保存部标服务配置失败')
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.section-card {
  border: 1px solid #e8eaef;
  border-radius: 8px;
}

.section-card >>> .el-card__header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.card-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.card-tip {
  font-size: 12px;
  color: #e6a23c;
}

.header-actions {
  margin-left: auto;
}

.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 8px 12px;
  margin-bottom: 16px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  word-break: break-all;
}

.config-form {
  max-width: 820px;
}

.field-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.save-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #ebeef5;
}

.save-hint {
  font-size: 12px;
  color: #909399;
}
</style>
