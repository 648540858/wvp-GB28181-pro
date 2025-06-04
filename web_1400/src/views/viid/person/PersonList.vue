<template>
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
        </el-form>

        <el-row type="flex" justify="space-between">
            <el-col :span="12">
                <el-button-group>
                    <el-button v-hasPerm="['viid:person:add']" type="primary" icon="el-icon-plus" size="mini"
                        @click="handleAdd">新增</el-button>
                    <el-button v-hasPerm="['viid:person:edit']" type="success" icon="el-icon-edit-outline" size="mini"
                        :disabled="single" @click="handleEdit">修改</el-button>
                    <el-button v-hasPerm="['viid:person:remove']" type="danger" icon="el-icon-delete" size="mini"
                        :disabled="multiple" @click="handleBatchDelete">删除</el-button>
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
                                <el-checkbox v-for="(item, index) in tableColumns" :key="index" :label="item.prop">{{
                                    item.label }}</el-checkbox>
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
            style="width: 100%;margin: 15px 0;" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <template v-for="(item, index) in tableColumns">
                <el-table-column v-if="item.show" :key="index" :prop="item.prop" :label="item.label"
                    :formatter="item.formatter" align="center" show-overflow-tooltip />
            </template>
            <el-table-column label="图片列表" align="center" show-overflow-tooltip>
                <template slot-scope="scope">
                    <el-row>
                        <el-col :span="12" v-for="(image, index) in scope.row.subImageList.SubImageInfoObject" :key="index">
                            <el-image 
                            :src="loadImagePath(image)" 
                            :preview-src-list="previewImages(image)" 
                            style="height: 50px"/>
                        </el-col>
                    </el-row>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                    <el-popover placement="left" trigger="click">
                        <el-button v-hasPerm="['viid:person:edit']" size="mini" type="text" icon="el-icon-edit-outline"
                            @click="handleEdit(scope.row)">修改</el-button>
                        <el-button v-hasPerm="['viid:person:remove']" size="mini" type="text" icon="el-icon-delete"
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
import { pagePerson, delPersons } from '@/api/datawork/person'

export default {
    name: 'VIIDPersonList',
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
                { prop: 'personId', label: '人员标识', show: true },
                { prop: 'deviceId', label: '设备编码', show: true },
                { prop: 'leftTopX', label: '左上角X坐标', show: false },
                { prop: 'leftTopY', label: '左上角Y坐标', show: false },
                { prop: 'rightBtmX', label: '右下角X坐标', show: false },
                { prop: 'rightBtmY', label: '右下角Y坐标', show: false },
                { prop: 'genderCode', label: '性别代码', show: true },
                { prop: 'ageUpLimit', label: '年龄上限', show: true },
                { prop: 'ageLowerLimit', label: '年龄下限', show: true },
                { prop: 'accompanyNumber', label: '同行人脸数', show: false },
                { prop: 'personAppearTime', label: '人员出现时间', show: true },
                { prop: 'personDisAppearTime', label: '人员消失时间', show: true }
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
            pagePerson(this.queryParams).then(response => {
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
        /** 删除按钮操作 */
        handleDelete(row) {
            this.$confirm('选中数据将被永久删除, 是否继续？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                delPersons(row.id).then(response => {
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
                delPersons(this.ids).then(response => {
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

<style lang="scss" scoped>
.right-toolbar {
    float: right;
}

.el-card ::v-deep .el-card__body {
    height: calc(100vh - 170px);
}
</style>
