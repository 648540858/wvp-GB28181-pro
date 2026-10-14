<template>
  <div v-loading="loading" class="web-server-panel">
    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span class="card-title">WEB服务配置</span>
        <span v-if="!writable" class="card-tip">
          <i class="el-icon-warning-outline" /> 当前配置文件不可写，仅可查看
        </span>
      </div>

      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        label-width="150px"
        class="config-form"
        @submit.native.prevent
      >
        <el-form-item label="服务端口" prop="port">
          <el-input
            v-model.number="form.port"
            type="number"
            :disabled="!writable"
            clearable
            placeholder="1-65535"
            style="width: 200px"
          />
          <span class="field-tip">网页与接口访问端口，运行中: {{ runtime.port }}</span>
        </el-form-item>

        <el-form-item label="开启HTTPS" prop="sslEnabled">
          <el-switch v-model="form.sslEnabled" :disabled="!writable" />
          <span class="field-tip">开启后需使用 https:// 访问，运行中: {{ runtime.sslEnabled ? '已开启' : '未开启' }}</span>
        </el-form-item>

        <template v-if="form.sslEnabled">
          <el-form-item label="PEM证书文件" prop="sslCertificate">
            <el-input
              v-model="form.sslCertificate"
              :disabled="!writable"
              clearable
              placeholder="如 /home/user/ssl.pem"
            />
          </el-form-item>
          <el-form-item label="私钥文件" prop="sslCertificatePrivateKey">
            <el-input
              v-model="form.sslCertificatePrivateKey"
              :disabled="!writable"
              clearable
              placeholder="如 /home/user/ssl.pem.key"
            />
          </el-form-item>
        </template>
      </el-form>

      <el-alert
        v-if="form.sslEnabled || urlAffected"
        type="warning"
        :closable="false"
        show-icon
        title="开启 HTTPS 或修改端口后，页面访问地址将发生变化，重启后请使用新地址访问。"
        class="tip-alert"
      />

      <div class="save-bar">
        <span class="save-hint">
          <template v-if="needRestart">配置已保存，重启服务后生效</template>
          <template v-else-if="dirty">配置已修改，保存后需重启服务生效</template>
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
export default {
  name: 'WebServerConfig',
  data() {
    const validatePort = (rule, value, callback) => {
      if (value === null || value === undefined || value === '') {
        callback(new Error('请输入服务端口'))
      } else if (isNaN(Number(value))) {
        callback(new Error('端口需为数字'))
      } else if (Number(value) < 1 || Number(value) > 65535) {
        callback(new Error('端口需在 1-65535 之间'))
      } else {
        callback()
      }
    }
    const requiredWhenSsl = (rule, value, callback) => {
      if (this.form.sslEnabled && !value) {
        callback(new Error('开启HTTPS时必填'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      saving: false,
      writable: false,
      profileId: '',
      filePath: '',
      form: {
        port: null,
        sslEnabled: false,
        sslCertificate: '',
        sslCertificatePrivateKey: ''
      },
      saved: {
        port: null,
        sslEnabled: false,
        sslCertificate: '',
        sslCertificatePrivateKey: ''
      },
      runtime: {
        port: null,
        sslEnabled: false
      },
      rules: {
        port: [{ validator: validatePort, trigger: 'blur' }],
        sslCertificate: [{ validator: requiredWhenSsl, trigger: 'blur' }],
        sslCertificatePrivateKey: [{ validator: requiredWhenSsl, trigger: 'blur' }]
      }
    }
  },
  computed: {
    profileLabel() {
      return this.profileId ? `application-${this.profileId}.yml` : '主配置文件'
    },
    dirty() {
      return this.form.port !== this.saved.port ||
        this.form.sslEnabled !== this.saved.sslEnabled ||
        (this.form.sslCertificate || '') !== (this.saved.sslCertificate || '') ||
        (this.form.sslCertificatePrivateKey || '') !== (this.saved.sslCertificatePrivateKey || '')
    },
    needRestart() {
      return this.saved.port !== this.runtime.port || this.saved.sslEnabled !== this.runtime.sslEnabled
    },
    // 当前表单值相对运行中生效值是否会改变访问地址(协议或端口)
    urlAffected() {
      return this.form.port !== this.runtime.port || this.form.sslEnabled !== this.runtime.sslEnabled
    }
  },
  created() {
    this.reload()
  },
  methods: {
    reload() {
      this.loading = true
      this.$store.dispatch('setting/getWebServerConfig')
        .then(data => {
          if (!data) {
            return
          }
          this.writable = !!data.writable
          this.profileId = data.profileId || ''
          this.filePath = data.filePath || ''
          this.runtime = {
            port: typeof data.runtimePort === 'number' ? data.runtimePort : null,
            sslEnabled: !!data.runtimeSslEnabled
          }
          const form = {
            port: typeof data.port === 'number' ? data.port : null,
            sslEnabled: !!data.sslEnabled,
            sslCertificate: data.sslCertificate || '',
            sslCertificatePrivateKey: data.sslCertificatePrivateKey || ''
          }
          this.form = { ...form }
          this.saved = { ...form }
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '读取WEB服务配置失败')
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
          this.urlAffected
            ? '修改端口或HTTPS后，页面访问地址将发生变化，重启后需使用新地址访问。确认保存吗？'
            : '确认保存WEB服务配置吗？保存后需要重启服务才能生效。',
          '保存WEB服务配置',
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
      const urlAffected = this.urlAffected
      this.saving = true
      this.$store.dispatch('setting/saveWebServerConfig', {
        port: this.form.port === '' || this.form.port === null || this.form.port === undefined
          ? null
          : Number(this.form.port),
        sslEnabled: this.form.sslEnabled,
        sslCertificate: this.form.sslCertificate,
        sslCertificatePrivateKey: this.form.sslCertificatePrivateKey
      }).then(() => {
        this.$message.success('WEB服务配置已保存')
        this.saved = { ...this.form }
        if (needRestart) {
          this.confirmRestart(urlAffected)
        } else {
          this.$message.info('配置将在下次服务重启后生效')
        }
      }).catch(error => {
        this.$message.error(typeof error === 'string' ? error : '保存WEB服务配置失败')
      }).finally(() => {
        this.saving = false
      })
    },
    confirmRestart(urlAffected) {
      this.$confirm(
        urlAffected
          ? `服务即将重启，重启后访问地址为 ${this.buildNewUrl()}，请使用新地址访问。`
          : '服务即将重启，重启期间页面将无法访问，请稍后刷新页面。',
        '重启服务',
        {
          confirmButtonText: '立即重启',
          cancelButtonText: '稍后手动重启',
          type: 'warning'
        }
      ).then(() => {
        this.doRestart(urlAffected)
      }).catch(() => {
        this.$message.info('可稍后手动重启服务使配置生效')
      })
    },
    doRestart(urlAffected) {
      this.$store.dispatch('setting/restartServer').then(() => {
        this.$message.success('服务正在重启...')
        if (urlAffected) {
          // 端口或协议变化后无法再同源轮询, 直接提示新地址
          const url = this.buildNewUrl()
          this.$alert(
            `服务重启后，请使用以下新地址访问：<br/><a href="${url}" target="_blank" rel="noopener" class="restart-link">${url}</a>`,
            '访问地址已变更',
            { dangerouslyUseHTMLString: true, confirmButtonText: '知道了', type: 'warning' }
          )
        } else {
          this.waitForRestart()
        }
      }).catch(error => {
        this.$message.error(typeof error === 'string' ? error : '触发重启失败')
      })
    },
    // 访问地址未变化时, 同源轮询等待服务恢复
    waitForRestart() {
      let retry = 0
      const maxRetry = 60
      const timer = setInterval(() => {
        retry += 1
        this.$store.dispatch('setting/getWebServerConfig')
          .then(() => {
            clearInterval(timer)
            this.$message.success('服务已恢复')
            this.reload()
          })
          .catch(() => {
            if (retry >= maxRetry) {
              clearInterval(timer)
              this.$message.warning('等待服务恢复超时，请手动刷新页面')
            }
          })
      }, 3000)
    },
    buildNewUrl() {
      const scheme = this.form.sslEnabled ? 'https' : 'http'
      const host = window.location.hostname
      const path = window.location.pathname || '/'
      return `${scheme}://${host}:${this.form.port}${path}`
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
  max-width: 760px;
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
