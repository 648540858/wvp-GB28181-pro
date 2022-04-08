<template>
    <div id="devicePosition" style="height: 100%">
        <el-container style="height: 100%">
            <el-header>
                <uiHeader></uiHeader>
            </el-header>
          <el-container>
            <el-aside width="250px" height="100%" style="background-color: #FFFFFF; margin: 0 0 20px 20px;">
              <div style=" padding-top: 10px">
                <el-tree class="el-scrollbar"
                         ref="tree"
                         id="deviceTree"
                         empty-text="未知节点"
                         node-key="id"
                         :highlight-current="false"
                         :expand-on-click-node="false"
                         :props="props"
                         :load="loadNode"
                         @node-contextmenu="contextmenuEventHandler"
                         @node-click="nodeClickHandler"
                         lazy>
                </el-tree>
              </div>

            </el-aside>
            <el-main>
              <MapComponent></MapComponent>
            </el-main>
          </el-container>
        </el-container>
    </div>
</template>

<script>
import uiHeader from "./UiHeader.vue";
import MapComponent from "./common/MapComponent.vue";
import DeviceService from "./service/DeviceService";
export default {
    name: "devicePosition",
    components: {
      MapComponent,
      uiHeader,
    },
    data() {
        return {
          deviceService: new DeviceService(),
          props: {
            label: 'name',
            children: 'children',
            isLeaf: 'leaf'
          },
        };
    },
    created() {
      this.init();
    },
    destroyed() {

    },
    methods: {
      init(){

      },
      loadNode: function(node, resolve){
        if (node.level === 0) {
          this.deviceService.getAllDeviceList((data)=>{
            console.log("all deivce")
            console.log(data)
            if (data.length > 0) {
              let nodeList = []
              for (let i = 0; i < data.length; i++) {
                let node = {
                  name: data[i].name || data[i].deviceId,
                  id: data[i].deviceId,
                  online: data[i].online,
                  deviceId: data[i].deviceId,
                }
                nodeList.push(node);
              }
              resolve(nodeList)
            }else {
              resolve([])
            }
          }, (error)=>{

          })
        }
        if (node.level === 1){
          console.log(node)
          this.deviceService.getAllCatalog(node.data.deviceId, (data)=>{
            console.log("all Catalog")
            console.log(data)
            if (data.length > 0) {
              let nodeList = []
              for (let i = 0; i < data.length; i++) {
                let node = {
                  name: data[i].name || data[i].channelId,
                  id: data[i].channelId,
                  online: data[i].status === 1,
                  deviceId: data[i].deviceId,
                  channelId: data[i].channelId,
                }
                nodeList.push(node);
              }
              resolve(nodeList)
            }else {
              resolve([])
            }
          }, (error)=>{

          })
        }

        if (node.level > 1) {
          console.log(node.data.channelId)
          this.deviceService.getAllSubCatalog(node.data.deviceId, node.data.channelId, (data)=>{
            console.log("all Catalog")
            console.log(data)
            if (data.length > 0) {
              let nodeList = []
              for (let i = 0; i < data.length; i++) {
                let node = {
                  name: data[i].name || data[i].channelId,
                  id: data[i].channelId,
                  online: data[i].status === 1,
                  deviceId: data[i].deviceId,
                  channelId: data[i].channelId,
                }
                nodeList.push(node);
              }
              resolve(nodeList)
            }else {
              resolve([])
            }
          }, (error)=>{

          })
        }
      },
      contextmenuEventHandler: function (event,data,node,element){
        if (node.data.type !== 0) {
          data.parentId = node.parent.data.id;
          this.$contextmenu({
            items: [
              {
                label: "移除通道",
                icon: "el-icon-delete",
                disabled: false,
                onClick: () => {
                  this.$axios({
                    method:"delete",
                    url:"/api/platform/catalog/relation/del",
                    data: data
                  }).then((res)=>{
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
        }else {
          this.$contextmenu({
            items: [
              {
                label: "刷新节点",
                icon: "el-icon-refresh",
                disabled: false,
                onClick: () => {
                  this.refreshCatalog(node);
                }
              },
              {
                label: "新建节点",
                icon: "el-icon-plus",
                disabled: false,
                onClick: () => {
                  this.addCatalog(data.id, node);
                }
              },
              {
                label: "修改节点",
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
                divided: true,
                onClick: () => {
                  this.$confirm('确定删除?', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                  }).then(() => {
                    this.removeCatalog(data.id, node)
                  }).catch(() => {

                  });
                }
              },
              {
                label: "设为默认",
                icon: "el-icon-folder-checked",
                disabled: node.data.id === this.defaultCatalogIdSign,
                onClick: () => {
                  this.setDefaultCatalog(data.id)
                },
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
      nodeClickHandler: function (data, node, tree){
        this.chooseId = data.id;
        this.chooseName = data.name;
        if (this.catalogIdChange)this.catalogIdChange(this.chooseId, this.chooseName);
      }
    },
};
</script>

<style>

</style>
