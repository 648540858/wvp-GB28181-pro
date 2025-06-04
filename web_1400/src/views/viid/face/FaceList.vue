<template>
  <div class="app-container">
    <el-card class="box-card" shadow="always">
        <el-form ref="queryForm" @submit.native.prevent :model="queryParams" :inline="true">
            <el-form-item label="设备编码" prop="deviceId">
                <el-input v-model="queryParams.deviceId" placeholder="请输入客户端名称" clearable size="small"
                    @keyup.enter.native="handleQuery" />
            </el-form-item>
            <el-form-item label="数据时间">
                <el-date-picker v-model="queryParams.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss"
                    size="small" placeholder="选择开始时间" align="center">
                </el-date-picker>~
                <el-date-picker v-model="queryParams.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss"
                    size="small" placeholder="选择结束时间" align="center">
                </el-date-picker>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
          <el-form-item>
            <el-tooltip content="刷新" effect="dark" placement="top">
              <el-button circle size="mini" @click="handleRefresh">
                <svg-icon class-name="size-icon" icon-class="shuaxin" />
              </el-button>
            </el-tooltip>
          </el-form-item>
        </el-form>
      <div v-if="tableList.length > 0">
        <el-row style="height: 50%;">
          <el-col :span="8" v-for="ele in tableList" :key="ele.id" class="col-card">
            <el-card :body-style="{ padding: '0px', width: '100%', height: '100%' }">
              <div>
                  <el-image v-for="(image,index) in ele.subImageList.SubImageInfoObject" :preview-src-list="[image.StoragePath]" :key="image" v-if="index===0" :src="image.StoragePath" class="image" :lazy="true">
                    <div slot="error" class="image-slot"
                         style="height: 65%; display: flex; align-items: center;">
                      <i class="el-icon-picture-outline"
                         style="font-size: 32px; margin: 0 auto;"></i>
                    </div>
                  </el-image>
              </div>
              <div style="padding: 10px;">
                <el-row>
                  <el-col :span="14">
                    <span style="margin-top: 10px">设备编码: {{ ele.deviceId }}</span>
                    <br />
                    <span style="margin-top: 10px">性别: {{ ele.genderCode }}</span>
                    <br />
                    <span style="margin-top: 10px">年龄: {{ ele.ageUpLimit }}</span>
                  </el-col>
                  <el-col v-for="(image,index) in ele.subImageList.SubImageInfoObject" :span="5" :key="image" v-if="index===1" style="text-align:right;">
                    <el-image :src="image.StoragePath" :preview-src-list="[image.StoragePath]" style="height: 70px;" />
                  </el-col>
                </el-row>
                <div class="bottom clearfix">
                  <time class="time">{{ ele.dataTime }}</time>
                  <el-button type="text" class="button" @click="handleDelete(ele)">删除</el-button>
<!--                  <el-button type="text" class="button" @click="handleDetail(ele)">详情</el-button>-->
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      <div v-else>
        <el-empty description="暂无数据"></el-empty>
      </div>
    </el-card>
    <el-pagination :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
                   :current-page.sync="queryParams.pageNum" :page-size.sync="queryParams.pageSize" :total="total"
                   @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    <el-drawer title="详情" :visible.sync="open" direction="rtl" size="45%" modal-append-to-body>
      <json-viewer :value="formValue" :expand-depth="4" copyable boxed expanded show-double-quotes class="w-100%"></json-viewer>
      <div class="dialog-footer" style="margin-top: 10px;">
        <el-button type="primary" @click="open = false">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { pageFace, delFaces } from '@/api/datawork/face'

