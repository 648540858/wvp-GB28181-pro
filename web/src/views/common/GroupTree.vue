<template>
  <div id="groupTree" style="border-right: 1px solid #EBEEF5; height: 100%">
    <div style="padding: 0 20px 0 10px;">
      <el-input size="small" v-model="searchStr" @input="searchChange" suffix-icon="el-icon-search" placeholder="请输入搜索内容" clearable>
        <!--        <el-select v-model="searchType" slot="prepend" placeholder="搜索类型" style="width: 80px">-->
        <!--          <el-option label="目录" :value="0"></el-option>-->
        <!--          <el-option label="通道" :value="1"></el-option>-->
        <!--        </el-select>-->
      </el-input>
    </div>
    <div v-if="!searchStr">
      <el-alert
        v-if="showAlert && edit"
        title="操作提示"
        description="你可以使用右键菜单管理节点"
        type="info"
        style="text-align: left"
      />
      <div v-if="edit" style="margin-top: 18px;font-size: 14px;position: absolute;left: 309px;z-index: 100;" >
        显示编号： <el-checkbox v-model="showCode" />
      </div>

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
        <template v-slot:default="{ node, data }">
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
            <span v-if="node.data.longitude && showPosition" class="iconfont icon-gps"></span>
          </span>
        </template>
      </vue-easy-tree>
    </div>
    <div v-if="searchStr" style="color: #606266; height: calc(100% - 32px); overflow: auto !important;">
      <ul v-if="groupList.length > 0" style="list-style: none; margin: 0; padding: 10px">
        <li v-for="item in groupList" :key="item.id" class="channel-list-li" style="height: 26px; align-items: center;cursor: pointer;" @click="listClickHandler(item)">
          <span
            v-if="chooseId !== item.deviceId"
            style="color: #409EFF; font-size: 20px"
            class="iconfont icon-bianzubeifen3"
          />
          <span
            v-if="chooseId === item.deviceId"
            style="color: #c60135; font-size: 20px"
            class="iconfont icon-bianzubeifen3"
          />
          <div>
            <div style="margin-left: 4px; margin-bottom: 3px; font-size: 15px">{{item.name}}</div>
            <div style="margin-left: 4px; font-size: 13px; color: #808181">{{item.deviceId}}</div>
          </div>
        </li>
      </ul>

      <ul v-if="channelList.length > 0" style="list-style: none; margin: 0; padding: 10px; overflow: auto">
        <li v-for="item in channelList" :key="item.id" class="channel-list-li" @click="channelLstClickHandler(item)">
          <span
            v-if="item.gbStatus === 'ON'"
            style="color: #409EFF; font-size: 20px"
            class="iconfont icon-shexiangtou2"
          />
          <span
            v-if="item.gbStatus !== 'ON'"
            style="color: #808181; font-size: 20px"
            class="iconfont icon-shexiangtou2"
          />
          <div>
            <div style="margin-left: 4px; margin-bottom: 3px; font-size: 15px">{{item.gbName}}</div>
            <div style="margin-left: 4px; font-size: 13px; color: #808181">{{item.gbDeviceId}}</div>
          </div>

        </li>
      </ul>
      <div v-if="this.currentPage * this.count < this.total" style="text-align: center;">
        <el-button type="text" @click="loadListMore">加载更多</el-button>
      </div>
    </div>
    <groupEdit ref="groupEdit" />
    <gbDeviceSelect ref="gbDeviceSelect" />
    <gbChannelSelect ref="gbChannelSelect" data-type="group" />
  </div>
</template>

<script>
import VueEasyTree from '@wchbrad/vue-easy-tree'
import groupEdit from './../dialog/groupEdit'
import gbDeviceSelect from './../dialog/GbDeviceSelect'
import GbChannelSelect from '../dialog/GbChannelSelect.vue'

