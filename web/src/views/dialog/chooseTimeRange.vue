<template>
  <div id="chooseDateTimeRange">
    <el-dialog
      v-el-drag-dialog
      title="选择时间段"
      width="500px"
      top="5rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div style="width:fit-content; margin: 0 auto">
        <el-time-picker
          v-model="timeRange"
          is-range
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          placeholder="选择时间范围"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="onSubmit">确认</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>

export default {
  name: 'ChooseDateTimeRange',
  props: {},
  data() {
    return {
      showDialog: false,
      endCallback: null,
      timeRange: '',
      businessGroup: ''
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(initTime, callback) {
      console.log(initTime)
      if (initTime) {
        this.timeRange = initTime
      }
      this.showDialog = true
      this.endCallback = callback
    },
    onSubmit: function() {
      if (this.endCallback) {
        this.endCallback(this.timeRange)
      }
      this.close()
    },
    close: function() {
      this.timeRange = ''
      this.showDialog = false
      this.endCallback = null
    }
  }
}
</script>
<style>
#chooseDateTimeRange .el-dialog__body {
  padding: 30px 20px 2px 20px;
}
</style>
