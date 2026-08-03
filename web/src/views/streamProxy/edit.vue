<template>
  <div id="StreamProxyEdit" class="stream-proxy-edit">
    <header class="edit-header">
      <el-page-header content="编辑拉流代理" @back="close" />
      <div v-if="streamProxy.app || streamProxy.stream" class="edit-context">
        <span class="edit-context__name">{{ streamProxy.gbName || '拉流代理' }}</span>
        <span class="edit-context__stream">{{ streamProxy.app }}/{{ streamProxy.stream }}</span>
      </div>
    </header>
    <el-tabs tab-position="top" class="edit-tabs">
      <el-tab-pane label="拉流代理信息">
        <div class="proxy-tab-panel">
          <el-form ref="streamProxy" :rules="rules" :model="streamProxy" label-width="128px" class="proxy-form">
            <section class="proxy-section">
              <h2 class="section-title">接入信息</h2>
              <div class="proxy-form-grid">
                <el-form-item label="类型" prop="type">
                  <el-select
                    v-model="streamProxy.type"
                    style="width: 100%"
                    placeholder="请选择代理类型"
                  >
                    <el-option key="默认" label="默认" value="default" />
                    <el-option key="FFmpeg" label="FFmpeg" value="ffmpeg" />
                  </el-select>
                </el-form-item>
                <el-form-item label="应用名" prop="app">
                  <el-input v-model="streamProxy.app" clearable />
                </el-form-item>
                <el-form-item label="流ID" prop="stream">
                  <el-input v-model="streamProxy.stream" clearable />
                </el-form-item>
                <el-form-item label="拉流地址" prop="url" class="proxy-field--wide">
                  <el-input v-model="streamProxy.srcUrl" clearable />
                </el-form-item>
              </div>
            </section>

            <section class="proxy-section">
              <h2 class="section-title">运行策略</h2>
              <div class="proxy-form-grid">
                <el-form-item label="超时时间(秒)" prop="timeoutMs">
                  <el-input v-model="streamProxy.timeout" clearable />
                </el-form-item>
                <el-form-item label="节点选择" prop="rtpType">
                  <el-select
                    v-model="streamProxy.relatesMediaServerId"
                    style="width: 100%"
                    placeholder="请选择拉流节点"
                    @change="mediaServerIdChange"
                  >
                    <el-option key="auto" label="自动选择" value="" />
                    <el-option
                      v-for="item in mediaServerList"
                      :key="item.id"
                      :label="item.id"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="streamProxy.type=='ffmpeg'" label="FFmpeg命令模板" prop="ffmpegCmdKey" class="proxy-field--wide">
                  <el-select
                    v-model="streamProxy.ffmpegCmdKey"
                    style="width: 100%"
                    placeholder="请选择FFmpeg命令模板"
                  >
                    <el-option
                      v-for="item in Object.keys(ffmpegCmdList)"
                      :key="item"
                      :label="ffmpegCmdList[item]"
                      :value="item"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="拉流方式(RTSP)" prop="rtspType">
                  <el-select
                    v-model="streamProxy.rtspType"
                    style="width: 100%"
                    placeholder="请选择拉流方式"
                  >
                    <el-option label="TCP" value="0" />
                    <el-option label="UDP" value="1" />
                    <el-option label="组播" value="2" />
                  </el-select>
                </el-form-item>
                <el-form-item label="无人观看" prop="noneReader">
                  <el-radio-group v-model="streamProxy.noneReader" class="proxy-option-group">
                    <el-radio :label="0">不做处理</el-radio>
                    <el-radio :label="1">停用</el-radio>
                    <el-radio :label="2">移除</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="其他选项" class="proxy-field--wide">
                  <div class="proxy-option-group">
                    <el-checkbox v-model="streamProxy.enable" label="启用" />
                    <el-checkbox v-model="streamProxy.enableAudio" label="开启音频" />
                    <el-checkbox v-model="streamProxy.enableMp4" label="录制" />
                  </div>
                </el-form-item>
              </div>
            </section>

            <div class="proxy-form-actions">
              <el-button @click="close">取消</el-button>
              <el-button type="primary" :loading="saveLoading" @click="onSubmit">保存</el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane v-if="streamProxy.id" label="国标通道配置">
        <CommonChannelEdit ref="commonChannelEdit" :showCancel="true" :data-form="streamProxy" @cancel="close" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from '../common/CommonChannelEdit'

