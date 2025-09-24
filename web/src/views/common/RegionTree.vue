<template>
  <div id="regionTree" style="border-right: 1px solid #EBEEF5; height: 100%">
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
      <div v-if="edit" style="float: right;margin-right: 24px;margin-top: 18px; font-size: 14px" >
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
              style=" padding-left: 1px"
              :title="node.data.deviceId"
            >{{ node.label }}</span>
            <span v-if="node.data.deviceId !=='' && showCode">（编号：{{ node.data.deviceId }}）</span>
            <span v-if="node.data.longitude && showPosition" class="iconfont icon-gps"></span>
          </span>
        </template>
      </vue-easy-tree>
    </div>
    <div v-if="searchStr" style="color: #606266; height: calc(100% - 32px); overflow: auto !important;">
      <ul v-if="regionList.length > 0" style="list-style: none; margin: 0; padding: 10px">
        <li v-for="item in regionList" :key="item.id" class="channel-list-li" style="height: 26px; align-items: center;cursor: pointer;" @click="listClickHandler(item)">
          <span
            v-if="chooseId !== item.deviceId"
            style="color: #409EFF;  font-size: 20px"
            class="iconfont icon-bianzubeifen3"
          />
          <span
            v-if="chooseId === item.deviceId"
            style="color: #c60135;  font-size: 20px"
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
import chooseCivilCode from '@/views/dialog/chooseCivilCode.vue'

export default {
  name: 'DeviceTree',
  components: {
    GbChannelSelect,
    VueEasyTree, regionEdit, gbDeviceSelect
  },
  props: ['edit', 'enableAddChannel', 'onChannelChange', 'showHeader', 'hasChannel', 'addChannelToCivilCode', 'treeHeight', 'showPosition', 'contextmenu'],
  data() {
    return {
      props: {
        label: 'name',
        children: 'children'
      },
      searchType: 0,
      showCode: false,
      showAlert: true,
      searchStr: '',
      chooseId: '',
      treeData: [],
      currentPage: this.defaultPage | 1,
      count: this.defaultCount | 15,
      total: 0,
      regionList: [],
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
        this.regionList = []
        this.queryRegion()
      }else {
        this.channelList = []
        this.queryChannelList()
      }
    },
    loadListMore: function() {
      this.currentPage += 1
      if (this.edit) {
        this.queryRegion()
      }else {
        this.queryChannelList()
      }
    },
    queryRegion: function() {
      this.$store.dispatch('region/queryTree', {
        query: this.searchStr,
        page: this.currentPage,
        count: this.count
      }).then(data => {
        this.total = data.total
        this.regionList = this.regionList.concat(data.list)
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
      } else if (node.data.deviceId.length <= 8) {
        if (node.data.leaf) {
          resolve([])
          return
        }
        this.$store.dispatch('region/getTreeList', {
          query: this.searchStr,
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
          allMenuItem.push(...menuItem)
        }
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
      return false
    },
    removeRegion: function(id, node) {
      this.$store.dispatch('region/deleteRegion', node.data.id)
        .then((data) => {
          console.log('移除成功')
          this.$emit('onChannelChange', node.data.deviceId)
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
          this.$emit('onChannelChange', node.data.deviceId)
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
            this.$emit('onChannelChange', node.data.deviceId)
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
      // 查询node
      const node = this.$refs.veTree.getNode(id)
      if (node) {
        if (id.includes('channel') >= 0) {
          node.parent.loaded = false
          node.parent.expand()
        }else {
          node.loaded = false
          node.expand()
        }
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

.tree-scroll{
  width: 200px;
  border: 1px solid #E7E7E7;
  height: 100%;
}

.flow-tree {
  overflow: auto;
  padding-top: 10px;
}
.flow-tree  .vue-recycle-scroller__item-wrapper{
  height: 100%;
  overflow-x: auto;
}
.channel-list-li {
  height: 24px;
  align-items: center;
  cursor: pointer;
  display: grid;
  grid-template-columns: 26px 1fr;
  margin-bottom: 18px
}
</style>
