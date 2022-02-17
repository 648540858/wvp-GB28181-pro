<template>
<div id="getCatalog" >

  <el-dialog title="选择要添加到的节点" v-if="showDialog"  width="50%" :append-to-body="true" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()" center>
    <div>
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
    <div style="float: right; height: 13rem">
      <el-button type="primary" size="mini" @click="submit()" >确认</el-button>
      <el-button @click="close()" size="mini">取消</el-button>
    </div>
  </el-dialog>

</div>
</template>


<script>

export default {
    name: 'getCatalog',
    beforeCreate(){

    },
    created() {
        this.chooseId = this.defaultCatalogId;
        this.defaultCatalogIdSign = this.defaultCatalogId;
        this.initData();
        setTimeout(()=>{
          if (this.catalogIdChange)this.catalogIdChange(this.defaultCatalogId);
        }, 100)

    },
    props: ['platformId'],
    data() {
        return {
          props: {
            label: 'name',
            children: 'children',
            isLeaf: 'leaf'
          },
          platformName: null,
          defaultCatalogId: null,
          catalogIdResult: null,
          showDialog: false,
          defaultCatalogIdSign: null,
          chooseNode: null,
          chooseId: "",
          catalogTree: null,
          contextmenuShow: false,

        };
    },
    methods: {
        openDialog(catalogIdResult) {
          this.showDialog = true
          this.catalogIdResult = catalogIdResult
        },
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
        loadNode: function(node, resolve){



          if (node.level === 0) {
            this.$axios({
              method:"get",
              url:`/api/platform/info/` + this.platformId,
            })
              .then((res)=> {
                if (res.data.code === 0) {
                  this.platformName = res.data.data.name;
                  this.defaultCatalogId = res.data.data.catalogId;
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
              })
              .catch(function (error) {
                console.log(error);
              });
          }
          if (node.level >= 1){
            this.getCatalog(node.data.id, resolve)
          }
        },
        nodeClickHandler: function (data, node, tree){
         this.chooseId = data.id;
        },
        close: function() {
          this.showDialog = false;
        },
        submit: function() {
          if (this.catalogIdResult)this.catalogIdResult(this.chooseId)
          this.showDialog = false;
        },
    }
};
</script>

<style>
#catalogTree{
  display: inline-block;
}
</style>