export default {
  name: 'ChannelEdit',
  components: {
    CommonChannelEdit
  },
  props: ['value', 'closeEdit'],
  data() {
    return {
      saveLoading: false,
      streamProxy: { ...(this.value || {}) },
      mediaServerList: {},
      ffmpegCmdList: {},
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        app: [{ required: true, message: '请输入应用名', trigger: 'blur' }],
        stream: [{ required: true, message: '请输入流ID', trigger: 'blur' }],
        srcUrl: [{ required: true, message: '请输入要代理的流', trigger: 'blur' }],
        timeout: [{ required: true, message: '请输入FFmpeg推流成功超时时间', trigger: 'blur' }],
        ffmpegCmdKey: [{ required: false, message: '请输入FFmpeg命令参数模板（可选）', trigger: 'blur' }]
      }
    }
  },
  watch: {
    value(newValue) {
      this.streamProxy = { ...(newValue || {}) }
    }
  },
  created() {
    console.log(this.streamProxy)
    this.$store.dispatch('server/getOnlineMediaServerList')
      .then((data) => {
        this.mediaServerList = data
      })
  },
  methods: {
    onSubmit: function() {
      this.saveLoading = true
      this.noneReaderHandler()
      if (this.streamProxy.id) {
        this.$store.dispatch('streamProxy/update', this.streamProxy)
          .then((data) => {
            this.saveLoading = false
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
            this.streamProxy = data
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
            this.saveLoading = false
          }).finally(() => {
            this.saveLoading = false
          })
      } else {
        this.$store.dispatch('streamProxy/add', this.streamProxy)
          .then((data) => {
            this.saveLoading = false
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
            this.streamProxy = data
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
            this.saveLoading = false
          })
          .finally(() => {
            this.saveLoading = false
          })
      }
    },
    close: function() {
      this.closeEdit()
    },
    mediaServerIdChange: function() {
      if (this.streamProxy.relatesMediaServerId !== 'auto') {
        this.$store.dispatch('streamProxy/queryFfmpegCmdList', this.streamProxy.relatesMediaServerId)
          .then((data) => {
            this.ffmpegCmdList = data
            this.streamProxy.ffmpegCmdKey = Object.keys(data)[0]
          })
      }
    },
    noneReaderHandler: function() {
      console.log(this.streamProxy)
      this.streamProxy.enableDisableNoneReader = this.streamProxy.noneReader && this.streamProxy.noneReader === 1
    }
  }
}
</script>

<style scoped>
.stream-proxy-edit {
  width: 100%;
  min-height: calc(100dvh - var(--wvp-shell-height));
  position: relative;
  overflow: hidden;
  background: var(--wvp-surface);
  container-type: inline-size;
}

.edit-header {
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 28px;
  border-bottom: 1px solid var(--wvp-border-light);
}

.edit-header :deep(.ant-page-header) {
  min-width: 0;
  padding: 0;
}

.edit-header :deep(.ant-page-header-heading-title) {
  color: var(--wvp-text-primary);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0;
}

.edit-context {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--wvp-text-secondary);
  font-size: 13px;
}

.edit-context__name,
.edit-context__stream {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-context__name {
  color: var(--wvp-text-regular);
  font-weight: 500;
}

.edit-context__stream {
  padding-left: 12px;
  border-left: 1px solid var(--wvp-border);
  font-variant-numeric: tabular-nums;
}

.edit-tabs :deep(.ant-tabs-nav) {
  margin: 0;
  padding: 0 28px;
  border-bottom: 1px solid var(--wvp-border-light);
}

.edit-tabs :deep(.ant-tabs-nav::before) {
  border-bottom: 0;
}

.edit-tabs :deep(.ant-tabs-tab) {
  padding: 15px 0;
  font-weight: 500;
}

.edit-tabs :deep(.ant-tabs-content-holder) {
  background: var(--wvp-surface);
}

.proxy-tab-panel {
  min-height: calc(100vh - 232px);
  padding: 0 28px 24px;
}

.proxy-form {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding-top: 24px;
}

.proxy-section + .proxy-section {
  margin-top: 8px;
  padding-top: 24px;
  border-top: 1px solid var(--wvp-border-light);
}

.section-title {
  margin: 0 0 20px;
  color: var(--wvp-text-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 24px;
  letter-spacing: 0;
}

.proxy-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 32px;
}

.proxy-field--wide {
  grid-column: 1 / -1;
}

.proxy-option-group {
  min-height: 32px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 24px;
}

.proxy-option-group :deep(.ant-radio-wrapper),
.proxy-option-group :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
  margin-inline-end: 0;
}

.proxy-form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
  padding: 16px 0;
  border-top: 1px solid var(--wvp-border-light);
}

.proxy-form-actions :deep(.ant-btn) {
  min-width: 80px;
}

@container (max-width: 900px) {
  .proxy-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .proxy-field--wide {
    grid-column: auto;
  }
}

@container (max-width: 640px) {
  .edit-header {
    min-height: auto;
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
    padding: 16px;
  }

  .edit-context {
    width: 100%;
  }

  .edit-tabs :deep(.ant-tabs-nav) {
    padding: 0 16px;
  }

  .proxy-tab-panel {
    min-height: auto;
    padding: 0 16px 16px;
  }

  .proxy-form {
    padding-top: 16px;
  }

  .proxy-form :deep(.ant-form-item) {
    display: block;
  }

  .proxy-form :deep(.ant-form-item-label) {
    width: 100% !important;
    padding: 0 0 6px;
    text-align: left;
  }

  .proxy-form :deep(.ant-form-item-control) {
    max-width: 100%;
  }

  .proxy-form :deep(.ant-input),
  .proxy-form :deep(.ant-select-selector) {
    min-height: 44px;
  }

  .proxy-form-actions :deep(.ant-btn) {
    min-height: 44px;
  }
}
</style>
