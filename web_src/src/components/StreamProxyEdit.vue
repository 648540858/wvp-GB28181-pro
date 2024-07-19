<template>
  <div id="StreamProxyEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-button icon="el-icon-back" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        <el-divider direction="vertical"></el-divider>
        编辑拉流代理信息
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-button icon="el-icon-close" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        </div>
      </div>
    </div>
    <el-tabs tab-position="left" style="background-color: #FFFFFF; padding-top: 1rem">
      <el-tab-pane label="拉流代理信息">
        <el-form ref="streamProxy" :rules="rules" :model="streamProxy" label-width="140px" style="width: 50%; margin: 0 auto">
          <el-form-item label="类型" prop="type">
            <el-select
              v-model="streamProxy.type"
              style="width: 100%"
              placeholder="请选择代理类型"
            >
              <el-option label="默认" value="default"></el-option>
              <el-option label="FFmpeg" value="ffmpeg"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="应用名" prop="app">
            <el-input v-model="streamProxy.app" clearable></el-input>
          </el-form-item>
          <el-form-item label="流ID" prop="stream">
            <el-input v-model="streamProxy.stream" clearable></el-input>
          </el-form-item>
          <el-form-item label="拉流地址" prop="url">
            <el-input v-model="streamProxy.srcUrl" clearable></el-input>
          </el-form-item>
          <el-form-item label="超时时间(秒)" prop="timeoutMs">
            <el-input v-model="streamProxy.timeout" clearable></el-input>
          </el-form-item>
          <el-form-item label="节点选择" prop="rtpType">
            <el-select
              v-model="streamProxy.mediaServerId"
              @change="mediaServerIdChange"
              style="width: 100%"
              placeholder="请选择拉流节点"
            >
              <el-option key="auto" label="自动选择" value=""></el-option>
              <el-option
                v-for="item in mediaServerList"
                :key="item.id"
                :label="item.id"
                :value="item.id">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="FFmpeg命令模板" prop="ffmpegCmdKey" v-if="streamProxy.type=='ffmpeg'">
            <el-select
              v-model="streamProxy.ffmpegCmdKey"
              style="width: 100%"
              placeholder="请选择FFmpeg命令模板"
            >
              <el-option
                v-for="item in Object.keys(ffmpegCmdList)"
                :key="item"
                :label="ffmpegCmdList[item]"
                :value="item">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="拉流方式(RTSP)" prop="rtpType" v-if="streamProxy.type ==='default'">
            <el-select
              v-model="streamProxy.rtspType"
              style="width: 100%"
              placeholder="请选择拉流方式"
            >
              <el-option label="TCP" value="0"></el-option>
              <el-option label="UDP" value="1"></el-option>
              <el-option label="组播" value="2"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="无人观看" prop="rtpType" >
            <el-radio v-model="streamProxy.noneReader" label="0">不做处理</el-radio>
            <el-radio v-model="streamProxy.noneReader" label="1">停用</el-radio>
            <el-radio v-model="streamProxy.noneReader" label="2">移除</el-radio>
          </el-form-item>
          <el-form-item label="其他选项">
            <div style="float: left;">
              <el-checkbox label="启用" v-model="streamProxy.enable" ></el-checkbox>
              <el-checkbox label="开启音频" v-model="streamProxy.enableAudio" ></el-checkbox>
              <el-checkbox label="录制" v-model="streamProxy.enableMp4" ></el-checkbox>
            </div>

          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit" :loading="locading" >保存</el-button>
              <el-button @click="close">取消</el-button>
            </div>

          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="国标通道配置" v-if="streamProxy.id">
        <CommonChannelEdit ref="commonChannelEdit" :dataForm="streamProxy" :cancel="close"></CommonChannelEdit>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from './common/CommonChannelEdit'
import MediaServer from "./service/MediaServer";

export default {
  name: "channelEdit",
  props: [ 'streamProxy', 'closeEdit'],
  components: {
    CommonChannelEdit,
  },
  created() {
    console.log(this.streamProxy)
    this.mediaServer.getOnlineMediaServerList((data)=>{
      this.mediaServerList = data.data;
    })
  },
  data() {
    return {
      locading: false,
      mediaServer: new MediaServer(),
      mediaServerList:{},
      ffmpegCmdList:{},
      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        app: [{ required: true, message: "请输入应用名", trigger: "blur" }],
        stream: [{ required: true, message: "请输入流ID", trigger: "blur" }],
        srcUrl: [{ required: true, message: "请输入要代理的流", trigger: "blur" }],
        timeout: [{ required: true, message: "请输入FFmpeg推流成功超时时间", trigger: "blur" }],
        ffmpegCmdKey: [{ required: false, message: "请输入FFmpeg命令参数模板（可选）", trigger: "blur" }],
      },
    };
  },
  methods: {
    onSubmit: function () {
      this.locading = true;
      this.noneReaderHandler();
      if (this.streamProxy.id) {
        this.$axios({
          method: 'post',
          url:`/api/proxy/update`,
          data: this.streamProxy
        }).then((res)=> {
          if (typeof (res.data.code) != "undefined" && res.data.code === 0) {
            this.$message.success("保存成功");
            this.streamProxy = res.data.data
          }else {
            this.$message.error(res.data.msg);
          }
        }).catch((error) =>{
          this.$message.error(res.data.error);
        }).finally(()=>{
          this.locading = false;
        })
      }else {
        this.$axios({
          method: 'post',
          url:`/api/proxy/add`,
          data: this.streamProxy
        }).then((res)=> {
          if (typeof (res.data.code) != "undefined" && res.data.code === 0) {
            this.$message.success("保存成功");
            this.streamProxy = res.data.data
          }else {
            this.$message.error(res.data.msg);
          }
        }).catch((error) =>{
          this.$message.error(res.data.error);
        }).finally(()=>{
          this.locading = false;
        })
      }

    },
    close: function () {
      this.closeEdit()
    },
    mediaServerIdChange:function (){
      if (this.streamProxy.mediaServerId !== "auto"){
        this.$axios({
          method: 'get',
          url:`/api/proxy/ffmpeg_cmd/list`,
          params: {
            mediaServerId: this.streamProxy.mediaServerId
          }
        }).then((res)=> {
          this.ffmpegCmdList = res.data.data;
          this.streamProxy.ffmpegCmdKey = Object.keys(res.data.data)[0];
        }).catch(function (error) {
          console.log(error);
        });
      }

    },
    noneReaderHandler: function() {
      console.log(this.streamProxy)
      if (this.streamProxy.noneReader === null || this.streamProxy.noneReader === "0" || !this.streamProxy.noneReader) {
        this.streamProxy.enableDisableNoneReader = false;
        this.streamProxy.enableRemoveNoneReader = false;
      }else if (this.streamProxy.noneReader === "1"){
        this.streamProxy.enableDisableNoneReader = true;
        this.streamProxy.enableRemoveNoneReader = false;
      }else if (this.streamProxy.noneReader ==="2"){
        this.streamProxy.enableDisableNoneReader = false;
        this.streamProxy.enableRemoveNoneReader = true;
      }
    },
  },
};
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
