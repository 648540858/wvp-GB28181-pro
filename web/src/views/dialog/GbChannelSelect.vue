<template>
  <div id="gbChannelSelect" v-loading="getChannelListLoading">
    <el-dialog
      v-el-drag-dialog
      title="添加国标通道"
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
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="getChannelList"
          />
        </el-form-item>
        <el-form-item label="在线状态">
          <el-select
            v-model="online"
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
          <el-button type="primary" style="float: right" @click="onSubmit">确 定</el-button>
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
          <i class="el-icon-info" /> 未找到通道，可在国标设备/通道中选择编辑按钮， 选择{{ dataType === 'civilCode'?'行政区划':'父节点编码' }}
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
  name: 'GbChannelSelect',
  directives: { elDragDialog },
  props: ['dataType', 'selected'],
  data() {
    return {
      showDialog: false,
      channelList: [], // 设备列表
      currentDevice: {}, // 当前操作设备对象
      searchSrt: '',
      online: null,
      channelType: '',
      videoComponentList: [],
      updateLooper: 0, // 数据刷新轮训标志
      currentDeviceChannelsLenth: 0,
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
      if (this.dataType === 'civilCode') {
        this.$store.dispatch('commonChanel/getCivilCodeList', {
          page: this.currentPage,
          count: this.count,
          channelType: this.channelType,
          query: this.searchSrt,
          online: this.online
        })
          .then(data => {
            this.total = data.total
            this.channelList = data.list
          }).finally(() => {
            this.getChannelListLoading = false
          })
      } else {
        this.$store.dispatch('commonChanel/getParentList', {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
          channelType: this.channelType,
          online: this.online
        })
          .then(data => {
            this.total = data.total
            this.channelList = data.list
          }).finally(() => {
            this.getChannelListLoading = false
          })
      }
    },
    openDialog: function(callback) {
      this.listChangeCallback = callback
      this.showDialog = true
      this.initData()
    },
    onSubmit: function() {
      if (this.listChangeCallback) {
        this.listChangeCallback(this.multipleSelection)
      }
      this.showDialog = false
    },
    close: function() {
      this.showDialog = false
    }

  }
}
</script>
