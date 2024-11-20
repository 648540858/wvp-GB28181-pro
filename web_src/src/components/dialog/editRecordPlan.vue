<template>
  <div id="editRecordPlan" v-loading="loading" style="text-align: left;">
    <el-dialog
      title="录制计划"
      width="700px"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <ByteWeektimePicker v-model="byteTime" name="name"/>
        <el-form >
          <el-form-item>
            <div style="float: right; margin-top: 20px">
              <el-button type="primary" @click="onSubmit">保存</el-button>
              <el-button @click="close">取消</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ByteWeektimePicker } from 'byte-weektime-picker'


export default {
  name: "editRecordPlan",
  props: {},
  components: {ByteWeektimePicker},
  created() {
  },
  data() {
    return {
      options: [],
      loading: false,
      showDialog: false,
      channel: "",
      deviceDbId: "",
      endCallback: "",
      byteTime: "",
      planList: [],
    };
  },
  methods: {
    openDialog: function (channel, deviceDbId, endCallback) {
      this.channel = channel;
      this.deviceDbId = deviceDbId;
      this.endCallback = endCallback;
      this.showDialog = true;
      this.byteTime= "";
      if (channel.recordPlanId) {
        // 请求plan信息

      }
    },
    onSubmit: function () {
      let planList = this.byteTime2PlanList();
      console.log(planList)
      this.$axios({
        method: 'post',
        url: "/api/record/plan/add",
        params: {
          channelId: this.channel?this.channel.id:null,
          deviceDbId: this.deviceDbId,
          planList: planList
        }
      }).then((res) => {
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: '添加成功',
            type: 'success',
          });
          this.showDialog = false;
          this.endCallback()
        } else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: 'error'
          });
        }
      }).catch((error) => {
        console.error(error)
      });
    },
    close: function () {
      this.channel = "";
      this.deviceDbId = "";
      this.showDialog = false;
      if(this.endCallback) {
        this.endCallback();
      }
    },
    byteTime2PlanList() {
      this.planList = []
      if (this.byteTime.length === 0) {
        return;
      }
      const DayTimes = 24 * 2;
      let planList = []
      let week = 1;
      // 把 336长度的 list 分成 7 组，每组 48 个
      for (let i = 0; i < this.byteTime.length; i += DayTimes) {
        let planArray = this.byteTime2Plan(this.byteTime.slice(i, i + DayTimes));
        console.log(planArray)
        if(!planArray || planArray.length === 0) {
          week ++;
          continue
        }
        for (let j = 0; j < planArray.length; j++) {
          console.log(planArray[j])
          planList.push({
            startTime: planArray[j].startTime,
            stopTime: planArray[j].stopTime,
            weekDay: week
          })
        }
        week ++;
      }
      return planList
    },
    byteTime2Plan(weekItem){
      let startTime = 0;
      let endTime = 0;
      let result = []

      for (let i = 0; i < weekItem.length; i++) {
        let item = weekItem[i]
        if (item === '1') {
          endTime = 30*i
          if (startTime === 0 ) {
            startTime = 30*i
          }
        } else {
          if (endTime !== 0){
            result.push({
              startTime: startTime * 60 * 1000,
              stopTime: endTime * 60 * 1000,
            })
            startTime = 0
            endTime = 0
          }
        }
      }
      return result;
    }
  },
};
</script>
