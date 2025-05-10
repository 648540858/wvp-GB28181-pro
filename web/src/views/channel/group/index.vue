<template>
  <div id="region" class="app-container" style="height: calc(100vh - 118px);display: grid; grid-template-columns: 400px auto">
    <GroupTree
      ref="groupTree"
      :show-header="true"
      :edit="true"
      :click-event="treeNodeClickEvent"
      :on-channel-change="onChannelChange"
      :enable-add-channel="true"
      :add-channel-to-group="addChannelToGroup"
    />
    <div style="padding: 0 20px">
      <el-form :inline="true" size="mini">
        <el-form-item>
          <el-breadcrumb v-if="regionParents.length > 0" separator="/" style="display: ruby">
            <el-breadcrumb-item v-for="key in regionParents" :key="key">{{ key }}</el-breadcrumb-item>
          </el-breadcrumb>
          <div v-else style="color: #00c6ff">未选择虚拟组织</div>
        </el-form-item>
        <div style="float: right;">
          <el-form-item label="搜索">
            <el-input
              v-model="searchSrt"
              style="margin-right: 1rem; width: 10rem; "
              placeholder="关键字"
              prefix-icon="el-icon-search"
              clearable
              @input="search"
            />
          </el-form-item>
          <el-form-item label="在线状态">
            <el-select
              v-model="online"
              style="width: 8rem; margin-right: 1rem;"
              placeholder="请选择"
              default-first-option
              @change="search"
            >
              <el-option label="全部" value="" />
              <el-option label="在线" value="true" />
              <el-option label="离线" value="false" />
            </el-select>
          </el-form-item>
          <el-form-item label="类型">
            <el-select
              v-model="channelType"
              style="width: 8rem; margin-right: 1rem;"
              placeholder="请选择"
              default-first-option
              @change="getChannelList"
            >
              <el-option label="全部" value="" />
              <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="add()">
              添加通道
            </el-button>
            <el-button :disabled="multipleSelection.length === 0" type="danger" @click="remove()">
              移除通道
            </el-button>
            <el-button plain type="warning" @click="showUnusualChanel()">
              异常挂载通道
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button icon="el-icon-refresh-right" circle @click="getChannelList()" />
          </el-form-item>
        </div>

      </el-form>
      <el-table
        ref="channelListTable"
        size="medium"
        :data="channelList"
        height="calc(100vh - 190px)"
        style="width: 100%"
        header-row-class-name="table-header"
        @selection-change="handleSelectionChange"
        @row-dblclick="rowDblclick"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="gbName" label="名称" min-width="180" />
        <el-table-column prop="gbDeviceId" label="编号" min-width="180" />
        <el-table-column prop="gbManufacturer" label="厂家" min-width="100" />
        <el-table-column label="类型" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" effect="plain" type="success" :style="$channelTypeList[scope.row.dataType].style">{{ $channelTypeList[scope.row.dataType].name }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.gbStatus === 'ON'" size="medium">在线</el-tag>
              <el-tag v-if="scope.row.gbStatus !== 'ON'" size="medium" type="info">离线</el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </div>

    <GbChannelSelect ref="gbChannelSelect" data-type="group" />
    <UnusualGroupChannelSelect ref="unusualGroupChannelSelect" />
  </div>
</template>

<script>
import GroupTree from '../../common/GroupTree.vue'
import GbChannelSelect from '../../dialog/GbChannelSelect.vue'
import UnusualGroupChannelSelect from '../../dialog/UnusualGroupChannelSelect.vue'

export default {
  name: 'Group',
  components: {
    GbChannelSelect,
    UnusualGroupChannelSelect,
    GroupTree
  },
  data() {
    return {
      channelList: [],
      searchSrt: '',
      channelType: '',
      online: '',
      hasGroup: 'false',
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      loadSnap: {},
      groupDeviceId: '',
      groupId: '',
      businessGroup: '',
      regionParents: [],
      multipleSelection: []
    }
  },

  created() {
    this.initData()
  },
  destroyed() {
  },
  methods: {
    initData: function() {
      this.getChannelList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.initData()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getChannelList()
    },
    getChannelList: function() {
      this.$store.dispatch('commonChanel/getParentList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        online: this.online,
        channelType: this.channelType,
        groupDeviceId: this.groupDeviceId
      })
        .then(data => {
          this.total = data.total
          this.channelList = data.list
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    rowDblclick: function(row, rowIndex) {

    },
    add: function(row) {
      if (this.regionDeviceId === '') {
        this.$message.info({
          showClose: true,
          message: '请选择左侧虚拟组织节点'
        })
        return
      }
      this.$refs.gbChannelSelect.openDialog((data) => {
        console.log('选择的数据')
        console.log(data)
        this.addChannelToGroup(this.groupDeviceId, this.businessGroup, data)
      })
    },
    addChannelToGroup: function(groupDeviceId, businessGroup, data) {
      if (data.length === 0) {
        return
      }
      const channels = []
      for (let i = 0; i < data.length; i++) {
        channels.push(data[i].gbId)
      }
      this.loading = true
      this.$store.dispatch('commonChanel/addToGroup', {
        parentId: groupDeviceId,
        businessGroup: businessGroup,
        channelIds: channels
      })
        .then(data => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
          this.getChannelList()
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          this.loading = false
        })
    },
    remove: function(row) {
      const channels = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channels.push(this.multipleSelection[i].gbId)
      }
      if (channels.length === 0) {
        this.$message.info({
          showClose: true,
          message: '请选择通道'
        })
        return
      }
      this.loading = true
      this.$store.dispatch('commonChanel/deleteFromGroup', channels)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
          this.getChannelList()
          // 刷新树节点
          this.$refs.groupTree.refresh(this.groupDeviceId)
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          this.loading = false
        })
    },
    getSnap: function(row) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      return ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + '/api/device/query/snap/' + this.deviceId + '/' + row.deviceId
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    refresh: function() {
      this.initData()
    },
    treeNodeClickEvent: function(group) {
      if (group.deviceId === '' || group.deviceId === group.businessGroup) {
        this.channelList = []
        this.regionParents = []
        this.$message.info({
          showClose: true,
          message: '当前为业务分组，挂载通道请选择其下的虚拟组织，如不存在可右键新建'
        })
        return
      }
      this.groupDeviceId = group.deviceId
      this.businessGroup = group.businessGroup
      this.initData()
      this.$store.dispatch('group/getPath', {
        deviceId: this.groupDeviceId,
        businessGroup: this.businessGroup
      })
        .then(data => {
          const path = []
          for (let i = 0; i < data.length; i++) {
            path.push(data[i].name)
          }
          this.regionParents = path
        })
    },
    onChannelChange: function(deviceId) {
      //
    },
    showUnusualChanel: function() {
      this.$refs.unusualGroupChannelSelect.openDialog()
    }
  }
}
</script>
