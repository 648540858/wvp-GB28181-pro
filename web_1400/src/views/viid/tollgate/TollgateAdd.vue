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
                    <el-form-item label="卡口标识" prop="tollgateId">
                        <el-input v-model="form.tollgateId" placeholder="请输入卡口标识" />
                    </el-form-item>
                </el-tooltip>
                <el-form-item label="卡口名称" prop="name">
                    <el-input v-model="form.name" placeholder="请输入卡口名称" />
                </el-form-item>
                <el-form-item label="卡口经度" prop="longitude">
                    <el-input v-model="form.longitude" type="number" placeholder="请输入卡口经度" />
                </el-form-item>
                <el-form-item label="卡口纬度" prop="latitude">
                    <el-input v-model="form.latitude" type="number" placeholder="请输入卡口纬度" />
                </el-form-item>
                <el-form-item label="位置编码" prop="placeCode">
                    <el-input v-model="form.placeCode" placeholder="请输入位置编码" />
                </el-form-item>
                <el-form-item label="卡口类型" prop="tollgateCat">
                    <el-select v-model="form.tollgateCat" placeholder="请选择卡口类型">
                        <el-option value="10" label="国际"></el-option>
                        <el-option value="20" label="省际"></el-option>
                        <el-option value="30" label="市际"></el-option>
                        <el-option value="31" label="市区"></el-option>
                        <el-option value="40" label="县际"></el-option>
                        <el-option value="41" label="县区"></el-option>
                        <el-option value="99" label="其他"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="卡口用途" prop="tollgateUsage">
                    <el-select v-model="form.tollgateUsage" placeholder="请选择卡口用途">
                        <el-option value="80" label="治安卡口"></el-option>
                        <el-option value="81" label="交通卡口"></el-option>
                        <el-option value="82" label="其他"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="车道号" prop="laneNum">
                    <el-input v-model="form.laneNum" placeholder="请输入车道号" />
                </el-form-item>
                <el-form-item label="管辖单位代码" type="number" prop="orgCode">
                    <el-input v-model="form.orgCode" placeholder="请输入组管辖单位代码" />
                </el-form-item>
                <el-tooltip class="item" effect="dark" content="卡口关联的设备编号" placement="top">
                    <el-form-item label="关联采集设备" type="number" prop="deviceId">
                        <el-select v-model="form.deviceId" clearable filterable remote :remote-method="loadDeviceOptions" placeholder="请选择关联采集设备">
                            <el-option v-for="item in deviceOptions" :key="item.apeId" :value="item.apeId" :label="item.name" />
                        </el-select>
                    </el-form-item>
                </el-tooltip>
            </el-form>
        </div>
    </el-card>
</template>

<script>
import { addTollgate } from '@/api/datawork/viid/tollgate'
import { getDeviceOptions } from '@/api/datawork/viid/apedevice'

export default {
    name: 'VIIDTollgateAdd',
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
            title: '视图库视频卡口新增',
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
                tollgateId: [{ required: true, message: '设备标识不能为空', trigger: 'blur' }],
                name: [{ required: true, message: '卡口名称不能为空', trigger: 'blur' }],
                longitude: [{ required: true, message: '卡口经度不能为空', trigger: 'blur' }],
                latitude: [{ required: true, message: '卡口纬度不能为空', trigger: 'blur' }],
                placeCode: [{ required: true, message: '位置编码不能为空', trigger: 'blur' }],
                tollgateCat: [{ required: true, message: '卡口类型不能为空', trigger: 'blur' }],
                tollgateUsage: [{ required: true, message: '卡口用途不能为空', trigger: 'blur' }],
                laneNum: [{ required: true, message: '车道数量不能为空', trigger: 'blur' }],
                orgCode: [{ required: true, message: '组织机构编码不能为空', trigger: 'blur' }],
                // deviceId: [{ required: true, message: '关联设备编号不能为空', trigger: 'blur' }]
            },
            deviceOptions: []
        }
    },
    created() {
        this.loadDeviceOptions()
    },
    methods: {
        showCard() {
            this.$emit('showCard', this.showOptions)
        },
        loadDeviceOptions(keyword, id) {
            getDeviceOptions({name: keyword, apeId: id}).then(response => {
                this.deviceOptions = response.data
            })
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    this.loadingOptions.loading = true
                    this.loadingOptions.loadingText = '保存中...'
                    this.loadingOptions.isDisabled = true
                    addTollgate(this.form).then(response => {
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
