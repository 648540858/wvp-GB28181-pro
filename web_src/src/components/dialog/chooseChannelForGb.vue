<template>
<div id="chooseChannelForGb" >
   <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
        搜索: <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字" prefix-icon="el-icon-search" v-model="searchSrt" clearable> </el-input>

        通道类型: <el-select size="mini" @change="search" style="margin-right: 1rem; width:6rem" v-model="channelType" placeholder="请选择" default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="设备" value="false"></el-option>
            <el-option label="子目录" value="true"></el-option>
        </el-select>

        选择状态: <el-select size="mini"  style="margin-right: 1rem; width:6rem" v-model="choosed"  @change="search" placeholder="请选择" default-first-option>
        <el-option label="全部" value=""></el-option>
        <el-option label="已选择" value="true"></el-option>
        <el-option label="未选择" value="false"></el-option>
        </el-select>

        在线状态: <el-select size="mini" style="margin-right: 1rem; width:6rem" @change="search" v-model="online" placeholder="请选择" default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="在线" value="true"></el-option>
            <el-option label="离线" value="false"></el-option>
        </el-select>

        <el-checkbox @change="shareAllCheckedChanage">全部共享</el-checkbox>
    </div>
    <el-table ref="gbChannelsTable" :data="gbChannels" border style="width: 100%" @selection-change="checkedChanage" >
        <el-table-column type="selection" width="55" align="center" fixed > </el-table-column>
        <el-table-column prop="channelId" label="通道编号" width="210">
        </el-table-column>
        <el-table-column prop="name" label="通道名称" show-overflow-tooltip>
        </el-table-column>
        <el-table-column prop="deviceId" label="设备编号" width="210" >
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
    </el-table>
    <el-pagination style="float: right;margin-top: 1rem;" @size-change="handleSizeChange" @current-change="currentChange" :current-page="currentPage" :page-size="count" :page-sizes="[10, 20, 30, 50]" layout="total, sizes, prev, pager, next" :total="total">
    </el-pagination>
</div>
</template>

<script>
export default {
    name: 'chooseChannelForGb',
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
            gbChannels: [],
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
            var tabelData = JSON.parse(JSON.stringify(this.$refs.gbChannelsTable.data));
            console.log("checkedChanage")
            console.log(val)

            var newData = {};
            var addData = [];
            var delData = [];
            if (val.length > 0) {
                for (let i = 0; i < val.length; i++) {
                    const element = val[i];
                    var key = element.deviceId + "_" + element.channelId;
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
                that.$axios({
                    method:"post",
                    url:"/api/platform/update_channel_for_gb",
                    data:{
                        platformId:  that.platformId,
                        channelReduces: addData
                    }
                }).then((res)=>{
                    console.log("保存成功")
                }).catch(function (error) {
                    console.log(error);
                });
            }
            if (Object.keys(delData).length >0) {
                 that.$axios({
                    method:"delete",
                    url:"/api/platform/del_channel_for_gb",
                    data:{
                        platformId:  that.platformId,
                        channelReduces: delData
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

            this.$axios.get(`/api/platform/channel_list`, {
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
                    that.gbChannels = res.data.list;
                    that.gbChoosechannel = {};
                    // 防止出现表格错位
                    that.$nextTick(() => {
                        that.$refs.gbChannelsTable.doLayout();
                        // 默认选中
                        var chooseGBS = [];
                        for (let i = 0; i < res.data.list.length; i++) {
                            const row = res.data.list[i];
                            console.log(row.platformId)
                            if (row.platformId == that.platformId) {
                                that.$refs.gbChannelsTable.toggleRowSelection(row, true);
                                chooseGBS.push(row)
                                that.gbChoosechannel[row.deviceId+ "_" + row.channelId] = row;
                               
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
        search: function() {
            this.initData();
        },
        handleGBSelectionChange: function() {
            this.initData();
        },
    }
};
</script>

<style>

</style>
