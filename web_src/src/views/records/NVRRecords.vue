<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row type="flex" :gutter="[16,0]" style="float: left">
            <a-col :flex="1">
              <a-form-item label="时间">
                <a-date-picker placeholder="请选择时间" style="width: 25rem" @change="dateChange"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="流名称">
                <a-input v-model="queryParam.stream" placeholder="请输入流名称, 支持模糊查询" style="width: 25rem;"/>
              </a-form-item>
            </a-col>
            <a-col>
              <span>
                <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
                <a-button style="margin-left: 8px;margin-right: 8px" @click="() => this.queryParam = {}">重置</a-button>
                <span style="margin-left: 10rem"><font-awesome-icon :icon="['fas', 'info-circle']" style="margin-right: 0.25rem;color: red"/>列表记录的是NVR录像缓存本地的文件</span>
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
        showPagination="auto"
      >
        <span slot="action" slot-scope="text, record">
          <a @click="goDetailPage(record)">录像详情</a>
        </span>
      </s-table>
    </a-card>
  </div>
</template>

<script>
import {STable} from '@/components'
import {getOnlineMediaServerList, getRecordList, resetRecordList} from "@/api/recordList";

const columns = [
  {
    title: '时间',
    dataIndex: 'time',
    align: 'center'
  },
  {
    title: '流名称',
    dataIndex: 'stream',
    align: 'center'
  },
  {
    title: '应用名称',
    dataIndex: 'app',
    align: 'center'
  },
  {
    title: '操作',
    dataIndex: 'action',
    align: 'center',
    scopedSlots: {customRender: 'action'}
  }]
export default {
  name: "NVRRecords",
  components: {
    STable
  },
  data() {
    this.columns = columns
    return {
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      // 查询参数
      queryParam: {},
      mediaServerList: [],
      mediaServerId: '',
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getOnlineMediaServerList(requestParameters).then(res => {
          this.mediaServerList = res.data;
          if (this.mediaServerList.length > 0) {
            this.mediaServerId = this.mediaServerList[0].id
          }
        }).then(() => {
          return getRecordList({mediaServerId: this.mediaServerId})
        }).then(res => {
          requestParameters.list = res.data
          requestParameters.NVR = 'NVR'
          return resetRecordList(requestParameters)
        }).then(res => {
          return res.data
        })
      }
    }
  },
  mounted() {

  },
  methods: {
    handleChange(val){
      this.$refs.table.refresh()
    },
    dateChange(date, dateStr){
      this.queryParam.time = dateStr
    },
    goDetailPage(record){
      record.mediaServerId = this.mediaServerId
      this.$emit('goDetailPage', record)
    }
  }
}
</script>

<style scoped>

</style>