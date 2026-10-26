<template>
  <div v-loading="loading" class="sip-server-panel">
    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span class="card-title">国标服务配置</span>
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
        label-width="150px"
        class="config-form"
        @submit.native.prevent
      >
        <el-form-item label="国标编号" prop="id">
          <template slot="label">
            <restart-field-label
              text="国标编号"
              :required="isRestartField('id')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model="form.id"
            :disabled="!writable"
            clearable
            maxlength="20"
            placeholder="20位数字, 如 41010500002000000001"
            style="width: 320px"
          />
          <span class="field-tip">SIP 服务的国标编号, 必须为20位数字</span>
        </el-form-item>

        <el-form-item label="国标域" prop="domain">
          <template slot="label">
            <restart-field-label
              text="国标域"
              :required="isRestartField('domain')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model="form.domain"
            :disabled="!writable"
            clearable
            maxlength="10"
            placeholder="10位数字, 如 4101050000"
            style="width: 320px"
          />
          <span class="field-tip">一般取国标编号前10位</span>
        </el-form-item>

        <el-form-item label="端口" prop="port">
          <template slot="label">
            <restart-field-label
              text="端口"
              :required="isRestartField('port')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model.number="form.port"
            type="number"
            :disabled="!writable"
            clearable
            placeholder="1-65535"
            style="width: 320px"
          />
          <span class="field-tip">当前监听: {{ runtime.port || '—' }}</span>
        </el-form-item>

        <el-form-item label="认证密码" prop="password">
          <template slot="label">
            <restart-field-label
              text="认证密码"
              :required="isRestartField('password')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model="form.password"
            :disabled="!writable"
            clearable
            placeholder="公共认证密码"
            style="width: 320px"
          />
        </el-form-item>

        <el-form-item label="监听网卡IP" prop="ip">
          <template slot="label">
            <restart-field-label
              text="监听网卡IP"
              :required="isRestartField('ip')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model="form.ip"
            :disabled="!writable"
            clearable
            placeholder="如 0.0.0.0, 不配置则自动获取"
            style="width: 320px"
          />
          <span class="field-tip">当前监听: {{ runtimeMonitorIps.join(', ') || '—' }}</span>
        </el-form-item>

        <el-form-item label="对外展示IP" prop="showIp">
          <template slot="label">
            <restart-field-label
              text="对外展示IP"
              :required="isRestartField('showIp')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model="form.showIp"
            :disabled="!writable"
            clearable
          placeholder="对外展示IP, 可以是域名, 置空则使用监听IP"
            style="width: 320px"
          />
          <span class="field-tip">当前展示: {{ runtime.showIp || runtimeMonitorIps.join(', ') || '—' }}</span>
        </el-form-item>

        <el-divider content-position="left">高级配置</el-divider>

        <el-form-item label="命令超时时间" prop="timeout">
          <template slot="label">
            <restart-field-label
              text="命令超时时间"
              :required="isRestartField('timeout')"
              :show="showRestartTags"
            />
          </template>
          <el-input
            v-model.number="form.timeout"
            type="number"
            :disabled="!writable"
            clearable
            placeholder="毫秒"
            style="width: 320px"
          >
            <template slot="append">毫秒</template>
          </el-input>
        </el-form-item>

        <el-form-item label="存储alarm信息" prop="alarm">
          <template slot="label">
            <restart-field-label
              text="存储alarm信息"
              :required="isRestartField('alarm')"
              :show="showRestartTags"
            />
          </template>
          <el-switch v-model="form.alarm" :disabled="!writable" />
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
    ip: '',
    showIp: '',
    port: null,
    domain: '',
    id: '',
    password: '',
    alarm: false,
    timeout: null
  }
}

export default {
  name: 'SipServerConfig',
  components: { RestartFieldLabel, RestartTagToggle },
  mixins: [configPanelMixin],
  data() {
    const requiredNumber = (rule, value, callback) => {
      if (value === null || value === undefined || value === '') {
        callback(new Error('请输入SIP服务端口'))
      } else if (Number(value) < 1 || Number(value) > 65535) {
        callback(new Error('端口需在 1-65535 之间'))
      } else {
        callback()
      }
    }
    const validId = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入国标编号'))
      } else if (!/^\d{20}$/.test(value.trim())) {
        callback(new Error('国标编号必须为20位数字'))
      } else {
        callback()
      }
    }
    const validDomain = (rule, value, callback) => {
      if (value && !/^\d{10}$/.test(value.trim())) {
        callback(new Error('国标域必须为10位数字'))
      } else {
        callback()
      }
    }
    const validTimeout = (rule, value, callback) => {
      if (value !== null && value !== '' && Number(value) <= 0) {
        callback(new Error('超时时间必须大于0'))
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
        port: null,
        ip: '',
        showIp: ''
      },
      runtimeMonitorIps: [],
      // 后端标识的“需重启才能生效”字段
      restartFieldKeys: [],
      restartTagVisible: false,
      rules: {
        id: [{ validator: validId, trigger: 'blur' }],
        domain: [{ validator: validDomain, trigger: 'blur' }],
        port: [{ validator: requiredNumber, trigger: 'blur' }],
        timeout: [{ validator: validTimeout, trigger: 'blur' }]
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
      this.$store.dispatch('setting/getSipServerConfig')
        .then(data => {
          if (!data) {
            return
          }
          this.writable = !!data.writable
          this.profileId = data.profileId || ''
          this.filePath = data.filePath || ''
          this.runtime = {
            port: typeof data.runtimePort === 'number' ? data.runtimePort : null,
            ip: data.runtimeIp || '',
            showIp: data.runtimeShowIp || ''
          }
          this.runtimeMonitorIps = Array.isArray(data.runtimeMonitorIps) ? data.runtimeMonitorIps : []
          this.restartFieldKeys = Array.isArray(data.restartRequiredFields) ? data.restartRequiredFields : []
          const form = {
            ip: data.ip || '',
            showIp: data.showIp || '',
            port: typeof data.port === 'number' ? data.port : null,
            domain: data.domain || '',
            id: data.id || '',
            password: data.password || '',
            alarm: !!data.alarm,
            timeout: typeof data.timeout === 'number' ? data.timeout : null
          }
          this.form = { ...form }
          this.saved = { ...form }
          this.pendingRestart = false
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '读取国标服务配置失败')
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
          '确认保存国标服务配置吗？保存后需要重启服务才能生效。',
          '保存国标服务配置',
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
      this.$store.dispatch('setting/saveSipServerConfig', {
        ip: this.form.ip,
        showIp: this.form.showIp,
        port: this.form.port === '' ? null : this.form.port,
        domain: this.form.domain,
        id: this.form.id,
        password: this.form.password,
        alarm: this.form.alarm,
        timeout: this.form.timeout === '' ? null : this.form.timeout
      }).then(() => {
        this.$message.success('国标服务配置已保存')
        this.saved = { ...this.form }
        this.pendingRestart = true
        if (needRestart) {
          this.confirmRestart(() => this.reload())
        } else {
          this.$message.info('配置将在下次服务重启后生效')
        }
      }).catch(error => {
        this.$message.error(typeof error === 'string' ? error : '保存国标服务配置失败')
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
  max-width: 420px;
}

.field-tip {
  margin-left: 2px;
  font-size: 12px;
  color: #909399;
}

.tip-alert {
  margin-bottom: 16px;
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
