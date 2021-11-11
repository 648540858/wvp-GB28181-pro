<template>
  <div>
    <a-card :bordered="false">
      <div slot="title">
        <span>上级平台列表</span>
        <a-button size="small" type="primary" style="float: right" @click="addParentPlatform">
          <font-awesome-icon :icon="['fas', 'plus']" style="margin-right: 0.25rem"/>
          添加
        </a-button>
      </div>
      <s-table
        ref="table"
        size="default"
        :rowKey="(record) => record.id"
        :columns="columns"
        :data="loadData"
        showPagination="auto"
      >
        <span slot="enable" slot-scope="text, record">
          <a-tag :color="text ? 'green' : 'gold'">
             {{ text ? '已启用' : '未启用' }}
          </a-tag>
        </span>
        <span slot="status" slot-scope="text, record">
          <a-tag :color="text ? 'green': 'gold'">
            {{ text ? '在线' : '离线' }}
          </a-tag>
        </span>
        <span slot="serverIP" slot-scope="text, record">
          <a-tag color="blue">
            {{ text }}:{{record.serverPort}}
          </a-tag>
        </span>
        <span slot="action" slot-scope="text, record">
          <a @click="editPlatform(record)"><a-icon type="edit"/> 编辑</a>
          <a-divider type="vertical"/>
          <a-popover trigger="click">
            <template slot="content">
              <a-space direction="vertical">
                <a @click="$emit('goGbChannelPage', record.serverGBId)"><font-awesome-icon
                  :icon="['fas','angle-double-right']" style="margin-right: 0.25rem"/> 国标通道</a>
                <a @click="$emit('goStreamPage', record.serverGBId)"><font-awesome-icon
                  :icon="['fas','angle-double-right']" style="margin-right: 0.25rem"/>直播流通道</a>
              </a-space>
            </template>
            <a><a-icon type="share-alt"/> 选择通道</a>
          </a-popover>
          <a-divider type="vertical"/>
          <a @click="deletePlatform(record)"><a-icon type="delete"/> 删除</a>
        </span>
      </s-table>
    </a-card>
    <gb-platform-edit ref="gbPlatformEdit" @refreshTable='refreshTable'/>
  </div>
</template>

<script>
import {STable} from '@/components'
import {deletePlatformCommit, getPlatformList} from "@/api/gbPlatform";
import GbPlatformEdit from "@/views/gbPlatform/GbPlatformEdit";

const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    align: 'center'
  },
  {
    title: '平台编号',
    dataIndex: 'serverGBId',
    align: 'center'
  },
  {
    title: '是否启用',
    dataIndex: 'enable',
    align: 'center',
    scopedSlots: {customRender: 'enable'}
  },
  {
    title: '状态',
    dataIndex: 'status',
    align: 'center',
    scopedSlots: {customRender: 'status'}
  },
  {
    title: '地址',
    dataIndex: 'serverIP',
    align: 'center',
    scopedSlots: {customRender: 'serverIP'}
  },
  {
    title: '设备国标编号',
    dataIndex: 'deviceGBId',
    align: 'center'
  },
  {
    title: '信令传输模式',
    dataIndex: 'transport',
    align: 'center'
  },
  {
    title: '通道数',
    dataIndex: 'channelCount',
    align: 'center'
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
    GbPlatformEdit
  },
  data() {
    this.columns = columns
    return {
      // 查询参数
      queryParam: {},
      loadData: parameter => {
        const requestParameters = Object.assign({}, parameter, this.queryParam)
        return getPlatformList(requestParameters).then(res => {
          console.log(res)
          return res.data
        })
      },
      formVisible: false
    }
  },
  methods: {
    refreshTable() {
      this.$refs.table.refresh()
    },
    addParentPlatform() {
      this.$refs.gbPlatformEdit.openDialog(null)
    },
    editPlatform(row) {
      this.$refs.gbPlatformEdit.openDialog(row)
    },
    chooseChannel(row) {
      this.$refs.chooseChannelDialog.openDialog(row.serverGBId)
    },
    deletePlatform(row) {
      let self = this
      this.$confirm({
        title: '删除上级平台',
        content: '确定删除上级平台信息？点击确定删除',
        onOk() {
          deletePlatformCommit({serverGBId: row.serverGBId}).then(res => {
            if (res === "success") {
              self.$message.success('删除成功');
              self.refreshTable()
            } else {
              self.$message.error('删除失败');
            }
          }).catch(err => {
            self.$message.error('删除失败：' + err);
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