<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row type="flex" :gutter="[10,0]" style="float: left">
            <a-col :flex="1">
              <a-form-item label="时间">
                <a-range-picker showTime style="width: 20rem" @change="dateChange"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="报警级别">
                <a-select v-model="queryParam.alarmPriority" placeholder="选择报警级别" style="width: 15rem;">
                  <a-select-option value="1">一级警情</a-select-option>
                  <a-select-option value="2">二级警情</a-select-option>
                  <a-select-option value="3">三级警情</a-select-option>
                  <a-select-option value="4">四级警情</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="报警方式">
                <a-select v-model="queryParam.alarmMethod" placeholder="选择报警方式" style="width: 15rem;">
                  <a-select-option value="1">电话报警</a-select-option>
                  <a-select-option value="2">设备报警</a-select-option>
                  <a-select-option value="3">短信报警</a-select-option>
                  <a-select-option value="4">GPS报警</a-select-option>
                  <a-select-option value="5">视频报警</a-select-option>
                  <a-select-option value="6">设备故障报警</a-select-option>
                  <a-select-option value="7">其他报警</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col>
              <span>
                <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
                <a-button style="margin-left: 8px;margin-right: 8px" @click="() => this.queryParam = {}">重置</a-button>
                <span style="margin-left: 10rem"><font-awesome-icon :icon="['fas', 'info-circle']"
                                                                    style="margin-right: 0.25rem;color: red"/>列表记录的是GB28181报警事件</span>
              </span>
            </a-col>
          </a-row>
        </a-form>
      </div>
    </a-card>

    <a-card :bordered="false">
      <s-table
        ref="table"
        size="default"
        :rowKey="(record) => record.id"
        :columns="columns"
        :data="loadData"
        showPagination="auto"
      >
         <span slot="action" slot-scope="text, record">
          <a @click="deleteRow(record)">删除</a>
        </span>

        <span slot="alarmMethod" slot-scope="text, record">
          {{ formatAlarmMethod(record) }}
        </span>

        <span slot="alarmPriority" slot-scope="text, record">
          <a-tag :color="calcAlarmPriorityColor(record)">
            {{ formatAlarmPriority(record) }}
          </a-tag>
        </span>

        <span slot="alarmDescription" slot-scope="text, record">
           <ellipsis :length="10" tooltip>{{ text }}</ellipsis>
        </span>
      </s-table>
    </a-card>
  </div>
</template>

<script>
import {Ellipsis, STable} from '@/components'
import {deleteWarning, getWarningList} from "@/api/warning";

const columns = [
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
    title: '报警级别',
    dataIndex: 'alarmPriority',
    align: 'center',
    scopedSlots: {customRender: 'alarmPriority'}
  },
  {
    title: '报警方式',
    dataIndex: 'alarmMethod',
    align: 'center',
    scopedSlots: {customRender: 'alarmMethod'}
  },
  {
    title: '报警时间',
    dataIndex: 'alarmTime',
    align: 'center'
  },
  {
    title: '报警内容描述',
    dataIndex: 'alarmDescription',
    align: 'center',
    scopedSlots: {customRender: 'alarmDescription'}
  },
  {
    title: '报警类型',
    dataIndex: 'alarmType',
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
    STable,
    Ellipsis
  },
  data() {
    this.columns = columns
    return {
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      // 查询参数
      queryParam: {},
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getWarningList(requestParameters).then(res => {
          return res.data
        })
      }
    }
  },
  mounted() {
  },
  methods: {
    calcAlarmPriorityColor(row) {
      let color = ''
      switch (row.alarmPriority) {
        case '1':
          color = '#FF9933'
          break;
        case '2':
          color = '#FF6633'
          break;
        case '3':
          color = '#FF0066'
          break;
        case '4':
          color = '#FF0033'
          break;
      }
      return color
    },
    formatAlarmPriority(row) {
      let sRt = '未知'
      switch (row.alarmPriority) {
        case '1':
          sRt = '一级警情'
          break;
        case '2':
          sRt = '二级警情'
          break;
        case '3':
          sRt = '三级警情'
          break;
        case '4':
          sRt = '四级警情'
          break;
      }
      return sRt
    },
    formatAlarmMethod(row) {
      let sRt = '未知'
      switch (row.alarmMethod) {
        case '1':
          sRt = '电话报警'
          break;
        case '2':
          sRt = '设备报警'
          break;
        case '3':
          sRt = '短信报警'
          break;
        case '4':
          sRt = 'GPS报警'
          break;
        case '5':
          sRt = '视频报警'
          break;
        case '6':
          sRt = '设备故障报警'
          break;
        case '7':
          sRt = '其他报警'
          break;
      }
      return sRt
    },
    dateChange(date, dateStr) {
      this.queryParam.startTime = dateStr[0]
      this.queryParam.endTime = dateStr[1]
    },
    deleteRow(row) {
      let self = this
      this.$confirm({
        title: '删除报警记录',
        content: '确定删除此条报警记录？点击确定删除',
        onOk() {
          let params = {id: row.id}
          deleteWarning(params).then(res => {
            if (res.code === 0) {
              self.$message.success('删除成功')
              self.$refs.table.refresh()
            } else {
              self.$message.error('删除失败')
            }
          }).catch(err => {
            self.$message.error('删除失败：' + err)
          })
        },
        onCancel() {
        }
      })
    }
  }
}
</script>

<style scoped>

</style>