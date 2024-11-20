<template>
  <div id="DeviceTree">
    <div v-if="showHeader" class="page-header" style="margin-bottom: 1rem;">
      <div class="page-title">业务分组</div>
      <div class="page-header-btn">
        <div style="display: inline;">
          <el-input @input="search" style="visibility:hidden; margin-right: 1rem; width: 12rem;" size="mini"
                    placeholder="关键字"
                    prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>

          <el-checkbox v-model="showCode">显示编号</el-checkbox>
        </div>
      </div>
    </div>
    <div v-if="showHeader" style="height: 2rem; background-color: #FFFFFF"></div>
    <div>
      <vue-easy-tree
        class="flow-tree"
        ref="veTree"
        node-key="treeId"
        :height="treeHeight?treeHeight:'78vh'"
        lazy
        style="padding: 0 0 2rem 0.5rem"
        :load="loadNode"
        :data="treeData"
        :props="props"
        :default-expanded-keys="['']"
        @node-contextmenu="contextmenuEventHandler"
        @node-click="nodeClickHandler"
      >
        <template v-slot:default="{ node, data }">
          <span class="custom-tree-node">
            <span v-if="node.data.type === 0 && chooseId !== node.data.deviceId" style="color: #409EFF" class="iconfont icon-bianzubeifen3"></span>
          <span v-if="node.data.type === 0 && chooseId === node.data.deviceId" style="color: #c60135;" class="iconfont icon-bianzubeifen3"></span>
            <span v-if="node.data.type === 1 && node.data.status === 'ON'" style="color: #409EFF" class="iconfont icon-shexiangtou2"></span>
            <span v-if="node.data.type === 1 && node.data.status !== 'ON'" style="color: #808181" class="iconfont icon-shexiangtou2"></span>
            <span style=" padding-left: 1px" v-if="node.data.deviceId !=='' && showCode" :title="node.data.deviceId">{{ node.label }}（编号：{{ node.data.deviceId }}）</span>
            <span style=" padding-left: 1px" v-if="node.data.deviceId ==='' || !showCode" :title="node.data.deviceId">{{ node.label }}</span>
          </span>
        </template>
      </vue-easy-tree>
    </div>
    <groupEdit ref="groupEdit"></groupEdit>
    <gbDeviceSelect ref="gbDeviceSelect"></gbDeviceSelect>
    <gbChannelSelect ref="gbChannelSelect" dataType="group"></gbChannelSelect>
  </div>
</template>

<script>
import VueEasyTree from "@wchbrad/vue-easy-tree";
import groupEdit from './../dialog/groupEdit'
import gbDeviceSelect from './../dialog/GbDeviceSelect'
import GbChannelSelect from "../dialog/GbChannelSelect.vue";

