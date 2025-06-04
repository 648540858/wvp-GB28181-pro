<template>
    <el-card class="box-card" shadow="always">
        <div slot="header" class="clearfix">
            <span>{{ title }}</span>
            <el-button-group style="float: right;">
                <el-button v-hasPerm="['viid:tollgate:add']" size="mini" icon="el-icon-plus" round
                    :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{
                        loadingOptions.loadingText }}</el-button>
                <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
            </el-button-group>
        </div>
        <div class="body-wrapper">
            <el-form ref="form" :model="form" :rules="rules" label-width="110px">
                <el-tooltip class="item" effect="dark" content="国标格式卡口编号(6位地区编码+0000121+7位随机数)设备唯一不可重复" placement="top">
                    <el-form-item label="卡口编号" prop="tollgateId">
                        <el-select v-model="form.tollgateId" clearable filterable remote :remote-method="loadDeviceOptions" placeholder="请选择卡口">
                            <el-option v-for="item in tollgateOptions" :key="item.id" :value="item.value" :label="item.label" />
                        </el-select>
                    </el-form-item>
                </el-tooltip>
                <el-form-item label="车道ID" prop="laneId">
                    <el-input type="number" v-model="form.laneId" placeholder="请输入车道ID" />
                </el-form-item>
                <el-form-item label="车道编号" prop="laneNo">
                    <el-input type="number" v-model="form.laneNo" placeholder="请输入车道编号" />
                </el-form-item>
                <el-form-item label="车道名称" prop="name">
                    <el-input v-model="form.name" placeholder="请输入车道名称" />
                </el-form-item>
                <el-form-item label="车道方向" prop="direction">
                    <el-select v-model="form.direction" placeholder="请选择车道方向">
                        <el-option value="1" label="东"></el-option>
                        <el-option value="2" label="西"></el-option>
                        <el-option value="3" label="南"></el-option>
                        <el-option value="4" label="北"></el-option>
                        <el-option value="5" label="东北"></el-option>
                        <el-option value="6" label="西南"></el-option>
                        <el-option value="7" label="东南"></el-option>
                        <el-option value="8" label="西北"></el-option>
                        <el-option value="9" label="其他"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="车道描述" prop="desc">
                    <el-input v-model="form.desc" placeholder="请输入车道描述" />
                </el-form-item>
                <el-form-item label="限速" prop="maxSpeed">
                    <el-input type="number" v-model="form.maxSpeed" placeholder="请输入车道限速" />
                </el-form-item>
                <el-form-item label="出入城" prop="cityPass">
                    <el-select v-model="form.cityPass" placeholder="请选择车道出入城">
                        <el-option :value="1" label="进城"></el-option>
                        <el-option :value="2" label="出城"></el-option>
                        <el-option :value="3" label="非进出城"></el-option>
                        <el-option :value="4" label="进出城混合"></el-option>
                    </el-select>
                </el-form-item>
                <el-tooltip class="item" effect="dark" content="车道关联的设备编号" placement="top">
                    <el-form-item label="关联采集设备" type="number" prop="apeId">
                        <el-select v-model="form.apeId" clearable filterable remote :remote-method="loadDeviceOptions" placeholder="请选择关联采集设备">
                            <el-option v-for="item in deviceOptions" :key="item.id" :value="item.value" :label="item.label" />
                        </el-select>
                    </el-form-item>
                </el-tooltip>
            </el-form>
        </div>
    </el-card>
</template>

<script>
import { addLane } from '@/api/datawork/viid/lane'
import { getDeviceOptions } from '@/api/datawork/viid/apedevice'
import { getTollgateOptions } from '@/api/datawork/viid/tollgate'

export default {
    name: 'VIIDLaneAdd',
    props: {
        data: {
            type: Object,
            default: function () {
                return {}
            }
        }
    },
    data() {
        return {
            title: '视图库车道新增',
            // 展示切换
            showOptions: {
                data: {},
                showList: true,
                showAdd: false,
                showEdit: false,
                showDetail: false
            },
            // 保存按钮
            loadingOptions: {
                loading: false,
                loadingText: '保存',
                isDisabled: false
            },
            // 表单参数
            form: {},
            rules: {
                tollgateId: [{ required: true, message: '卡口编号不能为空', trigger: 'blur' }],
                laneId: [{ required: true, message: '车道ID不能为空', trigger: 'blur' }],
                laneNo: [{ required: true, message: '车道编号不能为空', trigger: 'blur' }],
                name: [{ required: true, message: '车道名称不能为空', trigger: 'blur' }],
                direction: [{ required: true, message: '车道方向不能为空', trigger: 'blur' }]
            },
            deviceOptions: [],
            tollgateOptions: []
        }
    },
    created() {
        this.loadDeviceOptions()
        this.loadTollgateOptions()
    },
    methods: {
        showCard() {
            this.$emit('showCard', this.showOptions)
        },
        loadDeviceOptions(keyword, id) {
            getDeviceOptions({name: keyword, apeId: id}).then(response => {
                this.deviceOptions = response.data.map(ele => {
                    return {
                        id: ele.apeId,
                        value: ele.apeId,
                        label: ele.name
                    }
                })
            })
        },
        loadTollgateOptions(keyword, id) {
            getTollgateOptions({name: keyword, tollgateId: id}).then(response => {
                this.tollgateOptions = response.data.map(ele => {
                    return {
                        id: ele.tollgateId,
                        value: ele.tollgateId,
                        label: ele.name
                    }
                })
            })
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    this.loadingOptions.loading = true
                    this.loadingOptions.loadingText = '保存中...'
                    this.loadingOptions.isDisabled = true
                    addLane(this.form).then(response => {
                        if (response.success) {
                            this.$message.success('保存成功')
                            setTimeout(() => {
                                // 2秒后跳转列表页
                                this.$emit('showCard', this.showOptions)
                            }, 1000)
                        } else {
                            this.$message.error('保存失败')
                            this.loadingOptions.loading = false
                            this.loadingOptions.loadingText = '保存'
                            this.loadingOptions.isDisabled = false
                        }
                    }).catch(() => {
                        this.loadingOptions.loading = false
                        this.loadingOptions.loadingText = '保存'
                        this.loadingOptions.isDisabled = false
                    })
                }
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.el-card ::v-deep .el-card__body {
    height: calc(100vh - 230px);
    overflow-y: auto;
}
</style>
