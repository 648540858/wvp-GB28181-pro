<template>
  <div id="addStreamProxy" v-loading="isLoging">
    <el-dialog
      title="添加代理"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="streamProxy" :rules="rules" :model="proxyParam" label-width="140px" >
              <el-form-item label="类型" prop="type">
                <el-select
                  v-model="proxyParam.type"
                  style="width: 100%"
                  placeholder="请选择代理类型"
                >
                  <el-option label="默认" value="default"></el-option>
                  <el-option label="FFmpeg" value="ffmpeg"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="名称" prop="name">
                <el-input v-model="proxyParam.name" clearable></el-input>
              </el-form-item>
              <el-form-item label="流应用名" prop="app">
                <el-input v-model="proxyParam.app" clearable></el-input>
              </el-form-item>
              <el-form-item label="流ID" prop="stream">
                <el-input v-model="proxyParam.stream" clearable></el-input>
              </el-form-item>
              <el-form-item label="拉流地址" prop="url" v-if="proxyParam.type=='default'">
                <el-input v-model="proxyParam.url" clearable></el-input>
              </el-form-item>
              <el-form-item label="拉流地址" prop="src_url" v-if="proxyParam.type=='ffmpeg'">
                <el-input v-model="proxyParam.src_url" clearable></el-input>
              </el-form-item>
              <el-form-item label="超时时间:毫秒" prop="timeout_ms" v-if="proxyParam.type=='ffmpeg'">
                <el-input v-model="proxyParam.timeout_ms" clearable></el-input>
              </el-form-item>
              <el-form-item label="节点选择" prop="rtp_type">
                <el-select
                  v-model="proxyParam.mediaServerId"
                  @change="mediaServerIdChange"
                  style="width: 100%"
                  placeholder="请选择拉流节点"
                >
                  <el-option
                    v-for="item in mediaServerList"
                    :key="item.id"
                    :label="item.id"
                    :value="item.id">
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="FFmpeg命令模板" prop="ffmpeg_cmd_key" v-if="proxyParam.type=='ffmpeg'">
<!--                <el-input v-model="proxyParam.ffmpeg_cmd_key" clearable></el-input>-->
                <el-select
                  v-model="proxyParam.ffmpeg_cmd_key"
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
              <el-form-item label="国标编码" prop="gbId">
                <el-input v-model="proxyParam.gbId" placeholder="设置国标编码可推送到国标" clearable></el-input>
              </el-form-item>
              <el-form-item label="拉流方式" prop="rtp_type" v-if="proxyParam.type=='default'">
                <el-select
                  v-model="proxyParam.rtp_type"
                  style="width: 100%"
                  placeholder="请选择拉流方式"
                >
                  <el-option label="TCP" value="0"></el-option>
                  <el-option label="UDP" value="1"></el-option>
                  <el-option label="组播" value="2"></el-option>
                </el-select>
              </el-form-item>

              <el-form-item label="国标平台">
                <el-select
                  v-model="proxyParam.platformGbId"
                  style="width: 100%"
                  placeholder="请选择国标平台"
                >
                  <el-option
                    v-for="item in platformList"
                    :key="item.name"
                    :label="item.name"
                    :value="item.serverGBId">
                    <span style="float: left">{{ item.name }}</span>
                    <span style="float: right; color: #8492a6; font-size: 13px">{{ item.serverGBId }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="其他选项">
                <div style="float: left;">
                  <el-checkbox label="启用" v-model="proxyParam.enable" ></el-checkbox>
                  <el-checkbox label="转HLS" v-model="proxyParam.enable_hls" ></el-checkbox>
                  <el-checkbox label="MP4录制" v-model="proxyParam.enable_mp4" ></el-checkbox>
                  <el-checkbox label="无人观看自动删除" v-model="proxyParam.enable_remove_none_reader" ></el-checkbox>
                </div>

              </el-form-item>
              <el-form-item>
                <div style="float: right;">
                  <el-button type="primary" @click="onSubmit" :loading="dialogLoading" >{{onSubmit_text}}</el-button>
                  <el-button @click="close">取消</el-button>
                </div>

              </el-form-item>
            </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import MediaServer from './../service/MediaServer'

export default {
  name: "streamProxyEdit",
  props: {},
  computed: {},
  created() {},
  data() {
    // var deviceGBIdRules = async (rule, value, callback) => {
    //   console.log(value);
    //   if (value === "") {
    //     callback(new Error("请输入设备国标编号"));
    //   } else {
    //     var exit = await this.deviceGBIdExit(value);
    //     console.log(exit);
    //     console.log(exit == "true");
    //     console.log(exit === "true");
    //     if (exit) {
    //       callback(new Error("设备国标编号已存在"));
    //     } else {
    //       callback();
    //     }
    //   }
    // };
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      dialogLoading: false,
      onSubmit_text: "立即创建",
      platformList: [],
      mediaServer: new MediaServer(),
      proxyParam: {
          name: null,
          type: "default",
          app: null,
          stream: null,
          url: "",
          src_url: null,
          timeout_ms: null,
          ffmpeg_cmd_key: null,
          gbId: null,
          rtp_type: null,
          enable: true,
          enable_hls: true,
          enable_mp4: false,
          enable_remove_none_reader: false,
          platformGbId: null,
          mediaServerId: null,
      },
      mediaServerList:{},
      ffmpegCmdList:{},

      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        app: [{ required: true, message: "请输入应用名", trigger: "blur" }],
        stream: [{ required: true, message: "请输入流ID", trigger: "blur" }],
        url: [{ required: true, message: "请输入要代理的流", trigger: "blur" }],
        src_url: [{ required: true, message: "请输入要代理的流", trigger: "blur" }],
        timeout_ms: [{ required: true, message: "请输入FFmpeg推流成功超时时间", trigger: "blur" }],
        ffmpeg_cmd_key: [{ required: false, message: "请输入FFmpeg命令参数模板（可选）", trigger: "blur" }],
      },
    };
  },
  methods: {
    openDialog: function (proxyParam, callback) {
      this.showDialog = true;
      this.listChangeCallback = callback;
      if (proxyParam != null) {
        this.proxyParam = proxyParam;
      }

      let that = this;
      this.$axios({
        method: 'get',
        url:`/api/platform/query/10000/1`
      }).then(function (res) {
        that.platformList = res.data.list;
      }).catch(function (error) {
        console.log(error);
      });
      this.mediaServer.getOnlineMediaServerList((data)=>{
        this.mediaServerList = data.data;
        this.proxyParam.mediaServerId = this.mediaServerList[0].id
      })
    },
    mediaServerIdChange:function (){
      let that = this;
      if (that.proxyParam.mediaServerId !== "auto"){
        that.$axios({
          method: 'get',
          url:`/api/proxy/ffmpeg_cmd/list`,
          params: {
            mediaServerId: that.proxyParam.mediaServerId
          }
        }).then(function (res) {
          that.ffmpegCmdList = res.data.data;
        }).catch(function (error) {
          console.log(error);
        });
      }

    },
    onSubmit: function () {
      this.dialogLoading = true;
      var that = this;
      that.$axios({
        method: 'post',
        url:`/api/proxy/save`,
        data: that.proxyParam
      }).then(function (res) {
        that.dialogLoading = false;
        if (typeof (res.data.code) != "undefined" && res.data.code === 0) {
          that.$message({
            showClose: true,
            message: res.data.msg,
            type: "success",
          });
          that.showDialog = false;
          if (that.listChangeCallback != null) {
            that.listChangeCallback();
            that.dialogLoading = false;
          }
        }
      }).catch(function (error) {
        console.log(error);
        this.dialogLoading = false;
      });
    },
    close: function () {
      this.showDialog = false;
      this.dialogLoading = false;
      this.$refs.streamProxy.resetFields();
    },
    deviceGBIdExit: async function (deviceGbId) {
      var result = false;
      var that = this;
      await that.$axios({
        method: 'post',
        url:`/api/platform/exit/${deviceGbId}`
      }).then(function (res) {
        result = res.data;
      }).catch(function (error) {
        console.log(error);
      });
      return result;
    },
    checkExpires: function() {
      if (this.platform.enable && this.platform.expires == "0") {
        this.platform.expires = "300";
      }
    }
  },
};
</script>
