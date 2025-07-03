<template>
  <div id="ChannelEdit" v-loading="locading" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="编辑通道" @back="close" />
      </div>
    </div>
    <CommonChannelEdit :id="id" ref="commonChannelEdit" :save-success="close" :cancel="close" />
  </div>
</template>

<script>
import CommonChannelEdit from '../../common/CommonChannelEdit'

export default {
  name: 'ChannelEdit',
  components: {
    CommonChannelEdit
  },
  props: ['id', 'closeEdit'],
  data() {
    return {}
  },
  methods: {
    close: function() {
      this.closeEdit()
    }
  }
}
</script>
