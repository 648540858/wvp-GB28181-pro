<template>
  <div id="recordPLan" style="width: 100%">
    <div class="page-header">
        <div class="page-title">
          <div >录像计划</div>
        </div>
        <div class="page-header-btn">
          <div style="display: inline;">
            搜索:
            <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                      prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>
            <el-button size="mini" type="primary" @click="add()">
              添加
            </el-button>
            <el-button icon="el-icon-refresh-right" circle size="mini" @click="getRecordPlanList()"></el-button>
          </div>
        </div>
      </div>
      <el-table size="medium" ref="recordPlanListTable" :data="recordPlanList" :height="$tableHeght" style="width: 100%"
                header-row-class-name="table-header" >
        <el-table-column type="selection" width="55" >
        </el-table-column>
        <el-table-column prop="name" label="名称" >
        </el-table-column>
        <el-table-column prop="channelCount" label="关联通道" >
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间">
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间">
        </el-table-column>
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
        @size-change="handleSizeChange"
        @current-change="currentChange"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total">
      </el-pagination>
    <editRecordPlan ref="editRecordPlan"></editRecordPlan>
    <LinkChannelRecord ref="linkChannelRecord"></LinkChannelRecord>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import EditRecordPlan from "./dialog/editRecordPlan.vue";
import LinkChannelRecord from "./dialog/linkChannelRecord.vue";

export default {
  name: 'recordPLan',
  components: {
    EditRecordPlan,
    LinkChannelRecord,
    uiHeader,
  },
  data() {
    return {
      recordPlanList: [],
      searchSrt: "",
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false,
    };
  },

  created() {
    this.initData();
  },
  destroyed() {
  },
  methods: {
    initData: function () {
      this.getRecordPlanList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.initData();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getRecordPlanList();
    },
    getRecordPlanList: function () {
      this.$axios({
        method: 'get',
        url: `/api/record/plan/query`,
        params: {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.total = res.data.data.total;
          this.recordPlanList = res.data.data.list;
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.recordPlanListTable.doLayout();
          })
        }

      }).catch((error) => {
        console.log(error);
      });
    },
    getSnap: function (row) {
      let baseUrl = window.baseUrl ? window.baseUrl : "";
      return ((process.env.NODE_ENV === 'development') ? process.env.BASE_API : baseUrl) + '/api/device/query/snap/' + this.deviceId + '/' + row.deviceId;
    },
    search: function () {
      this.currentPage = 1;
      this.total = 0;
      this.initData();
    },
    refresh: function () {
      this.initData();
    },
    add: function () {
      this.$refs.editRecordPlan.openDialog(null, ()=>{
        this.initData()
      })
    },
    edit: function (plan) {
      this.$refs.editRecordPlan.openDialog(plan, ()=>{
        this.initData()
      })
    },
    link: function (plan) {
      this.$refs.linkChannelRecord.openDialog(plan.id, ()=>{
        this.initData()
      })
    },
    deletePlan: function (plan) {
      this.$confirm('确定删除?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$axios({
          method: 'delete',
          url: "/api/record/plan/delete",
          params: {
            planId: plan.id,
          }
        }).then((res) => {
          if (res.data.code === 0) {
            this.$message({
              showClose: true,
              message: '删除成功',
              type: 'success',
            });
            this.initData();
          } else {
            this.$message({
              showClose: true,
              message: res.data.msg,
              type: 'error'
            });
          }
        }).catch((error) => {
          console.error(error)
        });
      }).catch(() => {

      });

    },
  }
};
</script>

<style>
.videoList {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
}

.video-item {
  position: relative;
  width: 15rem;
  height: 10rem;
  margin-right: 1rem;
  background-color: #000000;
}

.video-item-img {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 100%;
  height: 100%;
}

.video-item-img:after {
  content: "";
  display: inline-block;
  position: absolute;
  z-index: 2;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 3rem;
  height: 3rem;
  background-image: url("../assets/loading.png");
  background-size: cover;
  background-color: #000000;
}

.video-item-title {
  position: absolute;
  bottom: 0;
  color: #000000;
  background-color: #ffffff;
  line-height: 1.5rem;
  padding: 0.3rem;
  width: 14.4rem;
}
</style>
