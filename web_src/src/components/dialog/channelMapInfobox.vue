<template>
  <div id="channelMapInfobox" style="display: none">
    <div >
      <el-descriptions class="margin-top" title="channel.name" :column="4" direction="vertical">
        <el-descriptions-item label="生产厂商">{{channel.manufacture}}</el-descriptions-item>
        <el-descriptions-item label="型号">{{channel.model}}</el-descriptions-item>
        <el-descriptions-item label="设备归属" >{{channel.owner}}</el-descriptions-item>
        <el-descriptions-item label="行政区域" >{{channel.civilCode}}</el-descriptions-item>
        <el-descriptions-item label="安装地址" >{{channel.address}}</el-descriptions-item>
        <el-descriptions-item label="云台类型" >{{channel.ptztypeText}}</el-descriptions-item>
        <el-descriptions-item label="经纬度" >{{channel.longitude}},{{channel.latitude}}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" v-if="channel.status === 1">在线</el-tag>
          <el-tag size="small" v-if="channel.status === 0">离线</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <devicePlayer ref="devicePlayer" v-loading="isLoging"></devicePlayer>
  </div>
</template>

<script>
import devicePlayer from '../dialog/devicePlayer.vue'

export default {
  name: "channelMapInfobox",
  props: ['channel'],
  computed: {devicePlayer},
  created() {},
  data() {
    return {
      showDialog: false,
      isLoging: false
    };
  },
  methods: {

    play: function (){
      let deviceId = this.channel.deviceId;
      this.isLoging = true;
      let channelId = this.channel.channelId;
      console.log("通知设备推流1：" + deviceId + " : " + channelId);
      let that = this;
      this.$axios({
        method: 'get',
        url: '/api/play/start/' + deviceId + '/' + channelId
      }).then(function (res) {
        that.isLoging = false;
        if (res.data.code === 0) {
          that.$refs.devicePlayer.openDialog("media", deviceId, channelId, {
            streamInfo: res.data.data,
            hasAudio: this.channel.hasAudio
          });
        } else {
          that.$message.error(res.data.msg);
        }
      }).catch(function (e) {
      });
    },
    close: function () {
    },
  },
};
</script>
