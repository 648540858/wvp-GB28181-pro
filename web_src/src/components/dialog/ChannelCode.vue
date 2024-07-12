<template>
  <div id="channelCode" v-loading="loading">
    <el-dialog
      title="生成国标编码"
      width="65rem"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showVideoDialog"
      :destroy-on-close="true"
      @close="closeModel()"
    >
      <el-tabs v-model="activeKey" centered style="padding: 0 1rem">
        <el-tab-pane name="0" >
          <div slot="label">
            <div class="show-code-item">{{ allVal[0].val }}</div>
            <div style="text-align: center">{{ allVal[0].meaning }}</div>
          </div>
          <el-radio-group v-model="allVal[0].val" :disabled="allVal[0].lock">
<!--            <el-radio v-for="item in regionList" :label="item.commonRegionDeviceId">-->
<!--              {{ item.commonRegionName }} - {{ item.commonRegionDeviceId }}-->
<!--            </el-radio>-->
            <el-radio :label="3">备选项</el-radio>
            <el-radio :label="6">备选项</el-radio>
            <el-radio :label="9">备选项</el-radio>
          </el-radio-group>
        </el-tab-pane>
<!--        <el-tab-pane name="1">-->
<!--          <div slot="label">-->
<!--            <div class="show-code-item">{{ allVal[1].val }}</div>-->
<!--            <div style="text-align: center">{{ allVal[1].meaning }}</div>-->
<!--          </div>-->
<!--          <el-radio-group v-model="allVal[1].val" :disabled="allVal[1].lock">-->
<!--            <el-radio v-for="item in regionList" :label="item.commonRegionDeviceId.substring(2)">-->
<!--              {{ item.commonRegionName }} - {{ item.commonRegionDeviceId.substring(2) }}-->
<!--            </el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-tab-pane>-->
<!--        <el-tab-pane name="2">-->
<!--          <div slot="label">-->
<!--            <div class="show-code-item">{{ allVal[2].val }}</div>-->
<!--            <div style="text-align: center">{{ allVal[2].meaning }}</div>-->
<!--          </div>-->
<!--          <el-radio-group v-model="allVal[2].val" :disabled="allVal[2].lock">-->
<!--            <el-radio v-for="item in regionList" :label="item.commonRegionDeviceId.substring(4)">-->
<!--              {{ item.commonRegionName }} - {{ item.commonRegionDeviceId.substring(4) }}-->
<!--            </el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-tab-pane>-->
<!--        <el-tab-pane name="3">-->
<!--          请手动输入基层接入单位编码,两位数字-->
<!--          <div slot="label">-->
<!--            <div class="show-code-item">{{ allVal[3].val }}</div>-->
<!--            <div style="text-align: center">{{ allVal[3].meaning }}</div>-->
<!--          </div>-->
<!--          <el-input-->
<!--            type="text"-->
<!--            placeholder="请输入内容"-->
<!--            v-model="allVal[3].val"-->
<!--            maxlength="2"-->
<!--            :disabled="allVal[3].lock"-->
<!--            show-word-limit-->
<!--          >-->
<!--          </el-input>-->
<!--        </el-tab-pane>-->
<!--        <el-tab-pane name="4">-->
<!--          <div slot="label">-->
<!--            <div class="show-code-item">{{ allVal[4].val }}</div>-->
<!--            <div style="text-align: center">{{ allVal[4].meaning }}</div>-->
<!--          </div>-->
<!--          <el-radio-group v-model="allVal[4].val" :disabled="allVal[4].lock">-->
<!--            <el-radio v-for="item in industryCodeTypeList" :label="item.code">-->
<!--              {{ item.name }} - {{ item.code }}-->
<!--            </el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-tab-pane>-->
<!--        <el-tab-pane name="5">-->
<!--          <div slot="label">-->
<!--            <div class="show-code-item">{{ allVal[5].val }}</div>-->
<!--            <div style="text-align: center">{{ allVal[5].meaning }}</div>-->
<!--          </div>-->
<!--          <el-radio-group v-model="allVal[5].val" :disabled="allVal[5].lock">-->
<!--            <el-radio v-for="item in deviceTypeList" :label="item.code">-->
<!--              {{ item.name }} - {{ item.code }}-->
<!--            </el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-tab-pane>-->
<!--        <el-tab-pane name="6">-->
<!--          <div slot="label">-->
<!--            <div class="show-code-item">{{ allVal[6].val }}</div>-->
<!--            <div style="text-align: center">{{ allVal[6].meaning }}</div>-->
<!--          </div>-->
<!--          <el-radio-group v-model="allVal[6].val" :disabled="allVal[6].lock">-->
<!--            <el-radio v-for="item in networkIdentificationTypeList" :label="item.code">-->
<!--              {{ item.name }} - {{ item.code }}-->
<!--            </el-radio>-->
<!--          </el-radio-group>-->
<!--        </el-tab-pane>-->
<!--        <el-tab-pane name="7">-->
<!--          请手动输入设备/用户序号, 六位数字-->
<!--          <div slot="label">-->
<!--            <div class="show-code-item">{{ allVal[7].val }}</div>-->
<!--            <div style="text-align: center">{{ allVal[7].meaning }}</div>-->
<!--          </div>-->
<!--          <el-input-->
<!--            type="text"-->
<!--            placeholder="请输入内容"-->
<!--            v-model="allVal[7].val"-->
<!--            maxlength="6"-->
<!--            :disabled="allVal[7].lock"-->
<!--            show-word-limit-->
<!--          >-->
<!--          </el-input>-->
<!--        </el-tab-pane>-->
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script>

