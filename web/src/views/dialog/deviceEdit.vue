<template>
  <div id="deviceEdit" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="设备编辑"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 50px;">
        <el-form ref="form" :rules="rules" :model="form" label-width="100px">
          <el-form-item label="设备编号" prop="deviceId">
            <el-input v-if="isEdit" v-model="form.deviceId" disabled />
            <el-input v-if="!isEdit" v-model="form.deviceId" clearable />
          </el-form-item>

          <el-form-item label="设备名称" prop="name">
            <el-input v-model="form.name" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" clearable />
          </el-form-item>
          <el-form-item label="收流IP" prop="sdpIp">
            <el-input v-model="form.sdpIp" type="sdpIp" clearable />
          </el-form-item>
          <el-form-item label="流媒体ID" prop="mediaServerId">
            <el-select v-model="form.mediaServerId" style="float: left; width: 100%">
              <el-option key="auto" label="自动负载最小" value="auto" />
              <el-option
                v-for="item in mediaServerList"
                :key="item.id"
                :label="item.id"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="字符集" prop="charset">
            <el-select v-model="form.charset" style="float: left; width: 100%">
              <el-option key="GB2312" label="GB2312" value="gb2312" />
              <el-option key="UTF-8" label="UTF-8" value="utf-8" />
            </el-select>
          </el-form-item>
          <el-form-item label="其他选项">
            <el-checkbox v-model="form.ssrcCheck" label="SSRC校验" style="float: left" />
            <el-checkbox v-model="form.asMessageChannel" label="作为消息通道" style="float: left" />
            <el-checkbox v-model="form.broadcastPushAfterAck" label="收到ACK后发流" style="float: left" />
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
  </div>
</template>

<script>
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'DeviceEdit',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      hostNames: [],
      mediaServerList: [], // 滅体节点列表
      form: {},
      isEdit: false,
      rules: {
        deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(row, callback) {
      console.log(row)
      this.showDialog = true
      this.isEdit = false
      if (row) {
        this.isEdit = true
      }
      this.form = {}
      this.listChangeCallback = callback
      if (row != null) {
        this.form = row
      }
      this.getMediaServerList()
    },
    getMediaServerList: function() {
      this.$store.dispatch('server/getOnlineMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
    },
    onSubmit: function() {
      if (this.isEdit) {
        this.$store.dispatch('device/update', this.form)
          .then((data) => {
            this.listChangeCallback()
          })
      } else {
        this.$store.dispatch('device/add', this.form)
          .then((data) => {
            this.listChangeCallback()
          })
      }
    },
    close: function() {
      this.showDialog = false
      this.$refs.form.resetFields()
    }
  }
}
</script>
