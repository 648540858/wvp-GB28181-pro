<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" @submit.native.prevent :model="queryParams" :inline="true">
      <el-form-item label="卡口标识" prop="tollgateId">
        <el-input v-model="queryParams.tollgateId" placeholder="请输入设备标识" clearable size="small"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="卡口名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入卡口名称" clearable size="small"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="管辖单位代码" prop="orgCode">
        <el-input v-model="queryParams.orgCode" placeholder="请输入组织机构编码" clearable size="small"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row type="flex" justify="space-between">
      <el-col :span="12">
        <el-button-group>
          <el-button v-hasPerm="['viid:tollgate:add']" type="primary" icon="el-icon-plus" size="mini"
            @click="handleAdd">新增</el-button>
        </el-button-group>
      </el-col>
      <el-col :span="12">
        <div class="right-toolbar">
          <el-tooltip content="密度" effect="dark" placement="top">
            <el-dropdown trigger="click" @command="handleCommand">
              <el-button circle size="mini">
                <svg-icon class-name="size-icon" icon-class="colum-height" />
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="medium">正常</el-dropdown-item>
                <el-dropdown-item command="small">中等</el-dropdown-item>
                <el-dropdown-item command="mini">紧凑</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </el-tooltip>
          <el-tooltip content="刷新" effect="dark" placement="top">
            <el-button circle size="mini" @click="handleRefresh">
              <svg-icon class-name="size-icon" icon-class="shuaxin" />
            </el-button>
          </el-tooltip>
          <el-tooltip content="列设置" effect="dark" placement="top">
            <el-popover placement="bottom" width="100" trigger="click">
              <el-checkbox-group v-model="checkedTableColumns" @change="handleCheckedColsChange">
                <el-checkbox v-for="(item, index) in tableColumns" :key="index" :label="item.prop">{{ item.label
                }}</el-checkbox>
              </el-checkbox-group>
              <span slot="reference">
                <el-button circle size="mini">
                  <svg-icon class-name="size-icon" icon-class="shezhi" />
                </el-button>
              </span>
            </el-popover>
          </el-tooltip>
        </div>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="tableList" border tooltip-effect="dark" :size="tableSize" :height="tableHeight"
      style="width: 100%;margin: 15px 0;">
      <el-table-column type="selection" width="55" align="center" />
      <template v-for="(item, index) in tableColumns">
        <el-table-column v-if="item.show" :key="index" :prop="item.prop" :label="item.label" :formatter="item.formatter"
          align="center" show-overflow-tooltip />
      </template>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-popover placement="left" trigger="click">
            <el-button v-hasPerm="['viid:tollgate:edit']" size="mini" type="text" icon="el-icon-edit-outline"
              @click="handleEdit(scope.row)">修改</el-button>
            <el-button v-hasPerm="['viid:tollgate:remove']" size="mini" type="text" icon="el-icon-delete"
              @click="handleDelete(scope.row)">删除</el-button>
            <el-button slot="reference">操作</el-button>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
      :current-page.sync="queryParams.pageNum" :page-size.sync="queryParams.pageSize" :total="total"
      @size-change="handleSizeChange" @current-change="handleCurrentChange" />
  </el-card>
</template>

<script>
import { pageTollgate, delTollgates } from '@/api/datawork/viid/tollgate'

export default {
  name: 'VIIDTollgateDeviceList',
  data() {
    return {
      tableHeight: null,
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      },
      // 遮罩层
      loading: true,
      // 表格头
      tableColumns: [
        { prop: 'tollgateId', label: '卡口标识', show: true },
        { prop: 'name', label: '卡口名称', show: true },
        { prop: 'longitude', label: '经度', show: true },
        { prop: 'latitude', label: '纬度', show: true },
        { prop: 'placeCode', label: '位置编码', show: true },
        { prop: 'tollgateCat', label: '卡口类型', show: true },
        { prop: 'tollgateUsage', label: '卡口用途', show: true },
        { prop: 'laneNum', label: '车道数量', show: true },
        { prop: 'orgCode', label: '管辖单位代码', show: true }
      ],
      // 默认选择中表格头
      checkedTableColumns: [],
      tableSize: 'medium',
      // 状态数据字典
      statusOptions: [],
      // 数据集表格数据
      tableList: [],
      // 总数据条数
      total: 0,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        tollgateId: '',
        name: '',
        orgCode: ''
      }
    }
  },
  created() {
    this.getList()
  },
  mounted() {
    this.tableHeight = document.body.offsetHeight ? (document.body.offsetHeight - 310 + 'px') : null
    this.initCols()
  },
  methods: {
    /** 查询数据Api脱敏列表 */
    getList() {
      this.loading = true
      pageTollgate(this.queryParams).then(response => {
        this.loading = false
        if (response.success) {
          this.tableList = response.data
          this.total = response.total
        }
      })
    },
    initCols() {
      this.checkedTableColumns = this.tableColumns.map(col => col.prop)
    },
    handleCheckedColsChange(val) {
      this.tableColumns.forEach(col => {
        if (!this.checkedTableColumns.includes(col.prop)) {
          col.show = false
        } else {
          col.show = true
        }
      })
    },
    handleCommand(command) {
      this.tableSize = command
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 20,
        taskName: ''
      }
      this.handleQuery()
    },
    /** 刷新列表 */
    handleRefresh() {
      this.getList()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.showOptions.data = {}
      this.showOptions.showList = false
      this.showOptions.showAdd = true
      this.showOptions.showEdit = false
      this.showOptions.showDetail = false
      this.$emit('showCard', this.showOptions)
    },
    /** 修改按钮操作 */
    handleEdit(row) {
      this.showOptions.data.id = row.tollgateId
      this.showOptions.showList = false
      this.showOptions.showAdd = false
      this.showOptions.showEdit = true
      this.showOptions.showDetail = false
      this.$emit('showCard', this.showOptions)
    },
    /** 详情按钮操作 */
    handleDetail(row) {
      this.showOptions.data.id = row.tollgateId
      this.showOptions.showList = false
      this.showOptions.showAdd = false
      this.showOptions.showEdit = false
      this.showOptions.showDetail = true
      this.$emit('showCard', this.showOptions)
    },
    handleExecute(row) {
      executeDateReload(row.id).then(response => {
        this.$message.success('任务已提交执行中')
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        delTollgates(row.tollgateId).then(response => {
          if (response.success) {
            this.$message.success('删除成功')
            this.getList()
          }
        })
      }).catch(() => {
      })
    },
    handleSizeChange(val) {
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = val
      this.getList()
    },
    handleCurrentChange(val) {
      this.queryParams.pageNum = val
      this.getList()
    }
  }
}
</script>

<style lang="scss" scoped>
.right-toolbar {
  float: right;
}

.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
</style>
