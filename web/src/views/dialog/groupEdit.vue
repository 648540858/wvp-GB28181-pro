<template>
  <div id="groupEdit" v-loading="loading">
    <el-dialog
      v-el-drag-dialog
      title="分组编辑"
      width="40%"
      top="2rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="form" :model="group" label-width="140px">
          <el-form-item label="节点编号" prop="id">
            <el-input v-model="group.deviceId" placeholder="请输入编码">
              <el-button slot="append" @click="buildDeviceIdCode(group.deviceId)">生成</el-button>
            </el-input>
          </el-form-item>
          <el-form-item label="节点名称" prop="name">
            <el-input v-model="group.name" clearable />
          </el-form-item>
          <el-form-item label="行政区划" prop="name">
            <el-input v-model="group.civilCode">
              <el-button slot="append" @click="buildCivilCode(group.civilCode)">选择</el-button>
            </el-input>
          </el-form-item>

          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit">确认</el-button>
              <el-button @click="close">取消</el-button>
            </div>

          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
    <channelCode ref="channelCode" />
    <chooseCivilCode ref="chooseCivilCode" />
  </div>
</template>

<script>
import channelCode from './channelCode.vue'
import ChooseCivilCode from './chooseCivilCode.vue'
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'GroupEdit',
  directives: { elDragDialog },
  components: { ChooseCivilCode, channelCode },
  props: [],
  data() {
    return {
      submitCallback: null,
      showDialog: false,
      loading: false,
      level: 0,
      group: {
        id: 0,
        deviceId: '',
        name: '',
        parentDeviceId: '',
        businessGroup: '',
        civilCode: '',
        platformId: ''

      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(group, callback) {
      console.log(group)
      if (group) {
        this.group = group
      }
      this.showDialog = true
      this.submitCallback = callback
    },
    onSubmit: function() {
      if (this.group.id) {
        this.$store.dispatch('group/update', this.group)
          .then(data => {
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
            if (this.submitCallback) this.submitCallback(this.group)
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      } else {
        this.$store.dispatch('group/add', this.group)
          .then(data => {
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
            if (this.submitCallback) this.submitCallback(this.group)
            this.close()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }
    },
    buildDeviceIdCode: function(deviceId) {
      console.log(this.group)
      const lockContent = this.group.businessGroup ? '216' : '215'
      this.$refs.channelCode.openDialog(code => {
        this.group.deviceId = code
      }, deviceId, 5, lockContent)
    },
    buildCivilCode: function(deviceId) {
      this.$refs.chooseCivilCode.openDialog(code => {
        this.group.civilCode = code
      })
    },
    close: function() {
      this.showDialog = false
      console.log(this.group)
    }
  }
}
</script>
