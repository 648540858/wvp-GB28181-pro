<template>
    <el-card class="box-card" shadow="always">
        <div slot="header" class="clearfix">
            <span>{{ title }}</span>
            <el-button-group style="float: right;">
                <el-button v-hasPerm="['viid:publish:add']" size="mini" icon="el-icon-plus" round
                    :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{
                        loadingOptions.loadingText }}</el-button>
                <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
            </el-button-group>
        </div>
        <div class="body-wrapper">
            <el-form ref="form" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="订阅节点" prop="serverId">
                    <el-select v-model="form.serverId" placeholder="请选择订阅节点">
                        <el-option v-for="server in serverOptions" :key="server.id" :label="server.label"
                            :value="server.value" :disabled="server.data === '0'"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="订阅标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入订阅标题" />
                </el-form-item>
                <el-form-item label="订阅类型" prop="subscribeDetail">
                    <el-select v-model="form.subscribeDetail" placeholder="请选择订阅类型">
                        <el-option v-for="item in subscribeDatailOptions" :key="item.id" :label="item.label"
                            :value="item.value"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="资源列表" prop="resourceUri">
                    <el-select v-model="form.resourceUri" multiple placeholder="请选择资源列表" style="width: 100%;">
                        <el-option v-for="item in serverOptions" :key="item.id" :label="item.label"
                            :value="item.value"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="图片格式" prop="resultImageDeclare">
                    <el-select v-model="form.resultImageDeclare" placeholder="请选择图片格式">
                        <el-option label="Base64" value="1" />
                        <el-option label="URL" value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="订阅回调地址" prop="receiveAddr">
                    <el-input v-model="form.receiveAddr" placeholder="请输入订阅回调地址" />
                </el-form-item>
                <el-form-item label="下发间隔(秒)" prop="reportInterval">
                    <el-input type="number" v-model="form.reportInterval" placeholder="请输入数据上报间隔" />
                </el-form-item>
                <el-form-item label="申请人" prop="applicationName">
                    <el-input v-model="form.applicationName" placeholder="请输入申请人" />
                </el-form-item>
                <el-form-item label="申请单位" prop="applicationOrg">
                    <el-input v-model="form.applicationOrg" placeholder="请输入申请单位" />
                </el-form-item>
                <el-form-item label="理由" prop="reason">
                    <el-input v-model="form.reason" placeholder="请输入理由" />
                </el-form-item>
                <el-form-item label="订阅时间">
                    <el-date-picker v-model="form.beginTime" value-format="yyyy-MM-dd HH:mm:ss" type="datetime" placeholder="订阅开始时间" default-time="00:00:00" />至
                    <el-date-picker v-model="form.endTime" value-format="yyyy-MM-dd HH:mm:ss" type="datetime" placeholder="订阅结束时间" default-time="23:59:59" />
                </el-form-item>
            </el-form>
        </div>
    </el-card>
</template>

<script>
import { getServerOptions, getSubscribeDetailOptions } from '@/api/datawork/viid/viidutils'
import { addPublish } from '@/api/datawork/viid/publish'

export default {
    name: 'VIIDPublishAdd',
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
            title: '视图库发布新增',
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
                serverId: [{ required: true, message: '视图库节点不能为空', trigger: 'blur' }],
                title: [{ required: true, message: '订阅标题不能为空', trigger: 'blur' }],
                subscribeDetail: [{ required: true, message: '订阅类型不能为空', trigger: 'blur' }],
                resourceUri: [{ required: true, message: '资源列表不能为空', trigger: 'blur' }]
            },
            serverOptions: [],
            subscribeDatailOptions: []
        }
    },
    created() {
        getServerOptions().then(response => {
            this.serverOptions = response.data
        })
        getSubscribeDetailOptions().then(response => {
            this.subscribeDatailOptions = response.data
        })
    },
    methods: {
        showCard() {
            this.$emit('showCard', this.showOptions)
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    this.loadingOptions.loading = true
                    this.loadingOptions.loadingText = '保存中...'
                    this.loadingOptions.isDisabled = true
                    addPublish(this.form).then(response => {
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
