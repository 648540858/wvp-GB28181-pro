<template>
<div id="chooseChannel" v-loading="isLoging">

    <el-dialog title="选择通道" top="0" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()">
        <el-container>
            <el-main style="background-color: #FFF;">
            <el-tabs v-model="tabActiveName" >

                    <el-tab-pane label="国标通道" name="gbChannel">
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
                        <el-table :data="gbChannels" border style="width: 100%" @selection-change="handleGBSelectionChange"  >
                            <el-table-column type="selection" width="55" align="center" fixed> </el-table-column>
                            <el-table-column prop="devicelId" label="设备编号" width="210" >
                            </el-table-column>
                            <el-table-column prop="channelId" label="通道编号" width="210">
                            </el-table-column>
                            <el-table-column prop="channelName" label="通道名称">
                            </el-table-column>
                            <el-table-column label="地址" width="180" align="center">
                                <template slot-scope="scope">
                                    <div slot="reference" class="name-wrapper">
                                        <el-tag size="medium">{{ scope.row.hostAddress }}</el-tag>
                                    </div>
                                </template>
                            </el-table-column>
                            <el-table-column prop="manufacturer" label="厂家" align="center">
                            </el-table-column>
                        </el-table>
                    </el-tab-pane>
                    <el-tab-pane label="直播流通道" name="streamchannel">
                        <!-- TODO -->
                    </el-tab-pane>
                </el-tabs>

            </el-main>
    </el-container>
    </el-dialog>
</div>
</template>

<script>
// import player from './player.vue'
export default {
    name: 'chooseChannel',
    props: {},
    components: {
        // player,
    },
    computed: {
        // getPlayerShared: function () {
        //     return {
        //         sharedUrl: window.location.host + '/' + this.videoUrl,
        //         sharedIframe: '<iframe src="' + window.location.host + '/' + this.videoUrl + '"></iframe>',
        //         sharedRtmp: this.videoUrl
        //     };
        // }
    },
    created() {},
    data() {
        return {
            isLoging: false,
            tabActiveName: "gbChannel",
            isLoging: false,
            showDialog: false,
            chooseChanageCallback: null,
            closeCallback: null,
            gbChannels: [
                {
                    devicelId: "34000000000200001",
                    channelId: "34000000000200001",
                    channelName: "test",
                    hostAddress: "192.168.1.1:5060",
                    manufacturer: "大华"
                },{
                    devicelId: "34000000000200001",
                    channelId: "34000000000200001",
                    channelName: "test",
                    hostAddress: "192.168.1.1:5060",
                    manufacturer: "大华"
                }
            ],
            searchSrt: "",
            channelType: "",
            online: "",
            
        };
    },
    methods: {
        
        openDialog: function (chooseChanageCallback, closeCallback) {
            this.showDialog = true
            this.closeCallback = closeCallback
            this.chooseChanageCallback = chooseChanageCallback
        },
        close: function() {

        },
        search: function() {

        },
        handleGBSelectionChange: function() {

        }
    }
};
</script>

<style>

</style>
