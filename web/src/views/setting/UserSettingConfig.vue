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
                style="width: 260px"
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
              <el-input
                v-else-if="field.type === 'list'"
                type="textarea"
                :rows="4"
                :value="listText[field.key]"
                :disabled="!writable"
                style="width: 480px"
                @input="value => onListInput(field.key, value)"
              />
              <el-input
                v-else-if="field.type === 'number'"
                v-model.number="form[field.key]"
                type="number"
                :disabled="!writable"
                clearable
                style="width: 220px"
              >
                <template v-if="field.unit" slot="append">{{ field.unit }}</template>
              </el-input>
              <el-input
                v-else
                v-model="form[field.key]"
                :disabled="!writable"
                clearable
                style="width: 320px"
              />
              <span v-if="field.tip" class="field-tip">{{ field.tip }}</span>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>

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
    serverId: '',
    broadcastForPlatform: '',
    civilCodeFile: '',
    jwkFile: '',
    docEnable: false,
    loginTimeout: null,
    autoRegisterPlatform: false,
    useAliasForGroupSync: false,
    groupSyncDeviceTemplate: '',
    deviceIdStrict: false,
    interfaceAuthentication: false,
    interfaceAuthenticationExcludes: [],
    allowedOrigins: [],
    gbDeviceOnline: null,
    registerAgainAfterTime: null,
    registerKeepIntDialog: false,
    sipUseSourceIpAsRemoteAddress: false,
    sipCacheServerConnections: false,
    disableDateHeader: false,
    deviceRegisterTimeMaxCount: null,
    deviceRegisterTimeTtlHours: null,
    deviceKeepaliveTimeMaxCount: null,
    deviceKeepaliveTimeTtlHours: null,
    syncChannelOnDeviceOnline: false,
    subscribeMobilePosition: false,
    savePositionHistory: false,
    sendPositionOnDemand: false,
    playTimeout: null,
    recordInfoTimeout: null,
    platformPlayTimeout: null,
    autoApplyPlay: false,
    streamOnDemand: false,
    seniorSdp: false,
    usePushingAsStatus: false,
    useSourceIpAsStreamIp: false,
    useCustomSsrcForParentInvite: false,
    ssrcRandom: false,
    pushAuthority: false,
    recordPushLive: false,
    recordSip: false,
    sipLog: false,
    sqlLog: false,
    deviceStatusNotify: false,
    sendToPlatformsWhenIdLost: false,
    refuseChannelStatusChannelFormNotify: false,
    maxNotifyCountQueue: null,
    alarmCatchSize: null,
    alarmKeepDays: null,
    alarmSnapByStream: false,
    snapAsync: false,
    allowedAlarmType: []
  }
}

