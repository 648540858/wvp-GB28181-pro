<template>
<div id="chooseChannelForGb" v-loading="loading">
  <div style="font-size: 17px; color: #606060; white-space: nowrap; line-height: 30px; font-family: monospace;">
    <span v-if="catalogId == null">{{catalogName}}的国标通道</span>
    <span v-if="catalogId != null">{{catalogName}}({{catalogId}})的国标通道</span>
  </div>
   <div style="background-color: #FFFFFF; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
        搜索: <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字" prefix-icon="el-icon-search" v-model="searchSrt" clearable> </el-input>

        通道类型: <el-select size="mini" @change="search" style="margin-right: 1rem; width:6rem" v-model="channelType" placeholder="请选择" default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="设备" value="false"></el-option>
            <el-option label="子目录" value="true"></el-option>
        </el-select>

        在线状态: <el-select size="mini" style="margin-right: 1rem; width:6rem" @change="search" v-model="online" placeholder="请选择" default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="在线" value="true"></el-option>
            <el-option label="离线" value="false"></el-option>
        </el-select>
     <el-button v-if="catalogId !== null" icon="el-icon-delete" size="mini" :disabled="gbChannels.length === 0 || multipleSelection.length === 0" type="danger" @click="batchDel">批量移除</el-button>
     <el-button v-if="catalogId === null" icon="el-icon-plus" size="mini" :disabled="gbChannels.length === 0 || multipleSelection.length === 0" @click="batchAdd">批量添加</el-button>
     <el-button v-if="catalogId === null" icon="el-icon-plus" size="mini" @click="add()">全部添加</el-button>
     <el-button v-if="catalogId !== null" type="danger" icon="el-icon-delete" size="mini" @click="remove()">全部移除</el-button>
    </div>

    <el-table ref="gbChannelsTable" :data="gbChannels" border style="width: 100%" :height="winHeight" :row-key="(row)=> row.deviceId + row.channelId" @selection-change="handleSelectionChange">
        <el-table-column align="center" type="selection" :reserve-selection="true" width="55">
        </el-table-column>
        <el-table-column prop="channelId" label="通道编号" width="180" align="center">
        </el-table-column>
        <el-table-column prop="name" label="通道名称" show-overflow-tooltip align="center">
        </el-table-column>
        <el-table-column prop="deviceId" label="设备编号" width="180" align="center">
        </el-table-column>
        <el-table-column label="设备地址" width="180" align="center">
            <template slot-scope="scope">
                <div slot="reference" class="name-wrapper">
                    <el-tag size="medium">{{ scope.row.hostAddress }}</el-tag>
                </div>
            </template>
        </el-table-column>
        <el-table-column prop="manufacturer" label="厂家" align="center">
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button-group>
              <el-button size="mini" icon="el-icon-plus" v-if="catalogId === null" @click="add(scope.row)">添加</el-button>
              <el-button size="mini" icon="el-icon-delete" v-if="catalogId !== null" type="danger" @click="remove(scope.row)">移除</el-button>
            </el-button-group>
          </template>
        </el-table-column>
    </el-table>
    <el-pagination style="float: right;margin-top: 1rem;" @size-change="handleSizeChange" @current-change="currentChange" :current-page="currentPage" :page-size="count" :page-sizes="[10, 20, 30, 50]" layout="total, sizes, prev, pager, next" :total="total">
    </el-pagination>
    <getCatalog ref="getCatalog" :platformId="platformId" ></getCatalog>
</div>
</template>