export default {
  name: 'DeviceTree',
  components: {
    GbChannelSelect,
    VueEasyTree, groupEdit, gbDeviceSelect
  },
  data() {
    return {
      props: {
        label: "name",
        id: "treeId"
      },
      showCode: false,
      searchSrt: "",
      chooseId: "",
      treeData: [],
    }
  },
  props: ['edit','enableAddChannel', 'clickEvent', 'onChannelChange', 'showHeader', 'hasChannel', 'addChannelToGroup', 'treeHeight'],
  created() {
  },
  methods: {
    search() {

    },
    loadNode: function (node, resolve) {
      if (node.level === 0) {
        resolve([{
          treeId: "",
          deviceId: "",
          name: "根资源组",
          isLeaf: false,
          type: 0
        }]);
      } else {
        if (node.data.leaf) {
          resolve([]);
          return
        }
        this.$axios({
          method: 'get',
          url: `/api/group/tree/list`,
          params: {
            query: this.searchSrt,
            parent: node.data.id,
            hasChannel: this.hasChannel
          }
        }).then((res) => {
          if (res.data.code === 0) {
            resolve(res.data.data);
          }

        }).catch(function (error) {
          console.log(error);
        });
      }
    },
    reset: function () {
      this.$forceUpdate();
    },
    contextmenuEventHandler: function (event, data, node, element) {
      if (!this.edit) {
        return;
      }
      if (node.data.type === 0) {
        let menuItem = [
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
              this.addGroup(data.id, node);
            }
          },
          {
            label: "编辑节点",
            icon: "el-icon-edit",
            disabled: node.level === 1,
            onClick: () => {
              this.editGroup(data, node);
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
                this.removeGroup(data.id, node)
              }).catch(() => {

              });
            }
          },
        ]

        if (this.enableAddChannel) {
          menuItem.push(
            {
              label: "添加设备",
              icon: "el-icon-plus",
              disabled: node.level <= 2,
              onClick: () => {
                this.addChannelFormDevice(data.id, node)
              }
            },
          )
          menuItem.push(
            {
              label: "移除设备",
              icon: "el-icon-delete",
              disabled: node.level <= 2,
              divided: true,
              onClick: () => {
                this.removeChannelFormDevice(data.id, node)
              }
            },
          )
          menuItem.push(
            {
              label: "添加通道",
              icon: "el-icon-plus",
              disabled: node.level <= 2,
              onClick: () => {
                this.addChannel(data.id, node)
              }
            },
          )
        }

        this.$contextmenu({
          items: menuItem,
          event, // 鼠标事件信息
          customClass: "custom-class", // 自定义菜单 class
          zIndex: 3000, // 菜单样式 z-index
        });
      }

      return false;
    },
    removeGroup: function (id, node) {
      this.$axios({
        method: "delete",
        url: `/api/group/delete`,
        params: {
          id: node.data.id,
        }
      }).then((res) => {
        if (res.data.code === 0) {
          console.log("移除成功")
          node.parent.loaded = false
          node.parent.expand();
          if (this.onChannelChange) {
            this.onChannelChange(node.data.deviceId)
          }
        }
      }).catch(function (error) {
        console.log(error);
      });
    },
    addChannelFormDevice: function (id, node) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        let deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$axios({
          method: 'post',
          url: `/api/common/channel/group/device/add`,
          data: {
            parentId: node.data.deviceId,
            businessGroup: node.data.businessGroup,
            deviceIds: deviceIds,
          }
        }).then((res) => {
          if (res.data.code === 0) {
            this.$message.success({
              showClose: true,
              message: "保存成功"
            })
            if (this.onChannelChange) {
              this.onChannelChange()
            }
            console.log(node)
            node.loaded = false
            node.expand();
          } else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
          this.loading = false
        }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      })
    },
    removeChannelFormDevice: function (id, node) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        let deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$axios({
          method: 'post',
          url: `/api/common/channel/group/device/delete`,
          data: {
            deviceIds: deviceIds,
          }
        }).then((res) => {
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
          } else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }
          this.loading = false
        }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
          this.loading = false
        });
      })
    },
    addChannel: function (id, node) {
      this.$refs.gbChannelSelect.openDialog((data) => {
        console.log("选择的数据")
        console.log(data)
        this.addChannelToGroup(node.data.deviceId, node.data.businessGroup, data)
      })
    },
    refreshNode: function (node) {
      console.log(node)
      node.loaded = false
      node.expand();
    },
    refresh: function (id) {
      console.log("刷新节点： " + id)
      // 查询node
      let node = this.$refs.veTree.getNode(id)
      if (node) {
        node.loaded = false
        node.expand();
      }
    },
    addGroup: function (id, node) {
      this.$refs.groupEdit.openDialog({
        id: 0,
        name: "",
        deviceId: "",
        civilCode: "",
        parentDeviceId: node.level > 2 ? node.data.deviceId : "",
        parentId: node.data.id,
        businessGroup: node.level > 2 ? node.data.businessGroup : node.data.deviceId,
      }, form => {
        console.log(node)
        node.loaded = false
        node.expand();
      }, id);
    },
    editGroup: function (id, node) {
      console.log(node)
      this.$refs.groupEdit.openDialog(node.data, form => {
        console.log(node)
        node.parent.loaded = false
        node.parent.expand();
      }, id);
    },
    nodeClickHandler: function (data, node, tree) {
      this.chooseId = data.deviceId;
      if (this.clickEvent) {
        this.clickEvent(data)
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
