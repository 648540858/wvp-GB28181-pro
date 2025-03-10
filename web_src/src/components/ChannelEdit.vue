<template>
  <div id="ChannelEdit" v-loading="locading" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header @back="close" content="编辑通道"></el-page-header>
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-button icon="el-icon-close" size="mini" style="font-size: 20px; color: #000;" type="text" @click="close" ></el-button>
        </div>
      </div>
    </div>
    <CommonChannelEdit ref="commonChannelEdit" :id="id" :saveSuccess="close" :cancel="close"></CommonChannelEdit>
  </div>
</template>

<script>
import CommonChannelEdit from './common/CommonChannelEdit'

export default {
  name: "channelEdit",
  props: [ 'id', 'closeEdit'],
  components: {
    CommonChannelEdit,
  },
  data() {
    return {};
  },
  methods: {
    close: function () {
      this.closeEdit()
    },
  },
};
</script>
