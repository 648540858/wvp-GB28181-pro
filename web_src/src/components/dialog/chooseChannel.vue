<template>
<div id="chooseChannel" v-loading="isLoging">

    <el-dialog title="选择通道" v-if="showDialog" top="2rem" width="70%" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()">
        <el-tabs v-model="tabActiveName" >
            <el-tab-pane label="国标通道" name="gbChannel">
                <el-container>
                    <el-main style="background-color: #FFF;">
                     <chooseChannelForGb :platformId=platformId ></chooseChannelForGb>
                    </el-main>
            </el-container>


            </el-tab-pane>
            <el-tab-pane label="直播流通道" name="streamchannel">
                <el-container>
                    <el-main style="background-color: #FFF;">
                     <chooseChannelFoStream :platformId=platformId ></chooseChannelFoStream>
                    </el-main>
            </el-container>
            </el-tab-pane>
        </el-tabs>
    </el-dialog>
</div>
</template>

<script>
import chooseChannelForGb from '../dialog/chooseChannelForGb.vue'
import chooseChannelFoStream from '../dialog/chooseChannelForStream.vue'
export default {
    name: 'chooseChannel',
    props: {},
    components: {
        chooseChannelForGb,
        chooseChannelFoStream,
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
    data() {
        return {
            isLoging: false,
            tabActiveName: "gbChannel",
            platformId: "",
            isLoging: false,
            showDialog: false,
            chooseData: {}

        };
    },
    methods: {
        openDialog: function (platformId,  closeCallback) {
            console.log(platformId)
            this.platformId = platformId
            this.showDialog = true
            this.closeCallback = closeCallback
        },
        close: function() {
          this.closeCallback()
        },
        search: function() {

        },
        save: function() {
            var that = this;

            this.$axios({
                method:"post",
                url:"/api/platform/update_channel_for_gb",
                data:{
                    platformId:  that.platformId,
                    channelReduces:  that.chooseData
                }
            }).then((res)=>{
                if (res.data == true) {
                    that.$message({
                        showClose: true,
                        message: '保存成功,',
                        type: 'success'
                    });
                }
            }).catch(function (error) {
                console.log(error);
            });
        }
    }
};
</script>

<style>

</style>
