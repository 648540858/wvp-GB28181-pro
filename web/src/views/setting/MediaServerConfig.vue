<template>
  <div v-loading="loading">
    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span class="card-title">流媒体服务配置</span>
        <span v-if="!writable" class="card-tip">
          <i class="el-icon-warning-outline" /> 当前配置文件不可写，仅可查看
        </span>
      </div>

      <div class="meta-row">
        <span class="meta-item"><i class="el-icon-collection-tag" /> {{ profileLabel }}</span>
        <span class="meta-item"><i class="el-icon-folder-opened" /> {{ filePath || '—' }}</span>
        <el-tag v-if="pendingRestart && !dirty" size="mini" type="warning" effect="plain">有改动待重启生效</el-tag>
      </div>

      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="170px"
        class="config-form"
        @submit.native.prevent
      >
        <el-divider content-position="left">基础配置</el-divider>

        <el-form-item label="流媒体服务器ID" prop="id">
          <el-input v-model="form.id" :disabled="!writable" clearable style="width: 320px" />
          <span class="field-tip">运行中: {{ runtime.id || '—' }}</span>
        </el-form-item>

        <el-form-item label="内网IP" prop="ip">
          <el-input v-model="form.ip" :disabled="!writable" clearable style="width: 320px" />
          <span class="field-tip">运行中: {{ runtime.ip || '—' }}</span>
        </el-form-item>

        <el-form-item label="公网IP" prop="wanIp">
          <el-input v-model="form.wanIp" :disabled="!writable" clearable style="width: 320px" />
        </el-form-item>

        <el-form-item label="返回流地址IP" prop="streamIp">
          <el-input v-model="form.streamIp" :disabled="!writable" clearable placeholder="置空使用内网IP" style="width: 320px" />
        </el-form-item>

        <el-form-item label="信令使用IP" prop="sdpIp">
          <el-input v-model="form.sdpIp" :disabled="!writable" clearable placeholder="置空使用内网IP" style="width: 320px" />
        </el-form-item>

        <el-form-item label="hook访问IP" prop="hookIp">
          <el-input v-model="form.hookIp" :disabled="!writable" clearable placeholder="默认 127.0.0.1" style="width: 320px" />
        </el-form-item>

        <el-form-item label="HTTP端口" prop="httpPort">
          <el-input v-model.number="form.httpPort" type="number" :disabled="!writable" clearable style="width: 200px" />
          <span class="field-tip">运行中: {{ runtime.httpPort || '—' }}</span>
        </el-form-item>

        <el-form-item label="鉴权密钥" prop="secret">
          <el-input v-model="form.secret" :disabled="!writable" clearable style="width: 320px" />
        </el-form-item>

        <el-form-item label="自动配置ZLM" prop="autoConfig">
          <el-switch v-model="form.autoConfig" :disabled="!writable" />
        </el-form-item>

        <el-form-item label="服务类型" prop="type">
          <el-input v-model="form.type" :disabled="!writable" clearable placeholder="zlm" style="width: 200px" />
        </el-form-item>

        <el-divider content-position="left">录像配置</el-divider>

        <el-form-item label="录像保存路径" prop="recordPath">
          <el-input v-model="form.recordPath" :disabled="!writable" clearable style="width: 320px" />
        </el-form-item>

        <el-form-item label="录像保存天数" prop="recordDay">
          <el-input v-model.number="form.recordDay" type="number" :disabled="!writable" clearable style="width: 160px">
            <template slot="append">天</template>
          </el-input>
        </el-form-item>

        <el-form-item label="录像辅助服务端口" prop="recordAssistPort">
          <el-input v-model.number="form.recordAssistPort" type="number" :disabled="!writable" clearable placeholder="0 表示不使用" style="width: 200px" />
        </el-form-item>

        <el-divider content-position="left">RTP配置</el-divider>

        <el-form-item label="多端口模式" prop="rtpEnable">
          <el-switch v-model="form.rtpEnable" :disabled="!writable" />
        </el-form-item>

        <el-form-item label="收流端口范围" prop="rtpPortRange">
          <el-input v-model="form.rtpPortRange" :disabled="!writable" clearable placeholder="如 30000,30500" style="width: 260px" />
        </el-form-item>

        <el-form-item label="发流端口范围" prop="rtpSendPortRange">
          <el-input v-model="form.rtpSendPortRange" :disabled="!writable" clearable placeholder="如 50502,50506" style="width: 260px" />
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
    id: '',
    ip: '',
    wanIp: '',
    streamIp: '',
    sdpIp: '',
    hookIp: '',
    httpPort: null,
    secret: '',
    autoConfig: false,
    rtpEnable: false,
    rtpPortRange: '',
    rtpSendPortRange: '',
    recordAssistPort: null,
    recordDay: null,
    recordPath: '',
    type: ''
  }
}

