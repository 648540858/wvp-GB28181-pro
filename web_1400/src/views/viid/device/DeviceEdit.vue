<template>
    <el-card class="box-card" shadow="always">
        <div slot="header" class="clearfix">
            <span>{{ title }}</span>
            <el-button-group style="float: right;">
                <el-button v-hasPerm="['viid:apedevice:edit']" size="mini" icon="el-icon-plus" round
                    :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{
                        loadingOptions.loadingText }}</el-button>
                <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
            </el-button-group>
        </div>
        <div class="body-wrapper">
            <el-form ref="form" :model="form" :rules="rules" label-width="110px">
                <el-tooltip class="item" effect="dark" content="国标格式设备编号(6位地区编码+0000132+7位随机数)设备唯一不可重复" placement="top">
                    <el-form-item label="设备标识" prop="apeId">
                        <el-input v-model="form.apeId" placeholder="请输入设备标识" :disabled="true"/>
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备标识性展示名称" placement="top">
                    <el-form-item label="设备名称" prop="name">
                        <el-input v-model="form.name" placeholder="请输入卡口名称" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备标识性展示型号" placement="top">
                    <el-form-item label="设备型号" prop="model">
                        <el-input v-model="form.model" placeholder="请输入设备型号" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备IP地址,单向对接数据可任意填写127.0.0.1" placement="top">
                    <el-form-item label="设备地址" prop="ipAddr">
                        <el-input v-model="form.ipAddr" placeholder="请输入设备地址" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备IP端口,单向对接数据可任意填写8080" placement="top">
                    <el-form-item label="设备端口" prop="port">
                        <el-input v-model="form.port" type="number" placeholder="请输入设备端口" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备安装地点经纬度 1.000001" placement="top">
                    <el-form-item label="设备经度" prop="longitude">
                        <el-input v-model="form.longitude" type="number" placeholder="请输入设备经度" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备安装地点经纬度 1.000001" placement="top">
                    <el-form-item label="设备纬度" prop="latitude">
                        <el-input v-model="form.latitude" type="number" placeholder="请输入设备纬度" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="12位设备安装地区编码 431021001000" placement="top">
                    <el-form-item label="地区编码" prop="placeCode">
                        <el-input v-model="form.placeCode" placeholder="请输入地区编码" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="具体到摄像机位置或街道门牌号，由 (乡镇街道)+ (街路巷)+ (门楼牌号)+ (门楼详细地址)构成" placement="top">
                    <el-form-item label="位置名" prop="place">
                        <el-input v-model="form.place" placeholder="请输入位置名" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="管辖单位代码" placement="top">
                    <el-form-item label="管辖单位代码" prop="orgCode">
                        <el-input v-model="form.orgCode" placeholder="请输入管辖单位代码" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备连接视图库使用的用户名" placement="top">
                    <el-form-item label="用户标识" prop="userId">
                        <el-input v-model="form.userId" placeholder="请输入用户标识" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="设备连接视图库使用的密码" placement="top">
                    <el-form-item label="口令" prop="password">
                        <el-input v-model="form.password" placeholder="请输入口令" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="车辆抓拍方向" placement="top">
                    <el-form-item label="车辆抓拍方向" prop="capDirection">
                      <el-select v-model="form.capDirection" placeholder="请选择抓拍方向">
                        <el-option value="0" label="拍车头"></el-option>
                        <el-option value="1" label="拍车尾"></el-option>
                      </el-select>
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="监视方向" placement="top">
                    <el-form-item label="监视方向" prop="monitorDirection">
                      <el-select v-model="form.monitorDirection" placeholder="请选择监视方向">
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
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="监视区域说明" placement="top">
                    <el-form-item label="监视区域说明" prop="monitorAreaDesc">
                        <el-input v-model="form.monitorAreaDesc" placeholder="请输入监视区域说明" />
                    </el-form-item>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="所属采集系统" placement="top">
                    <el-form-item label="所属采集系统" prop="ownerApsId">
                        <el-select v-model="form.ownerApsId" clearable filterable remote :remote-method="loadServerOptions" placeholder="请选择采集系统">
                            <el-option v-for="item in serverOptions" :key="item.id" :value="item.id" :label="item.label" />
                        </el-select>
                    </el-form-item>
                </el-tooltip>
            </el-form>
        </div>
    </el-card>
</template>

<script>
import { getApeDevice, updateApeDevice } from '@/api/datawork/viid/apedevice'
import { getServerOptions } from '@/api/datawork/viid/viidutils'

export default {
    name: 'VIIDApeDeviceAdd',
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
            title: '视图库APE设备编辑',
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
                apeId: [{ required: true, message: '设备标识不能为空', trigger: 'blur' }],
                name: [{ required: true, message: '设备名称不能为空', trigger: 'blur' }],
                longitude: [{ required: true, message: '卡口经度不能为空', trigger: 'blur' }],
                latitude: [{ required: true, message: '卡口纬度不能为空', trigger: 'blur' }],
                placeCode: [{ required: true, message: '位置编码不能为空', trigger: 'blur' }],
                userId: [{ required: true, message: '用户标识不能为空', trigger: 'blur' }],
                password: [{ required: true, message: '口令不能为空', trigger: 'blur' }],
                ipAddr: [{ required: true, message: '设备地址不能为空', trigger: 'blur' }],
                port: [{ required: true, message: '设备端口不能为空', trigger: 'blur' }]
            },
            serverOptions: []
        }
    },
    created() {
        getApeDevice(this.data.id).then(response => {
            this.form = response.data
            this.loadServerOptions(null, this.form.ownerApsId)
        })
    },
    methods: {
        showCard() {
            this.$emit('showCard', this.showOptions)
        },
        loadServerOptions(keyword, id) {
            getServerOptions({serverName: keyword, serverId: id}).then(response => {
                this.serverOptions = response.data
            })
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    this.loadingOptions.loading = true
                    this.loadingOptions.loadingText = '保存中...'
                    this.loadingOptions.isDisabled = true
                    updateApeDevice(this.form).then(response => {
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
