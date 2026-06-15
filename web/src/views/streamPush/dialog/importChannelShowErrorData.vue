<template>
  <div id="importChannelShowErrorData" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="导入通道数据成功，但数据存在重复"
      width="30rem"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        重复国标ID:
        <el-button v-clipboard="gbIds.join(',')" style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝" @success="$message({type:'success', message:'成功拷贝到粘贴板'})">复制</el-button>
        <ul class="errDataBox">
          <li v-for="id in gbIds">
            {{ id }}
          </li>
        </ul>
      </div>

      <div>
        重复App/stream:
        <el-button v-clipboard="streams.join(',')" style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝" @success="$message({type:'success', message:'成功拷贝到粘贴板'})">复制</el-button>
        <ul class="errDataBox">
          <li v-for="id in streams">
            {{ id }}
          </li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ImportChannelShowErrorData',
  directives: { elDragDialog },
  props: ['gbIds', 'streams'],
  data() {
    return {
      isLoging: false,
      showDialog: false
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function() {
      this.showDialog = true
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>
<style>
.errDataBox{
  max-height: 15rem;
  overflow: auto;
}
</style>
