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
              <el-form-item label="拉流地址" prop="srcUrl" v-if="proxyParam.type=='ffmpeg'">
                <el-input v-model="proxyParam.srcUrl" clearable></el-input>
              </el-form-item>
              <el-form-item label="超时时间:毫秒" prop="timeoutMs" v-if="proxyParam.type=='ffmpeg'">
                <el-input v-model="proxyParam.timeoutMs" clearable></el-input>
              </el-form-item>
              <el-form-item label="节点选择" prop="rtpType">
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
              <el-form-item label="FFmpeg命令模板" prop="ffmpegCmdKey" v-if="proxyParam.type=='ffmpeg'">
                <el-select
                  v-model="proxyParam.ffmpegCmdKey"
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
              <el-form-item label="拉流方式" prop="rtpType" v-if="proxyParam.type=='default'">
                <el-select
                  v-model="proxyParam.rtpType"
                  style="width: 100%"
                  placeholder="请选择拉流方式"
                >
                  <el-option label="TCP" value="0"></el-option>
                  <el-option label="UDP" value="1"></el-option>
                  <el-option label="组播" value="2"></el-option>
                </el-select>
              </el-form-item>

            <el-form-item label="无人观看" prop="rtpType" >
              <el-radio v-model="proxyParam.noneReader" label="0">不做处理</el-radio>
              <el-radio v-model="proxyParam.noneReader" label="1">停用</el-radio>
              <el-radio v-model="proxyParam.noneReader" label="2">移除</el-radio>
<!--              <el-select-->
<!--                @change="noneReaderHandler"-->
<!--                v-model="proxyParam.noneReader"-->
<!--                style="width: 100%"-->
<!--                placeholder="请选择无人观看的处理方式"-->
<!--              >-->
<!--                <el-option label="不做处理" value="0"></el-option>-->
<!--                <el-option label="停用" value="1"></el-option>-->
<!--                <el-option label="移除" value="2"></el-option>-->
<!--              </el-select>-->
            </el-form-item>
              <el-form-item label="其他选项">
                <div style="float: left;">
                  <el-checkbox label="启用" v-model="proxyParam.enable" ></el-checkbox>
                  <el-checkbox label="开启音频" v-model="proxyParam.enableAudio" ></el-checkbox>
                  <el-checkbox label="录制" v-model="proxyParam.enableMp4" ></el-checkbox>
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
          srcUrl: null,
          timeoutMs: null,
          ffmpegCmdKey: null,
          gbId: null,
          rtpType: null,
          enable: true,
          enableAudio: true,
          enableMp4: false,
          noneReader: null,
          enableRemoveNoneReader: false,
          enableDisableNoneReader: false,
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
        srcUrl: [{ required: true, message: "请输入要代理的流", trigger: "blur" }],
        timeoutMs: [{ required: true, message: "请输入FFmpeg推流成功超时时间", trigger: "blur" }],
        ffmpegCmdKey: [{ required: false, message: "请输入FFmpeg命令参数模板（可选）", trigger: "blur" }],
      },
    };
  },
  methods: {
    openDialog: function (proxyParam, callback) {
      this.showDialog = true;
      this.listChangeCallback = callback;
      if (proxyParam != null) {
        this.proxyParam = proxyParam;
        this.proxyParam.noneReader = null;
      }

      let that = this;
      this.$axios({
        method: 'get',
        url:`/api/platform/query/10000/1`
      }).then(function (res) {
        that.platformList = res.data.data.list;
      }).catch(function (error) {
        console.log(error);
      });
      this.mediaServer.getOnlineMediaServerList((data)=>{
        this.mediaServerList = data.data;
        this.proxyParam.mediaServerId = this.mediaServerList[0].id
        this.mediaServerIdChange()
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
          that.proxyParam.ffmpegCmdKey = Object.keys(res.data.data)[0];
        }).catch(function (error) {
          console.log(error);
        });
      }

    },
    onSubmit: function () {
      this.dialogLoading = true;
      this.noneReaderHandler();
      this.$axios({
        method: 'post',
        url:`/api/proxy/save`,
        data: this.proxyParam
      }).then((res)=> {
        this.dialogLoading = false;
        if (typeof (res.data.code) != "undefined" && res.data.code === 0) {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: "success",
          });
          this.showDialog = false;
          if (this.listChangeCallback != null) {
            this.listChangeCallback();
            this.dialogLoading = false;
          }
        }
      }).catch((error) =>{
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
        method: 'get',
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
    },
    noneReaderHandler: function() {
      if (this.proxyParam.noneReader === null || this.proxyParam.noneReader === "0") {
        this.proxyParam.enableDisableNoneReader = false;
        this.proxyParam.enableRemoveNoneReader = false;
      }else if (this.proxyParam.noneReader === "1"){
        this.proxyParam.enableDisableNoneReader = true;
        this.proxyParam.enableRemoveNoneReader = false;
      }else if (this.proxyParam.noneReader ==="2"){
        this.proxyParam.enableDisableNoneReader = false;
        this.proxyParam.enableRemoveNoneReader = true;
      }
    },
  },
};
</script>
