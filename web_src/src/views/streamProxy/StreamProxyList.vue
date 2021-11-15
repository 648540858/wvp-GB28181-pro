<template>
  <div>
    <a-card :bordered="false" :bodyStyle="tableStyle">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row style="float: left">
            <a-col>
              <a-button type="primary" @click="addStreamProxy">
                <font-awesome-icon :icon="['fas', 'cogs']" style="margin-right: 0.25rem"/>
                添加代理
              </a-button>
            </a-col>
          </a-row>
          <a-row type="flex" :gutter="[30,0]" style="float: right">
            <a-col :flex="1">
              <a-form-item label="关键字查询">
                <a-input v-model="queryParam.query" placeholder="输入关键字查询" style="width: 20rem"/>
              </a-form-item>
            </a-col>
            <a-col :flex="1">
              <a-form-item label="是否启用">
                <a-select v-model="queryParam.enable" placeholder="请选择" style="width: 20rem;">
                  <a-select-option value="0">未启用</a-select-option>
                  <a-select-option value="1">已启用</a-select-option>
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
         <span slot="url" slot-scope="text, record">
           <a-tag color="green">
             <a-icon type="copy"
                     v-clipboard:copy="text"
                     v-clipboard:success="()=>$message.success('成功拷贝到粘贴板')"
                     v-clipboard:error="()=>{$message.error('复制失败')}"/>
             <ellipsis :length="20" tooltip>
               {{ text }}
             </ellipsis>
           </a-tag>
        </span>
        <span slot="gbId" slot-scope="text, record">
          {{ text ? text : '未知' }}
        </span>
        <span slot="enable" slot-scope="text, record">
          <a-tag :color="text ? 'green' : 'gold'">
             {{ text ? '已启用' : '未启用' }}
          </a-tag>
        </span>
        <span slot="enable_hls" slot-scope="text, record">
          <a-tag :color="text ? 'green' : 'gold'">
             {{ text ? '已启用' : '未启用' }}
          </a-tag>
        </span>
        <span slot="enable_mp4" slot-scope="text, record">
          <a-tag :color="text ? 'green' : 'gold'">
             {{ text ? '已启用' : '未启用' }}
          </a-tag>
        </span>
        <span slot="action" slot-scope="text, record">
           <a-button-group>
             <a-button size="small" type="primary" v-if="record.enable" @click="play(record)">
               <font-awesome-icon :icon="['fas','play']" style="margin-right: 0.25rem"/>
               播放
             </a-button>
             <a-button size="small" v-if="record.enable" @click="stop(record)">
               <font-awesome-icon :icon="['fas','ban']" style="margin-right: 0.25rem"/>
               停用
             </a-button>
             <a-button size="small" type="primary" :loading="startBtnLoading" v-if="!record.enable"
                       @click="start(record)">
               <font-awesome-icon :icon="['fas','check']" style="margin-right: 0.25rem"/>
               启用
             </a-button>
             <a-button size="small" type="danger" @click="deleteStreamProxy(record)">
               <font-awesome-icon :icon="['fas','trash']" style="margin-right: 0.25rem"/>
               删除
             </a-button>
           </a-button-group>
        </span>
      </s-table>
    </a-card>
    <player-dialog ref="playerDialog"/>
    <stream-proxy-edit ref="streamProxyEdit" @refreshTable='refreshTable'/>
  </div>
</template>

<script>
import {Ellipsis, STable} from '@/components'
import {deleteProxy, getStreamInfoByAppAndStream, getStreamProxyList, startProxy, stop} from "@/api/streamProxy";
import StreamProxyEdit from "@/views/streamProxy/StreamProxyEdit";
import PlayerDialog from "@/views/streamProxy/PlayerDialog";
import {generateUUID} from "ant-design-vue/lib/vc-select/util";

const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '流应用名',
    dataIndex: 'app',
    align: 'center'
  },
  {
    title: '流ID',
    dataIndex: 'stream',
    align: 'center'
  },
  {
    title: '流地址',
    dataIndex: 'url',
    align: 'center',
    scopedSlots: {customRender: 'url'}
  },
  {
    title: '流媒体',
    dataIndex: 'mediaServerId',
    align: 'center'
  },
  {
    title: '类型',
    dataIndex: 'type',
    align: 'center'
  },
  {
    title: '国标编码',
    dataIndex: 'gbId',
    align: 'center',
    scopedSlots: {customRender: 'gbId'}
  },
  {
    title: '状态',
    dataIndex: 'enable',
    align: 'center',
    scopedSlots: {customRender: 'enable'}
  },
  {
    title: '转HLS',
    dataIndex: 'enable_hls',
    align: 'center',
    scopedSlots: {customRender: 'enable_hls'}
  },
  {
    title: 'MP4录制',
    dataIndex: 'enable_mp4',
    align: 'center',
    scopedSlots: {customRender: 'enable_mp4'}
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
    StreamProxyEdit,
    PlayerDialog
  },
  data() {
    this.columns = columns
    return {
      tableStyle: {'padding-bottom': '0px', 'margin-bottom': '10px'},
      // 查询参数
      queryParam: {},
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getStreamProxyList(requestParameters).then(res => {
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
    play(row) {
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
    stop(row) {
      stop({app: row.app, stream: row.stream}).then(res => {
        this.$message.info(res)
        this.refreshTable()
      }).catch(error => {
        console.log(error);
      });
    },
    start(row) {
      this.startBtnLaoding = true;
      startProxy({app: row.app, stream: row.stream}).then(res => {
        this.startBtnLaoding = false;
        if ('success' === res) {
          this.refreshTable()
        } else {
          this.$message.error('保存失败，请检查地址是否可用！')
        }
      }).catch(error => {
        console.log(error);
        this.startBtnLoading = false;
      });
    },
    addStreamProxy() {
      this.$refs.streamProxyEdit.open(null)
    },
    refreshTable() {
      this.$refs.table.refresh()
    },
    deleteStreamProxy(row) {
      let self = this
      this.$confirm({
        title: '删除代理',
        content: '确定删除此条代理？点击确定删除',
        onOk() {
          deleteProxy({app: row.app, stream: row.stream}).then(res => {
            if (res.code === 0) {
              self.$message.success('删除成功')
              self.refreshTable()
            } else {
              self.$message.error('删除失败: ' + res.msg)
            }
          }).catch(err => {
            console.log(err)
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