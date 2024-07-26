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
    <div>
      <vue-easy-tree
        ref="veTree"
        node-key="id"
        height="72vh"
        lazy
        style="height: 78vh; padding: 2rem"
        :load="loadNode"
        :data="treeData"
        :default-expanded-keys="['']"
        @node-contextmenu="contextmenuEventHandler"
        @node-click="nodeClickHandler"
      >
        <span class="custom-tree-node" slot-scope="{ node, data }">
          <span @click.stop >
            <el-radio v-if="node.data.type === 0 && node.level !== 1 " style="margin-right: 0" v-model="chooseId" @input="chooseIdChange" :label="node.data.id">{{''}}</el-radio>
          </span>
          <span v-if="node.data.type === 0" style="color: #409EFF" class="iconfont icon-bianzubeifen3"></span>
          <span v-if="node.data.type === 1" style="color: #409EFF" class="iconfont icon-file-stream2"></span>
          <span style=" padding-left: 1px" v-if="node.data.id !==''">{{ node.label }}({{ node.data.id }})</span>
          <span style=" padding-left: 1px" v-if="node.data.id ===''">{{ node.label }}</span>
        </span>
      </vue-easy-tree>
    </div>
    <regionCode ref="regionCode"></regionCode>
  </div>
</template>

<script>
import VueEasyTree from "@wchbrad/vue-easy-tree";
import regionCode from './../dialog/regionCode'

let treeData = []

export default {
  name: 'DeviceTree',
  components: {
    VueEasyTree, regionCode
  },
  data() {
    return {
      searchSrt: "",
      chooseId: "",
      // props: {
      //   label: "name",
      // },
      treeData: [],
    }
  },
  props: ['edit', 'clickEvent', 'chooseIdChange'],
  created() {
    // this.$axios({
    //   method: 'get',
    //   url: `/api/region/tree/list`,
    // }).then((res)=> {
    //   if (res.data.code === 0) {
    //
    //     for (let i = 0; i < res.data.data.length; i++) {
    //       let item = res.data.data[i]
    //       console.log(item)
    //       this.treeData.push({
    //         id: item.deviceId,
    //         label: item.name,
    //         children: [],
    //         isLeaf: false,
    //       })
    //     }
    //
    //   }
    //
    // }).catch(function (error) {
    //   console.log(error);
    // });
  },
  methods: {
    onClick(evt, treeId, treeNode) {

    },
    onCheck(evt, treeId, treeNode) {

    },
    handleCreated(ztreeObj) {

    },
    search() {

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
      console.log(22222)
      console.log(node)
      if (node.level === 0) {
        resolve([{
          id: "",
          label: "根资源组",
          isLeaf: false,
          type: 0
        }]);
      } else if (node.data.id.length < 8) {
        this.$axios({
          method: 'get',
          url: `/api/region/tree/list`,
          params: {
            query: this.searchSrt,
            parent: node.data.id
          }
        }).then((res) => {
          if (res.data.code === 0) {
            resolve(res.data.data);
          }

        }).catch(function (error) {
          console.log(error);
        });
      } else {
        resolve([]);
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
    },
    contextmenuEventHandler: function (event, data, node, element) {
      console.log(node.level)
      if (node.data.type === 1) {
        data.parentId = node.parent.data.id;
        this.$contextmenu({
          items: [
            {
              label: "移除通道",
              icon: "el-icon-delete",
              disabled: false,
              onClick: () => {
                this.$axios({
                  method: "delete",
                  url: "/api/platform/catalog/relation/del",
                  data: data
                }).then((res) => {
                  console.log("移除成功")
                  node.parent.loaded = false
                  node.parent.expand();
                }).catch(function (error) {
                  console.log(error);
                });
              }
            }
          ],
          event, // 鼠标事件信息
          customClass: "custom-class", // 自定义菜单 class
          zIndex: 3000, // 菜单样式 z-index
        });
      } else if (node.data.type === 0) {
        this.$contextmenu({
          items: [
            {
              label: "刷新节点",
              icon: "el-icon-refresh",
              disabled: false,
              onClick: () => {
                this.refreshNode(node);
              }
            },
            {
              label: "新建节点",
              icon: "el-icon-plus",
              disabled: false,
              onClick: () => {
                this.addRegion(data.id, node);
              }
            },
            {
              label: "重命名",
              icon: "el-icon-edit",
              disabled: node.level === 1,
              onClick: () => {
                this.editCatalog(data, node);
              }
            },
            {
              label: "删除节点",
              icon: "el-icon-delete",
              disabled: node.level === 1,
              onClick: () => {
                this.$confirm('确定删除?', '提示', {
                  confirmButtonText: '确定',
                  cancelButtonText: '取消',
                  type: 'warning'
                }).then(() => {
                  this.removeRegion(data.id, node)
                }).catch(() => {

                });
              }
            },
            // {
            //   label: "导出",
            //   icon: "el-icon-download",
            //   disabled: false,
            //   children: [
            //     {
            //       label: "导出到文件",
            //       onClick: () => {
            //
            //       },
            //     },
            //     {
            //       label: "导出到其他平台",
            //       onClick: () => {
            //
            //       },
            //     }
            //   ]
            // },

          ],
          event, // 鼠标事件信息
          customClass: "custom-class", // 自定义菜单 class
          zIndex: 3000, // 菜单样式 z-index
        });
      }

      return false;
    },
    removeRegion: function (id, node) {
      this.$axios({
        method: "delete",
        url: `/api/region/delete`,
        params: {
          deviceId: id,
        }
      }).then((res) => {
        if (res.data.code === 0) {
          console.log("移除成功")
          node.parent.loaded = false
          node.parent.expand();
        }
      })
        .catch(function (error) {
          console.log(error);
        });
    },
    refreshNode: function (node) {
      node.loaded = false
      node.expand();
    },
    addRegion: function (id, node) {

      console.log(node)

      this.$refs.regionCode.openDialog(form => {
        node.loaded = false
        node.expand();
      }, id);
    },
    nodeClickHandler: function (data, node, tree) {
      console.log(data)
      console.log(node)
      // this.chooseId = data.id;
      // this.chooseName = data.name;
      // if (this.catalogIdChange)this.catalogIdChange(this.chooseId, this.chooseName);
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
.custom-tree-node .el-radio__label {
  padding-left: 4px !important;
}
</style>
