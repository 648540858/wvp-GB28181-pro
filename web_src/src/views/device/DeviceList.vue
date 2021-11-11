<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row type="flex" :gutter="[16,0]">
            <a-col :flex="1">
              <a-form-item label="设备名">
                <a-input v-model="queryParam.name" placeholder="支持模糊查询" style="width: 15rem"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="状态">
                <a-select v-model="queryParam.online" placeholder="请选择状态" default-value="0" style="width: 15rem">
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="1">在线</a-select-option>
                  <a-select-option value="0">离线</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="生产商">
                <a-input v-model="queryParam.manufacturer" style="width: 15rem;" placeholder="支持模糊查询"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="型号">
                <a-input v-model="queryParam.model" placeholder="支持模糊查询" style="width: 15rem;"/>
              </a-form-item>
            </a-col>
            <a-col>
              <span class="table-page-search-submitButtons">
                <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
                <a-button style="margin-left: 8px" @click="() => this.queryParam = {}">重置</a-button>
              </span>
            </a-col>
          </a-row>
        </a-form>
      </div>
    </a-card>

    <a-card :bordered="false">
      <div class="table-operator">
        <a-button type="primary" icon="delete" @click="deleteBatch">批量删除</a-button>
      </div>

      <s-table
        ref="table"
        size="default"
        :rowKey="(record) => record.deviceId "
        :columns="columns"
        :data="loadData"
        :rowSelection="rowSelection"
        showPagination="auto"
      >
        <span slot="online" slot-scope="text">
          <a-badge :status="text | statusTypeFilter" :text="text | statusFilter"/>
        </span>
        <span slot="description" slot-scope="text">
          <ellipsis :length="4" tooltip>{{ text }}</ellipsis>
        </span>
        <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button size="small" type="primary" v-if="record.online!==0" @click="refreshDevice(record)">
                <a-icon type="reload"/>刷新
              </a-button>
              <a-button size="small" type="primary" :disabled="record.online === 0" @click="handleChannel(record)">
                <a-icon type="video-camera"/>通道
              </a-button>
              <a-button size="small" type="primary" :disabled="record.online === 0" @click="handlePosition(record)">
                <a-icon type="global"/>定位
              </a-button>
              <a-button size="small" type="primary" @click="handleEdit(record)">
                <a-icon type="edit"/>编辑
              </a-button>
              <a-button size="small" type="danger" v-if="record.online === 0" @click="deleteDevice(record)">
                <a-icon type="delete"/>删除
              </a-button>
            </a-button-group>
        </span>
      </s-table>
      <device-edit
        ref="deviceForm"
        :visible="formVisible"
        :loading="confirmLoading"
        :model="mdl"
        @cancel="cancelForm"
        @ok="handleOk"/>
    </a-card>
  </div>
</template>

<script>
import moment from 'moment'
import {Ellipsis, STable} from '@/components'
import {deleteDevice, getDeviceList, queryGBDeviceById, updateDeviceInfo} from "@/api/deviceList";
import DeviceEdit from "@/views/device/DeviceEdit";

const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '设备编号',
    dataIndex: 'deviceId',
    align: 'center'
  },
  {
    title: '制造商',
    dataIndex: 'manufacturer',
    align: 'center'
  },
  {
    title: '型号',
    dataIndex: 'model',
    align: 'center'
  },
  {
    title: '流传输模式',
    dataIndex: 'streamMode',
    align: 'center'
  },
  {
    title: '通道数',
    dataIndex: 'channelCount',
    align: 'center'
  },
  {
    title: '地址',
    dataIndex: 'hostAddress',
    align: 'center'
  },
  {
    title: '状态',
    dataIndex: 'online',
    align: 'center',
    scopedSlots: {customRender: 'online'}
  },
  {
    title: '操作',
    dataIndex: 'action',
    align: 'center',
    scopedSlots: {customRender: 'action'}
  }
]

const statusMap = {
  0: {
    status: 'default',
    text: '离线'
  },
  1: {
    status: 'success',
    text: '在线'
  }
}

export default {
  name: 'TableList',
  components: {
    STable,
    Ellipsis,
    DeviceEdit,
  },
  data() {
    this.columns = columns
    return {
      formVisible: false,
      confirmLoading: false,
      mdl: null,
      // 查询参数
      queryParam: {},
      // 加载数据方法 必须为 Promise 对象
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getDeviceList(requestParameters).then(res => {
          return res.data
        })
      },
      selectedRowKeys: [],
      selectedRows: [],
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      tableRefreshing: false
    }
  },
  filters: {
    statusFilter(type) {
      return statusMap[type].text
    },
    statusTypeFilter(type) {
      return statusMap[type].status
    }
  },
  computed: {
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange
      }
    }
  },
  methods: {
    handleChannel(record) {
      //跳转到channel page
      this.$emit('goChannelPage', record)
    },
    handlePosition(record) {
      this.$emit('goDevicePosition', record)
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },
    resetSearchForm() {
      this.queryParam = {
        date: moment(new Date())
      }
    },
    //gb28181平台对接
    //刷新设备信息
    refreshDevice(record) {
      if (this.tableRefreshing) {
        this.$message.warn("请等待刷新结果")
        return
      }
      this.tableRefreshing = true
      queryGBDeviceById(record).then(res => {
        this.tableRefreshing = false
        if (res.deviceId) {
          this.$refs.table.refresh(true)
          this.$message.success("刷新成功")
        } else {
          this.$message.error("刷新失败")
        }
      }).catch((e) => {
        this.tableRefreshing = false
        this.$message.error("刷新失败：" + e)
      })
    },
    handleEdit(record) {
      this.formVisible = true
      this.mdl = record
    },
    handleOk() {
      const form = this.$refs.deviceForm.form
      this.confirmLoading = true
      form.validateFields((errors, values) => {
        updateDeviceInfo(values).then(res => {
          if (res.success) {
            this.$message.success("设备修改成功，通道字符集将在下次更新生效")
            setTimeout(this.$refs.table.refresh, 200)
          }
        })
        this.confirmLoading = false
        this.cancelForm()
      })

    },
    cancelForm() {
      this.formVisible = false;
      // const form = this.$refs.deviceForm.form
      // 清理表单数据（可不做）
      // form.resetFields()
    },
    deleteDevice(record) {
      deleteDevice({deviceId: record.deviceId}).then(res => {
        console.log('删除设备：' + JSON.stringify(res))
        this.$refs.table.refresh()
      }).catch(err => {
        this.$message.error('操作失败')
      })
    },
    deleteBatch() {
      console.log('批量删除')
      if (this.selectedRows && this.selectedRows.length > 0) {
        for (let row of this.selectedRows) { //检查是否选择了在线设备
          if (row.online && row.online === 1) {
            this.$message.error('只能删除离线设备')
            return
          }
        }
        for (let row of this.selectedRows) { //删除离线设备
          deleteDevice({deviceId: row.deviceId}).then(res => {
            console.log('删除设备：' + row.deviceId + ' ' + res)
          }).catch(err => {
            console.log('删除设备：' + row.deviceId + ' 失败 ' + err)
          })
        }
      } else {
        this.$message.error('请至少选择一条记录')
      }
    }
  }
}
</script>
