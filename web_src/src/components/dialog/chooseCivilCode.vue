<template>
  <div id="chooseCivilCode" >
    <el-dialog
      title="选择行政区划"
      width="30%"
      top="5rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <RegionTree ref="regionTree" :showHeader=true :edit="true" :enableAddChannel="false"  :clickEvent="treeNodeClickEvent"
                  :onChannelChange="onChannelChange" :treeHeight="'45vh'"></RegionTree>
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

import RegionTree from "../common/RegionTree.vue";

export default {
  name: "chooseCivilCode",
  components: {RegionTree},
  props: {},
  computed: {},
  created() {},
  data() {
    return {
      showDialog: false,
      endCallback: false,
      regionDeviceId: "",
    };
  },
  methods: {
    openDialog: function (callback) {
      this.showDialog = true;
      this.endCallback = callback;
    },
    onSubmit: function () {
      if (this.endCallback) {
        this.endCallback(this.regionDeviceId)
      }
      this.close();
    },
    close: function () {
      this.showDialog = false;
    },
    treeNodeClickEvent: function (region) {
      this.regionDeviceId = region.deviceId;
    },
    onChannelChange: function (deviceId) {
      //
    },
  },
};
</script>
