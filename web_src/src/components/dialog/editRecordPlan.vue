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
        <el-form >
          <el-form-item label="名称">
            <el-input type="text" v-model="planName"></el-input>
          </el-form-item>
          <el-form-item>
            <ByteWeektimePicker v-model="byteTime" name="name"/>
          </el-form-item>
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
      edit: false,
      planName: null,
      id: null,
      showDialog: false,
      endCallback: "",
      byteTime: "",
    };
  },
  methods: {
    openDialog: function (recordPlan, endCallback) {
      console.log(recordPlan);
      this.endCallback = endCallback;
      this.showDialog = true;
      this.byteTime= "";
      if (recordPlan) {
        this.edit = true
        this.planName = recordPlan.name
        this.id = recordPlan.id
        this.$axios({
          method: 'get',
          url: "/api/record/plan/get",
          params: {
            planId: recordPlan.id,
          }
        }).then((res) => {
          if (res.data.code === 0) {
            this.byteTime = this.plan2Byte(res.data.data.planItemList)
          }
        }).catch((error) => {
          console.error(error)
        });

      }
    },
    onSubmit: function () {
      let planList = this.byteTime2PlanList();
      if (!this.edit) {
        this.$axios({
          method: 'post',
          url: "/api/record/plan/add",
          data: {
            name: this.planName,
            planItemList: planList
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
      }else {
        this.$axios({
          method: 'post',
          url: "/api/record/plan/update",
          data: {
            id: this.id,
            name: this.planName,
            planItemList: planList
          }
        }).then((res) => {
          if (res.data.code === 0) {
            this.$message({
              showClose: true,
              message: '更新成功',
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
      }

    },
    close: function () {
      this.showDialog = false;
      this.id = null
      this.planName = null
      this.byteTime = ""
      this.endCallback = ""
      if(this.endCallback) {
        this.endCallback();
      }
    },
    byteTime2PlanList() {
      if (this.byteTime.length === 0) {
        return;
      }
      const DayTimes = 24 * 2;
      let planList = []
      let week = 1;
      // 把 336长度的 list 分成 7 组，每组 48 个
      for (let i = 0; i < this.byteTime.length; i += DayTimes) {
        let planArray = this.byteTime2Plan(this.byteTime.slice(i, i + DayTimes));
        if(!planArray || planArray.length === 0) {
          week ++;
          continue
        }
        for (let j = 0; j < planArray.length; j++) {
          planList.push({
            id: this.id,
            start: planArray[j].start,
            stop: planArray[j].stop,
            weekDay: week
          })
        }
        week ++;
      }
      return planList
    },
    byteTime2Plan(weekItem){
      let start = null;
      let stop = null;
      let result = []

      for (let i = 0; i < weekItem.length; i++) {
        let item = weekItem[i]
        if (item === '1') { // 表示选中
          stop = i
          if (start === null ) {
            start = i
          }
          if (i === weekItem.length - 1) {
            result.push({
              start: start,
              stop: stop,
            })
          }
        } else {
          if (stop !== 0){
            result.push({
              start: start,
              stop: stop,
            })
            start = 0
            stop = 0
          }
        }
      }
      return result;
    },
    plan2Byte(planList) {
      let byte = ""
      let indexArray = {}
      for (let i = 0; i < planList.length; i++) {

        let weekDay = planList[i].weekDay
        let index = planList[i].start
        let endIndex = planList[i].stop
        console.log(index + "===" + endIndex)
        for (let j = index; j <= endIndex; j++) {
          indexArray["key_" + (j + (weekDay - 1 )*48)] = 1
          console.log("key_" + (j + (weekDay - 1 )*48))
        }
      }
      for (let i = 0; i < 336; i++) {
        if (indexArray["key_" + i]){
          byte += "1"
        }else {
          byte += "0"
        }
      }
      return byte
    }
  },
};
</script>
