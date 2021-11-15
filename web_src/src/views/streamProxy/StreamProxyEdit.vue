<template>
  <a-modal
    title="添加代理"
    :width="900"
    :visible="visible"
    :confirmLoading="loading"
    @ok="handleOk"
    @cancel="cancelForm"
  >
    <a-spin :spinning="loading">
      <a-form-model ref="form" :model="proxyParam" :layout="form.layout" :rules="rules" v-bind="formItemLayout">
        <a-form-model-item label="类型" prop="type">
          <a-select placeholder="请选择代理类型" v-model='proxyParam.type' style="width: 100%">
            <a-select-option value="default">默认</a-select-option>
            <a-select-option value="ffmpeg">FFmpeg</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input placeholder="请输入流名称" v-model="proxyParam.name"/>
        </a-form-model-item>
        <a-form-model-item label="流应用名" prop="app">
          <a-input placeholder="请输入流应用名" v-model="proxyParam.app"/>
        </a-form-model-item>
        <a-form-model-item label="流ID" prop="stream">
          <a-input placeholder="请输入流流ID" v-model="proxyParam.stream"/>
        </a-form-model-item>
        <a-form-model-item label="拉流地址" prop="url" v-if="proxyParam.type==='default'">
          <a-input placeholder="请输入拉流地址" v-model="proxyParam.url"/>
        </a-form-model-item>
        <a-form-model-item label="拉流地址" prop="src_url" v-if="proxyParam.type==='ffmpeg'">
          <a-input placeholder="请输入拉流地址" v-model="proxyParam.src_url"/>
        </a-form-model-item>
        <a-form-model-item label="超时时间:毫秒" prop="timeout_ms" v-if="proxyParam.type==='ffmpeg'">
          <a-input-number placeholder="请输入超时时间:毫秒" v-model="proxyParam.timeout_ms" style="width: 100%"/>
        </a-form-model-item>
        <a-form-model-item label="节点选择" prop="rtp_type">
          <a-select placeholder="请选择拉流节点" v-model="proxyParam.mediaServerId" style="width: 100%"
                    @change="mediaServerIdChange">
            <a-select-option value="auto">自动选择</a-select-option>
            <a-select-option v-for="item in mediaServerList"
                             :key="item.id"
                             :value="item.id">
              {{ item.id }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="FFmpeg命令模板" prop="ffmpeg_cmd_key" v-if="proxyParam.type==='ffmpeg'">
          <a-select placeholder="请选择FFmpeg命令模板" v-model="proxyParam.ffmpeg_cmd_key" style="width: 100%">
            <a-select-option v-for="item in Object.keys(ffmpegCmdList)"
                             :key="item"
                             :value="item">
              {{ ffmpegCmdList[item] }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="国标编码" prop="gbId">
          <a-input placeholder="设置国标编码可推送到国标" v-model="proxyParam.gbId"/>
        </a-form-model-item>
        <a-form-model-item label="拉流方式" prop="rtp_type" v-if="proxyParam.type==='default'">
          <a-select placeholder="请选择拉流方式" v-model="proxyParam.rtp_type" style="width: 100%">
            <a-select-option value="0">TCP</a-select-option>
            <a-select-option value="1">UDP</a-select-option>
            <a-select-option value="2">组播</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="国标平台">
          <a-select placeholder="请选择国标平台" v-model="proxyParam.platformGbId" style="width: 100%">
            <a-select-option v-for="item in platformList"
                             :key="item.name"
                             :value="item.serverGBId">
              <span style="float: left">{{ item.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ item.serverGBId }}</span>
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="其他选项">
          <div style="float: left;">
            <a-checkbox v-model="proxyParam.enable">启用</a-checkbox>
            <a-checkbox v-model="proxyParam.enable_hls">转HLS</a-checkbox>
            <a-checkbox v-model="proxyParam.enable_mp4">MP4录制</a-checkbox>
          </div>
        </a-form-model-item>
      </a-form-model>
    </a-spin>
  </a-modal>
</template>

<script>

import {getFFmpegCMDs, queryPlatforms, saveStreamProxy} from "@/api/streamProxy";
import {getOnlineMediaServerList} from "@/api/streamProxy";

export default {
  props: {},
  data() {
    return {
      visible: false,
      loading: false,
      proxyParam: {
        name: null,
        type: "default",
        app: null,
        stream: null,
        url: "rtmp://58.200.131.2/livetv/cctv5hd",
        src_url: null,
        timeout_ms: null,
        ffmpeg_cmd_key: null,
        gbId: null,
        rtp_type: undefined,
        enable: true,
        enable_hls: true,
        enable_mp4: false,
        platformGbId: undefined,
        mediaServerId: "auto",
      },
      form: {
        layout: 'horizontal'
      },
      type: 'default',
      rules: {
        name: [{required: true, message: "请输入名称", trigger: "blur"}],
        app: [{required: true, message: "请输入应用名", trigger: "blur"}],
        stream: [{required: true, message: "请输入流ID", trigger: "blur"}],
        url: [{required: true, message: "请输入要代理的流", trigger: "blur"}],
        src_url: [{required: true, message: "请输入要代理的流", trigger: "blur"}],
        timeout_ms: [{required: true, message: "请输入FFmpeg推流成功超时时间", trigger: "blur"}],
        ffmpeg_cmd_key: [{required: false, message: "请输入FFmpeg命令参数模板（可选）", trigger: "blur"}],
      },
      platformList: [],
      mediaServerList: {},
      ffmpegCmdList: {}
    }
  },
  computed: {
    formItemLayout() {
      const {layout} = this.form;
      return layout === 'horizontal'
        ? {
          labelCol: {
            xs: {span: 24},
            sm: {span: 7}
          },
          wrapperCol: {
            xs: {span: 24},
            sm: {span: 13}
          }
        }
        : {};
    },
  },
  methods: {
    open(proxyParam) {
      this.visible = true
      if (proxyParam != null) {
        this.proxyParam = proxyParam;
      }
      queryPlatforms().then(res => {
        this.platformList = res.data.data
      }).catch(err => {
        console.log(err)
      })
      getOnlineMediaServerList().then(res => {
        this.mediaServerList = res.data;
      })
    },
    mediaServerIdChange() {
      if (this.proxyParam.mediaServerId !== "auto") {
        getFFmpegCMDs({mediaServerId: this.proxyParam.mediaServerId}).then(res => {
          this.ffmpegCmdList = res.data
        }).catch(function (error) {
          console.log(error);
        });
      }
    },
    cancelForm() {
      this.visible = false
      this.confirmLoading = false
      const form = this.$refs.form
      form.resetFields()
    },
    handleOk() {
      this.confirmLoading = true
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.confirmLoading = false
          return
        }
        saveStreamProxy(this.proxyParam).then(res => {
          console.log(res)
          this.confirmLoading = false
          if (res.code === 0) {
            this.$message.success(res.msg)
            this.$emit('refreshTable')
            this.cancelForm()
          }
        }).catch(err => {
          console.log(err)
        })
      })
    },
  },
  mounted() {

  }
}
</script>