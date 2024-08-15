<template>
<div id="chooseChannel" >

    <el-dialog title="通道共享" v-loading="loading" v-if="showDialog" top="2rem" width="80%" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()">
      <el-row>
        <el-col :span="24">
          <el-tabs v-model="catalogTabActiveName" >
            <el-tab-pane label="共享通道" name="addShare">
              <el-container>
                <el-main v-bind:style="{backgroundColor: '#FFF', maxHeight:  winHeight + 'px'}">
                  <shareChannelAdd ref="shareChannelAdd" :platformId="platformId"></shareChannelAdd>
                </el-main>
              </el-container>
            </el-tab-pane>
            <el-tab-pane label="共享通道信息展示" name="showShare">

            </el-tab-pane>
            <el-tab-pane label="共享通道信息自定义" name="customShare">

            </el-tab-pane>
          </el-tabs>

        </el-col>

      </el-row>

    </el-dialog>
</div>
</template>

<script>
import shareChannelAdd from "./shareChannelAdd.vue";

export default {
    name: 'chooseChannel',
    props: {},
    components: {shareChannelAdd},
    computed: {},
    data() {
        return {
            loading: false,
            tabActiveName: "gbChannel",
            catalogTabActiveName: "addShare",
            platformId: "",
            showDialog: false,
            chooseData: {},
            winHeight: window.innerHeight - 250,

        };
    },
    methods: {
        openDialog(platformId, closeCallback) {
            this.platformId = platformId
            this.showDialog = true
            this.closeCallback = closeCallback
        },
        tabClick (tab, event){

        },
        close: function() {
          this.closeCallback()
        },
        search: function() {

        },
        save: function() {

            this.$axios({
                method:"post",
                url:"/api/platform/update_channel_for_gb",
                data:{
                    platformId:  this.platformId,
                    channelReduces:  this.chooseData
                }
            }).then((res)=>{
              if (res.data.code === 0) {
                this.$message({
                        showClose: true,
                        message: '保存成功,',
                        type: 'success'
                    });
                }
            }).catch(function (error) {
                console.log(error);
            });

        },
        catalogIdChange: function (id, name) {
            this.catalogId = id;
            this.catalogName = name;
        },
    }
};
</script>

<style>

</style>
