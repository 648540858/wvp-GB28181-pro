<template>
  <div id="linkChannelRecord" style="width: 100%;  background-color: #FFFFFF; display: grid; grid-template-columns: 200px auto;">
    <el-dialog v-el-drag-dialog v-if="showDialog" v-loading="dialogLoading" title="通道关联" top="2rem" width="80%" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()">
      <div style="display: grid; grid-template-columns: 100px auto;">
        <el-tabs v-model="hasLink" tab-position="left" style="" @tab-click="search">
          <el-tab-pane label="未关联" name="false" />
          <el-tab-pane label="已关联" name="true" />
        </el-tabs>
        <div>
          <el-form :inline="true" size="mini">
            <el-form-item label="搜索">
              <el-input
                v-model="searchSrt"
                style="margin-right: 1rem; width: auto;"
                placeholder="关键字"
                prefix-icon="el-icon-search"
                clearable
                @input="search"
              />
            </el-form-item>
            <el-form-item label="在线状态">
              <el-select
                v-model="online"
                size="mini"
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
                @change="search"
              >
                <el-option label="全部" value="" />
                <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <div v-if="hasLink !=='true'">
                <el-button size="mini" type="primary" @click="add()">添加</el-button>
                <el-button v-if="hasLink !=='true'" size="mini" @click="addByDevice()">按设备添加</el-button>
                <el-button v-if="hasLink !=='true'" size="mini" @click="addAll()">添加所有通道</el-button>
              </div>
              <div v-else>
                <el-button v-if="hasLink ==='true'" size="mini" type="danger" @click="remove()">移除</el-button>
                <el-button v-if="hasLink ==='true'" size="mini" @click="removeByDevice()">按设备移除</el-button>
                <el-button v-if="hasLink ==='true'" size="mini" @click="removeAll()">移除所有通道</el-button>
              </div>
            </el-form-item>
            <el-form-item style="float: right;">
              <el-button icon="el-icon-refresh-right" circle size="mini" @click="getChannelList()" />
            </el-form-item>
          </el-form>
          <el-table
            ref="channelListTable"
            size="small"
            :data="channelList"
            height="calc(100vh - 250px)"
            header-row-class-name="table-header"
            @selection-change="handleSelectionChange"
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
          <gbDeviceSelect ref="gbDeviceSelect" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import gbDeviceSelect from './GbDeviceSelect.vue'

export default {
  name: 'LinkChannelRecord',
  directives: { elDragDialog },
  components: { gbDeviceSelect },
  data() {
    return {
      dialogLoading: false,
      showDialog: false,
      chooseData: {},
      channelList: [],
      searchSrt: '',
      channelType: '',
      online: '',
      hasLink: 'false',
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
      planId: null,
      loadSnap: {},
      multipleSelection: []
    }
  },

  created() {},
  destroyed() {},
  methods: {
    openDialog(planId, closeCallback) {
      this.planId = planId
      this.showDialog = true
      this.closeCallback = closeCallback
      this.initData()
    },
    initData: function() {
      this.currentPage = 1
      this.count = 15
      this.total = 0
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
      this.$store.dispatch('recordPlan/queryChannelList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        online: this.online,
        channelType: this.channelType,
        planId: this.planId,
        hasLink: this.hasLink
      })
        .then(data => {
          this.total = data.total
          this.channelList = data.list
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
        .catch((error) => {
          console.log(error)
        })
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },

    linkPlan: function(data) {
      this.loading = true
      return this.$store.dispatch('recordPlan/linkPlan', data)
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

    add: function(row) {
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
      this.linkPlan({
        planId: this.planId,
        channelIds: channels
      })
    },
    addAll: function(row) {
      this.$confirm('添加所有通道将包括已经添加到其他计划的通道，确定添加所有通道？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.linkPlan({
          planId: this.planId,
          allLink: true
        })
      }).catch(() => {
      })
    },

    addByDevice: function(row) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.linkPlan({
          planId: this.planId,
          deviceDbIds: deviceIds
        })
      })
    },

    removeByDevice: function(row) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.linkPlan({
          deviceDbIds: deviceIds
        })
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

      this.linkPlan({
        channelIds: channels
      })
    },
    removeAll: function(row) {
      this.$confirm('确定移除所有通道？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.linkPlan({
          planId: this.planId,
          allLink: false
        })
      }).catch(() => {
      })
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    refresh: function() {
      this.initData()
    }
  }
}
</script>
