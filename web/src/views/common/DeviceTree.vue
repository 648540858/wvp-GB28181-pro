<template>
  <div id="DeviceTree" style="width: 100%;height: 100%; background-color: #FFFFFF; overflow: auto; padding: 30px">
    <div style="height: 30px; display: grid; grid-template-columns: auto auto">
      <div>通道列表</div>
      <div>
        <el-switch
          v-model="showRegion"
          active-color="#13ce66"
          inactive-color="rgb(64, 158, 255)"
          active-text="行政区划"
          inactive-text="业务分组"
        />
      </div>
    </div>
    <div>
      <RegionTree v-if="showRegion" ref="regionTree" :edit="false" :show-header="false" :has-channel="true" :click-event="treeNodeClickEvent" />
      <GroupTree v-if="!showRegion" ref="groupTree" :edit="false" :show-header="false" :has-channel="true" :click-event="treeNodeClickEvent" />
    </div>
  </div>
</template>

<script>
import RegionTree from './RegionTree.vue'
import GroupTree from './GroupTree.vue'

export default {
  name: 'DeviceTree',
  components: { GroupTree, RegionTree },
  props: ['device', 'onlyCatalog', 'clickEvent', 'contextMenuEvent'],
  data() {
    return {
      showRegion: true,
      defaultProps: {
        children: 'children',
        label: 'name',
        isLeaf: 'isLeaf'
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
  methods: {
    handleClick: function(tab, event) {
    },
    treeNodeClickEvent: function(data) {
      if (data.leaf) {
        console.log(23111)
        console.log(data)
        if (this.clickEvent) {
          this.clickEvent(data.id)
        }
      }
    }
  }
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