export default {
  name: "addUser",
  props: {},
  computed: {},
  created() {
    // this.getRegionList()
  },
  data() {
    return {
      showVideoDialog: false,
      okText: "完成",
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
      loading: false,
    };
  },
  methods: {
    openDialog: function (endCallBck, code, lockIndex, lockContent) {
      this.showVideoDialog = true
      if (typeof code != 'undefined' && code.length === 20) {
        this.allVal[0].val = code.substring(0, 2)
        this.allVal[1].val = code.substring(2, 4)
        this.allVal[2].val = code.substring(4, 6)
        this.allVal[3].val = code.substring(6, 8)
        this.allVal[4].val = code.substring(8, 10)
        this.allVal[5].val = code.substring(10, 13)
        this.allVal[6].val = code.substring(14, 15)
        this.allVal[7].val = code.substring(15)
      }
      if (typeof lockIndex != 'undefined') {
        this.allVal[lockIndex].lock = true
        this.allVal[lockIndex].val = lockContent
      }
      this.endCallBck = endCallBck;
    },
    getRegionList: () => {
      this.regionList = []
      if (this.activeKey === '0' || this.activeKey === '1' || this.activeKey === '2') {
        let parent = ''
        if (this.activeKey === '1') {
          parent = this.allVal[0].val
          if (parent === '11' || parent === '12' || parent === '31') {
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
        this.queryIndustryCodeList();
      } else if (this.activeKey === '5') {
        this.queryDeviceTypeList();
      } else if (this.activeKey === '6') {
        this.queryNetworkIdentificationTypeList();
      }
    },
    queryChildList: (parent)=>{
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
    queryIndustryCodeList: ()=>{
      this.$axios({
        method: 'get',
        url: "/api/common/channel/industry/list",
      }).then((res) => {
        if (res.data.code === 0) {
          this.industryCodeTypeList.value = res.data.data
        } else {
          this.$message.error(res.data.msg);
        }
      }).catch((error) => {
        this.$message.error(error);
      });
    },
    queryDeviceTypeList: ()=>{
      this.$axios({
        method: 'get',
        url: "/api/common/channel/type/list",
      }).then((res) => {
        if (res.data.code === 0) {
          this.deviceTypeList.value = res.data.data
        } else {
          this.$message.error(res.data.msg);
        }
      }).catch((error) => {
        this.$message.error(error);
      });
    },
    queryNetworkIdentificationTypeList: ()=>{
      this.$axios({
        method: 'get',
        url: "/api/common/channel/network/identification/list",
      }).then((res) => {
        if (res.data.code === 0) {
          this.networkIdentificationTypeList.value = res.data.data
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
    handleOk: () => {
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
