<template>
  <div v-loading="loading">
    <el-card shadow="never" class="section-card">
      <div slot="header" class="card-header">
        <span class="card-title">平台策略配置</span>
        <span v-if="!writable" class="card-tip">
          <i class="el-icon-warning-outline" /> 当前配置文件不可写，仅可查看
        </span>
        <div class="header-actions">
          <el-button size="mini" @click="toggleAll">{{ allExpanded ? '全部收起' : '全部展开' }}</el-button>
        </div>
      </div>

      <el-collapse v-model="activeGroups">
        <el-collapse-item v-for="group in groups" :key="group.title" :title="group.title" :name="group.title">
          <el-form label-width="240px" class="config-form" @submit.native.prevent>
            <el-form-item v-for="field in group.fields" :key="field.key" :label="field.label">
              <template slot="label">
                <span>{{ field.label }}</span>
                <el-tag
                  v-if="restartFieldKeys.length"
                  size="mini"
                  class="restart-tag"
                  :type="isRestartField(field.key) ? 'warning' : 'success'"
                >
                  {{ isRestartField(field.key) ? '重启' : '即时' }}
                </el-tag>
              </template>
              <el-switch
                v-if="field.type === 'switch'"
                v-model="form[field.key]"
                :disabled="!writable"
              />
              <el-select
                v-else-if="field.type === 'select'"
                v-model="form[field.key]"
                :disabled="!writable"
                clearable
                placeholder="请选择"
                style="width: 480px"
              >
                <el-option v-for="opt in field.options" :key="opt" :label="opt" :value="opt" />
              </el-select>
              <el-select
                v-else-if="field.type === 'multiselect'"
                v-model="form[field.key]"
                :disabled="!writable"
                multiple
                collapse-tags
                placeholder="不设置则不限制"
                style="width: 480px"
              >
                <el-option
                  v-for="opt in field.options"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <tag-list-input
                v-else-if="field.type === 'list'"
                v-model="form[field.key]"
                :disabled="!writable"
                :placeholder="field.placeholder"
                :tips="field.tips"
                :item-validator="listValidator(field.key)"
              />
              <el-input
                v-else-if="field.type === 'number'"
                v-model.number="form[field.key]"
                type="number"
                :disabled="!writable"
                clearable
                style="width: 480px"
              >
                <template v-if="field.unit" slot="append">{{ field.unit }}</template>
              </el-input>
              <el-input
                v-else
                v-model="form[field.key]"
                :disabled="!writable"
                clearable
                style="width: 480px"
              />
              <span v-if="field.tip" class="field-tip">{{ field.tip }}</span>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>

      <div class="save-bar">
        <span class="save-hint">
          <template v-if="dirty">配置已修改，保存后即时生效（标记为“重启”的字段需重启）</template>
          <template v-else-if="pendingRestart">配置已保存，部分配置需重启后生效</template>
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
import TagListInput from './components/TagListInput.vue'

function emptyForm() {
  return {
    // 基础配置
    interfaceAuthentication: false,
    interfaceAuthenticationExcludes: [],
    useSourceIpAsStreamIp: false,
    sqlLog: false,
    deviceStatusNotify: false,
    docEnable: false,
    serverId: '',
    civilCodeFile: '',
    allowedOrigins: [],
    maxNotifyCountQueue: null,
    loginTimeout: null,
    jwkFile: '',
    savePositionHistory: false,
    // 视频播放
    autoApplyPlay: false,
    playTimeout: null,
    recordInfoTimeout: null,
    streamOnDemand: false,
    snapAsync: false,
    // 推流控制
    pushAuthority: false,
    recordPushLive: false,
    usePushingAsStatus: false,
    useAliasForGroupSync: false,
    // 国标服务
    deviceIdStrict: false,
    seniorSdp: false,
    sipLog: false,
    sipUseSourceIpAsRemoteAddress: false,
    recordSip: false,
    syncChannelOnDeviceOnline: false,
    sendToPlatformsWhenIdLost: false,
    refuseChannelStatusChannelFormNotify: false,
    ssrcRandom: false,
    gbDeviceOnline: null,
    sipCacheServerConnections: false,
    disableDateHeader: false,
    groupSyncDeviceTemplate: '',
    subscribeMobilePosition: false,
    deviceRegisterTimeMaxCount: null,
    deviceRegisterTimeTtlHours: null,
    deviceKeepaliveTimeMaxCount: null,
    deviceKeepaliveTimeTtlHours: null,
    // 国标级联
    platformPlayTimeout: null,
    useCustomSsrcForParentInvite: false,
    broadcastForPlatform: '',
    registerAgainAfterTime: null,
    registerKeepIntDialog: false,
    autoRegisterPlatform: false,
    sendPositionOnDemand: false,
    // 报警控制
    alarmCatchSize: null,
    alarmSnapByStream: false,
    allowedAlarmType: [],
    alarmKeepDays: null
  }
}

