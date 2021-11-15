<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <a-space style="float: right">
        <div>
          <span>关键字搜索：</span>
          <a-input v-model="queryParam.query" placeholder="关键字" style="width: 15rem"/>
        </div>
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
        <span slot="streamType" slot-scope="text, record">
          <a-tag color="blue">
            {{ formatStreamType(text) }}
          </a-tag>
        </span>
      </s-table>
    </a-card>
  </div>
</template>

<script>
import {STable} from '@/components'
import {delChannelForGB, getChannelList, queryGbChannel, updateChannelForGB} from "@/api/gbPlatform";

const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '应用名',
    dataIndex: 'app',
    align: 'center'
  },
  {
    title: '国标编码',
    dataIndex: 'gbId',
    align: 'center'
  },
  {
    title: '流来源',
    dataIndex: 'streamType',
    align: 'center',
    scopedSlots: {customRender: 'streamType'}
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
        return queryGbChannel(requestParameters).then(res => {
          this.gbChooseChannel = {}
          let list = res.data.data
          list.forEach((item) => {
            if (item.platformId === this.platformId) {
              let rowKey = item.deviceId + "_" + item.channelId
              this.selectedRowKeys.push(rowKey);
              this.gbChooseChannel[rowKey] = item;
            }
          })
          return res.data
        }).catch(err => {
          console.log(err)
        })
      },
      selectedRowKeys: [],
      selectedRows: [],
      selections: [],
      gbChooseChannel: {},
      tableStyle: {'margin-bottom': '10px'}
    }
  },
  computed: {
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange,
        selections: this.selections
      }
    },

    formatStreamType(streamType) {
      return (streamType) => {
        if (streamType === 'proxy') return '拉流代理'
        if (streamType === 'push') return '推流'
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