<template>
    <el-card class="box-card" shadow="always">
        <div slot="header" class="clearfix">
            <span>{{ title }}</span>
            <el-button-group style="float: right;">
                <el-button v-hasPerm="['viid:server:edit']" size="mini" icon="el-icon-plus" round
                    :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{
                        loadingOptions.loadingText }}</el-button>
                <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
            </el-button-group>
        </div>
        <div class="body-wrapper">
            <el-form ref="form" :model="form" label-width="100px">
                <el-form-item label="视图库编号" prop="serverId">
                    <el-input v-model="form.serverId" placeholder="请输入视图库编号" :disabled="true"/>
                </el-form-item>
                <el-form-item label="节点类别" prop="category">
                    <el-select v-model="form.category" placeholder="请选择协议">
                        <el-option label="下级节点" value="1"></el-option>
                        <el-option label="上级节点" value="2"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="视图库名称" prop="serverName">
                    <el-input v-model="form.serverName" placeholder="请输入视图库编号" />
                </el-form-item>
                <el-form-item label="视图库地址" prop="serverName">
                    <el-row>
                        <el-col>
                            <el-input placeholder="视图库地址" v-model="form.host" class="input-with-select"
                                style="width: 300px;">
                                <el-select v-model="form.scheme" slot="prepend" placeholder="请选择协议" style="width: 120px;">
                                    <el-option label="HTTP" value="http"></el-option>
                                    <el-option label="HTTPS" value="https"></el-option>
                                </el-select>
                            </el-input>
                            <el-input v-model="form.port" placeholder="端口" style="width: 90px;" class="clear-number-input"
                                type="number" :min="1" />
                        </el-col>
                    </el-row>
                </el-form-item>
                <el-form-item label="授权用户" prop="username">
                    <el-input v-model="form.username" placeholder="请输入授权用户" />
                </el-form-item>
                <el-form-item label="授权凭证" prop="authenticate">
                    <el-input v-model="form.authenticate" placeholder="请输入授权凭证" />
                </el-form-item>
                <el-form-item label="数据传输类型" prop="transmission">
                    <el-select v-model="form.transmission" placeholder="请选择数据传输类型">
                        <el-option label="标准http协议" value="http"></el-option>
                        <el-option label="跨网websocket协议" value="websocket"></el-option>
                        <el-option label="设备直推" value="device"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="代理网络类型" prop="proxyNetwork">
                    <el-select v-model="form.proxyNetwork" placeholder="请选择代理网络类型">
                        <el-option label="直连网络" value="1"></el-option>
                        <el-option label="跨网边界" value="2"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
        </div>
    </el-card>
</template>
  
<script>
import { upsertServer, getServer } from '@/api/datawork/viid/server'

export default {
    name: 'ServerEdit',
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
            title: '视图库节点编辑',
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
        }
    },
    created() {
        getServer(this.data.id).then(response => {
            this.form = response.data
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
                    upsertServer(this.form).then(response => {
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
  