export default {
  name: 'UserSettingConfig',
  components: { TagListInput },
  mixins: [configPanelMixin],
  data() {
    return {
      loading: false,
      saving: false,
      pendingRestart: false,
      writable: false,
      profileId: '',
      filePath: '',
      form: emptyForm(),
      saved: emptyForm(),
      alarmTypes: [],
      restartFieldKeys: [],
      activeGroups: ['基础配置'],
      groups: [
        {
          title: '基础配置',
          fields: [
            { key: 'serverId', label: '服务ID', type: 'text', tip: '不写则为 000000' },
            { key: 'interfaceAuthentication', label: '开启接口鉴权', type: 'switch' },
            { key: 'interfaceAuthenticationExcludes', label: '鉴权例外接口', type: 'list', placeholder: '/api/device/query/...', tips: '需以 / 开头, 至少两级目录, 尽量不使用 /**' },
            { key: 'loginTimeout', label: '登录超时时间', type: 'number', unit: '分钟' },
            { key: 'allowedOrigins', label: '跨域白名单', type: 'list', placeholder: 'http://example.com:8080', tips: '需以 http:// 或 https:// 开头, 不设置则允许所有跨域' },
            { key: 'useSourceIpAsStreamIp', label: '使用来源IP拼接流地址', type: 'switch' },
            { key: 'sqlLog', label: 'sql日志', type: 'switch' },
            { key: 'deviceStatusNotify', label: '通知设备/通道状态变化', type: 'switch' },
            { key: 'civilCodeFile', label: '行政区划文件', type: 'text' },
            { key: 'maxNotifyCountQueue', label: 'notify缓存队列长度', type: 'number' },
            { key: 'jwkFile', label: 'jwk文件路径', type: 'text' },
            { key: 'savePositionHistory', label: '保存位置历史轨迹', type: 'switch' },
            { key: 'docEnable', label: '开启接口文档', type: 'switch' }
          ]
        },
        {
          title: '视频播放',
          fields: [
            { key: 'streamOnDemand', label: '按需拉流', type: 'switch' },
            { key: 'autoApplyPlay', label: '自动点播', type: 'switch' },
            { key: 'playTimeout', label: '点播超时时间', type: 'number', unit: '毫秒' },
            { key: 'recordInfoTimeout', label: '录像查询超时时间', type: 'number', unit: '毫秒' },
            { key: 'snapAsync', label: 'ZLM内部解码截图', type: 'switch' }
          ]
        },
        {
          title: '推流控制',
          fields: [
            { key: 'pushAuthority', label: '开启推流鉴权', type: 'switch' },
            { key: 'recordPushLive', label: '推流直播录制', type: 'switch' },
            { key: 'usePushingAsStatus', label: '推流状态作为通道状态', type: 'switch' },
            { key: 'useAliasForGroupSync', label: '分组同步使用别名', type: 'switch' }
          ]
        },
        {
          title: '国标服务',
          fields: [
            { key: 'syncChannelOnDeviceOnline', label: '自动同步通道', type: 'switch' },
            { key: 'recordSip', label: '国标录制', type: 'switch' },
            { key: 'ssrcRandom', label: '随机SSRC', type: 'switch' },
            { key: 'deviceIdStrict', label: '设备ID严格模式', type: 'switch' },
            { key: 'seniorSdp', label: '扩展SDP', type: 'switch' },
            { key: 'sipLog', label: 'sip日志', type: 'switch' },
            { key: 'sipUseSourceIpAsRemoteAddress', label: '来源IP作为回复IP', type: 'switch' },
            { key: 'sendToPlatformsWhenIdLost', label: '消息默认推送所有上级', type: 'switch' },
            { key: 'refuseChannelStatusChannelFormNotify', label: '禁用Notify通道状态变化', type: 'switch' },
            { key: 'sipCacheServerConnections', label: '保持服务器套接字', type: 'switch' },
            { key: 'disableDateHeader', label: '禁用时间同步', type: 'switch' },
            { key: 'groupSyncDeviceTemplate', label: '分组编号模板', type: 'text' },
            { key: 'subscribeMobilePosition', label: '默认开启位置订阅', type: 'switch' },
            { key: 'gbDeviceOnline', label: '离线上线策略', type: 'number', tip: '0-标准实现, 1-收到心跳即上线' },
            { key: 'deviceRegisterTimeMaxCount', label: '注册时间最大条数', type: 'number' },
            { key: 'deviceRegisterTimeTtlHours', label: '注册时间过期时间', type: 'number', unit: '小时' },
            { key: 'deviceKeepaliveTimeMaxCount', label: '心跳时间最大条数', type: 'number' },
            { key: 'deviceKeepaliveTimeTtlHours', label: '心跳时间过期时间', type: 'number', unit: '小时' }
          ]
        },
        {
          title: '国标级联',
          fields: [
            { key: 'registerKeepIntDialog', label: '续订方式', type: 'switch', tip: '开启为续订, 关闭为重新注册' },
            { key: 'registerAgainAfterTime', label: '级联重注册间隔', type: 'number', unit: '秒' },
            { key: 'broadcastForPlatform', label: '喊话发流模式', type: 'select', options: ['UDP', 'TCP-ACTIVE', 'TCP-PASSIVE'] },
            { key: 'platformPlayTimeout', label: '点播等待超时', type: 'number', unit: '毫秒' },
            { key: 'useCustomSsrcForParentInvite', label: '点播使用自定义SSRC', type: 'switch' },
            { key: 'autoRegisterPlatform', label: '集群自动注册上级', type: 'switch' },
            { key: 'sendPositionOnDemand', label: '按需发送位置', type: 'switch' }
          ]
        },
        {
          title: '报警控制',
          fields: [
            { key: 'alarmCatchSize', label: '报警通道缓存大小', type: 'number' },
            { key: 'alarmSnapByStream', label: '拉流方式获取快照', type: 'switch' },
            { key: 'allowedAlarmType', label: '报警订阅白名单', type: 'multiselect', options: [] },
            { key: 'alarmKeepDays', label: '报警记录保留天数', type: 'number', unit: '天', tip: '0 表示不自动清理' }
          ]
        }
      ]
    }
  },
  computed: {
    profileLabel() {
      return this.profileId ? `application-${this.profileId}.yml` : '主配置文件'
    },
    dirty() {
      return JSON.stringify(this.form) !== JSON.stringify(this.saved)
    },
    allExpanded() {
      return this.activeGroups.length === this.groups.length
    }
  },
  created() {
    this.loadAlarmTypes()
    this.reload()
  },
  methods: {
    loadAlarmTypes() {
      this.$store.dispatch('setting/getAlarmTypes').then(data => {
        this.alarmTypes = data || []
        const group = this.groups.find(item => item.title === '报警控制')
        if (group) {
          const field = group.fields.find(item => item.key === 'allowedAlarmType')
          if (field) {
            field.options = this.alarmTypes.map(item => ({ label: item.description, value: item.name }))
          }
        }
      }).catch(() => {
        // 报警类型加载失败不阻塞页面
      })
    },
    toggleAll() {
      this.activeGroups = this.allExpanded ? [] : this.groups.map(item => item.title)
    },
    // 按字段返回列表项的格式校验函数
    listValidator(key) {
      if (key === 'allowedOrigins') {
        return (value) => (/^https?:\/\/[^\s]+$/.test(value) ? '' : '需以 http:// 或 https:// 开头')
      }
      if (key === 'interfaceAuthenticationExcludes') {
        return (value) => {
          if (!value.startsWith('/')) {
            return '需以 / 开头, 如 /api/device/query/...'
          }
          return ''
        }
      }
      return null
    },
    reload() {
      this.loading = true
      this.$store.dispatch('setting/getUserSettingConfig')
        .then(data => {
          if (!data) {
            return
          }
          this.writable = !!data.writable
          this.profileId = data.profileId || ''
          this.filePath = data.filePath || ''
          this.restartFieldKeys = Array.isArray(data.restartRequiredFields) ? data.restartRequiredFields : []
          const form = emptyForm()
          const num = (value) => (typeof value === 'number' ? value : null)
          const bool = (value) => !!value
          const text = (value) => value || ''
          const list = (value) => (Array.isArray(value) ? value : [])
          // 基础配置
          form.interfaceAuthentication = bool(data.interfaceAuthentication)
          form.interfaceAuthenticationExcludes = list(data.interfaceAuthenticationExcludes)
          form.useSourceIpAsStreamIp = bool(data.useSourceIpAsStreamIp)
          form.sqlLog = bool(data.sqlLog)
          form.deviceStatusNotify = bool(data.deviceStatusNotify)
          form.docEnable = bool(data.docEnable)
          form.serverId = text(data.serverId)
          form.civilCodeFile = text(data.civilCodeFile)
          form.allowedOrigins = list(data.allowedOrigins)
          form.maxNotifyCountQueue = num(data.maxNotifyCountQueue)
          form.loginTimeout = num(data.loginTimeout)
          form.jwkFile = text(data.jwkFile)
          form.savePositionHistory = bool(data.savePositionHistory)
          // 视频播放
          form.autoApplyPlay = bool(data.autoApplyPlay)
          form.playTimeout = num(data.playTimeout)
          form.recordInfoTimeout = num(data.recordInfoTimeout)
          form.streamOnDemand = bool(data.streamOnDemand)
          form.snapAsync = bool(data.snapAsync)
          // 推流控制
          form.pushAuthority = bool(data.pushAuthority)
          form.recordPushLive = bool(data.recordPushLive)
          form.usePushingAsStatus = bool(data.usePushingAsStatus)
          form.useAliasForGroupSync = bool(data.useAliasForGroupSync)
          // 国标服务
          form.deviceIdStrict = bool(data.deviceIdStrict)
          form.seniorSdp = bool(data.seniorSdp)
          form.sipLog = bool(data.sipLog)
          form.sipUseSourceIpAsRemoteAddress = bool(data.sipUseSourceIpAsRemoteAddress)
          form.recordSip = bool(data.recordSip)
          form.syncChannelOnDeviceOnline = bool(data.syncChannelOnDeviceOnline)
          form.sendToPlatformsWhenIdLost = bool(data.sendToPlatformsWhenIdLost)
          form.refuseChannelStatusChannelFormNotify = bool(data.refuseChannelStatusChannelFormNotify)
          form.ssrcRandom = bool(data.ssrcRandom)
          form.gbDeviceOnline = num(data.gbDeviceOnline)
          form.sipCacheServerConnections = bool(data.sipCacheServerConnections)
          form.disableDateHeader = bool(data.disableDateHeader)
          form.groupSyncDeviceTemplate = text(data.groupSyncDeviceTemplate)
          form.subscribeMobilePosition = bool(data.subscribeMobilePosition)
          form.deviceRegisterTimeMaxCount = num(data.deviceRegisterTimeMaxCount)
          form.deviceRegisterTimeTtlHours = num(data.deviceRegisterTimeTtlHours)
          form.deviceKeepaliveTimeMaxCount = num(data.deviceKeepaliveTimeMaxCount)
          form.deviceKeepaliveTimeTtlHours = num(data.deviceKeepaliveTimeTtlHours)
          // 国标级联
          form.platformPlayTimeout = num(data.platformPlayTimeout)
          form.useCustomSsrcForParentInvite = bool(data.useCustomSsrcForParentInvite)
          form.broadcastForPlatform = text(data.broadcastForPlatform)
          form.registerAgainAfterTime = num(data.registerAgainAfterTime)
          form.registerKeepIntDialog = bool(data.registerKeepIntDialog)
          form.autoRegisterPlatform = bool(data.autoRegisterPlatform)
          form.sendPositionOnDemand = bool(data.sendPositionOnDemand)
          // 报警控制
          form.alarmCatchSize = num(data.alarmCatchSize)
          form.alarmSnapByStream = bool(data.alarmSnapByStream)
          form.allowedAlarmType = list(data.allowedAlarmType)
          form.alarmKeepDays = num(data.alarmKeepDays)

          this.form = { ...form, interfaceAuthenticationExcludes: [...form.interfaceAuthenticationExcludes], allowedOrigins: [...form.allowedOrigins], allowedAlarmType: [...form.allowedAlarmType] }
          this.saved = JSON.parse(JSON.stringify(this.form))
          this.pendingRestart = false
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '读取平台策略配置失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSave() {
      if (!this.dirty) {
        return
      }
      this.doSave()
    },
    // 是否属于后端标识的“需重启才能生效”字段
    isRestartField(key) {
      return this.restartFieldKeys.indexOf(key) >= 0
    },
    // 由字段 key 映射到中文名称, 用于保存提示
    fieldLabel(key) {
      for (const group of this.groups) {
        const field = group.fields.find(item => item.key === key)
        if (field) {
          return field.label
        }
      }
      return key
    },
    doSave() {
      const payload = {}
      Object.keys(this.form).forEach(key => {
        payload[key] = this.form[key] === '' ? null : this.form[key]
      })
      this.saving = true
      this.$store.dispatch('setting/saveUserSettingConfig', payload).then(res => {
        const result = (res && res.data) || {}
        this.saved = JSON.parse(JSON.stringify(this.form))
        if (result.restartRequired) {
          this.pendingRestart = true
          const names = (result.restartFields || []).map(key => this.fieldLabel(key))
          const detail = names.length ? `：${names.join('、')}` : ''
          this.$confirm(
            `配置已保存，以下配置需重启服务后生效${detail}。是否立即重启？`,
            '保存平台策略配置',
            {
              confirmButtonText: '立即重启',
              cancelButtonText: '稍后手动重启',
              type: 'warning'
            }
          ).then(() => {
            this.doRestart(() => this.reload())
          }).catch(() => {
            this.$message.info('配置将在下次服务重启后生效')
          })
        } else {
          this.pendingRestart = false
          this.$message.success('配置已保存并已生效')
        }
      }).catch(error => {
        this.$message.error(typeof error === 'string' ? error : '保存平台策略配置失败')
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
  max-width: 900px;
}

.field-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.restart-tag {
  margin-left: 6px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
  font-size: 10px;
  border-radius: 3px;
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