export default {
  name: 'MediaServerConfig',
  mixins: [configPanelMixin],
  data() {
    const validPort = (allowZero) => (rule, value, callback) => {
      if (value === null || value === undefined || value === '') {
        callback()
      } else {
        const min = allowZero ? 0 : 1
        if (Number(value) < min || Number(value) > 65535) {
          callback(new Error(`端口需在 ${min}-65535 之间`))
        } else {
          callback()
        }
      }
    }
    const requiredId = (rule, value, callback) => {
      callback(value ? undefined : new Error('请输入流媒体服务器ID'))
    }
    const requiredIp = (rule, value, callback) => {
      callback(value ? undefined : new Error('请输入流媒体服务器IP'))
    }
    const requiredSecret = (rule, value, callback) => {
      callback(value ? undefined : new Error('请输入鉴权密钥'))
    }
    const validRecordDay = (rule, value, callback) => {
      if (value !== null && value !== '' && Number(value) < 0) {
        callback(new Error('录像保存天数不能小于0'))
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
        id: '',
        ip: '',
        httpPort: null
      },
      rules: {
        id: [{ validator: requiredId, trigger: 'blur' }],
        ip: [{ validator: requiredIp, trigger: 'blur' }],
        secret: [{ validator: requiredSecret, trigger: 'blur' }],
        httpPort: [{ validator: validPort(true), trigger: 'blur' }],
        recordAssistPort: [{ validator: validPort(true), trigger: 'blur' }],
        recordDay: [{ validator: validRecordDay, trigger: 'blur' }]
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
      this.$store.dispatch('setting/getMediaServerConfig')
        .then(data => {
          if (!data) {
            return
          }
          this.writable = !!data.writable
          this.profileId = data.profileId || ''
          this.filePath = data.filePath || ''
          this.runtime = {
            id: data.runtimeId || '',
            ip: data.runtimeIp || '',
            httpPort: typeof data.runtimeHttpPort === 'number' ? data.runtimeHttpPort : null
          }
          const form = {
            id: data.id || '',
            ip: data.ip || '',
            wanIp: data.wanIp || '',
            streamIp: data.streamIp || '',
            sdpIp: data.sdpIp || '',
            hookIp: data.hookIp || '',
            httpPort: typeof data.httpPort === 'number' ? data.httpPort : null,
            secret: data.secret || '',
            autoConfig: !!data.autoConfig,
            rtpEnable: !!data.rtpEnable,
            rtpPortRange: data.rtpPortRange || '',
            rtpSendPortRange: data.rtpSendPortRange || '',
            recordAssistPort: typeof data.recordAssistPort === 'number' ? data.recordAssistPort : null,
            recordDay: typeof data.recordDay === 'number' ? data.recordDay : null,
            recordPath: data.recordPath || '',
            type: data.type || ''
          }
          this.form = { ...form }
          this.saved = { ...form }
          this.pendingRestart = false
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '读取流媒体服务配置失败')
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
          '确认保存流媒体服务配置吗？保存后需要重启服务才能生效。',
          '保存流媒体服务配置',
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
      const num = (value) => (value === '' || value === undefined ? null : value)
      this.saving = true
      this.$store.dispatch('setting/saveMediaServerConfig', {
        id: this.form.id,
        ip: this.form.ip,
        wanIp: this.form.wanIp,
        streamIp: this.form.streamIp,
        sdpIp: this.form.sdpIp,
        hookIp: this.form.hookIp,
        httpPort: num(this.form.httpPort),
        secret: this.form.secret,
        autoConfig: this.form.autoConfig,
        rtpEnable: this.form.rtpEnable,
        rtpPortRange: this.form.rtpPortRange,
        rtpSendPortRange: this.form.rtpSendPortRange,
        recordAssistPort: num(this.form.recordAssistPort),
        recordDay: num(this.form.recordDay),
        recordPath: this.form.recordPath,
        type: this.form.type
      }).then(() => {
        this.$message.success('流媒体服务配置已保存')
        this.saved = { ...this.form }
        this.pendingRestart = true
        if (needRestart) {
          this.confirmRestart(() => this.reload())
        } else {
          this.$message.info('配置将在下次服务重启后生效')
        }
      }).catch(error => {
        this.$message.error(typeof error === 'string' ? error : '保存流媒体服务配置失败')
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
