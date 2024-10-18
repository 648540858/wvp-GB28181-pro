<template>
  <div id="DeviceTree" style="width: 100%;height: 100%; background-color: #FFFFFF; overflow: auto">
    <el-container>
      <el-header>
        <div style="display: grid; grid-template-columns: auto auto">
          <div >通道列表</div>
          <div >
            <el-switch
              v-model="showRegion"
              active-color="#13ce66"
              inactive-color="rgb(64, 158, 255)"
              active-text="行政区划"
              inactive-text="业务分组">
            </el-switch>
          </div>
        </div>

      </el-header>
      <el-main style="background-color: #ffffff;">
        <RegionTree v-if="showRegion" ref="regionTree" :edit="false" :showHeader="false" :clickEvent="treeNodeClickEvent" ></RegionTree>
        <GroupTree  v-if="!showRegion" ref="groupTree"  :edit="false" :showHeader="false" :clickEvent="treeNodeClickEvent" ></GroupTree>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import DeviceService from "../service/DeviceService.js";
import RegionTree from "./RegionTree.vue";
import GroupTree from "./GroupTree.vue";

export default {
    name: 'DeviceTree',
  components: {GroupTree, RegionTree},
    data() {
        return {
          showRegion: true,
          deviceService: new DeviceService(),
          defaultProps: {
            children: 'children',
            label: 'name',
            isLeaf: 'isLeaf'
          }
        };
    },
    props: ['device', 'onlyCatalog', 'clickEvent', 'contextMenuEvent'],
    methods: {
      handleClick: function (tab, event){
      },
      treeNodeClickEvent: function (data){

        if (data.leaf) {
          console.log(23111)
          console.log(data)
          if (this.clickEvent){
            this.clickEvent(data.id)
          }
        }
      }
    },
    destroyed() {
      // if (this.jessibuca) {
      //   this.jessibuca.destroy();
      // }
      // this.playing = false;
      // this.loaded = false;
      // this.performance = "";
    },
}
</script>

<style>
.device-tree-main-box{
  text-align: left;
}
.device-online{
  color: #252525;
}
.device-offline{
  color: #727272;
}
</style>
