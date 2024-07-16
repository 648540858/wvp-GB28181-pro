<template>
  <el-dialog
    title="生成国标编码"
    width="60%"
    top="2rem"
    center
    :close-on-click-modal="false"
    :visible.sync="showVideoDialog"
    :destroy-on-close="false"
  >
    <el-tabs v-model="activeKey" style="padding: 0 1rem" @tab-click="getRegionList">
      <el-tab-pane name="0" >
        <div slot="label" >
          <div class="show-code-item">{{ allVal[0].val }}</div>
          <div style="text-align: center">{{ allVal[0].meaning }}</div>
        </div>
        <el-radio-group v-model="allVal[0].val" >
          <el-radio v-for="item in regionList" :key="item.commonRegionDeviceId" :label="item.commonRegionDeviceId" style="line-height: 2rem">
            {{ item.commonRegionName }} - {{ item.commonRegionDeviceId }}
          </el-radio>
        </el-radio-group>
      </el-tab-pane>
        <el-tab-pane name="1">
          <div slot="label">
            <div class="show-code-item">{{ allVal[1].val }}</div>
            <div style="text-align: center">{{ allVal[1].meaning }}</div>
          </div>
          <el-radio-group v-model="allVal[1].val" :disabled="allVal[1].lock">
            <el-radio v-for="item in regionList" :key="item.commonRegionDeviceId" :label="item.commonRegionDeviceId.substring(2)" style="line-height: 2rem">
              {{ item.commonRegionName }} - {{ item.commonRegionDeviceId.substring(2) }}
            </el-radio>
          </el-radio-group>
        </el-tab-pane>
        <el-tab-pane name="2">
          <div slot="label">
            <div class="show-code-item">{{ allVal[2].val }}</div>
            <div style="text-align: center">{{ allVal[2].meaning }}</div>
          </div>
          <el-radio-group v-model="allVal[2].val" :disabled="allVal[2].lock">
            <el-radio v-for="item in regionList" :key="item.commonRegionDeviceId" :label="item.commonRegionDeviceId.substring(4)" style="line-height: 2rem">
              {{ item.commonRegionName }} - {{ item.commonRegionDeviceId.substring(4) }}
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
            type="text"
            placeholder="请输入内容"
            v-model="allVal[3].val"
            maxlength="2"
            :disabled="allVal[3].lock"
            show-word-limit
          >
          </el-input>
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
          <el-radio-group v-model="allVal[5].val" :disabled="allVal[5].lock" >
            <el-radio v-for="item in deviceTypeList" :label="item.code" :key="item.code" style="line-height: 2rem">
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
            <el-radio v-for="item in networkIdentificationTypeList" :label="item.code" :key="item.code" style="line-height: 2rem">
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
            type="text"
            placeholder="请输入内容"
            v-model="allVal[7].val"
            maxlength="6"
            :disabled="allVal[7].lock"
            show-word-limit
          >
          </el-input>
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

