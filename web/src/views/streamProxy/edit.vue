<template>
  <div id="StreamProxyEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="编辑拉流代理信息" @back="close" />
      </div>
    </div>
    <el-tabs tab-position="top" style="padding-top: 1rem">
      <el-tab-pane label="拉流代理信息" style="padding-top: 1rem">
        <el-form ref="streamProxy" :rules="rules" :model="streamProxy" label-width="140px" style="width: 50%; margin: 0 auto">
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
          <el-form-item label="拉流地址" prop="url">
            <el-input v-model="streamProxy.srcUrl" clearable />
          </el-form-item>
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
          <el-form-item v-if="streamProxy.type=='ffmpeg'" label="FFmpeg命令模板" prop="ffmpegCmdKey">
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
            <el-radio-group v-model="streamProxy.noneReader">
              <el-radio :label="0">不做处理</el-radio>
              <el-radio :label="1">停用</el-radio>
              <el-radio :label="2">移除</el-radio>
            </el-radio-group>

          </el-form-item>
          <el-form-item label="其他选项">
            <div style="float: left;">
              <el-checkbox v-model="streamProxy.enable" label="启用" />
              <el-checkbox v-model="streamProxy.enableAudio" label="开启音频" />
              <el-checkbox v-model="streamProxy.enableMp4" label="录制" />
            </div>

          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" :loading="saveLoading" @click="onSubmit">保存</el-button>
              <el-button @click="close">取消</el-button>
            </div>

          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane v-if="streamProxy.id" label="国标通道配置">
        <CommonChannelEdit ref="commonChannelEdit" :data-form="streamProxy" :cancel="close" />
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
      streamProxy: this.value,
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
    value(newValue, oldValue) {
      this.streamProxy = newValue
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
      if (!this.streamProxy.noneReader || this.streamProxy.noneReader === 0) {
        this.streamProxy.enableDisableNoneReader = false
        this.streamProxy.enableRemoveNoneReader = false
      } else if (this.streamProxy.noneReader === 1) {
        this.streamProxy.enableDisableNoneReader = true
        this.streamProxy.enableRemoveNoneReader = false
      } else if (this.streamProxy.noneReader === 2) {
        this.streamProxy.enableDisableNoneReader = false
        this.streamProxy.enableRemoveNoneReader = true
      }
    }
  }
}
</script>
