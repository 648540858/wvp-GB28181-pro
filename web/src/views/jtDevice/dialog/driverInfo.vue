<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="驾驶员信息"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared">
        <el-descriptions :column="2" v-if="driverInfo" style="margin-bottom: 1rem;">
          <el-descriptions-item label="状态">{{ getStatus(driverInfo.status) }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ driverInfo.time }}</el-descriptions-item>
          <el-descriptions-item label="IC卡读取结果">{{ getICInfo(driverInfo.result) }}</el-descriptions-item>
          <el-descriptions-item label="驾驶员姓名">{{ driverInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="从业资格证编码">{{ driverInfo.certificateCode }}</el-descriptions-item>
          <el-descriptions-item label="发证机构名称">{{ driverInfo.certificateIssuanceMechanismName }}</el-descriptions-item>
          <el-descriptions-item label="证件有效期">{{ driverInfo.expire }}</el-descriptions-item>
          <el-descriptions-item label="驾驶员身份证号">{{ driverInfo.driverIdNumber }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      driverInfo: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.driverInfo = data
    },
    close: function() {
      this.showDialog = false
    },
    getStatus: function(status) {
      switch (status) {
        case 1:
          return 'IC卡插入'
        case 2:
          return 'IC卡拔出'
        default:
          return '未知'

      }
    },
    getICInfo: function(result) {
      switch (result) {
        case 0:
          return 'IC卡读卡成功'
        case 1:
          return '读卡失败：卡片密钥认证未通过'
        case 2:
          return '读卡失败：卡片已被锁定'
        case 3:
          return '读卡失败：卡片被拔出'
        case 4:
          return '读卡失败：数据校验错误'
        default:
          return '未知失败原因'

      }
    }
  }
}
</script>
