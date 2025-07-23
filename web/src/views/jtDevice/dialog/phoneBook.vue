<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="设置电话本"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <el-form :inline="true" size="mini" @submit.native.prevent>
        <el-form-item>
          <el-button-group>
            <el-button v-if="!showUpload" icon="el-icon-upload2" size="mini" type="primary" @click="uploadData" :disabled="phoneBookList.length === 0">导入数据</el-button>
            <el-button v-if="showUpload" icon="el-icon-close" size="mini" type="danger" @click="uploadData">结束导入</el-button>
            <el-button icon="el-icon-download">
              <a style="text-align: center; text-decoration: none"
                 href="/static/file/设置电话本模板.xlsx"
                 download="设置电话本模板.xlsx"
               >下载模板</a>
            </el-button>
          </el-button-group>

        </el-form-item>
        <el-form-item style="float: right;">
          <el-button-group>
            <el-button icon="el-icon-delete" size="mini" @click="clearPhoneBook">清空电话本</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="uploadPhoneBook">更新电话本</el-button>
            <el-button icon="el-icon-document-add" size="mini" @click="appendPhoneBook">追加电话本</el-button>
            <el-button icon="el-icon-edit-outline" size="mini" @click="editPhoneBook">修改电话本</el-button>
          </el-button-group>

        </el-form-item>
      </el-form>

      <el-table :data="phoneBookList" v-if="!showUpload && phoneBookList.length > 0" :height="500" stripe style="width: 100%" empty-text="暂无数据，点击选择或者拖入文件" @click.stop="()=>{}">
        <el-table-column label="标志">
          <template v-slot:default="scope">
            <span >{{ signLabel(scope.row.sign) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" />
        <el-table-column prop="phoneNumber" label="电话号码" />
        <el-table-column label="操作" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              type="text"
              style="color: #f56c6c"
              icon="el-icon-delete"
              :loading="scope.row.addRegionLoading"
              @click="removeRow(scope.$index)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-upload
        v-if="showUpload || phoneBookList.length === 0"
        style="width: fit-content; height: 300px; margin: 86px auto 0 auto"
        drag
        accept=".xls,.xlsx"
        action=""
        :auto-upload="false"
        :show-file-list="false"
        :on-change="loadFiled">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">只能上传xls/xlsx文件</div>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>

import * as XLSX from 'xlsx'
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      phoneNumber: null,
      showDialog: false,
      showUpload: false,
      phoneBookList: []
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(phoneNumber) {
      this.showDialog = true
      this.phoneNumber = phoneNumber
    },
    close: function() {
      this.showDialog = false
    },
    signLabel: function(sign) {
      switch (sign){
        case 1:
          return '呼入'
        case 2:
          return '呼出'
        case 3:
          return '呼入/呼出'
        default:
          return '错误： 设置范围（1:呼入,2:呼出,3:呼入/呼出）'
      }
    },
    uploadData: function() {
      this.showUpload = !this.showUpload
    },
    loadFiled: function(file) {
      if (!file.name.endsWith('.xls') && !file.name.endsWith('.xlsx')) {
        this.$message.error('文件格式错误')
        return
      }
      const fileReader = new FileReader()
      fileReader.onload = (event) => {
        const data = new Uint8Array(event.target.result)
        const workbook = XLSX.read(data, { type: 'array' })
        const sheetName = workbook.SheetNames[0]
        const worksheet = workbook.Sheets[sheetName]
        const jsonData = XLSX.utils.sheet_to_json(worksheet)
        for (let i = 0; i < jsonData.length; i++) {
          let item = jsonData[i]
          this.phoneBookList.push({
            sign: item['标志'],
            phoneNumber: item['电话号码'],
            contactName: item['联系人']
          })
        }
        this.showUpload = false
      }
      fileReader.readAsArrayBuffer(file.raw)
    },
    removeRow: function(index) {
      this.phoneBookList.splice(index, 1)
    },
    clearPhoneBook: function() {
      this.$confirm('将清空终端中已有全部联系人， 确认？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.submit({
          phoneNumber: this.phoneNumber,
          type: 0
        })
      })

    },
    uploadPhoneBook: function() {
      this.$confirm('将删除终端中已有全部联系人并追加当前的联系人， 确认？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.submit({
          phoneNumber: this.phoneNumber,
          type: 1,
          phoneBookContactList: this.phoneBookList
        })
      })

    },
    appendPhoneBook: function() {
      this.$confirm('将追加当前的联系人到终端中， 确认？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.submit({
          phoneNumber: this.phoneNumber,
          type: 2,
          phoneBookContactList: this.phoneBookList
        })
      })

    },
    editPhoneBook: function() {
      this.submit({
        phoneNumber: this.phoneNumber,
        type: 3,
        phoneBookContactList: this.phoneBookList
      })
    },
    submit: function(data) {
      this.$store.dispatch("jtDevice/setPhoneBook", data)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: '消息已下发'
          })
        })
    }

  }
}
</script>

<style scoped>
>>> .el-upload {
  width: 100% !important;
}
</style>
