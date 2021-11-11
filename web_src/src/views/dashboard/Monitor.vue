<template>
  <div>
    <a-card style="text-align: right" size="small" :bordered="false">
      <a-space>
        <div>节点选择：
          <a-select v-model="mediaServerId" style="width: 15rem">
            <a-select-option v-for="(item,index) in mediaServerList" :key="index" :value="item.id">
              {{ item.id }}({{ item.streamIp }})
            </a-select-option>
          </a-select>
        </div>
        <a-button type="primary" @click="getMediaServerConfig">
          <font-awesome-icon :icon="['fas', 'cog']" style="margin-right: 0.25rem"/>
          媒体服务器配置
        </a-button>
        <a-button type="primary" @click="getWVPServerConfig">
          <font-awesome-icon :icon="['fas', 'cogs']" style="margin-right: 0.25rem"/>
          信令服务器配置
        </a-button>
        <a-button type="danger" @click="restartServer">
          <font-awesome-icon :icon="['fas', 'redo']" style="margin-right: 0.25rem"/>
          重启媒体服务器
        </a-button>
      </a-space>
    </a-card>
    <a-row type="flex" :gutter="[1,0]">
      <a-col :flex="1">
        <a-card title="延迟率" size="small" :bordered="false" style="margin-top: 10px">
          <div class="control-table" id="ThreadsLoad"></div>
        </a-card>
      </a-col>
      <a-col :flex="1.5">
        <a-card title="负载率" size="small" :bordered="false" style="width:100%;margin-top: 10px">
          <div class="control-table" id="WorkThreadsLoad"></div>
        </a-card>
      </a-col>
    </a-row>
    <a-card :bordered="false" size="small" title="服务器会话" style="margin-top: 10px">
      <a-table
        ref="table"
        size="default"
        :rowKey="(record) => record.id "
        :columns="columns"
        :data-source="sessions">

        <span slot="action" slot-scope="text, record">
          <a-popconfirm
            title="此操作将断开该通信链路, 是否继续?"
            @confirm="deleteRow(record)"
          >
            <a href="#">移除</a>
          </a-popconfirm>
        </span>

      </a-table>
    </a-card>
  </div>
</template>

<script>
import echarts from 'echarts'
import {
  deleteSession,
  getAllSession,
  getOnlineMediaServerList,
  getServerConfig,
  getThreadsLoad,
  getWVPServerConfig,
  restartServer
} from "@/api/controller";
import moment from "moment";

