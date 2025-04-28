<template>
  <el-dialog
    v-el-drag-dialog
    title="生成国标编码"
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
        <el-radio-group v-model="allVal[0].val">
          <el-radio v-for="item in regionList" :key="item.deviceId" :label="item.deviceId" style="line-height: 2rem">
            {{ item.name }} - {{ item.deviceId }}
          </el-radio>
        </el-radio-group>
      </el-tab-pane>
      <el-tab-pane name="1">
        <div slot="label">
          <div class="show-code-item">{{ allVal[1].val }}</div>
          <div style="text-align: center">{{ allVal[1].meaning }}</div>
        </div>
        <el-radio-group v-model="allVal[1].val" :disabled="allVal[1].lock">
          <el-radio v-for="item in regionList" :key="item.deviceId" :label="item.deviceId.substring(2)" style="line-height: 2rem">
            {{ item.name }} - {{ item.deviceId.substring(2) }}
          </el-radio>
        </el-radio-group>
      </el-tab-pane>
      <el-tab-pane name="2">
        <div slot="label">
          <div class="show-code-item">{{ allVal[2].val }}</div>
          <div style="text-align: center">{{ allVal[2].meaning }}</div>
        </div>
        <el-radio-group v-model="allVal[2].val" :disabled="allVal[2].lock">
          <el-radio v-for="item in regionList" :key="item.deviceId" :label="item.deviceId.substring(4)" style="line-height: 2rem">
            {{ item.name }} - {{ item.deviceId.substring(4) }}
          </el-radio>
        </el-radio-group>
      </el-tab-pane>
      <el-tab-pane name="3">
        请手动输入基层接入单位编码,两位数字
        <div slot="label">
          <div class="show-code-item">{{ allVal[3].val }}</div>
          <div style="text-align: center">{{ allVal[3].meaning }}</div>
        </div>
        <el-input
          v-model="allVal[3].val"
          type="text"
          placeholder="请输入内容"
          maxlength="2"
          :disabled="allVal[3].lock"
          show-word-limit
        />
      </el-tab-pane>
      <el-tab-pane name="4">
        <div slot="label">
          <div class="show-code-item">{{ allVal[4].val }}</div>
          <div style="text-align: center; ">{{ allVal[4].meaning }}</div>
        </div>
        <el-radio-group v-model="allVal[4].val" :disabled="allVal[4].lock">
          <el-radio v-for="item in industryCodeTypeList" :key="item.code" :label="item.code" style="line-height: 2rem">
            {{ item.name }} - {{ item.code }}
          </el-radio>
        </el-radio-group>
      </el-tab-pane>
      <el-tab-pane name="5">
        <div slot="label">
          <div class="show-code-item">{{ allVal[5].val }}</div>
          <div style="text-align: center">{{ allVal[5].meaning }}</div>
        </div>
        <el-radio-group v-model="allVal[5].val" :disabled="allVal[5].lock">
          <el-radio v-for="item in deviceTypeList" :key="item.code" :label="item.code" style="line-height: 2rem">
            {{ item.name }} - {{ item.code }}
          </el-radio>
        </el-radio-group>
      </el-tab-pane>
      <el-tab-pane name="6">
        <div slot="label">
          <div class="show-code-item">{{ allVal[6].val }}</div>
          <div style="text-align: center">{{ allVal[6].meaning }}</div>
        </div>
        <el-radio-group v-model="allVal[6].val" :disabled="allVal[6].lock">
          <el-radio v-for="item in networkIdentificationTypeList" :key="item.code" :label="item.code" style="line-height: 2rem">
            {{ item.name }} - {{ item.code }}
          </el-radio>
        </el-radio-group>
      </el-tab-pane>
      <el-tab-pane name="7">
        请手动输入设备/用户序号, 六位数字
        <div slot="label">
          <div class="show-code-item">{{ allVal[7].val }}</div>
          <div style="text-align: center">{{ allVal[7].meaning }}</div>
        </div>
        <el-input
          v-model="allVal[7].val"
          type="text"
          placeholder="请输入内容"
          maxlength="6"
          :disabled="allVal[7].lock"
          show-word-limit
        />
      </el-tab-pane>
    </el-tabs>
    <el-form style="">

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
          val: '01',
          type: '中心编码',
          lock: false
        },
        {
          id: [5, 6],
          meaning: '区级编码',
          val: '01',
          type: '中心编码',
          lock: false
        },
        {
          id: [7, 8],
          meaning: '基层接入单位编码',
          val: '01',
          type: '中心编码',
          lock: false
        },
        {
          id: [9, 10],
          meaning: '行业编码',
          val: '00',
          type: '行业编码',
          lock: false
        },
        {
          id: [11, 13],
          meaning: '类型编码',
          val: '132',
          type: '类型编码',
          lock: false
        },
        {
          id: [14],
          meaning: '网络标识编码',
          val: '7',
          type: '网络标识',
          lock: false
        },
        {
          id: [15, 20],
          meaning: '设备/用户序号',
          val: '000001',
          type: '序号',
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
    openDialog: function(endCallBck, code, lockIndex, lockContent) {
      console.log(code)
      this.showVideoDialog = true
      this.activeKey = '0'
      this.regionList = []

      this.getRegionList()
      if (typeof code !== 'undefined' && code.length === 20) {
        this.allVal[0].val = code.substring(0, 2)
        this.allVal[1].val = code.substring(2, 4)
        this.allVal[2].val = code.substring(4, 6)
        this.allVal[3].val = code.substring(6, 8)
        this.allVal[4].val = code.substring(8, 10)
        this.allVal[5].val = code.substring(10, 13)
        this.allVal[6].val = code.substring(13, 14)
        this.allVal[7].val = code.substring(14)
      }
      console.log(this.allVal)
      if (typeof lockIndex !== 'undefined') {
        this.allVal[lockIndex].lock = true
        this.allVal[lockIndex].val = lockContent
      }
      this.endCallBck = endCallBck
    },
    getRegionList: function() {
      if (this.activeKey === '0' || this.activeKey === '1' || this.activeKey === '2') {
        let parent = ''
        if (this.activeKey === '1') {
          parent = this.allVal[0].val
        }
        if (this.activeKey === '2') {
          parent = this.allVal[0].val + this.allVal[1].val
        }
        if (this.activeKey !== '0' && parent === '') {
          this.$message.error({
            showClose: true,
            message: '请先选择上级行政区划'
          })
        }
        this.queryChildList(parent)
      } else if (this.activeKey === '4') {
        console.log(222)
        this.queryIndustryCodeList()
      } else if (this.activeKey === '5') {
        this.queryDeviceTypeList()
      } else if (this.activeKey === '6') {
        this.queryNetworkIdentificationTypeList()
      }
    },
    queryChildList: function(parent) {
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
    queryIndustryCodeList: function() {
      this.industryCodeTypeList = []
      this.$store.dispatch('commonChanel/getIndustryList')
        .then(data => {
          this.industryCodeTypeList = data
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
    },
    queryDeviceTypeList: function() {
      this.deviceTypeList = []
      this.$store.dispatch('commonChanel/getTypeList')
        .then(data => {
          this.deviceTypeList = data
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
    },
    queryNetworkIdentificationTypeList: function() {
      this.networkIdentificationTypeList = []
      this.$store.dispatch('commonChanel/getNetworkIdentificationList')
        .then(data => {
          this.networkIdentificationTypeList = data
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
    handleOk: function() {
      const code =
        this.allVal[0].val +
        this.allVal[1].val +
        this.allVal[2].val +
        this.allVal[3].val +
        this.allVal[4].val +
        this.allVal[5].val +
        this.allVal[6].val +
        this.allVal[7].val
      console.log(code)
      if (this.endCallBck) {
        this.endCallBck(code)
      }
      this.showVideoDialog = false
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
