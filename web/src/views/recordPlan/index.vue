<template>
  <div id="recordPLan" class="app-container">
    <div style="height: calc(100vh - 124px);">
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
        <el-form-item>
          <el-button size="mini" type="primary" @click="add()">
            添加
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle size="mini" @click="getRecordPlanList()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="recordPlanListTable"
        size="small"
        :data="recordPlanList"
        height="calc(100% - 64px)"
        style="width: 100%"
        header-row-class-name="table-header"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="channelCount" label="关联通道" />
        <el-table-column prop="updateTime" label="更新时间" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="300" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-link" type="text" @click="link(scope.row)">关联通道</el-button>
            <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">编辑</el-button>
            <el-button size="medium" icon="el-icon-delete" style="color: #f56c6c" type="text" @click="deletePlan(scope.row)">删除</el-button>
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

    <editRecordPlan ref="editRecordPlan" />
    <LinkChannelRecord ref="linkChannelRecord" />
  </div>
</template>

<script>
import EditRecordPlan from '../dialog/editRecordPlan.vue'
import LinkChannelRecord from '../dialog/linkChannelRecord.vue'

export default {
  name: 'RecordPlan',
  components: {
    EditRecordPlan,
    LinkChannelRecord
  },
  data() {
    return {
      recordPlanList: [],
      searchSrt: '',
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false
    }
  },

  created() {
    this.initData()
  },
  destroyed() {
  },
  methods: {
    initData: function() {
      this.getRecordPlanList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.initData()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getRecordPlanList()
    },
    getRecordPlanList: function() {
      this.$store.dispatch('recordPlan/queryList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt
      })
        .then(data => {
          this.total = data.total
          this.recordPlanList = data.list
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.recordPlanListTable.doLayout()
          })
        })
        .catch((error) => {
          console.log(error)
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
    add: function() {
      this.$refs.editRecordPlan.openDialog(null, () => {
        this.initData()
      })
    },
    edit: function(plan) {
      this.$refs.editRecordPlan.openDialog(plan, () => {
        this.initData()
      })
    },
    link: function(plan) {
      this.$refs.linkChannelRecord.openDialog(plan.id, () => {
        this.initData()
      })
    },
    deletePlan: function(plan) {
      this.$confirm('确定删除?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('recordPlan/deletePlan', plan.id)
          .then(() => {
            this.$message({
              showClose: true,
              message: '删除成功',
              type: 'success'
            })
            this.initData()
          })
          .catch((error) => {
            console.error(error)
          })
      }).catch(() => {

      })
    }
  }
}
</script>