const columns = [
  {
    title: '远端',
    dataIndex: 'peer_ip',
    align: 'center'
  },
  {
    title: '本地',
    dataIndex: 'local_ip',
    align: 'center'
  },
  {
    title: '类型',
    dataIndex: 'typeid',
    align: 'center'
  },
  {
    title: '查询时间',
    dataIndex: 'time',
    align: 'center'
  },
  {
    title: '操作',
    dataIndex: 'action',
    align: 'center',
    scopedSlots: {customRender: 'action'}
  }
]
export default {
  components: {},
  data() {
    this.columns = columns
    return {
      delayChartOption: {
        // legend: {},
        xAxis: {},
        yAxis: {},
        label: {},
        tooltip: {},
        dataZoom: [],
        series: []
      },
      loadChartOption: {
        // legend: {},
        xAxis: {},
        yAxis: {},
        label: {},
        tooltip: {},
        series: []
      },
      delayChart: null,
      loadChart: null,
      charZoomStart: 0,
      charZoomEnd: 100,
      chartInterval: 0, //更新图表统计图定时任务标识
      mediaServerList: [],
      mediaServerId: '',
      sessions: [],
      loadData: []
    }
  },
  created() {
  },
  mounted() {
    this.initChart()
    this.chartInterval = setInterval(this.getAllData, 10 * 1000);
    this.getMediaServerList()
  },
  methods: {
    restartServer() {
      const self = this
      this.$confirm({
        title: '重启媒体服务器',
        content: '此操作将重启媒体服务器, 是否继续?',
        onOk() {
          restartServer({mediaServerId: self.mediaServerId}).then(res => {
            console.log(res)
            self.getTableData();
            if (res.code === 0) {
              self.$message.success('操作完成');
            } else {
              self.$message.error('重启失败');
            }
          }).catch(err => {
            self.$message.error('重启失败：' + err);
          })
        },
        onCancel() {
        }
      })
    },
    getWVPServerConfig() {
      getWVPServerConfig().then(res => {
        console.log(res)
        if (res.code === 0) {
          this.$emit('goSipPage', res.data)
        } else {
          this.$message.error('获取信息失败')
        }
      }).catch(err => {
        this.$message.error('发生错误：' + err)
      })
    },
    getMediaServerConfig() {
      getServerConfig({mediaServerId: this.mediaServerId}).then(res => {
        if (res.code === 0) {
          this.$emit('goMediaPage', res.data[0])
        } else {
          this.$message.error('获取信息失败')
        }
      }).catch(err => {
        this.$message.error('发生错误：' + err)
      })
    },
    initChart() {
      this.delayChartOption.xAxis = {
        type: 'category',
        data: []// x轴数据
      };
      this.delayChartOption.yAxis = {
        type: 'value',
        boundaryGap: [0, '100%'],
        max: 100,
        axisLabel: {
          show: true,
          interval: 'auto',
          formatter: '{value} %'
        }
      };
      this.delayChartOption.dataZoom = [{
        show: true,
        start: this.charZoomStart,
        end: this.charZoomEnd
      }];
      let chartContainer = document.getElementById('ThreadsLoad')
      this.delayChart = echarts.init(chartContainer);
      this.delayChart.setOption(this.delayChartOption);
      this.delayChart.on('dataZoom', function (event) {
        if (event.batch) {
          this.charZoomStart = event.batch[0].start;
          this.charZoomEnd = event.batch[0].end;
        } else {
          this.charZoomStart = event.start;
          this.charZoomEnd = event.end;
        }
      });

      this.loadChartOption.xAxis = {
        type: 'category',
        data: [] // x轴数据
      };
      this.loadChartOption.yAxis = {
        type: 'value',
        boundaryGap: [0, '100%'],
        max: 100,
        axisLabel: {
          show: true,
          interval: 'auto',
          formatter: '{value} %'
        }
      };
      this.loadChartOption.dataZoom = [{
        show: true,
        start: this.charZoomStart,
        end: this.charZoomEnd
      }];
      this.loadChart = echarts.init(document.getElementById('WorkThreadsLoad'));
      this.loadChart.setOption(this.loadChartOption);
      this.loadChart.on('dataZoom', function (event) {
        if (event.batch) {
          this.charZoomStart = event.batch[0].start;
          this.charZoomEnd = event.batch[0].end;
        } else {
          this.charZoomStart = event.start;
          this.charZoomEnd = event.end;
        }
      });
    },
    getMediaServerList() {
      getOnlineMediaServerList().then(res => {
        this.mediaServerList = res.data;
        if (this.mediaServerList.length > 0) {
          this.mediaServerId = this.mediaServerList[0].id
          this.getAllData()
        } else {
          this.$message.error('获取媒体服务器列表失败')
        }
      })
    },
    getAllData() {
      this.getTableData()
      this.getThreadsLoad()
    },
    getTableData() {
      getAllSession({mediaServerId: this.mediaServerId}).then(res => {
        if (res.code === 0) {
          this.sessions = res.data
          this.sessions.forEach(item => {
            item.local_ip = item.local_ip + ':' + item.local_port
            item.peer_ip = item.peer_ip + ':' + item.peer_port
            item.time = moment(new Date()).format('YYYY-MM-DD HH:mm:ss')
          })
        }
      })
    },
    getThreadsLoad() {
      getThreadsLoad({mediaServerId: this.mediaServerId}).then(res => {
        if (res.code === 0) {
          this.delayChartOption.xAxis.data.push(new Date().toLocaleTimeString('chinese', {
            hour12: false
          }));
          this.loadChartOption.xAxis.data.push(new Date().toLocaleTimeString('chinese', {
            hour12: false
          }));
          for (let i = 0; i < res.data.length; i++) {
            if (this.delayChartOption.series[i] === undefined) {
              let delayData = {
                data: [],
                type: 'line'
              };
              let loadData = {
                data: [],
                type: 'line'
              };
              delayData.data.push(res.data[i].delay);
              loadData.data.push(res.data[i].load);
              this.delayChartOption.series.push(delayData);
              this.loadChartOption.series.push(loadData);
            } else {
              this.delayChartOption.series[i].data.push(res.data[i].delay);
              this.loadChartOption.series[i].data.push(res.data[i].load);
            }
          }
          this.delayChartOption.dataZoom[0].start = this.charZoomStart;
          this.delayChartOption.dataZoom[0].end = this.charZoomEnd;
          this.loadChartOption.dataZoom[0].start = this.charZoomStart;
          this.loadChartOption.dataZoom[0].end = this.charZoomEnd;
          this.delayChart.setOption(this.delayChartOption, true);
          this.loadChart.setOption(this.loadChartOption, true);
        }

      })
    },
    deleteRow(row) {
      deleteSession({mediaServerId: this.mediaServerId, id: row.id}).then(res => {
        this.$message.success('删除成功')
        this.$refs.table.refresh()
      }).catch(err => {
        console.log(err)
        this.$message.error('删除失败')
      })
    }
  },
  destroyed() {
    clearInterval(this.chartInterval); //释放定时任务
    if (this.delayChart) this.delayChart.dispose() //销毁charts
    if (this.loadChart) this.loadChart.dispose()
  }
}

</script>

<style scoped>
.control-table {
  height: 20rem;
  width: 115%;
}
</style>