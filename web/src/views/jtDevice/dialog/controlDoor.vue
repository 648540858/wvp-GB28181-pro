<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="车门控制"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div style="padding: 0 20px 0 10px">
        <el-form >
          <el-form-item style="text-align: center">
            <el-button @click="controlDoor(true)">开车门</el-button>
            <el-button @click="controlDoor(false)" >关车门</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConnectionServer',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      phoneNumber: null,
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.phoneNumber = data
    },
    close: function() {
      this.showDialog = false
    },
    controlDoor: function(open) {
      this.$store.dispatch('jtDevice/controlDoor', {
        phoneNumber: this.phoneNumber,
        open: open
      }).then(data => {
          this.$message.success({
            showClose: true,
            message: '发送成功'
          })
        })
    }
  }
}
</script>
