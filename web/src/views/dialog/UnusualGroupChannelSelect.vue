<template>
  <div id="gbChannelSelect" v-loading="getChannelListLoading">
    <el-dialog
      v-el-drag-dialog
      title="异常挂载通道"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      append-to-body
      @close="close()"
    >
      <el-form :inline="true" size="mini">
        <el-form-item label="搜索">
          <el-input
            v-model="searchSrt"
            style="margin-right: 1rem; width: auto;"
            size="mini"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="getChannelList"
          />
        </el-form-item>
        <el-form-item label="在线状态">
          <el-select
            v-model="online"
            size="mini"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="getChannelList"
          >
            <el-option label="全部" value="" />
            <el-option label="在线" value="true" />
            <el-option label="离线" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select
            v-model="channelType"
            size="mini"
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
          <el-button
            size="mini"
            type="primary"
            :loading="getChannelListLoading"
            :disabled="multipleSelection.length ===0"
            @click="clearUnusualRegion()"
          >清除</el-button>
          <el-button
            size="mini"
            :loading="getChannelListLoading"
            @click="clearUnusualRegion(true)"
          >全部清除</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle :loading="getChannelListLoading" @click="getChannelList()" />
        </el-form-item>
      </el-form>
      <!--通道列表-->
      <el-table
        ref="channelListTable"
        size="small"
        :data="channelList"
        :height="winHeight"
        style="width: 100%;"
        header-row-class-name="table-header"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="gbName" label="名称" min-width="180" />
        <el-table-column prop="gbDeviceId" label="编号" min-width="180" />
        <el-table-column prop="gbManufacturer" label="厂家" min-width="100" />
        <el-table-column prop="gbCivilCode" label="行政区划" min-width="100" />
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
      <div style="display: grid; grid-template-columns: 1fr 1fr">
        <div style="text-align: left; line-height: 32px">
          <i class="el-icon-info" /> 清除后通道可正常添加到分组节点。
        </div>
        <el-pagination
          style="text-align: right"
          :current-page="currentPage"
          :page-size="count"
          :page-sizes="[10, 25, 35, 50, 200, 1000, 50000]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="currentChange"
        />
      </div>

    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'


export default {
  name: 'UnusualGroupChannelSelect',
  directives: { elDragDialog },
  props: [],
  data() {
    return {
      showDialog: false,
      channelList: [], // 设备列表
      searchSrt: '',
      online: null,
      channelType: '',
      winHeight: 580,
      currentPage: 1,
      count: 10,
      total: 0,
      getChannelListLoading: false,
      multipleSelection: []
    }
  },
  computed: {},
  methods: {
    initData: function() {
      this.getChannelList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getChannelList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getChannelList()
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    getChannelList: function() {
      this.getChannelListLoading = true
      this.$store.dispatch('commonChanel/getUnusualParentList', {
        page: this.currentPage,
        count: this.count,
        channelType: this.channelType,
        query: this.searchSrt,
        online: this.online
      })
        .then(data => {
          this.total = data.total
          for (let i = 0; i < data.list.length; i++) {
            data.list[i]['addRegionLoading'] = false
          }
          this.channelList = data.list
        })
        .finally(() => {
          this.getChannelListLoading = false
        })
    },
    openDialog: function() {
      this.showDialog = true
      this.initData()
    },
    close: function() {
      this.showDialog = false
    },
    clearUnusualRegion: function(all) {
      let channels = null
      if (all || this.multipleSelection.length > 0) {
        channels = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          channels.push(this.multipleSelection[i].gbId)
        }
      }
      this.$store.dispatch('commonChanel/clearUnusualParentList', {
        all: all,
        channelIds: channels
      })
        .then(data => {
          this.$message.success({
            showClose: true,
            message: '清除成功'
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
    addRegion: function(row) {
      row.addRegionLoading = true
      this.$store.dispatch('region/description', row.gbCivilCode)
        .then(data => {
          this.$confirm(`确定添加： ${data}`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }).then(() => {
            this.$store.dispatch('region/addByCivilCode', row.gbCivilCode)
              .then(data => {
                this.$message.success({
                  showClose: true,
                  message: '添加成功'
                })
                this.initData()
              })
              .catch((error) => {
                console.error(error)
              })
          }).catch(() => {

          })
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          row.addRegionLoading = false
        })
    }

  }
}
</script>
