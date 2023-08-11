<template>
  <div id="DeviceTree" style="width: 100%;height: 100%; background-color: #FFFFFF; overflow: auto">
    <el-container>
      <el-header>设备列表</el-header>
      <el-main style="background-color: #ffffff;">
        <div class="device-tree-main-box">
          <tree
            :nodes="nodes"
            @onClick="onClick"
            @onCheck="onCheck"
            @onExpand="onExpand"
            @onCreated="handleCreated"
          />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import DeviceService from "../service/DeviceService.js";
import tree from "vue-giant-tree";

export default {
    name: 'DeviceTreeForZtree',
    components: {
      tree
    },
    data() {
        return {
          deviceService: new DeviceService(),
          device: null,
          nodes:[],
          setting: {
            callback: {
              beforeExpand: this.beforeExpand
            },
            // async: {
            //   enable: true,
            //   type: "get",
            //   url: `/api/device/query/tree`,
            //   contentType: "application/json",
            //   // autoParam: ["pid=parentId"],
            // },
            check: {
              enable: false,
            },
            edit: {
              enable: false,
            }
          },
          defaultProps: {
            children: 'children',
            label: 'name',
            isLeaf: 'isLeaf'
          }
        };
    },
    props: ['clickEvent', 'contextMenuEvent'],
    mounted() {
      this.deviceService.getAllDeviceList((data)=>{
        console.log(data)
        for (let i = 0; i < data.length; i++) {
          console.log(data[i].name)
          let node = {
            name: data[i].name || data[i].deviceId,
            id: data[i].deviceId,
            isParent: true,
          }
          this.nodes.push(node)
        }
      }, (list)=>{
        console.log("设备加载完成")
      })
    },
    methods: {
      onClick(evt, treeId, treeNode) {
        console.log(evt)
        console.log(treeId)
        console.log(treeNode)
      },
      onCheck(evt, treeId, treeNode) {
        console.log(evt)
      },
      beforeExpand(treeId, treeNode) {
        console.log(treeId)
        console.log(treeNode)
        return true;
      },
      onExpand(evt, treeId, treeNode) {
        // console.log(evt)
        // console.log(treeId)
        // console.log(treeNodes)
      },
      handleCreated(ztreeObj) {
        console.log(ztreeObj)
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
