<template>
  <el-dialog
    v-el-drag-dialog
    title="生成行政区划编码"
    width="65rem"
    top="2rem"
    center
    :append-to-body="true"
    :close-on-click-modal="false"
    :visible.sync="showVideoDialog"
    :destroy-on-close="false"
  >
    <el-tabs v-model="activeKey" style="padding: 0 1rem; margin: auto 0" @tab-click="getRegionList">
      <el-tab-pane name="0">
        <div slot="label">
          <div class="show-code-item">{{ allVal[0].val }}</div>
          <div style="text-align: center">{{ allVal[0].meaning }}</div>
        </div>
        <el-radio v-for="item in regionList" :key="item.deviceId" v-model="allVal[0].val" :name="item.name" :label="item.deviceId" style="line-height: 2rem" @input="deviceChange(item)">
          {{ item.name }} - {{ item.deviceId }}
        </el-radio>
      </el-tab-pane>
      <el-tab-pane name="1">
        <div slot="label">
          <div class="show-code-item">{{ allVal[1].val?allVal[1].val:"--" }}</div>
          <div style="text-align: center">{{ allVal[1].meaning }}</div>
        </div>
        <el-radio :key="-1" v-model="allVal[1].val" label="" style="line-height: 2rem" @input="deviceChange">
          不添加
        </el-radio>
        <el-radio v-for="item in regionList" :key="item.deviceId" v-model="allVal[1].val" :label="item.deviceId.substring(2)" style="line-height: 2rem" @input="deviceChange(item)">
          {{ item.name }} - {{ item.deviceId.substring(2) }}
        </el-radio>
      </el-tab-pane>
      <el-tab-pane name="2">
        <div slot="label">
          <div class="show-code-item">{{ allVal[2].val?allVal[2].val:"--" }}</div>
          <div style="text-align: center">{{ allVal[2].meaning }}</div>
        </div>
        <el-radio :key="-1" v-model="allVal[2].val" label="" style="line-height: 2rem" @input="deviceChange">
          不添加
        </el-radio>
        <el-radio v-for="item in regionList" :key="item.deviceId" v-model="allVal[2].val" :label="item.deviceId.substring(4)" style="line-height: 2rem" @input="deviceChange(item)">
          {{ item.name }} - {{ item.deviceId.substring(4) }}
        </el-radio>
      </el-tab-pane>
      <el-tab-pane name="3">
        请手动输入基层接入单位编码,两位数字
        <div slot="label">
          <div class="show-code-item">{{ allVal[3].val?allVal[3].val:"--" }}</div>
          <div style="text-align: center">{{ allVal[3].meaning }}</div>
        </div>
        <el-input
          v-model="allVal[3].val"
          type="text"
          placeholder="请输入内容"
          maxlength="2"
          :disabled="allVal[3].lock"
          show-word-limit
          @input="deviceChange"
        />
      </el-tab-pane>
    </el-tabs>
    <el-form ref="form" style="  display: grid; padding: 1rem 2rem 0 2rem;grid-template-columns: 1fr 1fr 1fr; gap: 1rem;">
      <el-form-item label="名称" prop="name" size="mini">
        <el-input v-model="form.name" autocomplete="off" />
      </el-form-item>
      <el-form-item label="编号" prop="deviceId" size="mini">
        <el-input v-model="form.deviceId" autocomplete="off" />
      </el-form-item>
      <el-form-item style="margin-top: 22px; margin-bottom: 0;">
        <div style="float:right;">
          <el-button type="primary" @click="handleOk">保存</el-button>
          <el-button @click="closeModel">取消</el-button>
        </div>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showVideoDialog: false,
      activeKey: '0',
      form: {
        name: '',
        deviceId: '',
        parentId: ''
      },
      allVal: [
        {
          id: [1, 2],
          meaning: '省级编码',
          val: '11',
          type: '中心编码',
          lock: false
        },
        {
          id: [3, 4],
          meaning: '市级编码',
          val: '',
          type: '中心编码',
          lock: false
        },
        {
          id: [5, 6],
          meaning: '区级编码',
          val: '',
          type: '中心编码',
          lock: false
        },
        {
          id: [7, 8],
          meaning: '基层接入单位编码',
          val: '',
          type: '中心编码',
          lock: false
        }
      ],
      regionList: [],
      deviceTypeList: [],
      industryCodeTypeList: [],
      networkIdentificationTypeList: [],
      endCallBck: null
    }
  },
  computed: {},
  methods: {
    openDialog: function(endCallBck, region, code, lockContent) {
      this.showVideoDialog = true
      this.activeKey = '0'
      this.regionList = []
      this.form = region
      this.allVal = [
        {
          id: [1, 2],
          meaning: '省级编码',
          val: '11',
          type: '中心编码',
          lock: false
        },
        {
          id: [3, 4],
          meaning: '市级编码',
          val: '',
          type: '中心编码',
          lock: false
        },
        {
          id: [5, 6],
          meaning: '区级编码',
          val: '',
          type: '中心编码',
          lock: false
        },
        {
          id: [7, 8],
          meaning: '基层接入单位编码',
          val: '',
          type: '中心编码',
          lock: false
        }
      ]
      if (this.form.deviceId) {
        if (this.form.deviceId.length >= 2) {
          this.allVal[0].val = this.form.deviceId.substring(0, 2)
          this.activeKey = '0'
        }
        if (this.form.deviceId.length >= 4) {
          this.allVal[1].val = this.form.deviceId.substring(2, 4)
          this.activeKey = '1'
        }
        if (this.form.deviceId.length >= 6) {
          this.allVal[2].val = this.form.deviceId.substring(4, 6)
          this.activeKey = '2'
        }
        if (this.form.deviceId.length === 8) {
          this.allVal[3].val = this.form.deviceId.substring(6, 8)
          this.activeKey = '3'
        }
      } else {
        if (this.form.parentDeviceId) {
          if (this.form.parentDeviceId.length >= 2) {
            this.allVal[0].val = this.form.parentDeviceId.substring(0, 2)
            this.activeKey = '1'
          }
          if (this.form.parentDeviceId.length >= 4) {
            this.allVal[1].val = this.form.parentDeviceId.substring(2, 4)
            this.activeKey = '2'
          }
          if (this.form.parentDeviceId.length >= 6) {
            this.allVal[2].val = this.form.parentDeviceId.substring(4, 6)
            this.activeKey = '3'
          }
        }
      }

      this.getRegionList()
      this.endCallBck = endCallBck
    },
    getRegionList: function() {
      console.log('getRegionList')
      if (this.activeKey === '0') {
        this.queryChildList()
      } else if (this.activeKey === '1' || this.activeKey === '2') {
        let parent = ''
        if (this.activeKey === '1') {
          parent = this.allVal[0].val
        }
        if (this.activeKey === '2') {
          if (this.allVal[1].val === '') {
            parent = ''
          } else {
            parent = this.allVal[0].val + this.allVal[1].val
          }
        }
        if (this.activeKey !== '0' && parent === '') {
          this.$message.error({
            showClose: true,
            message: '请先选择上级行政区划'
          })
        }
        if (parent !== '') {
          this.queryChildList(parent)
        } else {
          this.regionList = []
        }
      }
    },
    queryChildList: function(parent) {
      console.log('queryChildList')
      this.regionList = []
      this.$store.dispatch('region/queryChildListInBase', parent)
        .then(data => {
          this.regionList = data
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
    },
    closeModel: function() {
      this.showVideoDialog = false
    },
    deviceChange: function(item) {
      console.log(item)
      let code = this.allVal[0].val
      if (this.allVal[1].val) {
        code += this.allVal[1].val
        if (this.allVal[2].val) {
          code += this.allVal[2].val
          if (this.allVal[3].val) {
            code += this.allVal[3].val
          }
        } else {
          this.allVal[3].val = ''
        }
      } else {
        this.allVal[2].val = ''
        this.allVal[3].val = ''
      }
      this.form.deviceId = code
      if (item) {
        this.form.name = item.name
      }
    },
    handleOk: function() {
      if (this.form.id) {
        this.$store.dispatch('region/update', this.form)
          .then((data) => {
            if (typeof this.endCallBck === 'function') {
              this.endCallBck(this.form)
            }
            this.showVideoDialog = false
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
          })
      } else {
        this.$store.dispatch('region/add', this.form)
          .then((data) => {
            if (typeof this.endCallBck === 'function') {
              this.endCallBck(this.form)
            }
            this.showVideoDialog = false
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
          })
      }
    }
  }
}
</script>

<style>
.show-code-item {
  text-align: center;
  font-size: 3rem;
}
</style>
