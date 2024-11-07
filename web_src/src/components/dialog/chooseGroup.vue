<template>
  <div id="chooseGroup" >
    <el-dialog
      title="选择虚拟组织"
      width="30%"
      top="5rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <GroupTree ref="regionTree" :showHeader=true :edit="true" :enableAddChannel="false"  :clickEvent="treeNodeClickEvent"
                  :onChannelChange="onChannelChange" :treeHeight="'45vh'"></GroupTree>
      <el-form>
        <el-form-item>
          <div style="text-align: right">
            <el-button type="primary" @click="onSubmit">保存</el-button>
            <el-button @click="close">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>

import GroupTree from "../common/GroupTree.vue";

export default {
  name: "chooseCivilCode",
  components: {GroupTree},
  props: {},
  computed: {},
  created() {},
  data() {
    return {
      showDialog: false,
      endCallback: false,
      groupDeviceId: "",
      businessGroup: "",
    };
  },
  methods: {
    openDialog: function (callback) {
      this.showDialog = true;
      this.endCallback = callback;
    },
    onSubmit: function () {
      if (this.endCallback) {
        this.endCallback(this.groupDeviceId, this.businessGroup)
      }
      this.close();
    },
    close: function () {
      this.showDialog = false;
    },
    treeNodeClickEvent: function (group) {
      if (group.deviceId === "" || group.deviceId === group.businessGroup) {
        return
      }
      this.groupDeviceId = group.deviceId;
      this.businessGroup = group.businessGroup;
    },
    onChannelChange: function (deviceId) {
      //
    },
  },
};
</script>
