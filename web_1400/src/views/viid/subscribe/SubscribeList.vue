<template>
  <el-card class="box-card" shadow="always">
    <el-form ref="queryForm" @submit.native.prevent :model="queryParams" :inline="true">
      <el-form-item label="订阅标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入订阅标题" clearable size="small"
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
          <el-button v-hasPerm="['viid:subscribe:add']" type="primary" icon="el-icon-plus" size="mini"
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
        </div>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="tableList" border tooltip-effect="dark" :size="tableSize" :height="tableHeight"
      style="width: 100%; margin: 15px 0">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column prop="subscribeId" label="订阅标识符" align="center" show-overflow-tooltip />
      <el-table-column prop="title" label="订阅标题" align="center" show-overflow-tooltip />
      <el-table-column prop="subscribeDetail" label="订阅类型" align="center" show-overflow-tooltip />
      <el-table-column prop="resourceUri" label="资源列表" align="center" show-overflow-tooltip />
      <el-table-column prop="beginTime" label="订阅时间" align="center" show-overflow-tooltip>
          <template slot-scope="scope">
            <div>{{scope.row.beginTime}}至{{scope.row.endTime}}</div>
          </template>
        </el-table-column>
        <el-table-column prop="applicationName" label="申请信息" align="center" show-overflow-tooltip >
          <template slot-scope="scope">
            <div>申请单位:{{scope.row.applicationOrg}},申请人:{{scope.row.applicationOrg}},理由:{{scope.row.reason}}</div>
          </template>
        </el-table-column>
      <el-table-column prop="receiveAddr" label="订阅回调地址" align="center" show-overflow-tooltip />
      <el-table-column prop="reportInterval" label="数据上报间隔" align="center" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-popover placement="left" trigger="click">
            <el-button v-hasPerm="['viid:subscribe:remove']" size="mini" type="text" icon="el-icon-delete"
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
import { pageSubscribe, delSubscribes } from "@/api/datawork/viid/subscribe";

export default {
  name: "VIIDSubscribeList",
  data() {
    return {
      tableHeight: null,
      // 展示切换
      showOptions: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false,
      },
      // 遮罩层
      loading: true,
      // 默认选择中表格头
      checkedTableColumns: [],
      tableSize: "medium",
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
        title: "",
      },
    };
  },
  created() {
    this.getList();
  },
  mounted() {
    this.tableHeight = document.body.offsetHeight
      ? document.body.offsetHeight - 310 + "px"
      : null;
  },
  methods: {
    /** 查询数据Api脱敏列表 */
    getList() {
      this.loading = true;
      pageSubscribe(this.queryParams).then((response) => {
        this.loading = false;
        if (response.success) {
          this.tableList = response.data;
          this.total = response.total;
        }
      });
    },
    handleCheckedColsChange(val) {
      this.tableColumns.forEach((col) => {
        if (!this.checkedTableColumns.includes(col.prop)) {
          col.show = false;
        } else {
          col.show = true;
        }
      });
    },
    handleCommand(command) {
      this.tableSize = command;
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 20,
        taskName: "",
      };
      this.handleQuery();
    },
    /** 刷新列表 */
    handleRefresh() {
      this.getList();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.showOptions.data = {};
      this.showOptions.showList = false;
      this.showOptions.showAdd = true;
      this.showOptions.showEdit = false;
      this.showOptions.showDetail = false;
      this.$emit("showCard", this.showOptions);
    },
    /** 修改按钮操作 */
    handleEdit(row) {
      this.showOptions.data.id = row.subscribeId;
      this.showOptions.showList = false;
      this.showOptions.showAdd = false;
      this.showOptions.showEdit = true;
      this.showOptions.showDetail = false;
      this.$emit("showCard", this.showOptions);
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$confirm("选中数据将被永久删除, 是否继续？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          delSubscribes(row.subscribeId).then((response) => {
            debugger
            const responseStatusObjects = response.ResponseStatusListObject?.ResponseStatusObject;
            if (responseStatusObjects) {
              for (let index in responseStatusObjects) {
                let responseStatusObject = responseStatusObjects[index];
                if (responseStatusObject.StatusCode == "0") {
                  this.$message.success("删除成功");
                } else {
                  this.$message.error(
                    "取消订阅失败:" + responseStatusObject.statusString
                  );
                }
              }
              this.getList();
            } else {
              this.$message.error("删除失败");
            }
          });
        })
        .catch(() => { });
    },
    handleSizeChange(val) {
      this.queryParams.pageNum = 1;
      this.queryParams.pageSize = val;
      this.getList();
    },
    handleCurrentChange(val) {
      this.queryParams.pageNum = val;
      this.getList();
    },
  },
};
</script>

<style lang="scss" scoped>
.right-toolbar {
  float: right;
}

.el-card ::v-deep .el-card__body {
  height: calc(100vh - 170px);
}
</style>
