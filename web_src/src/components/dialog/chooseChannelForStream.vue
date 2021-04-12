<template>
<div id="chooseChannelFoStream" >
    <el-table ref="gbStreamsTable" :data="gbStreams" border style="width: 100%" @selection-change="checkedChanage" >
        <el-table-column type="selection" width="55" align="center" fixed > </el-table-column>
        <el-table-column prop="name" label="名称" show-overflow-tooltip>
        </el-table-column>
        <el-table-column prop="app" label="应用名" show-overflow-tooltip>
        </el-table-column>
        <el-table-column prop="stream" label="流ID"  show-overflow-tooltip>
        </el-table-column>
        <el-table-column prop="gbId" label="国标编码" show-overflow-tooltip>
        </el-table-column>
        <el-table-column label="流来源" width="100" align="center">
            <template slot-scope="scope">
            <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.streamType == 'proxy'">拉流代理</el-tag>
                <el-tag size="medium" v-if="scope.row.streamType == 'push'">推流</el-tag>
            </div>
            </template>
        </el-table-column>
    </el-table>
    <el-pagination style="float: right;margin-top: 1rem;" @size-change="handleSizeChange" @current-change="currentChange" :current-page="currentPage" :page-size="count" :page-sizes="[10, 20, 30, 50]" layout="total, sizes, prev, pager, next" :total="total">
    </el-pagination>
</div>
</template>

<script>
export default {
    name: 'chooseChannelFoStream',
    props: {},
    computed: {
        // getPlayerShared: function () {
        //     return {
        //         sharedUrl: window.location.host + '/' + this.videoUrl,
        //         sharedIframe: '<iframe src="' + window.location.host + '/' + this.videoUrl + '"></iframe>',
        //         sharedRtmp: this.videoUrl
        //     };
        // }
    },
    props: ['platformId'],
    created() {
        this.initData();
    },
    data() {
        return {
            gbStreams: [],
            gbChoosechannel:{},
            searchSrt: "",
            channelType: "",
            online: "",
            choosed: "",
            currentPage: 0,
            count: 10,
            total: 0,
            eventEnanle: false

        };
    },
    watch:{
        platformId(newData, oldData){
            console.log(newData)
            this.initData()
            
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
        rowcheckedChanage: function (val, row) {
            console.log(val)
            console.log(row)
        },
        checkedChanage: function (val) {
            var that = this;
            if (!that.eventEnanle) {
                return;
            }
            var tabelData = JSON.parse(JSON.stringify(this.$refs.gbStreamsTable.data));
            console.log("checkedChanage")
            console.log(val)

            var newData = {};
            var addData = [];
            var delData = [];
            if (val.length > 0) {
                for (let i = 0; i < val.length; i++) {
                    const element = val[i];
                    var key = element.app + "_" + element.stream;
                    newData[key] = element;
                    if (!!!that.gbChoosechannel[key]){
                        addData.push(element)
                    }else{
                        delete that.gbChoosechannel[key]
                    }
                }
                 
                 var oldKeys = Object.keys(that.gbChoosechannel);
                if (oldKeys.length > 0) {
                    for (let i = 0; i < oldKeys.length; i++) {
                        const key = oldKeys[i];
                        delData.push(that.gbChoosechannel[key])
                    }
                }
                
            }else{
                var oldKeys = Object.keys(that.gbChoosechannel);
                if (oldKeys.length > 0) {
                    for (let i = 0; i < oldKeys.length; i++) {
                        const key = oldKeys[i];
                        delData.push(that.gbChoosechannel[key])
                    }
                }
            }

            that.gbChoosechannel = newData;
            if (Object.keys(addData).length >0) {
                console.log(addData)
                that.$axios({
                    method:"post",
                     url:"/api/gbStream/add",
                    data:{
                        platformId: that.platformId,
                        gbStreams:  addData,
                    }
                }).then((res)=>{
                    console.log("保存成功")
                }).catch(function (error) {
                    console.log(error);
                });
            }
            if (Object.keys(delData).length >0) {
                console.log(delData)
                 that.$axios({
                    method:"delete",
                    url:"/api/gbStream/del",
                    data:{
                        gbStreams:  delData,
                    }
                }).then((res)=>{
                    console.log("移除成功")
                }).catch(function (error) {
                    console.log(error);
                });

            }

        },
        shareAllCheckedChanage: function (val) {
            this.chooseChanage(null, val)
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
                    online: that.online,
                    choosed: that.choosed,
                    platformId: that.platformId,
                    channelType: that.channelType
                }
                })
                .then(function (res) {
                    that.total = res.data.total;
                    that.gbStreams = res.data.list;
                    that.gbChoosechannel = {};
                    // 防止出现表格错位
                    that.$nextTick(() => {
                        that.$refs.gbStreamsTable.doLayout();
                        // 默认选中
                        var chooseGBS = [];
                        for (let i = 0; i < res.data.list.length; i++) {
                            const row = res.data.list[i];
                            console.log(row.platformId)
                            if (row.platformId == that.platformId) {
                                that.$refs.gbStreamsTable.toggleRowSelection(row, true);
                                chooseGBS.push(row)
                                that.gbChoosechannel[row.app+ "_" + row.stream] = row;
                               
                            }
                        }
                         that.eventEnanle = true;
                        // that.checkedChanage(chooseGBS)
                    })
                    console.log(that.gbChoosechannel)
                })
                .catch(function (error) {
                    console.log(error);
                });

        },
        handleGBSelectionChange: function() {
            this.initData();
        },
    }
};
</script>

<style>

</style>
