<template>
  <el-dialog
    v-el-drag-dialog
    title="选择待重置字段"
    width="45rem"
    top="10rem"
    center
    :append-to-body="true"
    :close-on-click-modal="false"
    :visible.sync="showVideoDialog"
    v-if="showVideoDialog"
    :destroy-on-close="true"
  >
    <div style="padding: 0 1rem">
      <el-checkbox v-for="(item,index) in allVal" v-bind:key="item.field" v-model="item.checked" :label="item.name" ></el-checkbox>
    </div>

    <div slot="footer">
      <el-form size="small">
        <el-form-item style="text-align: left">
          <el-button @click="checkedSome" size="mini" >常用</el-button>
          <el-button @click="checkedAll" size="mini" >全选</el-button>
          <el-button @click="clearChecked" size="mini" >清空</el-button>
        </el-form-item>
        <el-form-item style="text-align: right">
          <el-button type="primary" @click="handleOk">保存</el-button>
          <el-button @click="closeModel" >取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showVideoDialog: false,
      allVal: null
    }
  },
  beforeMount() {
    this.initData()
  },
  methods: {
    openDialog: function() {
      this.showVideoDialog = true
      this.initData()
    },
    closeModel: function() {
      this.showVideoDialog = false
    },
    initData: function() {
      this.allVal = [
        {
          name: '名称',
          field: 'gbName',
          checked: true,
          disable: false
        },
        {
          name: '编码',
          field: 'gbDeviceId',
          checked: true,
          disable: false
        },
        {
          name: '设备厂商',
          field: 'gbManufacturer',
          checked: true,
          disable: false
        },
        {
          name: '设备型号',
          field: 'gbModel',
          checked: true,
          disable: false
        },
        {
          name: '行政区域',
          field: 'gbCivilCode',
          checked: true,
          disable: false
        },
        {
          name: '安装地址',
          field: 'gbAddress',
          checked: true,
          disable: false
        },
        {
          name: '监视方位',
          field: 'gbDirectionType',
          checked: true,
          disable: false
        },
        {
          name: '父节点编码',
          field: 'gbParentId',
          checked: true,
          disable: false
        },
        {
          name: '设备状态',
          field: 'gbStatus',
          checked: true,
          disable: false
        },
        {
          name: '经度',
          field: 'gbLongitude',
          checked: true,
          disable: false
        },
        {
          name: '纬度',
          field: 'gbLatitude',
          checked: true,
          disable: false
        },
        {
          name: '摄像机类型',
          field: 'gbPtzType',
          checked: true,
          disable: false
        },
        {
          name: '业务分组',
          field: 'gbBusinessGroupId',
          checked: true,
          disable: false
        },
        {
          name: '警区',
          field: 'gbBlock',
          checked: true,
          disable: false
        },
        {
          name: '保密属性',
          field: 'gbSecrecy',
          checked: true,
          disable: false
        },
        {
          name: 'IP地址',
          field: 'gbIpAddress',
          checked: true,
          disable: false
        },
        {
          name: '端口',
          field: 'gbPort',
          checked: true,
          disable: false
        },
        {
          name: '设备归属',
          field: 'gbOwner',
          checked: true,
          disable: false
        },
        {
          name: '是否有子设备',
          field: 'gbParental',
          checked: true,
          disable: false
        },
        {
          name: '位置类型',
          field: 'gbPositionType',
          checked: true,
          disable: false
        },
        {
          name: '室内/室外',
          field: 'gbRoomType',
          checked: true,
          disable: false
        },
        {
          name: '用途',
          field: 'gbUseType',
          checked: true,
          disable: false
        },
        {
          name: '补光',
          field: 'gbSupplyLightType',
          checked: true,
          disable: false
        },
        {
          name: '分辨率',
          field: 'gbResolution',
          checked: true,
          disable: false
        },
        {
          name: '下载倍速',
          field: 'gbDownloadSpeed',
          checked: true,
          disable: false
        }
      ]
    },
    clearChecked: function() {
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        item.checked = false
      }
    },
    checkedAll: function() {
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        item.checked = true
      }
    },
    checkedSome: function() {
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        item.checked = (item.field === 'gbName' || item.field === 'gbStatus'
          || item.field === 'gbLongitude' || item.field === 'gbLatitude'
          || item.field === 'gbBusinessGroupId' || item.field === 'gbParentId')
      }
    },
    handleOk: function() {
      this.showVideoDialog = false
      let fileArray = []
      for (let i = 0; i < this.allVal.length; i++) {
        let item = this.allVal[i]
        if (item.checked) {
          fileArray.push(item.field)
        }
      }
      this.$emit('submit', fileArray)
    }
  }
}
</script>