export default {
  props: {},
  computed: {},
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
          lock: false,
        },
        {
          id: [3, 4],
          meaning: '市级编码',
          val: '01',
          type: '中心编码',
          lock: false,
        },
        {
          id: [5, 6],
          meaning: '区级编码',
          val: '01',
          type: '中心编码',
          lock: false,
        },
        {
          id: [7, 8],
          meaning: '基层接入单位编码',
          val: '01',
          type: '中心编码',
          lock: false,
        },
        {
          id: [9, 10],
          meaning: '行业编码',
          val: '00',
          type: '行业编码',
          lock: false,
        },
        {
          id: [11, 13],
          meaning: '类型编码',
          val: '132',
          type: '类型编码',
          lock: false,
        },
        {
          id: [14],
          meaning: '网络标识编码',
          val: '7',
          type: '网络标识',
          lock: false,
        },
        {
          id: [15, 20],
          meaning: '设备/用户序号',
          val: '000001',
          type: '序号',
          lock: false,
        }
      ],
      regionList: [],
      deviceTypeList: [],
      industryCodeTypeList: [],
      networkIdentificationTypeList: [],
      endCallBck: null,
    };
  },
  methods: {
    openDialog: function (endCallBck, code, lockIndex, lockContent) {
      console.log(code)
      this.showVideoDialog = true
      this.activeKey= '0';
      this.regionList = []

      this.getRegionList()
      if (typeof code != 'undefined' && code.length === 20) {
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
      if (typeof lockIndex != 'undefined') {
        this.allVal[lockIndex].lock = true
        this.allVal[lockIndex].val = lockContent
      }
      this.endCallBck = endCallBck;
    },
    getRegionList: function() {
      if (this.activeKey === '0' || this.activeKey === '1' || this.activeKey === '2') {
        let parent = ''
        if (this.activeKey === '1') {
          parent = this.allVal[0].val
          if (parent === '11' || parent === '12' || parent === '31') {
            this.regionList = []
            this.regionList.push({
              // 数据库自增ID
              commonRegionId: -1,
              // 区域国标编号
              commonRegionDeviceId: parent + '01',
              // 区域名称
              commonRegionName: '市辖区',
              // 父区域国标ID
              commonRegionParentId: parent,
            })
            console.log(this.regionList)
            return
          } else if (parent === '50') {
            this.regionList = [
              {
                // 数据库自增ID
                commonRegionId: -1,
                // 区域国标编号
                commonRegionDeviceId: parent + '01',
                // 区域名称
                commonRegionName: '市辖区',
                // 父区域国标ID
                commonRegionParentId: parent,
              },
              {
                // 数据库自增ID
                commonRegionId: -1,
                // 区域国标编号
                commonRegionDeviceId: parent + '02',
                // 区域名称
                commonRegionName: '县',
                // 父区域国标ID
                commonRegionParentId: parent,
              },
            ]
            return
          }
        }
        if (this.activeKey === '2') {
          if (this.allVal[0].val === '11' || this.allVal[0].val === '12' || this.allVal[0].val === '31' || this.allVal[0].val === '50') {
            parent = this.allVal[0].val
          } else {
            parent = this.allVal[0].val + this.allVal[1].val
          }

          console.log(parent)
        }
        if (this.activeKey !== '0' && parent === '') {
          this.$message.error('请先选择上级行政区划');
        }
        this.queryChildList(parent);
      } else if (this.activeKey === '4') {
        console.log(222)
        this.queryIndustryCodeList();
      } else if (this.activeKey === '5') {
        this.queryDeviceTypeList();
      } else if (this.activeKey === '6') {
        this.queryNetworkIdentificationTypeList();
      }
    },
    queryChildList: function(parent){
      this.regionList = []
      this.$axios({
        method: 'get',
        url: "/api/region/base/child/list",
        params: {
          parent: parent,
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.regionList = res.data.data
        } else {
          this.$message.error(res.data.msg);
        }
      }).catch((error) => {
        this.$message.error(error);
      });
    },
    queryIndustryCodeList: function(){
      this.industryCodeTypeList = []
      this.$axios({
        method: 'get',
        url: "/api/common/channel/industry/list",
      }).then((res) => {
        if (res.data.code === 0) {
          this.industryCodeTypeList = res.data.data
        } else {
          this.$message.error(res.data.msg);
        }
      }).catch((error) => {
        this.$message.error(error);
      });
    },
    queryDeviceTypeList: function(){
      this.deviceTypeList = []
      this.$axios({
        method: 'get',
        url: "/api/common/channel/type/list",
      }).then((res) => {
        if (res.data.code === 0) {
          this.deviceTypeList = res.data.data
        } else {
          this.$message.error(res.data.msg);
        }
      }).catch((error) => {
        this.$message.error(error);
      });
    },
    queryNetworkIdentificationTypeList: function(){
      this.networkIdentificationTypeList = []
      this.$axios({
        method: 'get',
        url: "/api/common/channel/network/identification/list",
      }).then((res) => {
        if (res.data.code === 0) {
          this.networkIdentificationTypeList = res.data.data
        } else {
          this.$message.error(res.data.msg);
        }
      }).catch((error) => {
        this.$message.error(error);
      });
    },
    closeModel: function (){
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
  },
};
</script>

<style>
.show-code-item {
  text-align: center;
  font-size: 3rem;
}
</style>
