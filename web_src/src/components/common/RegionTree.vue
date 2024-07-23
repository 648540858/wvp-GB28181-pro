<template>
  <div id="DeviceTree" style="width: 100%;height: 100%; background-color: #FFFFFF; overflow: auto">
    <el-container>
      <el-header>设备列表</el-header>
      <el-main style="background-color: #ffffff;">
        <div class="device-tree-main-box">
          <tree :nodes="nodes" @onClick="onClick"
                @onCheck="onCheck"
                @onCreated="handleCreated"></tree>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import tree from "vue-giant-tree";

export default {
    name: 'DeviceTree',
    components: {
      tree
    },
    data() {
      return {
        nodes: [
          { id:1, pid:0, name:"随意勾选 1", open:true},
          { id:11, pid:1, name:"随意勾选 1-1", open:true},
          { id:111, pid:11, name:"随意勾选 1-1-1"},
          { id:112, pid:11, name:"随意勾选 1-1-2"},
          { id:12, pid:1, name:"随意勾选 1-2", open:true},
          { id:121, pid:12, name:"随意勾选 1-2-1"},
          { id:122, pid:12, name:"随意勾选 1-2-2"},
          { id:2, pid:0, name:"随意勾选 2", checked:true, open:true},
          { id:21, pid:2, name:"随意勾选 2-1"},
          { id:22, pid:2, name:"随意勾选 2-2", open:true},
          { id:221, pid:22, name:"随意勾选 2-2-1", checked:true},
          { id:222, pid:22, name:"随意勾选 2-2-2"},
          { id:23, pid:2, name:"随意勾选 2-3"}
        ]
      }
    },
    props: ['device', 'onlyCatalog', 'clickEvent', 'contextMenuEvent'],
    methods: {
      onClick(evt, treeId, treeNode) {

      },
      onCheck(evt, treeId, treeNode) {

      },
      handleCreated(ztreeObj) {

      },
      handleNodeClick(data,node,element) {
        let deviceNode = this.$refs.gdTree.getNode(data.userData.deviceId)
        if(typeof (this.clickEvent) == "function") {
          this.clickEvent(deviceNode.data.userData, data.userData, data.type === 2)
        }
      },
      handleContextMenu(event,data,node,element) {
        console.log("右键点击事件")
        let deviceNode = this.$refs.gdTree.getNode(data.userData.deviceId)
        if(typeof (this.contextMenuEvent) == "function") {
          this.contextMenuEvent(deviceNode.data.userData, event, data.userData, data.type === 2)
        }
      },
      loadNode: function(node, resolve){
        console.log(this.device)
        if (node.level === 0) {
          if (this.device) {
            let node = {
              name: this.device.name || this.device.deviceId,
              isLeaf: false,
              id: this.device.deviceId,
              type: this.device.online,
              online: this.device.online === 1,
              userData: this.device
            }
            resolve([node])
          }else {
            this.deviceService.getAllDeviceList((data)=>{
              console.log(data)
              if (data.length > 0) {
                let nodeList = []
                for (let i = 0; i < data.length; i++) {
                  console.log(data[i].name)
                  let node = {
                    name: data[i].name || data[i].deviceId,
                    isLeaf: false,
                    id: data[i].deviceId,
                    type: data[i].online,
                    online: data[i].online === 1,
                    userData: data[i]
                  }
                  nodeList.push(node);
                }
                resolve(nodeList)
              }else {
                resolve([])
              }
            }, (list)=>{
              console.log("设备加载完成")
            }, (error)=>{

            })
          }
        }else {
          let channelArray = []

          this.deviceService.getTree(node.data.userData.deviceId, node.data.id, this.onlyCatalog, catalogData =>{
            console.log(catalogData)
            channelArray = channelArray.concat(catalogData)
            this.channelDataHandler(channelArray, resolve)
          },(endCatalogData) => {

          })
        }

      },
      channelDataHandler: function (data, resolve) {
        if (data.length > 0) {
          let nodeList = []
          for (let i = 0; i <data.length; i++) {
            let item = data[i];
            let type = 3;
            if (item.id.length <= 10) {
              type = 2;
            }else {
              if (item.id.length > 14) {
                let channelType = item.id.substring(10, 13)
                console.log("channelType: " + channelType)
                if (channelType === '215' || channelType === '216') {
                  type = 2;
                }
                console.log(type)
                if (item.basicData.ptzType === 1 ) { // 1-球机;2-半球;3-固定枪机;4-遥控枪机
                  type = 4;
                }else if (item.basicData.ptzType === 2) {
                  type = 5;
                }else if (item.basicData.ptzType === 3 || item.basicData.ptzType === 4) {
                  type = 6;
                }
              }else {
                if (item.basicData.subCount > 0 || item.basicData.parental === 1) {
                  type = 2;
                }
              }
            }
            let node = {
              name: item.name || item.basicData.channelId,
              isLeaf: type !== 2,
              id: item.id,
              deviceId: item.deviceId,
              type: type,
              online: item.basicData.status === 1,
              hasGPS: item.basicData.longitude*item.basicData.latitude !== 0,
              userData: item.basicData
            }
            nodeList.push(node);
          }
          resolve(nodeList)
        }else {
          resolve([])
        }
      },
      reset: function (){
        this.$forceUpdate();
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
