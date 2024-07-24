<template>
  <div id="DeviceTree">
    <div class="page-header" style="margin-bottom: 1rem">
      <div class="page-title">行政区划</div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                    prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>

        </div>
      </div>
    </div>
    <vue-easy-tree
      ref="veTree"
      node-key="id"
      height="72vh"
      style="height: 78vh"
      :loadNode="loadNode"
      :data="treeData"
      :props="props"
    ></vue-easy-tree>
  </div>
</template>

<script>
import VueEasyTree from "@wchbrad/vue-easy-tree";

let treeData = []

export default {
  name: 'DeviceTree',
  components: {
    VueEasyTree
  },
  data() {
    return {
      props: {
        label: "name",
      },
      treeData: [],
    }
  },
  props: ['edit', 'clickEvent', 'contextMenuEvent'],
  created() {
    this.$axios({
      method: 'get',
      url: `/api/region/tree/list`,
    }).then((res)=> {
      if (res.data.code === 0) {
        this.treeData.push(res.data.data)
      }

    }).catch(function (error) {
      console.log(error);
    });
  },
  methods: {
    onClick(evt, treeId, treeNode) {

    },
    onCheck(evt, treeId, treeNode) {

    },
    handleCreated(ztreeObj) {

    },
    handleNodeClick(data, node, element) {
      let deviceNode = this.$refs.gdTree.getNode(data.userData.deviceId)
      if (typeof (this.clickEvent) == "function") {
        this.clickEvent(deviceNode.data.userData, data.userData, data.type === 2)
      }
    },
    handleContextMenu(event, data, node, element) {
      console.log("右键点击事件")
      let deviceNode = this.$refs.gdTree.getNode(data.userData.deviceId)
      if (typeof (this.contextMenuEvent) == "function") {
        this.contextMenuEvent(deviceNode.data.userData, event, data.userData, data.type === 2)
      }
    },
    loadNode: function (node, resolve) {
      console.log(node)
      if (node.level === 0) {

      } else {

      }

    },
    channelDataHandler: function (data, resolve) {
      if (data.length > 0) {
        let nodeList = []
        for (let i = 0; i < data.length; i++) {
          let item = data[i];
          let type = 3;
          if (item.id.length <= 10) {
            type = 2;
          } else {
            if (item.id.length > 14) {
              let channelType = item.id.substring(10, 13)
              console.log("channelType: " + channelType)
              if (channelType === '215' || channelType === '216') {
                type = 2;
              }
              console.log(type)
              if (item.basicData.ptzType === 1) { // 1-球机;2-半球;3-固定枪机;4-遥控枪机
                type = 4;
              } else if (item.basicData.ptzType === 2) {
                type = 5;
              } else if (item.basicData.ptzType === 3 || item.basicData.ptzType === 4) {
                type = 6;
              }
            } else {
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
            hasGPS: item.basicData.longitude * item.basicData.latitude !== 0,
            userData: item.basicData
          }
          nodeList.push(node);
        }
        resolve(nodeList)
      } else {
        resolve([])
      }
    },
    reset: function () {
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
.device-tree-main-box {
  text-align: left;
}

.device-online {
  color: #252525;
}

.device-offline {
  color: #727272;
}
</style>
