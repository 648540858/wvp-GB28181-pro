<template>
<div id="chooseChannelForGb" >
   <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
        搜索: <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字" prefix-icon="el-icon-search" v-model="searchSrt" clearable> </el-input>

        通道类型: <el-select size="mini" @change="search" style="margin-right: 1rem;" v-model="channelType" placeholder="请选择" default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="设备" value="false"></el-option>
            <el-option label="子目录" value="true"></el-option>
        </el-select>
        在线状态: <el-select size="mini" style="margin-right: 1rem;" @change="search" v-model="online" placeholder="请选择" default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="在线" value="true"></el-option>
            <el-option label="离线" value="false"></el-option>
        </el-select>
    </div>
    <el-table ref="gbChannelsTable" :data="gbChannels" border style="width: 100%" @selection-change="chooseChanage"  >
        <el-table-column type="selection" width="55" align="center" fixed> </el-table-column>
        <el-table-column prop="channelId" label="通道编号" width="210">
        </el-table-column>
        <el-table-column prop="name" label="通道名称">
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
    <el-pagination style="float: right;margin-top: 1rem;" @size-change="handleSizeChange" @current-change="currentChange" :current-page="currentPage" :page-size="count" :page-sizes="[15, 20, 30, 50]" layout="total, sizes, prev, pager, next" :total="total">
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
    props: ['chooseChanage'],
    created() {
        this.initData();
    },
    data() {
        return {
            gbChannels: [],
            searchSrt: "",
            channelType: "",
            online: "",
            currentPage: parseInt(this.$route.params.page),
            count: parseInt(this.$route.params.count),
            total: 0
            
        };
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
            this.initData();

        },
        getChannelList: function () {
            let that = this;

            this.$axios.get(`/api/platforms/channelList`, {
                    params: {
                        page: that.currentPage - 1,
                        count: that.count,
                        query: that.searchSrt,
                        online: that.online,
                        channelType: that.channelType
                    }
                })
                .then(function (res) {
                    console.log(res);
                    that.total = res.data.total;
                    that.gbChannels = res.data.list;
                    // 防止出现表格错位
                    that.$nextTick(() => {
                        that.$refs.gbChannelsTable.doLayout();
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
    }
};
</script>

<style>

</style>