export default {
  name: 'DeviceTree',
  components: {
    GbChannelSelect,
    VueEasyTree, groupEdit, gbDeviceSelect
  },
  props: ['edit', 'enableAddChannel', 'onChannelChange', 'showHeader', 'hasChannel', 'addChannelToGroup', 'treeHeight', 'showPosition', 'contextmenu'],
  data() {
    return {
      props: {
        label: 'name',
        id: 'treeId'
      },
      showCode: false,
      showAlert: true,
      searchStr: '',
      chooseId: '',
      treeData: [],
      currentPage: this.defaultPage | 1,
      count: this.defaultCount | 15,
      total: 0,
      groupList: [],
      channelList: []
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
    searchChange() {
      this.currentPage = 1
      this.total = 0
      if (this.edit) {
        this.groupList = []
        this.queryGroup()
      }else {
        this.channelList = []
        this.queryChannelList()
      }
    },
    loadListMore: function() {
      this.currentPage += 1
      if (this.edit) {
        this.queryGroup()
      }else {
        this.queryChannelList()
      }
    },
    queryGroup: function() {
      this.$store.dispatch('group/queryTree', {
        query: this.searchStr,
        page: this.currentPage,
        count: this.count
      }).then(data => {
        this.total = data.total
        this.groupList = this.groupList.concat(data.list)
      })
    },
    queryChannelList: function() {
      this.$store.dispatch('commonChanel/getList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr
      }).then(data => {
        this.total = data.total
        this.channelList = this.channelList.concat(data.list)
      })
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
      } else {
        if (node.data.leaf) {
          resolve([])
          return
        }
        this.$store.dispatch('group/getTreeList', {
          query: this.searchStr,
          parent: node.data.id,
          hasChannel: this.hasChannel
        }).then(data => {
          if (data.length > 0) {
            this.showAlert = false
          }
          resolve(data)
        }).finally(() => {
          this.locading = false
        })
      }
    },
    reset: function() {
      this.$forceUpdate()
    },
    contextmenuEventHandler: function(event, data, node, element) {
      if (!this.edit && !this.contextmenu) {
        return
      }
      const allMenuItem = []
      if (node.data.type === 0) {
        if (this.edit) {
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
                this.addGroup(data.id, node)
              }
            },
            {
              label: '编辑节点',
              icon: 'el-icon-edit',
              disabled: node.level === 1,
              onClick: () => {
                this.editGroup(data, node)
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
                  this.removeGroup(data.id, node)
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
                disabled: node.level <= 2,
                onClick: () => {
                  this.addChannelFormDevice(data.id, node)
                }
              }
            )
            menuItem.push(
              {
                label: '移除设备',
                icon: 'el-icon-delete',
                disabled: node.level <= 2,
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
                disabled: node.level <= 2,
                onClick: () => {
                  this.addChannel(data.id, node)
                }
              }
            )
          }
          allMenuItem.push(...menuItem)
        }
        if (this.contextmenu) {
          for (let i = 0; i < this.contextmenu.length; i++) {
            let item = this.contextmenu[i]
            if (item.type === node.data.type) {
              allMenuItem.push({
                label: item.label,
                icon: item.icon,
                onClick: () => {
                  item.onClick(event, data, node)
                }
              })
            }
          }
        }
        if (allMenuItem.length === 0) {
          return
        }

        this.$contextmenu({
          items: allMenuItem,
          event, // 鼠标事件信息
          customClass: 'custom-class', // 自定义菜单 class
          zIndex: 3000 // 菜单样式 z-index
        })
      }

      return false
    },
    removeGroup: function(id, node) {
      this.$store.dispatch('group/deleteGroup', node.data.id)
        .then(data => {
          node.parent.loaded = false
          node.parent.expand()
          this.$emit('onChannelChange', node.data.deviceId)
        })
    },
    addChannelFormDevice: function(id, node) {
      this.$refs.gbDeviceSelect.openDialog((rows) => {
        const deviceIds = []
        for (let i = 0; i < rows.length; i++) {
          deviceIds.push(rows[i].id)
        }
        this.$store.dispatch('commonChanel/addDeviceToGroup', {
          parentId: node.data.deviceId,
          businessGroup: node.data.businessGroup,
          deviceIds: deviceIds
        })
          .then(data => {
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
            this.$emit('onChannelChange', node.data.deviceId)
            node.loaded = false
            node.expand()
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
        this.$store.dispatch('commonChanel/deleteDeviceFromGroup', deviceIds)
          .then(data => {
            this.$message.success({
              showClose: true,
              message: '保存成功'
            })
            this.$emit('onChannelChange', node.data.deviceId)
            node.loaded = false
            node.expand()
          }).finally(() => {
            this.loading = false
          })
      })
    },
    addChannel: function(id, node) {
      this.$refs.gbChannelSelect.openDialog((data) => {
        console.log('选择的数据')
        console.log(data)
        this.addChannelToGroup(node.data.deviceId, node.data.businessGroup, data)
      })
    },
    refreshNode: function(node) {
      console.log(node)
      node.loaded = false
      node.expand()
    },
    refresh: function(id) {
      console.log('刷新节点： ' + id)
      // 查询node
      const node = this.$refs.veTree.getNode(id)
      if (node) {
        node.loaded = false
        node.expand()
      }
    },
    addGroup: function(id, node) {
      this.$refs.groupEdit.openDialog({
        id: 0,
        name: '',
        deviceId: '',
        civilCode: '',
        parentDeviceId: node.level > 2 ? node.data.deviceId : '',
        parentId: node.data.id,
        businessGroup: node.level > 2 ? node.data.businessGroup : node.data.deviceId
      }, form => {
        console.log(node)
        node.loaded = false
        node.expand()
      }, id)
    },
    editGroup: function(id, node) {
      console.log(node)
      this.$refs.groupEdit.openDialog(node.data, form => {
        console.log(node)
        node.parent.loaded = false
        node.parent.expand()
      }, id)
    },
    nodeClickHandler: function(data, node, tree) {
      this.chooseId = data.deviceId
      this.$emit('clickEvent', data)
    },
    listClickHandler: function(data) {
      this.chooseId = data.deviceId
      this.$emit('clickEvent', data)
    },
    channelLstClickHandler: function(data) {
      this.$emit('clickEvent', {
        leaf: true,
        id: data.gbId
      })
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

.flow-tree {
  overflow: auto;
  padding-top: 10px;
}
.flow-tree  .vue-recycle-scroller__item-wrapper{
  height: 100%;
  overflow-x: auto;
}
</style>
