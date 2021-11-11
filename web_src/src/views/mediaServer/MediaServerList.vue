<template>
  <div>
    <a-card :bordered="false">
      <div slot="title">
        流媒体节点列表
        <a-button type="primary" style="float: right" @click="$emit('goAddPage')">
          <font-awesome-icon :icon="['fas', 'plus']" style="margin-right: 0.25rem"/>
          添加节点
        </a-button>
      </div>
      <a-list :grid="{gutter: 5, column: 4 }" :data-source="mediaServerList">
        <a-list-item slot="renderItem" slot-scope="item, index" :key="item.id">
          <a-card hoverable style="width: 90%" :body-style="{'padding-top':0, 'padding-bottom': 0}">
            <img
              slot="cover"
              alt="zlm logo"
              src="@/assets/zlm-logo.png"
              style="border-bottom: 1px solid lightgray"
            />
            <a-card-meta :style="{'padding' : 0,'margin' : 0}">
              <div slot="description">
                <a-space direction="vertical" :style="{'margin': '10px 0'}">
                  <strong style="width: 100%; font-size: 12px">媒体ID: {{ item.id }}</strong>
                  <strong style="width: 100%; font-size: 12px">IP地址: {{ item.ip }}</strong>
                  <strong style="width: 100%;font-size: 12px">创建时间: {{ item.createTime }}</strong>
                </a-space>
              </div>
              <div slot="avatar" :style="{'color': item.status?'lightgreen':'lightgray'}">
                <div :style="{'margin-top': '25px'}">
                  <font-awesome-icon :icon="['fas','circle']" style="margin-left: 0.25rem; font-size: 18px"/>
                  <p>{{ item.status ? '在线' : '离线' }}</p>
                </div>
              </div>
            </a-card-meta>
            <template slot="actions" class="ant-card-actions">
              <a-icon key="edit" type="edit" @click="$emit('goEditPage', item)"/>
              <a-icon key="delete" type="delete" @click="deleteServer(item)"/>
              <a-icon key="ellipsis" type="ellipsis" @click="$emit('goDetailPage', item)"/>
            </template>
          </a-card>
        </a-list-item>
      </a-list>
    </a-card>
  </div>

</template>

<script>
import {deleteServer, getMediaServerList} from "@/api/mediaServer";

export default {
  name: "MediaServerIndex",
  data() {
    return {
      mediaServerList: []
    }
  },
  mounted() {
    this.initData()
  },
  methods: {
    initData() {
      getMediaServerList().then(res => {
        if (res.code === 0) {
          console.log(res)
          this.mediaServerList = res.data
        } else {
          this.$message.error('获取流媒体服务列表失败')
        }
      }).catch(err => {
        this.$message.error('获取流媒体服务列表失败：' + err)
      })
    },
    deleteServer(row) {
      let self = this
      this.$confirm({
        title: '确定删除此节点？',
        content: '点击确定删除此节点',
        onOk() {
          deleteServer({id: row.id}).then(res => {
            if (res.code === 0) {
              self.$message.success('删除成功')
              self.initData()
            } else {
              self.$message.error('删除失败')
            }
          }).catch(err => {
            self.$message.error('删除失败：' + err)
          })
        },
        onCancel() {}
      })
    }
  }
}
</script>

<style scoped>

</style>