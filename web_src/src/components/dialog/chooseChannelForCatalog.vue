<template>
<div id="chooseChannelForCatalog" >
   <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
     <el-tree class="el-scrollbar"
       ref="tree"
       id="catalogTree"
       empty-text="未知节点"
       node-key="id"
       default-expand-all
       :highlight-current="false"
       :expand-on-click-node="false"
       :props="props"
       :load="loadNode"
       @node-contextmenu="contextmenuEventHandler"
       @node-click="nodeClickHandler"
       lazy>
       <span class="custom-tree-node" slot-scope="{ node, data }" style="width: 100%">
         <el-radio v-if="node.data.type === 0 || node.data.type === -1" style="margin-right: 0" v-model="chooseId" :label="node.data.id">{{''}}</el-radio>
         <span v-if="node.data.type === -1 && node.level === 1" style="font-size: 12px" class="iconfont icon-ziyuan"></span>
         <span v-if="node.data.type === 0 && node.level === 1" class="el-icon-s-home"></span>
         <span v-if="node.data.type === 0 && node.level > 1"  class="el-icon-folder-opened"></span>
         <span v-if="node.data.type === 1" class="iconfont icon-shexiangtou"></span>
         <span v-if="node.data.type === 2" class="iconfont icon-zhibo"></span>
        <span style=" padding-left: 1px">{{ node.label }}</span>
        <span>
          <i style="margin-left: 5rem; color: #9d9d9d; padding-right: 20px" v-if="node.data.id === defaultCatalogIdSign">默认</i>
        </span>
      </span>
     </el-tree>
   </div>
    <catalogEdit ref="catalogEdit" :platformId="platformId"></catalogEdit>
</div>
</template>


<script>

import catalogEdit from './catalogEdit.vue'
export default {
    name: 'chooseChannelForCatalog',
    props: ['platformId', 'platformName', 'defaultCatalogId', 'catalogIdChange', 'treeType'],
    created() {
        this.chooseId = this.defaultCatalogId;
        this.defaultCatalogIdSign = this.defaultCatalogId;
        this.initData();
        setTimeout(()=>{
          if (this.catalogIdChange)this.catalogIdChange(this.defaultCatalogId, this.platformName);
        }, 100)

    },
    components: {
      catalogEdit,
    },
    data() {
        return {
          props: {
            label: 'name',
            children: 'children',
            isLeaf: 'leaf'
          },
          defaultCatalogIdSign: null,
          chooseNode: null,
          chooseId: "",
          chooseName: "",
          catalogTree: null,
          contextmenuShow: false

        };
    },
    watch:{
        platformId(newData, oldData){
            console.log(newData)
            this.initData()
        },
    },
    methods: {
        initData: function () {
            this.getCatalog();
        },

        getCatalog: function(parentId, callback) {
            let that = this;
            this.$axios({
                    method:"get",
                    url:`/api/platform/catalog`,
                    params: {
                        platformId: that.platformId,
                        parentId: parentId
                    }
                })
                .then((res)=> {
                  if (res.data.code === 0) {
                    if (typeof(callback) === 'function') {
                      callback(res.data.data)
                    }
                  }
                })
                .catch(function (error) {
                    console.log(error);
                });

        },
        addCatalog: function (parentId, node){
          let that = this;
          console.log(this.treeType)
          // 打开添加弹窗
          that.$refs.catalogEdit.openDialog(false, null, null, parentId, this.treeType, node.level, ()=>{
            node.loaded = false
            node.expand();
          });

        },
        refreshCatalog: function (node){
          node.loaded = false
          node.expand();
        },
        refreshCatalogById: function (id) {
          if (id) {
            let node = this.$refs.tree.getNode(id);
            this.refreshCatalog(node);
          }
        },
        editCatalog: function (data, node){
          let that = this;
          // 打开添加弹窗
          that.$refs.catalogEdit.openDialog(true, data.id, data.name, data.parentId, (newData)=>{
            node.parent.loaded = false
            node.parent.expand();
            if (data.id === this.chooseId && newData.name !== data.name) {
              if (this.catalogIdChange)this.catalogIdChange(this.chooseId, newData.name);
            }
          });

        },
        removeCatalog: function (id, node){
          this.$axios({
            method:"delete",
            url:`/api/platform/catalog/del`,
            params: {
              id: id,
              platformId: this.platformId,
            }
          })
            .then((res) => {
              if (res.data.code === 0) {
                console.log("移除成功")
                node.parent.loaded = false
                node.parent.expand();
                if (res.data.data) {
                  this.defaultCatalogIdSign = res.data.data;
                }
              }
            })
            .catch(function (error) {
              console.log(error);
            });
        },
        setDefaultCatalog: function (id){
          this.$axios({
            method:"post",
            url:`/api/platform/catalog/default/update`,
            params: {
              platformId: this.platformId,
              catalogId: id,
            }
          })
            .then((res)=> {
              if (res.data.code === 0) {
                this.defaultCatalogIdSign = id;
              }
            })
            .catch(function (error) {
              console.log(error);
            });
        },
        loadNode: function(node, resolve){
          if (node.level === 0) {
            resolve([
              {
              name: "未分配",
              id:  null,
              type:  -1
              },{
                name: this.platformName,
                id:  this.platformId,
                type:  0
              }
            ]);
          }
          if (node.level >= 1){
            this.getCatalog(node.data.id, resolve)
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
    }
};
</script>

<style>
#catalogTree{
  display: inline-block;
}
</style>
