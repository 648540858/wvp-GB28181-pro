<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row type="flex" :gutter="[5, 0]">
            <a-col :flex="1">
              <a-form-item label="搜索">
                <a-input v-model="queryParam.searchSrt" placeholder="关键字" style="width: 15rem"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="通道类型">
                <a-select v-model="queryParam.channelType" placeholder="选择通道类型" style="width: 15rem">
                  <a-select-option value="false">设备</a-select-option>
                  <a-select-option value="true">子目录</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="选择状态">
                <a-select v-model="queryParam.choosed" placeholder="请选择状态" style="width: 15rem">
                  <a-select-option value="true">已选择</a-select-option>
                  <a-select-option value="false">未选择</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="在线状态">
                <a-select v-model="queryParam.online" placeholder="请选择在线状态" style="width: 15rem">
                  <a-select-option value="true">在线</a-select-option>
                  <a-select-option value="false">离线</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col>
              <a-space>
                <a-button type="primary" @click="$refs.table.refresh(true)">
                  <font-awesome-icon :icon="['fas','search']" style="margin-right: 0.25rem"/>
                  查询
                </a-button>
                <a-button type="danger" @click="() => this.queryParam = {}">
                  <font-awesome-icon :icon="['fas','redo']" style="margin-right: 0.25rem"/>
                  重置
                </a-button>
                <a-button @click="() => $emit('goBack')">
                  <font-awesome-icon :icon="['fas','arrow-left']" style="margin-right: 0.25rem"/>
                  返回
                </a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </div>
    </a-card>

    <a-card :bordered="false">
      <a-button type="primary" style="margin-bottom: 10px" @click="shareChannel">
        <font-awesome-icon :icon="['fas','share']" style="margin-right: 0.25rem"/>
        共享通道
      </a-button>
      <s-table
        ref="table"
        size="default"
        :rowKey="(record) => record.deviceId + '_' + record.channelId"
        :columns="columns"
        :data="loadData"
        :rowSelection="rowSelection"
        showPagination="auto"
      >
        <span slot="hostAddress" slot-scope="text, record">
          <a-tag color="blue">{{ text }}</a-tag>
        </span>
      </s-table>
    </a-card>
  </div>
</template>

<script>
import {STable} from '@/components'
import {delChannelForGB, getChannelList, updateChannelForGB} from "@/api/gbPlatform";

const columns = [
  {
    title: '通道编号',
    dataIndex: 'channelId',
    align: 'center'
  },
  {
    title: '通道名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '设备编号',
    dataIndex: 'deviceId',
    align: 'center'
  },
  {
    title: '设备地址',
    dataIndex: 'hostAddress',
    align: 'center',
    scopedSlots: {customRender: 'hostAddress'}
  },
  {
    title: '厂家',
    dataIndex: 'manufacturer',
    align: 'center'
  }]

export default {
  name: 'DeviceChannelList',
  props: {
    platformId: {
      type: String,
      default: ''
    }
  },
  components: {
    STable
  },
  data() {
    this.columns = columns
    return {
      // 查询参数
      queryParam: {},
      // 加载数据方法必须为Promise对象
      loadData: parameter => {
        const requestParameters = Object.assign({platformId: this.platformId}, parameter, this.queryParam)
        return getChannelList(requestParameters).then(res => {
          this.gbChooseChannel = {}
          let list = res.data.data
          list.forEach((item) => {
            if (item.platformId === this.platformId) {
              let rowKey = item.deviceId + "_" + item.channelId
              this.selectedRowKeys.push(rowKey);
              this.gbChooseChannel[rowKey] = item;
            }
          })
          this.eventEnable = true;
          return res.data
        }).catch(err => {
          console.log(err)
        })
      },
      selectedRowKeys: [],
      selectedRows: [],
      selections: [],
      gbChooseChannel: {},
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'}
    }
  },
  computed: {
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange,
        selections: this.selections
      }
    }
  },
  methods: {
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },

    shareChannel() {
      let newData = {}
      let addData = []
      let delData = []
      if (this.selectedRows.length > 0) {
        this.selectedRows.forEach(item => {
          let key = item.deviceId + "_" + item.channelId;
          newData[key] = item;
          if (!!!this.gbChooseChannel[key]) {
            addData.push(item)
          } else {
            delete this.gbChooseChannel[key]
          }
        })

        let oldKeys = Object.keys(this.gbChooseChannel);
        oldKeys.forEach(key => {
          delData.push(this.gbChooseChannel[key])
        })

      } else {
        let oldKeys = Object.keys(this.gbChooseChannel);
        oldKeys.forEach(key => {
          delData.push(this.gbChooseChannel[key])
        })
      }

      this.gbChooseChannel = newData;
      if (Object.keys(addData).length > 0) {
        updateChannelForGB({platformId: this.platformId, channelReduces: addData}).then(res => {
          res ? this.$message.success('共享通道成功') : this.$message.error('共享通道失败')
          console.log('操作成功？' + res)
        }).catch(err => {
          this.$message.error('请求错误：' + err)
          console.log('请求错误：' + err)
        })
      }

      if (Object.keys(delData).length > 0) {
        delChannelForGB({platformId: this.platformId, channelReduces: delData}).then(res => {
          res ? this.$message.success('从上级平台移除国标通道成功') : this.$message.error('从上级平台移除国标通道失败')
          console.log('操作成功？' + res)
        }).catch(err => {
          this.$message.error('请求错误：' + err)
          console.log('请求错误：' + err)
        })
      }
    }
  }
}
</script>