<script>
import getCatalog from './getCatalog'
export default {
    name: 'chooseChannelForGb',
    computed: {
        // getPlayerShared: function () {
        //     return {
        //         sharedUrl: window.location.host + '/' + this.videoUrl,
        //         sharedIframe: '<iframe src="' + window.location.host + '/' + this.videoUrl + '"></iframe>',
        //         sharedRtmp: this.videoUrl
        //     };
        // }
    },
    props: ['platformId','catalogId',  'catalogName'],
    created() {
        this.initData();
    },
    components: {
      getCatalog,
    },
    data() {
        return {
            loading: false,
            gbChannels: [],
            gbChoosechannel:{},
            searchSrt: "",
            channelType: "",
            online: "",
            choosed: "",
            currentPage: 1,
            count: 10,
            total: 0,
            eventEnable: false,
            multipleSelection: [],
            winHeight: window.innerHeight - 400,

        };
    },
    watch:{
        platformId(newData, oldData){
            console.log(newData)
            this.getChannelList()
        },
        catalogId(newData, oldData){
          this.getChannelList()
        },
    },
    methods: {
        initData: function() {
            this.getChannelList();
        },
        currentChange: function (val) {
            this.currentPage = val;
            this.initData();
        },
        handleSizeChange: function (val) {
            this.count = val;
            console.log(val)
            this.initData();
        },
        add: function (row) {
          let all = typeof(row) === "undefined"

          this.getCatalogFromUser((catalogId)=> {
            let task = null;
            this.$axios({
              method:"post",
              url:"/api/platform/update_channel_for_gb",
              data:{
                platformId:  this.platformId,
                all: all,
                channelReduces: all?[]:[row],
                catalogId: catalogId
              }
            }).then((res)=>{
              console.log("保存成功")
              window.clearTimeout(task);
              this.loading = false;
              this.getChannelList();
            }).catch((error)=> {
              window.clearTimeout(task);
              this.loading = false;
              console.log(error);
            });
            task= setTimeout(()=>{
              this.loading = true;
            }, 200)
          })


        },
        remove: function (row) {
          let all = typeof(row) === "undefined"
          this.$confirm(`确定移除${all?"所有通道":""}吗？`, '提示', {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            console.log(row)

            this.$axios({
              method:"delete",
              url:"/api/platform/del_channel_for_gb",
              data:{
                platformId:  this.platformId,
                all: all,
                channelReduces: all?[]:[row],
              }
            }).then((res)=>{
              console.log("移除成功")
              this.getChannelList();
            }).catch(function (error) {
              console.log(error);
            });
          }).catch(() => {

          });


        },
        // checkedChange: function (val) {
        //     let that = this;
        //     if (!that.eventEnable) {
        //         return;
        //     }
        //     let newData = {};
        //     let addData = [];
        //     let delData = [];
        //     if (val.length > 0) {
        //         for (let i = 0; i < val.length; i++) {
        //             const element = val[i];
        //             let key = element.deviceId + "_" + element.channelId;
        //             newData[key] = element;
        //             if (!!!that.gbChoosechannel[key]){
        //                 addData.push(element)
        //             }else{
        //                 delete that.gbChoosechannel[key]
        //             }
        //         }
        //
        //         let oldKeys = Object.keys(that.gbChoosechannel);
        //         if (oldKeys.length > 0) {
        //             for (let i = 0; i < oldKeys.length; i++) {
        //                 const key = oldKeys[i];
        //                 delData.push(that.gbChoosechannel[key])
        //             }
        //         }
        //
        //     }else{
        //         let oldKeys = Object.keys(that.gbChoosechannel);
        //         if (oldKeys.length > 0) {
        //             for (let i = 0; i < oldKeys.length; i++) {
        //                 const key = oldKeys[i];
        //                 delData.push(that.gbChoosechannel[key])
        //             }
        //         }
        //     }
        //
        //     that.gbChoosechannel = newData;
        //     if (Object.keys(addData).length >0) {
        //         that.$axios({
        //             method:"post",
        //             url:"/api/platform/update_channel_for_gb",
        //             data:{
        //                 platformId:  that.platformId,
        //                 channelReduces: addData,
        //                 catalogId: that.catalogId
        //             }
        //         }).then((res)=>{
        //             console.log("保存成功")
        //         }).catch(function (error) {
        //             console.log(error);
        //         });
        //     }
        //     if (delData.length >0) {
        //          that.$axios({
        //             method:"delete",
        //             url:"/api/platform/del_channel_for_gb",
        //             data:{
        //                 platformId:  that.platformId,
        //                 channelReduces: delData
        //             }
        //         }).then((res)=>{
        //             console.log("移除成功")
        //            let nodeIds = new Array();
        //            for (let i = 0; i < delData.length; i++) {
        //              nodeIds.push(delData[i].channelId)
        //            }
        //         }).catch(function (error) {
        //             console.log(error);
        //         });
        //     }
        // },
        // shareAllCheckedChange: function (val) {
        //
        // },
        getChannelList: function () {
            let that = this;

            this.$axios({
                    method:"get",
                    url:`/api/platform/channel_list`,
                    params: {
                        page: that.currentPage,
                        count: that.count,
                        query: that.searchSrt,
                        online: that.online,
                        catalogId: that.catalogId,
                        platformId: that.platformId,
                        channelType: that.channelType
                    }
                })
                .then(function (res) {
                  if (res.data.code === 0 ) {
                    that.total = res.data.data.total;
                    that.gbChannels = res.data.data.list;
                    that.gbChoosechannel = {};
                  }
                  // 防止出现表格错位
                  that.$nextTick(() => {
                      that.$refs.gbChannelsTable.doLayout();
                      that.eventEnable = true;
                  })
                })
                .catch(function (error) {
                    console.log(error);
                });

        },
        search: function() {
            this.initData();
        },
        handleGBSelectionChange: function() {
            this.initData();
        },
      batchDel: function() {
        this.$confirm(`确认这${this.multipleSelection.length}个通道吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$axios({
            method:"delete",
            url:"/api/platform/del_channel_for_gb",
            data:{
              platformId:  this.platformId,
              channelReduces: this.multipleSelection
            }
          }).then((res)=>{
            console.log("移除成功")
            this.$refs.gbChannelsTable.clearSelection()
            this.getChannelList();
          }).catch(function (error) {
            console.log(error);
          });
        }).catch(() => {
        });
      },
      batchAdd: function() {
        this.getCatalogFromUser((catalogId)=> {

          this.$axios({
            method: "post",
            url: "/api/platform/update_channel_for_gb",
            data: {
              platformId: this.platformId,
              channelReduces: this.multipleSelection,
              catalogId: catalogId,
            }
          }).then((res) => {
            console.log("保存成功")
            this.$refs.gbChannelsTable.clearSelection()
            this.getChannelList();
          }).catch(function (error) {
            console.log(error);
          });
        });
      },
      handleSelectionChange: function (val) {
        this.multipleSelection = val;
      },
      getCatalogFromUser(callback){
        this.$refs.getCatalog.openDialog(callback)
      },
    }
};
</script>

<style>

</style>