export default {
    name: 'VIIDFaceList',
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
            // 选中数组
            ids: [],
            // 非单个禁用
            single: true,
            // 非多个禁用
            multiple: true,
            // 表格头
            tableColumns: [
                { prop: 'faceId', label: '人脸标识', show: true },
                { prop: 'deviceId', label: '设备编码', show: true },
                { prop: 'leftTopX', label: '左上角X坐标', show: false },
                { prop: 'leftTopY', label: '左上角Y坐标', show: false },
                { prop: 'rightBtmX', label: '右下角X坐标', show: false },
                { prop: 'rightBtmY', label: '右下角Y坐标', show: false },
                { prop: 'genderCode', label: '性别代码', show: true },
                { prop: 'ageUpLimit', label: '年龄上限', show: true },
                { prop: 'ageLowerLimit', label: '年龄下限', show: true },
                { prop: 'accompanyNumber', label: '同行人脸数', show: false },
                { prop: 'skinColor', label: '肤色', show: false },
                { prop: 'faceStyle', label: '脸型', show: false },
                { prop: 'facialFeature', label: '脸部特征', show: false },
                { prop: 'physicalFeature', label: '体貌特征', show: false },
                { prop: 'attitude', label: '姿态分布', show: false },
                { prop: 'similaritydegree', label: '相似度', show: false },
                { prop: 'eyebrowStyle', label: '眉型', show: false },
                { prop: 'noseStyle', label: '鼻型', show: false },
                { prop: 'mustacheStyle', label: '胡型', show: false },
                { prop: 'lipStyle', label: '嘴唇', show: false },
                { prop: 'wrinklePouch', label: '皱纹眼袋', show: false },
                { prop: 'acneStain', label: '痤疮色斑', show: false },
                { prop: 'freckleBirthmark', label: '黑痣胎记', show: false },
                { prop: 'faceAppearTime', label: '人脸出现时间', show: true },
                { prop: 'faceDisAppearTime', label: '人脸消失时间', show: true }
            ],
            // 默认选择中表格头
            checkedTableColumns: [],
            tableSize: 'medium',
            // 参数表格数据
            tableList: [],
            // 总数据条数
            total: 0,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                deviceId: '',
                startTime: null,
                endTime: null
            },
            //json展示组件
            open: false,
            formValue: null,
            // 状态数据字典
            statusOptions: []
        }
    },
    created() {
        this.getList()
    },
    mounted() {
        this.tableHeight = document.body.offsetHeight - 310 + 'px'
        this.initCols()
    },
    methods: {
        /** 查询参数列表 */
        getList() {
            this.loading = true
            pageFace(this.queryParams).then(response => {
                this.loading = false
                if (response.success) {
                    this.tableList = response.data
                    this.total = response.total
                }
            })
        },
        initCols() {
            this.checkedTableColumns = this.tableColumns.filter(col => col.show === true).map(col => col.prop)
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
                configName: ''
            }
            this.handleQuery()
        },
        /** 刷新列表 */
        handleRefresh() {
            this.getList()
        },
        /** 多选框选中数据 */
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.id)
            this.single = selection.length !== 1
            this.multiple = !selection.length
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
            this.showOptions.data.id = row.id || this.ids[0]
            this.showOptions.showList = false
            this.showOptions.showAdd = false
            this.showOptions.showEdit = true
            this.showOptions.showDetail = false
            this.$emit('showCard', this.showOptions)
        },
        handleDetail(row) {
          this.formValue = row;
          this.open = true;
          console.log("form："+JSON.stringify(this.formValue));
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                delFaces(row.id).then(response => {
                    if (response.success) {
                        this.$message.success('删除成功')
                        this.getList()
                    }
                })
            }).catch(() => {
            })
        },
        /** 批量删除按钮操作 */
        handleBatchDelete() {
            if (!this.ids.length) {
                this.$message({
                    message: '请先选择需要操作的数据',
                    type: 'warning'
                })
            }
            this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                delFaces(this.ids).then(response => {
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
        },
        loadImagePath(image) {
            if (image) {
                return image.StoragePath
            }
            return null
        },
        previewImages(image) {
            if (image) {
                return [image.StoragePath]
            }
            return []
        }
    }
}
</script>

<style>
.col-card {
  aspect-ratio: 23/20;
  width: 23%;
  height: 20%;
  margin-left: 1%;
  margin-right: 1%;
  margin-top: 1%;
}

.time {
  font-size: 13px;
  color: #999;
}

.bottom {
  margin-top: 13px;
  line-height: 12px;
}

.button {
  padding: 0;
  float: right;
}

.image {
  width: 100%;
  height: 100%;
  display: block;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both
}
</style>

<!--<style lang="scss" scoped>-->
<!--.right-toolbar {-->
<!--    float: right;-->
<!--}-->

<!--.el-card ::v-deep .el-card__body {-->
<!--    height: calc(100vh - 170px);-->
<!--}-->
<!--</style>-->
