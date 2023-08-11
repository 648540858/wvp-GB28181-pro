<template >
  <div id="consoleResource" style="width: 100%; height: 100%; background: #FFFFFF; text-align: center">
    <div style="width: 50%;height: 50%; float:left; ">
      <el-progress v-if="deviceInfo.total > 0" :width="100" :stroke-width="8" type="circle" :percentage="Math.floor(deviceInfo.online/deviceInfo.total*100)" style="margin-top: 20px; font-size: 18px"></el-progress>
      <el-progress v-if="deviceInfo.total === 0" :width="100" :stroke-width="8" type="circle" :percentage="0" style="margin-top: 20px; font-size: 18px"></el-progress>
      <div class="resourceInfo">
        设备总数:{{deviceInfo.total}}<br/>
        在线数:{{deviceInfo.online}}
      </div>
    </div>
    <div style="width: 50%;height: 50%; float:left; ">
      <el-progress v-if="channelInfo.total > 0" :width="100" :stroke-width="10" type="circle" :percentage="Math.floor(channelInfo.online/channelInfo.total*100)" style="margin-top: 20px"></el-progress>
      <el-progress v-if="channelInfo.total === 0" :width="100" :stroke-width="10" type="circle" :percentage="0" style="margin-top: 20px"></el-progress>
      <div class="resourceInfo">
        通道总数:{{channelInfo.total}}<br/>
        在线数:{{channelInfo.online}}
      </div>
    </div>
    <div style="width: 50%;height: 50%; float:left; ">
      <el-progress v-if="pushInfo.total > 0" :width="100" :stroke-width="10" type="circle" :percentage="Math.floor(pushInfo.online/pushInfo.total*100)" style="margin-top: 20px"></el-progress>
      <el-progress v-if="pushInfo.total === 0"  :width="100" :stroke-width="10" type="circle" :percentage="0" style="margin-top: 20px"></el-progress>
      <div class="resourceInfo">
        推流总数:{{pushInfo.total}}<br/>
        在线数:{{pushInfo.online}}
      </div>
    </div>
    <div style="width: 50%;height: 50%; float:left; ">
      <el-progress v-if="proxyInfo.total > 0" :width="100" :stroke-width="10" type="circle" :percentage="Math.floor(proxyInfo.online/proxyInfo.total*100)" style="margin-top: 20px"></el-progress>
      <el-progress v-if="proxyInfo.total === 0" :width="100" :stroke-width="10" type="circle" :percentage="0" style="margin-top: 20px"></el-progress>
      <div class="resourceInfo">
        拉流代理总数:{{proxyInfo.total}}<br/>
        在线数:{{proxyInfo.online}}
      </div>
    </div>
  </div>
</template>

<script>

export default {
  name: 'consoleResource',
  data() {
    return {
      deviceInfo: {
        total: 0,
        online: 0
      },
      channelInfo: {
        total: 0,
        online: 0
      },
      pushInfo: {
        total: 0,
        online: 0
      },
      proxyInfo: {
        total: 0,
        online: 0
      },
    };
  },
  created() {


  },
  mounted() {
  },
  destroyed() {
  },
  methods: {
    setData: function(data) {
      this.deviceInfo = data.device;
      this.channelInfo = data.channel;
      this.pushInfo = data.push;
      this.proxyInfo = data.proxy;
    }
  }
};
</script>

<style>
.resourceInfo{
  width: 100%;
  text-align: center;
  font-size: 12px
}
.el-progress__text {
  font-size: 18px !important;
}
</style>
