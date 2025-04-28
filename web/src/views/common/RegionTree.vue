<template>
  <div id="DeviceTree" style="border-right: 1px solid #EBEEF5; padding: 0 20px">
    <div v-if="showHeader" class="page-header">
      <el-form :inline="true" size="mini">
        <el-form-item style="visibility: hidden">
          <el-input
            v-model="searchSrt"
            style="margin-right: 1rem; width: 12rem;"
            size="mini"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="search"
          />
        </el-form-item>
        <el-form-item label="显示编号">
          <el-checkbox v-model="showCode" />
        </el-form-item>
      </el-form>
    </div>
    <div>
      <el-alert
        v-if="showAlert && edit"
        title="操作提示"
        description="你可以使用右键菜单管理节点"
        type="info"
        style="text-align: left"
      />
      <vue-easy-tree
        ref="veTree"
        class="flow-tree"
        node-key="treeId"
        :height="treeHeight?treeHeight:'78vh'"
        lazy
        :load="loadNode"
        :data="treeData"
        :props="props"
        :default-expanded-keys="['']"
        @node-contextmenu="contextmenuEventHandler"
        @node-click="nodeClickHandler"
      >
        <template v-slot:default="{ node, data }" class="custom-tree-node">
          <span class="custom-tree-node">
            <span
              v-if="node.data.type === 0 && chooseId !== node.data.deviceId"
              style="color: #409EFF"
              class="iconfont icon-bianzubeifen3"
            />
            <span
              v-if="node.data.type === 0 && chooseId === node.data.deviceId"
              style="color: #c60135;"
              class="iconfont icon-bianzubeifen3"
            />
            <span
              v-if="node.data.type === 1 && node.data.status === 'ON'"
              style="color: #409EFF"
              class="iconfont icon-shexiangtou2"
            />
            <span
              v-if="node.data.type === 1 && node.data.status !== 'ON'"
              style="color: #808181"
              class="iconfont icon-shexiangtou2"
            />
            <span
              v-if="node.data.deviceId !=='' && showCode"
              style=" padding-left: 1px"
              :title="node.data.deviceId"
            >{{ node.label }}（编号：{{ node.data.deviceId }}）</span>
            <span
              v-if="node.data.deviceId ==='' || !showCode"
              style=" padding-left: 1px"
              :title="node.data.deviceId"
            >{{ node.label }}</span>
          </span>
        </template>
      </vue-easy-tree>
    </div>
    <regionEdit ref="regionEdit" />
    <gbDeviceSelect ref="gbDeviceSelect" />
    <GbChannelSelect ref="gbChannelSelect" data-type="civilCode" />
  </div>
</template>

<script>
import VueEasyTree from '@wchbrad/vue-easy-tree'
import regionEdit from './../dialog/regionEdit'
import gbDeviceSelect from './../dialog/GbDeviceSelect'
import GbChannelSelect from '../dialog/GbChannelSelect.vue'

