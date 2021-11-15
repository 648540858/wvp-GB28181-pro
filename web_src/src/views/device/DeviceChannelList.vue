<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row type="flex" :gutter="[16, 0]">
            <a-col :flex="1">
              <a-form-item label="搜索设备">
                <a-input v-model="queryParam.query" placeholder="关键字: 通道名称/通道ID" style="width: 20rem"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="通道类型">
                <a-select v-model="queryParam.channelType" placeholder="请选择" default-value="" style="width: 20rem">
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="false">设备</a-select-option>
                  <a-select-option value="true">子目录</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="在线状态">
                <a-select v-model="queryParam.online" placeholder="请选择" default-value="" style="width: 20rem">
                  <a-select-option value="0">全部</a-select-option>
                  <a-select-option value="true">在线</a-select-option>
                  <a-select-option value="false">离线</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col>
              <span class="table-page-search-submitButtons">
                <a-button type="primary" @click="$refs.table.refresh(true)">查询</a-button>
                <a-button style="margin: 0 8px " @click="() => this.queryParam = {}">重置</a-button>
                <a-button @click="handleGoBack">返回</a-button>
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
        :rowKey="(record) => record.channelId"
        :columns="columns"
        :data="loadData"
        showPagination="auto"
      >
        <span slot="hasAudio" slot-scope="text, record">
          <a-switch checked-children="开"
                    un-checked-children="关"
                    :checked="text"
                    v-model="record.hasAudio"
                    @change="updateChannel(record)"/>
        </span>
        <span slot="status" slot-scope="text">
          <a-badge :status="text | statusTypeFilter" :text="text | statusFilter"/>
        </span>
        <span slot="snap" slot-scope="record">
          <a-tooltip title="点击查看大图">
            <img style="max-height: 1rem;max-width: 4rem;"
                 :id="record.deviceId+'_'+record.channelId"
                 :src="getSnap(record)"
                 @click="previewImage(record)"
                 @error="getSnapErrorEvent($event.target.id)"
                 alt="快照"/>
          </a-tooltip>
        </span>
        <span slot="action" slot-scope="text, record">
          <a-button-group>
            <a-button size="small" @click="sendDevicePush(record)">
              <font-awesome-icon :icon="['fas','play']" style="margin-right: 0.25rem;font-size: 12px"/>
              播放
            </a-button>
            <a-button size="small" type="danger" v-if="!!record.streamId" @click="stopDevicePush(record)"
                      :loading="closePushBtnLoading">
              <font-awesome-icon :icon="['fas','stop']" style="margin-right: 0.25rem;font-size: 12px"/>
              关闭流
            </a-button>
            <a-button size="small" type="primary" v-if="record.parental === 1" @click="changeSubchannel(record)">
              <font-awesome-icon :icon="['fas','info-circle']" style="margin-right: 0.25rem;font-size: 12px"/>
              查看
            </a-button>
            <a-button size="small" type="primary" @click="queryRecords(record)">
              <font-awesome-icon :icon="['fas', 'video']" style="margin-right: 0.25rem;font-size: 12px"/>
              设备录象
            </a-button>
          </a-button-group>
        </span>
      </s-table>
    </a-card>
    <dialog-player ref="dialogPlayer" @updateTable="refreshTable"></dialog-player>
  </div>
</template>

<script>
import moment from 'moment'
import {Ellipsis, STable} from '@/components'
import {getDeviceChannelList, noticePushStream, stopDevicePush, updateChannel} from "@/api/deviceList";
import dialogPlayer from "@/components/VideoPlayer/dialogPlayer";

const columns = [
  {
    title: '通道名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '快照',
    align: 'center',
    scopedSlots: {customRender: 'snap'}
  },
  {
    title: '通道编号',
    dataIndex: 'channelId',
    align: 'center'
  },
  {
    title: '子节点数',
    dataIndex: 'subCount',
    align: 'center'
  },
  {
    title: '开启音频',
    dataIndex: 'hasAudio',
    align: 'center',
    scopedSlots: {customRender: 'hasAudio'}
  },
  {
    title: '云台类型',
    dataIndex: 'ptztypeText',
    align: 'center'
  },
  {
    title: '状态',
    dataIndex: 'status',
    align: 'center',
    scopedSlots: {customRender: 'status'}
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
    Ellipsis,
    dialogPlayer
  },
  data() {
    this.columns = columns
    return {
      // create model
      visible: false,
      mdl: null,
      // 查询参数
      queryParam: {},
      // 加载数据方法必须为Promise对象
      loadData: parameter => {
        this.queryParam.deviceId = this.record.deviceId
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getDeviceChannelList(requestParameters).then(res => {
          return res.data
        })
      },
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      closePushBtnLoading: false,
      loadSnap: {}
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
  mounted() {
    setInterval(this.$refs.table.refresh, 60 * 1000)
  },
  methods: {
    handleGoBack() {
      this.$emit('goBack')
    },
    sendDevicePush(channelInfo) { //通知设备上传媒体流
      let deviceId = channelInfo.deviceId
      let channelId = channelInfo.channelId
      console.log("通知设备推流：" + deviceId + " : " + channelId);
      noticePushStream({deviceId, channelId}).then(res => {
        if (res.code === 0) {
          setTimeout(() => {
            console.log("下载截图")
            let snapId = deviceId + "_" + channelId;
            this.loadSnap[snapId] = 0;
            this.getSnapErrorEvent(snapId)
          }, 5000)
          this.$refs.dialogPlayer.openDialog(deviceId, channelId, {
            streamInfo: res.data,
            hasAudio: channelInfo.hasAudio
          });
          this.$refs.table.refresh()
        } else {
          this.$message.error(res.msg);
        }
      })
    },
    stopDevicePush(channelInfo) {
      this.closePushBtnLoading = true
      let params = {
        deviceId: channelInfo.deviceId,
        channelId: channelInfo.channelId
      }
      stopDevicePush(params).then(res => {
        console.log('停止推流：' + res)
        this.$refs.table.refresh()
        this.closePushBtnLoading = false
      }).catch(err => {
        this.closePushBtnLoading = false
        this.$refs.table.refresh()
      })
    },
    resetSearchForm() {
      this.queryParam = {
        date: moment(new Date())
      }
    },
    refreshTable() {
      this.$refs.table.refresh()
    },
    changeSubchannel(channelInfo) {
      this.$message.warn('敬请期待')
    },
    updateChannel(channelInfo) {
      console.log(channelInfo)
      updateChannel(channelInfo).then(res => {
        if (res.success) {
          this.$message.success('操作成功')
        } else {
          this.$message.error('操作失败')
        }
      }).catch(err => {
        this.$message.error('操作失败')
      })
    },
    getSnap: function (row) {
      return '/static/snap/' + row.deviceId + '_' + row.channelId + '.jpg?' + new Date().getTime()
    },
    getSnapErrorEvent(id) {
      if (typeof (this.loadSnap[id]) != "undefined") {
        console.log("下载截图" + this.loadSnap[id])
        if (this.loadSnap[id] > 5) {
          delete this.loadSnap[id];
          return;
        }
        setTimeout(() => {
          this.loadSnap[id]++
          document.getElementById(id).setAttribute("src", '/static/snap/' + id + '.jpg?' + new Date().getTime())
        }, 1000)
      }
    },
    previewImage(row) {
      window.open(this.getSnap(row))
    },
    queryRecords(row){
      this.$emit('goGBRecords', row)
    }
  }

}
</script>
