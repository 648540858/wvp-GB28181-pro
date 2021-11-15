<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row type="flex" :gutter="[30,0]" style="float: right">
            <a-col :flex="1">
              <a-form-item label="关键字查询">
                <a-input v-model="queryParam.query" placeholder="输入关键字查询" style="width: 20rem"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="是否推流">
                <a-select v-model="queryParam.online" placeholder="请选择" style="width: 20rem;">
                  <a-select-option value="false">未推流</a-select-option>
                  <a-select-option value="true">已推流</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col>
              <span>
                <a-button type="primary" @click="$refs.table.refresh(true)">
                  <font-awesome-icon :icon="['fas', 'search']" style="margin-right: 0.25rem"/>
                  查询
                </a-button>
                <a-button style="margin-left: 8px;margin-right: 8px" @click="() => this.queryParam = {}">
                  <font-awesome-icon :icon="['fas', 'redo']" style="margin-right: 0.25rem"/>
                  重置
                </a-button>
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
        :rowKey="uuid"
        :columns="columns"
        :data="loadData"
        showPagination="auto"
      >
        <span slot="gbId" slot-scope="text, record">
          {{ text ? text : '未知' }}
        </span>
        <span slot="status" slot-scope="text, record">
          <a-tag :color="(text == false && record.gbId == null) || text ? 'green' : 'gold'">
             {{ (text == false && record.gbId == null) || text ? '正在推流' : '未推流' }}
          </a-tag>
        </span>
        <span slot="action" slot-scope="text, record">
           <a-button-group>
             <a-button size="small" type="primary" @click="playPuhsh(record)">
               <font-awesome-icon :icon="['fas','play']" style="margin-right: 0.25rem"/>
               播放
             </a-button>
             <a-button size="small" v-if="!!record.streamId" @click="stopPuhsh(record)">
               <font-awesome-icon :icon="['fas','ban']" style="margin-right: 0.25rem"/>
               停用
             </a-button>
             <a-button size="small" type="primary" v-if="!!!record.gbId" @click="addToGB(record)">
               <font-awesome-icon :icon="['fas','trash']" style="margin-right: 0.25rem"/>
               加入国标
             </a-button>
             <a-button size="small" type="primary" v-if="!!record.gbId" @click="removeFromGB(record)">
               <font-awesome-icon :icon="['fas','trash']" style="margin-right: 0.25rem"/>
               移出国标
             </a-button>
           </a-button-group>
        </span>
      </s-table>
    </a-card>
    <player-dialog ref="playerDialog"/>
    <addStreamTOGB ref="addStreamTOGB" @refreshTable='refreshTable'/>
  </div>
</template>
<script>
import {Ellipsis, STable} from '@/components'
import {addTOGBPush, getStreamInfoByAppAndStream, getStreamPushList, removeTOGBPush} from "@/api/streamProxy";
import addStreamTOGB from '@/views/streamPush/addStreamTOGB.vue'
import PlayerDialog from "@/views/streamProxy/PlayerDialog";
import {generateUUID} from "ant-design-vue/lib/vc-select/util";

const columns = [
  {
    title: 'APP',
    dataIndex: 'app',
    align: 'center'
  },
  {
    title: '流ID',
    dataIndex: 'stream',
    align: 'center'
  },
  {
    title: '流媒体',
    dataIndex: 'mediaServerId',
    align: 'center'

  },
  {
    title: '国标编码',
    dataIndex: 'gbId',
    align: 'center',
    scopedSlots: {customRender: 'gbId'}
  },
  {
    title: '开始时间',
    dataIndex: 'createStamp',
    align: 'center'
  },
  {
    title: '正在推流',
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
export default {
  components: {
    STable,
    Ellipsis,
    PlayerDialog,
    addStreamTOGB
  },
  data() {
    this.columns = columns
    return {
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      // 查询参数
      queryParam: {},
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getStreamPushList(requestParameters).then(res => {
          console.log(res)
          return res.data
        })
      },
      formVisible: false,
      confirmLoading: false,
      startBtnLoading: false
    }
  },
  computed: {
    uuid() {
      return generateUUID
    }
  },
  methods: {

    playPuhsh(row) {
      let params = {
        app: row.app,
        stream: row.stream,
        mediaServerId: row.mediaServerId
      }
      getStreamInfoByAppAndStream(params).then(res => {
        if (res.code === 0) {
          let streamInfo = res.data
          let videoUrl = ''
          if (location.protocol === "https:") {
            if (streamInfo.wss_flv === null) {
              this.$message.error('媒体服务器未配置ssl端口');
            } else {
              videoUrl = streamInfo.wss_flv;
            }
          } else {
            videoUrl = streamInfo.ws_flv;
          }
          this.$refs.playerDialog.play(videoUrl, true);
        } else {
          this.$message.error("获取地址失败：" + res.msg)
        }
      }).catch(function (error) {
        console.log(error)
      })
    },
    removeFromGB(row){
      const requestParameters = Object.assign({},row)
      removeTOGBPush(requestParameters).then(res => {
        console.log(res)
        this.confirmLoading = false
        if (res.code === 200) {
          this.$message.success(res.message)
          this.refreshTable();
        }
      }).catch(err => {
        console.log(err)
      })
    },
    addToGB(row){
      this.$refs.addStreamTOGB.open({app: row.app, stream: row.stream, mediaServerId: row.mediaServerId},this.initData);
    },
    refreshTable() {
      this.$refs.table.refresh()
    },
    // stopPuhsh(row) {
    //   stop({app: row.app, stream: row.stream}).then(res => {
    //     this.$message.info(res)
    //     this.refreshTable()
    //   }).catch(error => {
    //     console.log(error);
    //   });
    // },
  }
}
</script>

<style scoped>

</style>