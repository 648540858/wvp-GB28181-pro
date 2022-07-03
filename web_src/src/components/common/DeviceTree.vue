<template>
  <div id="DeviceTree" style="width: 100%;height: 100%; background-color: #FFFFFF; overflow: auto">
    <el-container>
      <el-header>设备列表</el-header>
      <el-main style="background-color: #ffffff;">
        <div class="device-tree-main-box">
          <el-tree ref="gdTree" :props="defaultProps" :load="loadNode" lazy @node-click="handleNodeClick"@node-contextmenu="handleContextMenu" node-key="id" style="min-width: 100%; display:inline-block !important;">
            <span class="custom-tree-node" slot-scope="{ node, data }" style="width: 100%">
              <span v-if="node.data.type === 0 && node.data.online" title="在线设备" class="device-online iconfont icon-jiedianleizhukongzhongxin2"></span>
              <span v-if="node.data.type === 0 && !node.data.online " title="离线设备" class="device-offline iconfont icon-jiedianleizhukongzhongxin2"></span>
              <span v-if="node.data.type === 2 && node.data.online" title="目录"  class="device-online iconfont icon-jiedianleilianjipingtai"></span>
              <span v-if="node.data.type === 2 && !node.data.online" title="目录"  class="device-offline iconfont icon-jiedianleilianjipingtai"></span>
              <span v-if="node.data.type === 3 && node.data.online " title="在线通道" class="device-online iconfont icon-shebeileijiankongdian"></span>
              <span v-if="node.data.type === 3 && !node.data.online" title="在线通道" class="device-offline iconfont icon-shebeileijiankongdian"></span>
              <span v-if="node.data.type === 4 && node.data.online " title="在线通道-球机" class="device-online iconfont icon-shebeileiqiuji"></span>
              <span v-if="node.data.type === 4 && !node.data.online" title="在线通道-球机" class="device-offline iconfont icon-shebeileiqiuji"></span>
              <span v-if="node.data.type === 5 && node.data.online " title="在线通道-半球" class="device-online iconfont icon-shebeileibanqiu"></span>
              <span v-if="node.data.type === 5 && !node.data.online" title="在线通道-半球" class="device-offline iconfont icon-shebeileibanqiu"></span>
              <span v-if="node.data.type === 6 && node.data.online " title="在线通道-枪机" class="device-online iconfont icon-shebeileiqiangjitongdao"></span>
              <span v-if="node.data.type === 6 && !node.data.online" title="在线通道-枪机" class="device-offline iconfont icon-shebeileiqiangjitongdao"></span>
              <span v-if="node.data.online" style="padding-left: 1px" class="device-online">{{ node.label }}</span>
              <span v-if="!node.data.online" style="padding-left: 1px" class="device-offline">{{ node.label }}</span>
              <span>
                <i v-if="node.data.hasGPS && node.data.online" style="color: #9d9d9d" class="device-online iconfont icon-dizhi"></i>
                <i v-if="node.data.hasGPS && !node.data.online" style="color: #9d9d9d" class="device-offline iconfont icon-dizhi"></i>
              </span>
            </span>
          </el-tree>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import DeviceService from "../service/DeviceService.js";

export default {
    name: 'DeviceTree',
    data() {
        return {
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
                if (item.basicData.ptztype === 1 ) { // 1-球机;2-半球;3-固定枪机;4-遥控枪机
                  type = 4;
                }else if (item.basicData.ptztype === 2) {
                  type = 5;
                }else if (item.basicData.ptztype === 3 || item.basicData.ptztype === 4) {
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
