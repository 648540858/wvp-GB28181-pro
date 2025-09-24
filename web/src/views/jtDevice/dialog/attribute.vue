<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="终端属性"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared">
        <el-descriptions title="基本属性" :column="2" v-if="attributeData" style="margin-bottom: 1rem;">
          <el-descriptions-item label="制造商ID">{{ attributeData.makerId }}</el-descriptions-item>
          <el-descriptions-item label="终端型号">{{ attributeData.deviceModel }}</el-descriptions-item>
          <el-descriptions-item label="终端ID">{{ attributeData.terminalId }}</el-descriptions-item>
          <el-descriptions-item label="SIM卡ICCID">{{ attributeData.iccId }}</el-descriptions-item>
          <el-descriptions-item label="硬件版本号">{{ attributeData.hardwareVersion }}</el-descriptions-item>
          <el-descriptions-item label="固件版本号">{{ attributeData.firmwareVersion }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="终端类型" :column="2" v-if="attributeData.type" style="margin-bottom: 1rem;">
          <el-descriptions-item label="客运车辆">{{ attributeData.type.passengerVehicles }}</el-descriptions-item>
          <el-descriptions-item label="危险品车辆">{{ attributeData.type.dangerousGoodsVehicles }}</el-descriptions-item>
          <el-descriptions-item label="普通货运车辆">{{ attributeData.type.freightVehicles }}</el-descriptions-item>
          <el-descriptions-item label="出租车辆">{{ attributeData.type.rentalVehicles }}</el-descriptions-item>
          <el-descriptions-item label="硬盘录像">{{ attributeData.type.hardDiskRecording?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ attributeData.type.splittingMachine?'分体机': '一体机' }}</el-descriptions-item>
          <el-descriptions-item label="适用挂车">{{ attributeData.type.trailer?'是': '否' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="GNSS模块" :column="2" v-if="attributeData.gnssAttribute" style="margin-bottom: 1rem;">
          <el-descriptions-item label="GPS卫星">{{ attributeData.gnssAttribute.gps?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="北斗卫星">{{ attributeData.gnssAttribute.beidou?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="GLONASS卫星">{{ attributeData.gnssAttribute.glonass?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="Galileo卫星">{{ attributeData.gnssAttribute.gaLiLeo?'支持': '不支持' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="通信模块" :column="2" v-if="attributeData.communicationModuleAttribute" style="margin-bottom: 1rem;">
          <el-descriptions-item label="GPRS通信">{{ attributeData.communicationModuleAttribute.gprs?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="CDMA通信">{{ attributeData.communicationModuleAttribute.cdma?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="TD-SCDMA通信">{{ attributeData.communicationModuleAttribute.tdScdma?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="WCDMA通信">{{ attributeData.communicationModuleAttribute.wcdma?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="CDMA2000通信">{{ attributeData.communicationModuleAttribute.cdma2000?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="TD-LTE通信">{{ attributeData.communicationModuleAttribute.tdLte?'支持': '不支持' }}</el-descriptions-item>
          <el-descriptions-item label="其他通信方式">{{ attributeData.communicationModuleAttribute.other?'支持': '不支持' }}</el-descriptions-item>
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
      attributeData: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.attributeData = data
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>
