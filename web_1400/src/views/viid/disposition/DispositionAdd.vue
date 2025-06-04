<template>
  <el-card class="box-card" shadow="always">
    <div slot="header" class="clearfix">
      <span>{{ title }}</span>
      <el-button-group style="float: right;">
        <el-button size="mini" icon="el-icon-plus" round
          :loading="loadingOptions.loading" :disabled="loadingOptions.isDisabled" @click="submitForm">{{
            loadingOptions.loadingText }}</el-button>
        <el-button size="mini" icon="el-icon-back" round @click="showCard">返回</el-button>
      </el-button-group>
    </div>
    <div class="body-wrapper">
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="订阅节点" prop="serverId">
          <el-select v-model="form.serverId" placeholder="请选择订阅节点">
            <el-option v-for="server in serverOptions" :key="server.id" :label="server.label"
              :value="server.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="布控标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入布控标题" />
        </el-form-item>
        <el-row>
          <el-col :span="7">
            <el-form-item label="布控类别" prop="dispositionCategory">
              <el-select v-model="form.dispositionCategory" placeholder="请选择布控类别">
                <el-option value="1" label="人"></el-option>
                <el-option value="2" label="机动车"></el-option>
                <el-option value="3" label="非机动车"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="优先等级" prop="priorityLevel">
              <el-input-number v-model="form.priorityLevel" :min="1" controls-position="right" placeholder="请输入优先等级" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
            <el-col :span="7">
              <el-form-item label="布控开始时间" prop="beginTime">
                <el-date-picker v-model="form.beginTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" size="small"
                  placeholder="选择开始时间" align="center"></el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item label="布控结束时间" prop="endTime">
                <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" size="small"
                  placeholder="选择结束时间" align="center"></el-date-picker>
              </el-form-item>
            </el-col>
        </el-row>
        <el-form-item label="应用名称" prop="applicantName">
          <el-input v-model="form.applicantName" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="应用信息" prop="applicantInfo">
          <el-input v-model="form.applicantInfo" placeholder="请输入应用信息" />
        </el-form-item>
        <el-form-item label="应用组织机构" prop="applicantOrg">
          <el-input v-model="form.applicantOrg" placeholder="请输入应用组织机构" />
        </el-form-item>
        <el-form-item label="布控范围" prop="dispositionRange">
          <el-select v-model="form.dispositionRange" placeholder="请选择布控范围">
            <el-option value="1" label="卡口"></el-option>
            <el-option value="2" label="区域布控"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.dispositionRange == '1'" label="布控卡口" prop="tollgateList">
          <el-select v-model="form.tollgateList" multiple clearable filterable remote :remote-method="loadTollgateOptions"
            placeholder="请选择布控卡口">
            <el-option v-for="tollgate in tollgateOptions" :key="tollgate.id" :label="tollgate.label"
              :value="tollgate.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.dispositionRange == '2'" label="布控行政区域" prop="dispositionArea">
          <el-select v-model="form.dispositionArea" multiple filterable allow-create default-first-option
          placeholder="请输入布控行政区域">
          </el-select>
        </el-form-item>
      </el-form>
    </div>
  </el-card>
</template>

<script>
import { addDisposition } from '@/api/datawork/viid/disposition'
import { getServerOptions, getTollgateOptions } from '@/api/datawork/viid/viidutils'

export default {
  name: 'VIIDDispositionAdd',
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
      title: '布控新增',
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
        title: [{ required: true, message: '布控标题不能为空', trigger: 'blur' }],
        dispositionCategory: [{ required: true, message: '布控类别不能为空', trigger: 'blur' }],
        beginTime: [{ required: true, message: '布控开始时间不能为空', trigger: 'blur' }],
        endTime: [{ required: true, message: '布控结束时间不能为空', trigger: 'blur' }],
        dispositionRange: [{ required: true, message: '布控范围不能为空', trigger: 'blur' }]
      },
      serverOptions: [],
      tollgateOptions: []
    }
  },
  created() {
    getServerOptions().then(response => {
      this.serverOptions = response.data
    })
    this.loadTollgateOptions()
  },
  methods: {
    showCard() {
      this.$emit('showCard', this.showOptions)
    },
    loadTollgateOptions(keyword, id) {
      getTollgateOptions({ tollgateId: id, name: keyword }).then(response => {
        this.tollgateOptions = response.data
      })
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.loadingOptions.loading = true
          this.loadingOptions.loadingText = '保存中...'
          this.loadingOptions.isDisabled = true
          if (this.form.dispositionRange == '1') {
            if (this.form.tollgateList) {
              const str = this.form.tollgateList.join(',')
              this.form.tollgateList = str
            }
          } else if (this.form.dispositionRange == '2') {
            if (this.form.dispositionArea) {
              const str = this.form.dispositionArea.join(',')
              this.form.dispositionArea = str
            }
          }
          addDisposition(this.form).then(response => {
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
