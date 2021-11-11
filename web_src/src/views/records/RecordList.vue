<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row :gutter="48">
            <a-col :md="8" :sm="24">
              <a-form-item label="品牌">
                <a-input v-model="queryParam.manufacturer" placeholder="设备品牌，支持模糊查询"/>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item label="通道名">
                <a-input v-model="queryParam.name" placeholder="通道名称，支持模糊查询"/>
              </a-form-item>
            </a-col>
            <template v-if="advanced">
              <a-col :md="8" :sm="24">
                <a-form-item label="设备ID">
                  <a-input v-model="queryParam.deviceId" placeholder="设备编号，支持模糊查询"/>
                </a-form-item>
              </a-col>
            </template>
            <a-col :md="!advanced && 8 || 24" :sm="24">
              <span class="table-page-search-submitButtons"
                    :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
                <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
                <a-button style="margin-left: 8px;margin-right: 8px" @click="() => this.queryParam = {}">重置</a-button>
                <a @click="toggleAdvanced" style="margin-left: 8px">
                  {{ advanced ? '收起' : '展开' }}
                  <a-icon :type="advanced ? 'up' : 'down'"/>
                </a>
              </span>
            </a-col>
          </a-row>
        </a-form>
      </div>
    </a-card>

    <a-card :bordered="false">
      <div class="table-operator">
        录像节点：
        <a-select v-model="mediaServerId" style="width: 200px" @change="handleChange">
          <a-select-option v-for="(item,index) in mediaServerList" :key="index" :value="item.id">
            {{ item.id }}
          </a-select-option>
        </a-select>
      </div>

      <s-table
        ref="table"
        size="default"
        rowKey="key"
        :columns="columns"
        :data="loadData"
        :rowSelection="rowSelection"
        showPagination="auto"
      >
        <span slot="action" slot-scope="text, record">
          <a @click="goDetailPage(record)">录像详情</a>
        </span>
      </s-table>
    </a-card>
    <dialog-player ref="dialogPlayer"></dialog-player>
  </div>
</template>

<script>
import moment from 'moment'
import {STable} from '@/components'
import dialogPlayer from "@/components/VideoPlayer/dialogPlayer";
import {getOnlineMediaServerList, getRecordList, resetRecordList} from "@/api/recordList";

const columns = [
  {
    title: '通道名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '品牌',
    dataIndex: 'manufacturer',
    align: 'center'
  },
  {
    title: '时间',
    dataIndex: 'time',
    align: 'center'
  },
  {
    title: '地址',
    dataIndex: 'hostAddress',
    align: 'center'
  },
  {
    title: '设备ID',
    dataIndex: 'deviceId',
    align: 'center'
  },
  {
    title: '通道ID',
    dataIndex: 'channelId',
    align: 'center'
  },
  {
    title: '操作',
    dataIndex: 'action',
    align: 'center',
    scopedSlots: {customRender: 'action'}
  }]

const statusMap = {
  0: {
    status: 'default',
    text: '关闭'
  },
  1: {
    status: 'success',
    text: '开启'
  }
}

export default {
  name: 'DeviceChannelList',
  props: {
    record: {
      type: [Object, String],
      default: ''
    }
  },
  components: {
    STable,
    dialogPlayer
  },
  data() {
    this.columns = columns
    return {
      // create model
      visible: false,
      mdl: null,
      // 高级搜索 展开/关闭
      advanced: false,
      // 查询参数
      queryParam: {},
      // 加载数据方法必须为Promise对象
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getOnlineMediaServerList(requestParameters)
          .then(res => {
            this.mediaServerList = res.data;
            if (this.mediaServerList.length > 0) {
              this.mediaServerId = this.mediaServerList[0].id
            }
          })
          .then(() => {
            return getRecordList({mediaServerId: this.mediaServerId})
          })
          .then(res => {
            requestParameters.list = res.data
            requestParameters.NVR = 'deviceChannel'
            return resetRecordList(requestParameters)
          }).then(res => {
            return res.data
          })
      },
      selectedRowKeys: [],
      selectedRows: [],
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      mediaServerList: [],
      mediaServerId: ''
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
    goDetailPage(record) {
      record.mediaServerId = this.mediaServerId
      this.$emit('goDetailPage', record)
    },

    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys
      this.selectedRows = selectedRows
    },
    toggleAdvanced() {
      this.advanced = !this.advanced
    },
    resetSearchForm() {
      this.queryParam = {
        date: moment(new Date())
      }
    },
    handleChange(val){
      this.$refs.table.refresh()
    }
  }
}
</script>
