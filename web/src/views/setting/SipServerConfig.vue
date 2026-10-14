<template>
  <div v-loading="loading" class="sip-server-panel">
    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span class="card-title">国标服务配置</span>
        <span v-if="!writable" class="card-tip">
          <i class="el-icon-warning-outline" /> 当前配置文件不可写，仅可查看
        </span>
      </div>

      <div class="meta-row">
        <span class="meta-item"><i class="el-icon-collection-tag" /> {{ profileLabel }}</span>
        <span class="meta-item"><i class="el-icon-folder-opened" /> {{ filePath || '—' }}</span>
        <el-tag v-if="pendingRestart && !dirty" size="mini" type="warning" effect="plain">有改动待重启生效</el-tag>
      </div>

      <el-alert
        v-if="runtimeMonitorIps.length"
        type="info"
        :closable="false"
        show-icon
        :title="`运行中实际监听网卡: ${runtimeMonitorIps.join(', ')}`"
        class="tip-alert"
      />

      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="150px"
        class="config-form"
        @submit.native.prevent
      >
        <el-form-item label="国标编号" prop="id">
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
          <el-input
            v-model="form.domain"
            :disabled="!writable"
            clearable
            maxlength="10"
            placeholder="10位数字, 如 4101050000"
            style="width: 260px"
          />
          <span class="field-tip">一般取国标编号前10位</span>
        </el-form-item>

        <el-form-item label="SIP服务端口" prop="port">
          <el-input
            v-model.number="form.port"
            type="number"
            :disabled="!writable"
            clearable
            placeholder="1-65535"
            style="width: 200px"
          />
          <span class="field-tip">运行中: {{ runtime.port || '—' }}</span>
        </el-form-item>

        <el-form-item label="认证密码" prop="password">
          <el-input
            v-model="form.password"
            :disabled="!writable"
            clearable
            placeholder="公共认证密码"
            style="width: 260px"
          />
        </el-form-item>

        <el-form-item label="监听网卡IP" prop="ip">
          <el-input
            v-model="form.ip"
            :disabled="!writable"
            clearable
            placeholder="如 0.0.0.0, 不配置则自动获取"
            style="width: 320px"
          />
          <span class="field-tip">运行中监听: {{ runtime.ip || '—' }}</span>
        </el-form-item>

        <el-form-item label="对外展示IP" prop="showIp">
          <el-input
            v-model="form.showIp"
            :disabled="!writable"
            clearable
            placeholder="如 192.168.1.3, 不配置则使用监听IP"
            style="width: 320px"
          />
          <span class="field-tip">运行中展示: {{ runtime.showIp || '—' }}</span>
        </el-form-item>

        <el-divider content-position="left">高级配置</el-divider>

        <el-form-item label="注册重试间隔" prop="registerTimeInterval">
          <el-input
            v-model.number="form.registerTimeInterval"
            type="number"
            :disabled="!writable"
            clearable
            placeholder="秒"
            style="width: 160px"
          >
            <template slot="append">秒</template>
          </el-input>
        </el-form-item>

        <el-form-item label="云台控制速度" prop="ptzSpeed">
          <el-input
            v-model.number="form.ptzSpeed"
            type="number"
            :disabled="!writable"
            clearable
            placeholder="0-255"
            style="width: 160px"
          />
        </el-form-item>

        <el-form-item label="命令超时时间" prop="timeout">
          <el-input
            v-model.number="form.timeout"
            type="number"
            :disabled="!writable"
            clearable
            placeholder="毫秒"
            style="width: 160px"
          >
            <template slot="append">毫秒</template>
          </el-input>
        </el-form-item>

        <el-form-item label="存储alarm信息" prop="alarm">
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

function emptyForm() {
  return {
    ip: '',
    showIp: '',
    port: null,
    domain: '',
    id: '',
    password: '',
    registerTimeInterval: null,
    ptzSpeed: null,
    alarm: false,
    timeout: null
  }
}

export default {
  name: 'SipServerConfig',
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
    const validPtzSpeed = (rule, value, callback) => {
      if (value !== null && value !== '' && (Number(value) < 0 || Number(value) > 255)) {
        callback(new Error('云台控制速度需在 0-255 之间'))
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
      rules: {
        id: [{ validator: validId, trigger: 'blur' }],
        domain: [{ validator: validDomain, trigger: 'blur' }],
        port: [{ validator: requiredNumber, trigger: 'blur' }],
        ptzSpeed: [{ validator: validPtzSpeed, trigger: 'blur' }],
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
    }
  },
  created() {
    this.reload()
  },
  methods: {
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
          const form = {
            ip: data.ip || '',
            showIp: data.showIp || '',
            port: typeof data.port === 'number' ? data.port : null,
            domain: data.domain || '',
            id: data.id || '',
            password: data.password || '',
            registerTimeInterval: typeof data.registerTimeInterval === 'number' ? data.registerTimeInterval : null,
            ptzSpeed: typeof data.ptzSpeed === 'number' ? data.ptzSpeed : null,
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
        registerTimeInterval: this.form.registerTimeInterval === '' ? null : this.form.registerTimeInterval,
        ptzSpeed: this.form.ptzSpeed === '' ? null : this.form.ptzSpeed,
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
