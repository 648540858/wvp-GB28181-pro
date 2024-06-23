<template>
<div id="chooseChannelFoStream" v-loading="loading">
    <div style="font-size: 17px; color: #606060; white-space: nowrap; line-height: 30px; font-family: monospace;">
      <span v-if="catalogId == null">{{catalogName}}的部标通道</span>
      <span v-if="catalogId != null">{{catalogName}}({{catalogId}})的部标通道</span>
    </div>
    <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">

      搜索: <el-input @input="getChannelList" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字" prefix-icon="el-icon-search" v-model="searchSrt" clearable> </el-input>

      <el-button v-if="catalogId !== null" icon="el-icon-delete" size="mini" style="margin-right: 1rem;" :disabled="jtChannels.length === 0 || multipleSelection.length === 0" type="danger" @click="batchDel">批量移除</el-button>
      <el-button v-if="catalogId === null" icon="el-icon-plus" size="mini" style="margin-right: 1rem;" :disabled="jtChannels.length === 0 || multipleSelection.length === 0" @click="batchAdd">批量添加</el-button>
      <el-button v-if="catalogId === null"  icon="el-icon-plus" size="mini" style="margin-right: 1rem;" @click="add()">全部添加</el-button>
      <el-button v-if="catalogId !== null" type="danger" icon="el-icon-delete" size="mini" style="margin-right: 1rem;" @click="remove()">全部移除</el-button>
    </div>
    <el-table ref="jtChannelsTable" :data="jtChannels" border style="width: 100%" :height="winHeight" :row-key="(row)=> row.app + row.stream" @selection-change="handleSelectionChange">
        <el-table-column align="center" type="selection" :reserve-selection="true" width="55">
        </el-table-column>
        <el-table-column prop="name" label="名称" show-overflow-tooltip align="center">
        </el-table-column>
        <el-table-column prop="通道ID" label="通道ID" show-overflow-tooltip align="center">
        </el-table-column>
        <el-table-column prop="gbDeviceId" label="国标-编码"  show-overflow-tooltip align="center">
        </el-table-column>
        <el-table-column prop="gbName" label="国标-名称" show-overflow-tooltip align="center">
        </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right" >
        <template slot-scope="scope">
          <el-button-group>
            <el-button size="mini" icon="el-icon-plus" v-if="catalogId === null" @click="add(scope.row, scope)">添加</el-button>
            <el-button size="mini" icon="el-icon-delete" v-if="catalogId !== null" type="danger" @click="remove(scope.row, scope)">移除</el-button>
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
import MediaServer from './../service/MediaServer'
import getCatalog from './getCatalog'
export default {
    name: 'chooseChannelFoStream',
    computed: {
        // getPlayerShared: function () {
        //     return {
        //         sharedUrl: window.location.host + '/' + this.videoUrl,
        //         sharedIframe: '<iframe src="' + window.location.host + '/' + this.videoUrl + '"></iframe>',
        //         sharedRtmp: this.videoUrl
        //     };
        // }
    },
    props: ['platformId', 'catalogId',  'catalogName'],
    created() {
        this.initData();
        console.log(this.catalogId)
    },
    components: {
      getCatalog,
    },
    data() {
        return {
            loading: false,
            jtChannels: [],
            gbChoosechannel:{},
            channelType: "",
            online: "",
            choosed: "",
            currentPage: 1,
            count: 10,
            total: 0,
            searchSrt: "",
            pushing: "",
            mediaServerId: "",
            mediaServerList: [],
            mediaServerObj : new MediaServer(),
            eventEnable: false,
            multipleSelection: [],
            winHeight: window.innerHeight - 350,

        };
    },
    watch:{
        platformId(newData, oldData){
            this.getChannelList()
        },
        catalogId(newData, oldData){
            this.getChannelList()
        },
    },
    methods: {
        initData: function() {
            this.mediaServerObj.getOnlineMediaServerList((data)=>{
              this.mediaServerList = data.data;
            })
            this.getChannelList();
        },
        currentChange: function (val) {
            this.currentPage = val;
            this.getChannelList();
        },
        handleSizeChange: function (val) {
            this.count = val;
            console.log(val)
            this.getChannelList();

        },
        add: function (row, scope) {
          let all = typeof(row) === "undefined"
          this.getCatalogFromUser((catalogId)=>{
            let task = null;
            this.$axios({
              method:"post",
              url:"/api/gbStream/add",
              data:{
                platformId: this.platformId,
                catalogId: catalogId,
                all: all,
                jtChannels: all?[]:[row],
              }
            }).then((res)=>{
              console.log("保存成功")
              window.clearTimeout(task);
              this.loading = false;
              // this.jtChannels.splice(scope.$index,1)
              this.getChannelList();
            }).catch(function (error) {
              window.clearTimeout(task);
              this.loading = false;
              console.log(error);
            });
            task= setTimeout(()=>{
              this.loading = true;
            }, 200)
          })


        },
        remove: function (row, scope) {
          let all = typeof(row) === "undefined"
          this.$confirm(`确定移除${all?"所有通道":""}吗？`, '提示', {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {

            this.$axios({
              method:"delete",
              url:"/api/gbStream/del",
              data:{
                platformId: this.platformId,
                all: all,
                jtChannels: all?[]:[row],
              }
            }).then((res)=>{
              console.log("移除成功")
              // this.jtChannels.splice(scope.$index,1)
              this.getChannelList();
            }).catch(function (error) {
              console.log(error);
            });
          }).catch(() => {

          });

        },
        getChannelList: function () {
            let that = this;

            this.$axios({
                method: 'get',
                url:`/api/gbStream/list`,
                params: {
                    page: that.currentPage,
                    count: that.count,
                    query: that.searchSrt,
                    platformId: that.platformId,
                    catalogId: that.catalogId,
                    mediaServerId: that.mediaServerId
                }
                })
                .then(function (res) {
                    if (res.data.code === 0) {
                      that.total = res.data.data.total;
                      that.jtChannels = res.data.data.list;
                      that.gbChoosechannel = {};
                      // 防止出现表格错位
                      that.$nextTick(() => {
                        that.$refs.jtChannelsTable.doLayout();
                        // 默认选中
                        that.eventEnable = true;
                      })
                    }
                })
                .catch(function (error) {
                    console.log(error);
                });

        },
        batchDel: function() {
          this.$confirm(`确认这${this.multipleSelection.length}个通道吗？`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            this.$axios({
              method:"delete",
              url:"/api/gbStream/del",
              data:{
                platformId: this.platformId,
                jtChannels:  this.multipleSelection,
              }
            }).then((res)=>{
              console.log("移除成功")
              this.$refs.jtChannelTable.clearSelection()
              this.getChannelList();
            }).catch(function (error) {
              console.log(error);
            });
          }).catch(() => {
          });

        },
        batchAdd: function() {
          this.getCatalogFromUser((catalogId)=>{
            this.$axios({
              method:"post",
              url:"/api/gbStream/add",
              data:{
                platformId: this.platformId,
                catalogId: catalogId,
                jtChannel:  this.multipleSelection,
              }
            }).then((res)=>{
              console.log("保存成功")
              this.$refs.jtChannelTable.clearSelection()
              this.getChannelList();
            }).catch(function (error) {
              console.log(error);
            });
          })
        },
        getCatalogFromUser(callback){
            this.$refs.getCatalog.openDialog(callback)
        },
        handleSelectionChange: function (val) {
          this.multipleSelection = val;
        },
    }
};
</script>

<style>

</style>