export default {
  name: 'UserSettingConfig',
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
      activeGroups: ['平台基础'],
      listText: {
        interfaceAuthenticationExcludes: '',
        allowedOrigins: ''
      },
      groups: [
        {
          title: '平台基础',
          fields: [
            { key: 'serverId', label: '服务ID', type: 'text', tip: '不写则为 000000' },
            { key: 'broadcastForPlatform', label: '喊话发流模式', type: 'select', options: ['UDP', 'TCP-ACTIVE', 'TCP-PASSIVE'] },
            { key: 'civilCodeFile', label: '行政区划文件', type: 'text' },
            { key: 'jwkFile', label: 'jwk文件路径', type: 'text' },
            { key: 'docEnable', label: '开启接口文档', type: 'switch' },
            { key: 'loginTimeout', label: '登录超时时间', type: 'number', unit: '分钟' },
            { key: 'autoRegisterPlatform', label: '集群自动注册上级', type: 'switch' },
            { key: 'useAliasForGroupSync', label: '分组同步使用别名', type: 'switch' },
            { key: 'groupSyncDeviceTemplate', label: '分组编号模板', type: 'text' },
            { key: 'deviceIdStrict', label: '设备ID严格模式', type: 'switch' }
          ]
        },
        {
          title: '接口鉴权与跨域',
          fields: [
            { key: 'interfaceAuthentication', label: '开启接口鉴权', type: 'switch' },
            { key: 'interfaceAuthenticationExcludes', label: '鉴权例外接口', type: 'list', tip: '每行一个' },
            { key: 'allowedOrigins', label: '跨域白名单', type: 'list', tip: '每行一个，不设置则允许所有' }
          ]
        },
        {
          title: '设备注册与心跳',
          fields: [
            { key: 'gbDeviceOnline', label: '离线上线策略', type: 'number', tip: '0-标准实现, 1-收到心跳即上线' },
            { key: 'registerAgainAfterTime', label: '级联重注册间隔', type: 'number', unit: '秒' },
            { key: 'registerKeepIntDialog', label: '续订方式', type: 'switch', tip: '开启为续订, 关闭为重新注册' },
            { key: 'sipUseSourceIpAsRemoteAddress', label: '使用来源IP作为回复IP', type: 'switch' },
            { key: 'sipCacheServerConnections', label: '保持服务器套接字', type: 'switch' },
            { key: 'disableDateHeader', label: '禁用date头', type: 'switch' },
            { key: 'deviceRegisterTimeMaxCount', label: '注册时间列表最大条数', type: 'number' },
            { key: 'deviceRegisterTimeTtlHours', label: '注册时间过期时间', type: 'number', unit: '小时' },
            { key: 'deviceKeepaliveTimeMaxCount', label: '心跳时间列表最大条数', type: 'number' },
            { key: 'deviceKeepaliveTimeTtlHours', label: '心跳时间过期时间', type: 'number', unit: '小时' }
          ]
        },
        {
          title: '设备与通道',
          fields: [
            { key: 'syncChannelOnDeviceOnline', label: '上线自动同步通道', type: 'switch' },
            { key: 'subscribeMobilePosition', label: '默认开启位置订阅', type: 'switch' },
            { key: 'savePositionHistory', label: '保存位置历史轨迹', type: 'switch' },
            { key: 'sendPositionOnDemand', label: '按需发送位置', type: 'switch' }
          ]
        },
        {
          title: '点播与转发',
          fields: [
            { key: 'playTimeout', label: '点播等待超时时间', type: 'number', unit: '毫秒' },
            { key: 'recordInfoTimeout', label: '录像查询超时时间', type: 'number', unit: '毫秒' },
            { key: 'platformPlayTimeout', label: '上级点播等待超时', type: 'number', unit: '毫秒' },
            { key: 'autoApplyPlay', label: '自动点播', type: 'switch' },
            { key: 'streamOnDemand', label: '国标点播按需拉流', type: 'switch' },
            { key: 'seniorSdp', label: '扩展SDP', type: 'switch' },
            { key: 'usePushingAsStatus', label: '推流状态作为通道状态', type: 'switch' },
            { key: 'useSourceIpAsStreamIp', label: '使用来源IP作为streamIp', type: 'switch' },
            { key: 'useCustomSsrcForParentInvite', label: '上级点播使用自定义SSRC', type: 'switch' },
            { key: 'ssrcRandom', label: '多端口模式随机SSRC', type: 'switch' },
            { key: 'pushAuthority', label: '开启推流鉴权', type: 'switch' }
          ]
        },
        {
          title: '录像',
          fields: [
            { key: 'recordPushLive', label: '推流直播录制', type: 'switch' },
            { key: 'recordSip', label: '国标录制', type: 'switch' }
          ]
        },
        {
          title: '日志',
          fields: [
            { key: 'sipLog', label: '开启sip日志', type: 'switch' },
            { key: 'sqlLog', label: '开启sql日志', type: 'switch' }
          ]
        },
        {
          title: '消息与通知',
          fields: [
            { key: 'deviceStatusNotify', label: '设备/通道状态变化发送消息', type: 'switch' },
            { key: 'sendToPlatformsWhenIdLost', label: '缺少ID时给所有上级发消息', type: 'switch' },
            { key: 'refuseChannelStatusChannelFormNotify', label: '不接受notify通道状态变化', type: 'switch' },
            { key: 'maxNotifyCountQueue', label: 'notify缓存队列长度', type: 'number' }
          ]
        },
        {
          title: '报警与快照',
          fields: [
            { key: 'alarmCatchSize', label: '报警通道缓存大小', type: 'number' },
            { key: 'alarmKeepDays', label: '报警记录保留天数', type: 'number', unit: '天', tip: '0 表示不自动清理' },
            { key: 'alarmSnapByStream', label: '拉流方式获取快照', type: 'switch' },
            { key: 'snapAsync', label: '使用ZLM内部解码截图', type: 'switch' },
            { key: 'allowedAlarmType', label: '报警订阅白名单', type: 'multiselect', options: [] }
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
        const group = this.groups.find(item => item.title === '报警与快照')
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
    onListInput(key, value) {
      this.listText[key] = value
      this.form[key] = value.split('\n').map(item => item.trim()).filter(item => item)
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
          const form = emptyForm()
          const num = (value) => (typeof value === 'number' ? value : null)
          const bool = (value) => !!value
          const text = (value) => value || ''
          const list = (value) => (Array.isArray(value) ? value : [])
          form.serverId = text(data.serverId)
          form.broadcastForPlatform = text(data.broadcastForPlatform)
          form.civilCodeFile = text(data.civilCodeFile)
          form.jwkFile = text(data.jwkFile)
          form.docEnable = bool(data.docEnable)
          form.loginTimeout = num(data.loginTimeout)
          form.autoRegisterPlatform = bool(data.autoRegisterPlatform)
          form.useAliasForGroupSync = bool(data.useAliasForGroupSync)
          form.groupSyncDeviceTemplate = text(data.groupSyncDeviceTemplate)
          form.deviceIdStrict = bool(data.deviceIdStrict)
          form.interfaceAuthentication = bool(data.interfaceAuthentication)
          form.interfaceAuthenticationExcludes = list(data.interfaceAuthenticationExcludes)
          form.allowedOrigins = list(data.allowedOrigins)
          form.gbDeviceOnline = num(data.gbDeviceOnline)
          form.registerAgainAfterTime = num(data.registerAgainAfterTime)
          form.registerKeepIntDialog = bool(data.registerKeepIntDialog)
          form.sipUseSourceIpAsRemoteAddress = bool(data.sipUseSourceIpAsRemoteAddress)
          form.sipCacheServerConnections = bool(data.sipCacheServerConnections)
          form.disableDateHeader = bool(data.disableDateHeader)
          form.deviceRegisterTimeMaxCount = num(data.deviceRegisterTimeMaxCount)
          form.deviceRegisterTimeTtlHours = num(data.deviceRegisterTimeTtlHours)
          form.deviceKeepaliveTimeMaxCount = num(data.deviceKeepaliveTimeMaxCount)
          form.deviceKeepaliveTimeTtlHours = num(data.deviceKeepaliveTimeTtlHours)
          form.syncChannelOnDeviceOnline = bool(data.syncChannelOnDeviceOnline)
          form.subscribeMobilePosition = bool(data.subscribeMobilePosition)
          form.savePositionHistory = bool(data.savePositionHistory)
          form.sendPositionOnDemand = bool(data.sendPositionOnDemand)
          form.playTimeout = num(data.playTimeout)
          form.recordInfoTimeout = num(data.recordInfoTimeout)
          form.platformPlayTimeout = num(data.platformPlayTimeout)
          form.autoApplyPlay = bool(data.autoApplyPlay)
          form.streamOnDemand = bool(data.streamOnDemand)
          form.seniorSdp = bool(data.seniorSdp)
          form.usePushingAsStatus = bool(data.usePushingAsStatus)
          form.useSourceIpAsStreamIp = bool(data.useSourceIpAsStreamIp)
          form.useCustomSsrcForParentInvite = bool(data.useCustomSsrcForParentInvite)
          form.ssrcRandom = bool(data.ssrcRandom)
          form.pushAuthority = bool(data.pushAuthority)
          form.recordPushLive = bool(data.recordPushLive)
          form.recordSip = bool(data.recordSip)
          form.sipLog = bool(data.sipLog)
          form.sqlLog = bool(data.sqlLog)
          form.deviceStatusNotify = bool(data.deviceStatusNotify)
          form.sendToPlatformsWhenIdLost = bool(data.sendToPlatformsWhenIdLost)
          form.refuseChannelStatusChannelFormNotify = bool(data.refuseChannelStatusChannelFormNotify)
          form.maxNotifyCountQueue = num(data.maxNotifyCountQueue)
          form.alarmCatchSize = num(data.alarmCatchSize)
          form.alarmKeepDays = num(data.alarmKeepDays)
          form.alarmSnapByStream = bool(data.alarmSnapByStream)
          form.snapAsync = bool(data.snapAsync)
          form.allowedAlarmType = list(data.allowedAlarmType)

          this.listText = {
            interfaceAuthenticationExcludes: form.interfaceAuthenticationExcludes.join('\n'),
            allowedOrigins: form.allowedOrigins.join('\n')
          }
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
      this.$confirm(
        '确认保存平台策略配置吗？保存后需要重启服务才能生效。',
        '保存平台策略配置',
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
    },
    doSave(needRestart) {
      const payload = {}
      Object.keys(this.form).forEach(key => {
        payload[key] = this.form[key] === '' ? null : this.form[key]
      })
      this.saving = true
      this.$store.dispatch('setting/saveUserSettingConfig', payload).then(() => {
        this.$message.success('平台策略配置已保存')
        this.saved = JSON.parse(JSON.stringify(this.form))
        this.pendingRestart = true
        if (needRestart) {
          this.confirmRestart(() => this.reload())
        } else {
          this.$message.info('配置将在下次服务重启后生效')
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
