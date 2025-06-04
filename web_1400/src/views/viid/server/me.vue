<template>
    <el-card class="box-card" shadow="always">
        <div slot="header" class="clearfix">
            <span>{{ title }}</span>
            <el-button-group style="float: right;">
                <el-button v-hasPerm="['viid:server:edit']" size="mini" icon="el-icon-plus" round
                    :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{
                        loadingOptions.loadingText }}</el-button>
            </el-button-group>
        </div>
        <div class="body-wrapper">
            <el-form ref="form" :model="form" label-width="100px">
                <el-form-item label="视图库编号" prop="serverId">
                    <el-input v-model="form.serverId" placeholder="请输入视图库编号" :disabled="true" />
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
                                    <el-option label="HTTP" value="HTTP"></el-option>
                                    <el-option label="HTTPS" value="HTTPS"></el-option>
                                </el-select>
                            </el-input>
                            <el-input v-model="form.port" placeholder="端口" style="width: 90px;" class="clear-number-input"
                                type="number" />
                        </el-col>
                    </el-row>
                </el-form-item>
                <el-form-item label="授权用户" prop="username">
                    <el-input v-model="form.username" placeholder="请输入授权用户" />
                </el-form-item>
                <el-form-item label="授权凭证" prop="authenticate">
                    <el-input v-model="form.authenticate" placeholder="请输入授权凭证" />
                </el-form-item>
            </el-form>
        </div>
    </el-card>
</template>
  
<script>
import { getCurrentServer, updateCurrentServer } from '@/api/datawork/viid/server'

export default {
    name: 'CurrentServerInfo',
    data() {
        return {
            title: '本级视图库信息',
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
        this.getServerInfo()
    },
    methods: {
        getServerInfo() {
            getCurrentServer().then(response => {
                this.form = response.data
            })
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    this.loadingOptions.loading = true
                    this.loadingOptions.loadingText = '保存中...'
                    this.loadingOptions.isDisabled = true
                    updateCurrentServer(this.form).then(response => {
                        if (response.success) {
                            this.$message.success('保存成功')
                            this.loadingOptions.loading = false
                            this.loadingOptions.loadingText = '保存'
                            this.loadingOptions.isDisabled = false
                            setTimeout(() => {
                                this.getServerInfo()
                            }, 500)
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
  