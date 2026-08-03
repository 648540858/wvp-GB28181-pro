<template>
  <div class="app-container security-config" v-loading="loading">
    <div class="page-toolbar">
      <div>
        <h2 class="page-title"><ant-icon name="el-icon-lock" />安全配置</h2>
        <span class="page-subtitle">当前节点运行时配置</span>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" icon="el-icon-refresh" :loading="loading" :disabled="saving" @click="loadConfig">刷新</el-button>
        <el-button
          size="small"
          type="primary"
          icon="el-icon-check"
          :loading="saving"
          :disabled="loading || !loaded || !hasChanges"
          @click="saveConfig"
        >保存配置</el-button>
      </div>
    </div>

    <el-alert
      v-if="loaded"
      :title="config.fastjsonRiskStatus || 'JSON 组件安全状态未知'"
      :type="config.fastjson1Present ? 'error' : 'success'"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <template v-if="loaded">
      <section class="config-section">
        <div class="section-heading">
          <h3 class="section-title">动态安全策略</h3>
          <el-tag size="small" type="success">实时生效</el-tag>
        </div>

        <el-form :model="form" label-position="top" class="runtime-form">
          <div class="setting-list">
            <div class="setting-row">
              <div class="setting-label">
                <span>接口鉴权</span>
                <el-tag size="small" :type="form.interfaceAuthentication ? 'success' : 'danger'">
                  {{ form.interfaceAuthentication ? '已启用' : '未启用' }}
                </el-tag>
              </div>
              <el-switch v-model="form.interfaceAuthentication" aria-label="接口鉴权" />
            </div>

            <div class="setting-row">
              <div class="setting-label">
                <span>登录超时时间</span>
              </div>
              <div class="number-control">
                <el-input-number
                  v-model="form.loginTimeoutMinutes"
                  :min="0"
                  :max="999"
                  :precision="0"
                  controls-position="right"
                  aria-label="登录超时时间"
                />
                <span>{{ form.loginTimeoutMinutes === 0 ? '分钟（不超时）' : '分钟' }}</span>
              </div>
            </div>

            <div class="setting-row">
              <div class="setting-label">
                <span>接口文档</span>
                <el-tag size="small" :type="form.docEnabled ? 'warning' : 'success'">
                  {{ form.docEnabled ? '已启用' : '未启用' }}
                </el-tag>
              </div>
              <el-switch v-model="form.docEnabled" aria-label="接口文档" />
            </div>

            <div class="setting-row">
              <div class="setting-label">
                <span>MIME 嗅探防护</span>
                <el-tag size="small" :type="form.contentTypeOptionsEnabled ? 'success' : 'warning'">
                  {{ form.contentTypeOptionsEnabled ? '已启用' : '未启用' }}
                </el-tag>
              </div>
              <el-switch v-model="form.contentTypeOptionsEnabled" aria-label="MIME 嗅探防护" />
            </div>

            <div class="setting-row setting-row--textarea">
              <div class="setting-label">
                <span>跨域来源</span>
                <el-tag size="small" :type="allowedOrigins.length === 0 ? 'warning' : 'success'">
                  {{ allowedOrigins.length === 0 ? '全部来源' : `${allowedOrigins.length} 项` }}
                </el-tag>
              </div>
              <el-input
                v-model="form.allowedOriginsText"
                type="textarea"
                :rows="4"
                placeholder="https://console.example.com"
                aria-label="跨域来源"
              />
            </div>

            <div class="setting-row setting-row--textarea">
              <div class="setting-label">
                <span>鉴权例外接口</span>
                <el-tag size="small" type="info">{{ authenticationExcludes.length }} 项</el-tag>
              </div>
              <el-input
                v-model="form.interfaceAuthenticationExcludesText"
                type="textarea"
                :rows="4"
                placeholder="/api/public/**"
                aria-label="鉴权例外接口"
              />
            </div>
          </div>
        </el-form>
      </section>

      <section class="config-section">
        <div class="section-heading">
          <h3 class="section-title">启动期配置</h3>
          <el-tag size="small" type="info">只读</el-tag>
        </div>
        <el-descriptions
          border
          size="medium"
          :column="descriptionColumns"
          :label-style="descriptionLabelStyle"
        >
          <el-descriptions-item label="访问令牌请求头"><code>{{ config.accessTokenHeader || '-' }}</code></el-descriptions-item>
          <el-descriptions-item label="API Key 请求头"><code>{{ config.apiKeyHeader || '-' }}</code></el-descriptions-item>
          <el-descriptions-item label="密码算法">{{ config.passwordEncoder || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Session 策略">{{ config.sessionCreationPolicy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="CSRF 防护">
            <el-tag size="small" :type="config.csrfEnabled ? 'success' : 'warning'">
              {{ config.csrfEnabled ? '已启用' : '未启用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="HTTPS">
            <el-tag size="small" :type="config.httpsEnabled ? 'success' : 'warning'">
              {{ config.httpsEnabled ? '已启用' : '未启用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务端口">{{ config.serverPort || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SIP 连接缓存">
            <el-tag size="small" :type="config.sipCacheServerConnections ? 'warning' : 'success'">
              {{ config.sipCacheServerConnections ? '已启用' : '未启用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="JWK 文件" :span="descriptionColumns"><code>{{ config.jwkFile || '-' }}</code></el-descriptions-item>
          <el-descriptions-item label="JSON 组件">{{ config.jsonLibrary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="JSON 组件版本">{{ config.jsonLibraryVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Fastjson 1.x">
            <el-tag size="small" :type="config.fastjson1Present ? 'danger' : 'success'">
              {{ config.fastjson1Present ? '已检测到' : '未检测到' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="config-section risk-section">
        <h3 class="section-title">风险提示</h3>
        <el-empty
          v-if="!config.warnings || config.warnings.length === 0"
          description="未发现明显的高风险配置"
          :image-size="72"
        />
        <el-alert
          v-for="warning in config.warnings"
          :key="warning"
          :title="warning"
          type="warning"
          :closable="false"
          show-icon
          class="warning-item"
        />
      </section>
    </template>

    <el-empty v-else-if="!loading" description="安全配置加载失败">
      <el-button size="small" type="primary" @click="loadConfig">重新加载</el-button>
    </el-empty>
  </div>
</template>

<script>
import { getSecurityConfig, updateSecurityConfig } from '@/api/server'

export default {
  name: 'OperationsSecurityConfig',
  data() {
    return {
      loading: false,
      saving: false,
      loaded: false,
      savedSnapshot: '',
      descriptionColumns: window.innerWidth < 900 ? 1 : 2,
      config: {},
      form: {
        interfaceAuthentication: true,
        loginTimeoutMinutes: 120,
        docEnabled: false,
        contentTypeOptionsEnabled: false,
        allowedOriginsText: '',
        interfaceAuthenticationExcludesText: ''
      }
    }
  },
  computed: {
    descriptionLabelStyle() {
      return { width: this.descriptionColumns === 1 ? '132px' : '164px' }
    },
    allowedOrigins() {
      return this.normalizeLines(this.form.allowedOriginsText)
    },
    authenticationExcludes() {
      return this.normalizeLines(this.form.interfaceAuthenticationExcludesText)
    },
    hasChanges() {
      return this.loaded && this.savedSnapshot !== JSON.stringify(this.buildRequest())
    }
  },
  created() {
    this.loadConfig()
    window.addEventListener('resize', this.updateColumns)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.updateColumns)
  },
  methods: {
    updateColumns() {
      this.descriptionColumns = window.innerWidth < 900 ? 1 : 2
    },
    normalizeLines(value) {
      return [...new Set(String(value || '')
        .split(/\r?\n/)
        .map(item => item.trim())
        .filter(Boolean))]
    },
    normalizeLoginTimeout(value, fallback = 120) {
      if (value === null || value === undefined || value === '') return fallback
      const timeout = Number(value)
      return Number.isInteger(timeout) && timeout >= 0 && timeout <= 999 ? timeout : fallback
    },
    booleanOrDefault(value, fallback) {
      return typeof value === 'boolean' ? value : fallback
    },
    buildRequest() {
      return {
        interfaceAuthentication: this.form.interfaceAuthentication,
        interfaceAuthenticationExcludes: this.authenticationExcludes,
        loginTimeoutMinutes: Number(this.form.loginTimeoutMinutes),
        allowedOrigins: this.allowedOrigins,
        docEnabled: this.form.docEnabled,
        contentTypeOptionsEnabled: this.form.contentTypeOptionsEnabled
      }
    },
    applyConfig(data) {
      const responseConfig = data || {}
      const currentForm = this.form
      const returnedTimeout = responseConfig.loginTimeoutMinutes != null
        ? responseConfig.loginTimeoutMinutes
        : responseConfig.jwtExpirationMinutes
      const loginTimeoutMinutes = this.normalizeLoginTimeout(
        returnedTimeout,
        this.normalizeLoginTimeout(currentForm.loginTimeoutMinutes)
      )
      const allowedOrigins = Array.isArray(responseConfig.allowedOrigins)
        ? responseConfig.allowedOrigins
        : this.normalizeLines(currentForm.allowedOriginsText)
      const authenticationExcludes = Array.isArray(responseConfig.interfaceAuthenticationExcludes)
        ? responseConfig.interfaceAuthenticationExcludes
        : this.normalizeLines(currentForm.interfaceAuthenticationExcludesText)

      this.config = { ...responseConfig, loginTimeoutMinutes }
      this.form = {
        interfaceAuthentication: this.booleanOrDefault(
          responseConfig.interfaceAuthentication,
          currentForm.interfaceAuthentication
        ),
        loginTimeoutMinutes,
        docEnabled: this.booleanOrDefault(responseConfig.docEnabled, currentForm.docEnabled),
        contentTypeOptionsEnabled: this.booleanOrDefault(
          responseConfig.contentTypeOptionsEnabled,
          currentForm.contentTypeOptionsEnabled
        ),
        allowedOriginsText: allowedOrigins.join('\n'),
        interfaceAuthenticationExcludesText: authenticationExcludes.join('\n')
      }
      this.savedSnapshot = JSON.stringify(this.buildRequest())
      this.loaded = true
    },
    errorMessage(error, fallback) {
      if (typeof error === 'string') return error
      return error && error.message ? error.message : fallback
    },
    loadConfig() {
      this.loading = true
      getSecurityConfig()
        .then(this.applyConfig)
        .catch(error => {
          this.loaded = false
          this.$message.error(this.errorMessage(error, '安全配置加载失败，请稍后重试'))
        })
        .finally(() => {
          this.loading = false
        })
    },
    saveConfig() {
      const request = this.buildRequest()
      const loginTimeout = request.loginTimeoutMinutes
      if (!Number.isInteger(loginTimeout) || loginTimeout < 0 || loginTimeout > 999) {
        this.$message.error('登录超时时间必须在 0 到 999 分钟之间')
        return
      }
      this.saving = true
      updateSecurityConfig(request)
        .then(data => {
          this.applyConfig(data)
          this.$message.success('安全配置已生效，当前登录令牌已刷新')
        })
        .catch(error => {
          this.$message.error(this.errorMessage(error, '安全配置保存失败，请稍后重试'))
        })
        .finally(() => {
          this.saving = false
        })
    }
  }
}
</script>

<style scoped>
.security-config {
  max-width: 1280px;
  margin: 0 auto;
}

.page-toolbar,
.section-heading,
.setting-row,
.setting-label,
.toolbar-actions,
.number-control {
  display: flex;
  align-items: center;
}

.page-toolbar {
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e5e7eb;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 5px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
}

.page-title :deep(.anticon) {
  color: #1677ff;
}

.page-subtitle {
  color: #6b7280;
  font-size: 13px;
  line-height: 20px;
}

.toolbar-actions,
.number-control,
.setting-label {
  gap: 8px;
}

.status-alert {
  margin-top: 16px;
}

.config-section {
  padding: 22px 0;
  border-bottom: 1px solid #e5e7eb;
}

.section-heading {
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title {
  margin: 0 0 12px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.section-heading .section-title {
  margin-bottom: 0;
}

.setting-list {
  border-top: 1px solid #e5e7eb;
}

.setting-row {
  min-height: 58px;
  justify-content: space-between;
  gap: 32px;
  padding: 12px 4px;
  border-bottom: 1px solid #eef0f3;
}

.setting-row--textarea {
  align-items: flex-start;
  padding-top: 16px;
  padding-bottom: 16px;
}

.setting-row--textarea > .el-textarea {
  width: min(100%, 680px);
}

.setting-label {
  min-width: 220px;
  color: #374151;
  font-size: 14px;
  font-weight: 500;
}

.number-control {
  color: #6b7280;
  font-size: 13px;
}

.number-control :deep(.ant-input-number) {
  width: 160px;
}

.warning-item + .warning-item {
  margin-top: 8px;
}

code {
  color: #374151;
  font-family: Consolas, Monaco, monospace;
  word-break: break-all;
}

.security-config :deep(.ant-descriptions-view table) {
  table-layout: fixed;
}

.security-config :deep(.ant-descriptions-item-label) {
  color: #4b5563;
  font-weight: 500;
}

.security-config :deep(.ant-descriptions-item-content) {
  color: #1f2937;
  word-break: break-word;
}

.risk-section :deep(.el-empty) {
  padding: 8px 0 0;
}

@media (max-width: 700px) {
  .security-config {
    padding: 16px 12px;
  }

  .page-toolbar,
  .setting-row {
    align-items: flex-start;
  }

  .page-toolbar,
  .setting-row,
  .setting-row--textarea {
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .setting-row {
    gap: 12px;
  }

  .setting-label {
    min-width: 0;
  }

  .setting-row--textarea > .el-textarea,
  .number-control {
    width: 100%;
  }

  .number-control :deep(.ant-input-number) {
    flex: 1;
    width: auto;
  }
}
</style>
