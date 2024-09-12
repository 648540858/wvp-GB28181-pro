<template>
  <div id="DeviceTree">
    <div class="page-header" style="margin-bottom: 1rem;" v-if="showHeader">
      <div class="page-title">行政区划</div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-input @input="search" style="visibility:hidden; margin-right: 1rem; width: 12rem;" size="mini" placeholder="关键字"
                    prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>

          <el-checkbox v-model="showCode">显示编号</el-checkbox>
        </div>
      </div>
    </div>
    <div v-if="showHeader" style="height: 2rem; background-color: #FFFFFF" ></div>
    <div >
      <vue-easy-tree
        class="flow-tree"
        ref="veTree"
        node-key="deviceId"
        height="78vh"
        lazy
        style="padding: 0 0 2rem 0.5rem"
        :load="loadNode"
        :data="treeData"
        :props="props"
        :default-expanded-keys="['']"
        @node-contextmenu="contextmenuEventHandler"
        @node-click="nodeClickHandler"
      >
        <span class="custom-tree-node" slot-scope="{ node, data }">
          <span @click.stop v-if="edit">
            <el-radio v-if="node.data.type === 0 && node.level !== 1 " style="margin-right: 0" v-model="chooseId" @input="chooseIdChange" :label="node.data.deviceId">{{''}}</el-radio>
          </span>
          <span v-if="node.data.type === 0" style="color: #409EFF" class="iconfont icon-bianzubeifen3"></span>
          <span v-if="node.data.type === 1" style="color: #409EFF" class="iconfont icon-shexiangtou2"></span>
          <span style=" padding-left: 1px" v-if="node.data.deviceId !=='' && showCode" :title="node.data.deviceId">{{ node.label }}（编号：{{ node.data.deviceId }}）</span>
          <span style=" padding-left: 1px" v-if="node.data.deviceId ==='' || !showCode" :title="node.data.deviceId">{{ node.label }}</span>
        </span>
      </vue-easy-tree>
    </div>
    <regionEdit ref="regionEdit"></regionEdit>
    <gbDeviceSelect ref="gbDeviceSelect"></gbDeviceSelect>
  </div>
</template>

<script>
import VueEasyTree from "@wchbrad/vue-easy-tree";
import regionEdit from './../dialog/regionEdit'
import gbDeviceSelect from './../dialog/GbDeviceSelect'

export default {
  name: 'DeviceTree',
  components: {
    VueEasyTree, regionEdit, gbDeviceSelect
  },
  data() {
    return {
      props: {
        label: "name",
      },
      showCode: false,
      searchSrt: "",
      chooseId: "",
      treeData: [],
    }
  },
  props: ['edit', 'clickEvent', 'chooseIdChange', 'onChannelChange', 'showHeader'],
  created() {
  },
  methods: {
    search() {

    },
    loadNode: function (node, resolve) {
      if (node.level === 0) {
        resolve([{
          deviceId: "",
          name: "根资源组",
          isLeaf: false,
          type: 0
        }]);
      } else if (node.data.deviceId.length <= 8) {
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
    reset: function () {
      this.$forceUpdate();
    },
    contextmenuEventHandler: function (event, data, node, element) {
      if (!this.edit) {
        return
      }
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
                console.log(data)
                this.$axios({
                  method: "post",
                  url: `/api/common/channel/region/delete`,
                  data: {
                    channelIds: [data.id]
                  }
                }).then((res) => {
                  console.log("移除成功")
                  if (this.onChannelChange) {
                    this.onChannelChange()
                  }
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
              label: "编辑节点",
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
                  this.removeRegion(data.id, node)
                }).catch(() => {

                });
              }
            },
            {
              label: "添加设备",
              icon: "el-icon-plus",
              disabled: node.level === 1,
              onClick: () => {
                this.addChannelFormDevice(data.id, node)
              }
            },
            {
              label: "移除设备",
              icon: "el-icon-delete",
              disabled: node.level === 1,
              onClick: () => {
                this.removeChannelFormDevice(data.id, node)
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
          id: node.data.id,
        }
      }).then((res) => {
        if (res.data.code === 0) {
          console.log("移除成功")
          node.parent.loaded = false
          node.parent.expand();
        }
      }).catch(function (error) {
          console.log(error);
      });
    },
    addChannelFormDevice: function (id, node) {
      this.$refs.gbDeviceSelect.openDialog((rows)=>{
        let deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$axios({
          method: 'post',
          url: `/api/common/channel/region/device/add`,
          data: {
            civilCode: node.data.deviceId,
            deviceIds: deviceIds,
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            this.$message.success({
            showClose: true,
            message: "保存成功"
          })
            if (this.onChannelChange) {
              this.onChannelChange()
            }
            node.loaded = false
            node.expand();
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
          this.loading = false
        }).catch((error)=> {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      })
    },
    removeChannelFormDevice: function (id, node) {
      this.$refs.gbDeviceSelect.openDialog((rows)=>{
        let deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$axios({
          method: 'post',
          url: `/api/common/channel/region/device/delete`,
          data: {
            deviceIds: deviceIds,
          }
        }).then((res)=> {
          if (res.data.code === 0) {
            this.$message.success({
              showClose: true,
              message: "保存成功"
            })
            if (this.onChannelChange) {
              this.onChannelChange()
            }
            node.loaded = false
            node.expand();
          }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
          this.loading = false
        }).catch((error)=> {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      })
    },
    refreshNode: function (node) {
      node.loaded = false
      node.expand();
    },
    refresh: function (id) {
      // 查询node
      let node = this.$refs.veTree.getNode(id)
      node.loaded = false
      node.expand();
    },
    addRegion: function (id, node) {

      console.log(node)

      this.$refs.regionEdit.openDialog(form => {
        node.loaded = false
        node.expand();
      }, {
        deviceId: "",
        name: "",
        parentId: node.data.id,
        parentDeviceId: node.data.deviceId,
      });
    },
    editCatalog: function (data, node){
      // 打开添加弹窗
      this.$refs.regionEdit.openDialog(form => {
        node.loaded = false
        node.expand();
      }, node.data);
    },
    nodeClickHandler: function (data, node, tree) {
      console.log(data)
      console.log(node)
      if (this.clickEvent) {
        this.clickEvent(data)
      }
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
