<template>
  <div id="deviceEdit" v-loading="isLoging">
    <el-dialog
      title="设备编辑"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="form" :rules="rules" :model="form" label-width="200px" >
          <el-form-item label="设备编号" >
            <el-input v-model="form.deviceId" disabled></el-input>
          </el-form-item>

          <el-form-item label="设备名称" prop="name">
            <el-input v-model="form.name" clearable></el-input>
          </el-form-item>
<!--          <el-form-item label="流媒体ID" prop="mediaServerId">-->
<!--            <el-select v-model="form.mediaServerId" style="float: left; width: 100%" >-->
<!--              <el-option key="auto" label="自动负载最小" value="null"></el-option>-->
<!--              <el-option-->
<!--                v-for="item in mediaServerList"-->
<!--                :key="item.id"-->
<!--                :label="item.id"-->
<!--                :value="item.id">-->
<!--              </el-option>-->
<!--            </el-select>-->
<!--          </el-form-item>-->

          <el-form-item label="字符集" prop="charset" >
            <el-select v-model="form.charset" style="float: left; width: 100%" >
                <el-option key="GB2312" label="GB2312" value="gb2312"></el-option>
              <el-option key="UTF-8" label="UTF-8" value="utf-8"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="地理坐标系" prop="geoCoordSys" >
            <el-select v-model="form.geoCoordSys" style="float: left; width: 100%" >
              <el-option key="GCJ02" label="GCJ02" value="GCJ02"></el-option>
              <el-option key="WGS84" label="WGS84" value="WGS84"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="目录订阅" title="0为取消订阅" prop="subscribeCycleForCatalog" >
            <el-input v-model="form.subscribeCycleForCatalog" clearable ></el-input>
          </el-form-item>
          <el-form-item label="移动位置订阅" title="0为取消订阅" prop="subscribeCycleForCatalog" >
            <el-input v-model="form.subscribeCycleForMobilePosition" clearable ></el-input>
          </el-form-item>
          <el-form-item v-if="form.subscribeCycleForMobilePosition > 0" label="移动位置报送间隔" prop="subscribeCycleForCatalog" >
            <el-input v-model="form.mobilePositionSubmissionInterval" clearable ></el-input>
          </el-form-item>
          <el-form-item label="其他选项">
            <el-checkbox label="SSRC校验" v-model="form.ssrcCheck" style="float: left"></el-checkbox>
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit" >确认</el-button>
              <el-button @click="close">取消</el-button>
            </div>

          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import MediaServer from '../service/MediaServer'
export default {
  name: "deviceEdit",
  props: {},
  computed: {},
  created() {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      hostNames:[],
      mediaServerList: [], // 滅体节点列表
      mediaServerObj : new MediaServer(),
      form: {},
      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }]
      },
    };
  },
  methods: {
    openDialog: function (row, callback) {
      console.log(row)
      this.showDialog = true;
      this.listChangeCallback = callback;
      if (row != null) {
        this.form = row;
      }
      this.getMediaServerList();
    },
    getMediaServerList: function (){
      let that = this;
      that.mediaServerObj.getOnlineMediaServerList((data)=>{
        that.mediaServerList = data.data;
      })
    },
    onSubmit: function () {
      console.log("onSubmit");
      console.log(this.form);
      this.form.subscribeCycleForCatalog = this.form.subscribeCycleForCatalog||0
      this.form.subscribeCycleForMobilePosition = this.form.subscribeCycleForMobilePosition||0
      this.form.mobilePositionSubmissionInterval = this.form.mobilePositionSubmissionInterval||0
      this.$axios({
        method: 'post',
        url:`/api/device/query/device/update/`,
        params: this.form
      }).then((res) => {
        console.log(res.data)
        if (res.data.code == 0) {
          this.listChangeCallback()
        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: "error",
          });
        }
      }).catch(function (error) {
        console.log(error);
      });
    },
    close: function () {
      this.showDialog = false;
      this.$refs.form.resetFields();
    },
  },
};
</script>