export default {
  name: 'DeviceTree',
  components: {
    GbChannelSelect,
    VueEasyTree, regionEdit, gbDeviceSelect
  },
  props: ['edit', 'enableAddChannel', 'clickEvent', 'onChannelChange', 'showHeader', 'hasChannel', 'addChannelToCivilCode', 'treeHeight'],
  data() {
    return {
      props: {
        label: 'name'
      },
      showCode: false,
      showAlert: true,
      searchSrt: '',
      chooseId: '',
      treeData: []
    }
  },
  created() {
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
    search() {

    },
    loadNode: function(node, resolve) {
      if (node.level === 0) {
        resolve([{
          treeId: '',
          deviceId: '',
          name: '根资源组',
          isLeaf: false,
          type: 0
        }])
      } else if (node.data.deviceId.length <= 8) {
        if (node.data.leaf) {
          resolve([])
          return
        }
        this.$store.dispatch('region/getTreeList', {
          query: this.searchSrt,
          parent: node.data.id,
          hasChannel: this.hasChannel
        })
          .then(data => {
            if (data.length > 0) {
              this.showAlert = false
            }
            resolve(data)
          }).finally(() => {
            this.locading = false
          })
      } else {
        resolve([])
      }
    },
    reset: function() {
      this.$forceUpdate()
    },
    contextmenuEventHandler: function(event, data, node, element) {
      if (!this.edit) {
        return
      }
      console.log(node.level)
      if (node.data.type === 0) {
        const menuItem = [
          {
            label: '刷新节点',
            icon: 'el-icon-refresh',
            disabled: false,
            onClick: () => {
              this.refreshNode(node)
            }
          },
          {
            label: '新建节点',
            icon: 'el-icon-plus',
            disabled: false,
            onClick: () => {
              this.addRegion(data.id, node)
            }
          },
          {
            label: '编辑节点',
            icon: 'el-icon-edit',
            disabled: node.level === 1,
            onClick: () => {
              this.editCatalog(data, node)
            }
          },
          {
            label: '删除节点',
            icon: 'el-icon-delete',
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

              })
            }
          }
        ]
        if (this.enableAddChannel) {
          menuItem.push(
            {
              label: '添加设备',
              icon: 'el-icon-plus',
              disabled: node.level === 1,
              onClick: () => {
                this.addChannelFormDevice(data.id, node)
              }
            }
          )
          menuItem.push(
            {
              label: '移除设备',
              icon: 'el-icon-delete',
              disabled: node.level === 1,
              divided: true,
              onClick: () => {
                this.removeChannelFormDevice(data.id, node)
              }
            }
          )
          menuItem.push(
            {
              label: '添加通道',
              icon: 'el-icon-plus',
              disabled: node.level === 1,
              onClick: () => {
                this.addChannel(data.id, node)
              }
            }
          )
        }

        this.$contextmenu({
          items: menuItem,
          event, // 鼠标事件信息
          customClass: 'custom-class', // 自定义菜单 class
          zIndex: 3000 // 菜单样式 z-index
        })
      }

      return false
    },
    removeRegion: function(id, node) {
      this.$store.dispatch('region/deleteRegion', node.data.id)
        .then((data) => {
          console.log('移除成功')
          node.parent.loaded = false
          node.parent.expand()
        }).catch(function(error) {
          console.log(error)
        })
    },
    addChannelFormDevice: function(id, node) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$store.dispatch('commonChanel/addDeviceToRegion', {
          civilCode: node.data.deviceId,
          deviceIds: deviceIds
        }).then((data) => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
          if (this.onChannelChange) {
            this.onChannelChange()
          }
          node.loaded = false
          node.expand()
        }).catch(function(error) {
          console.log(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    removeChannelFormDevice: function(id, node) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$store.dispatch('commonChanel/deleteDeviceFromRegion', deviceIds)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
            if (this.onChannelChange) {
              this.onChannelChange(node.data.deviceId)
            }
            node.loaded = false
            node.expand()
          }).catch(function(error) {
            console.log(error)
          }).finally(() => {
            this.loading = false
          })
      })
    },
    addChannel: function(id, node) {
      this.$refs.gbChannelSelect.openDialog((data) => {
        console.log('选择的数据')
        console.log(data)
        this.addChannelToCivilCode(node.data.deviceId, data)
      })
    },
    refreshNode: function(node) {
      node.loaded = false
      node.expand()
    },
    refresh: function(id) {
      console.log(id)
      // 查询node
      const node = this.$refs.veTree.getNode(id)
      if (node) {
        node.loaded = false
        node.expand()
      }
    },
    addRegion: function(id, node) {
      console.log(node)

      this.$refs.regionEdit.openDialog(form => {
        node.loaded = false
        node.expand()
      }, {
        deviceId: '',
        name: '',
        parentId: node.data.id,
        parentDeviceId: node.data.deviceId
      })
    },
    editCatalog: function(data, node) {
      // 打开添加弹窗
      this.$refs.regionEdit.openDialog(form => {
        node.loaded = false
        node.expand()
      }, node.data)
    },
    nodeClickHandler: function(data, node, tree) {
      this.chooseId = data.deviceId
      if (this.clickEvent) {
        this.clickEvent(data)
      }
    }
  }
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

.tree-scroll{
  width: 200px;
  border: 1px solid #E7E7E7;
  height: 100%;
}

.flow-tree {
  overflow: auto;
  margin: 10px;
}
.flow-tree  .vue-recycle-scroller__item-wrapper{
  height: 100%;
  overflow-x: auto;
}

</style>
