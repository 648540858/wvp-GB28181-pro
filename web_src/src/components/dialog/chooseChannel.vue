<template>
<div id="chooseChannel" v-loading="isLoging">

    <el-dialog title="选择通道" v-if="showDialog" top="2rem" width="90%" :close-on-click-modal="false" :visible.sync="showDialog" :destroy-on-close="true" @close="close()">
      <el-row>
        <el-col :span="10">
          <el-tabs v-model="catalogTabActiveName" >
            <el-tab-pane label="目录结构" name="catalog">
              <el-container>
                <el-main v-bind:style="{backgroundColor: '#FFF', maxHeight:  winHeight + 'px'}">
                  <chooseChannelForCatalog ref="chooseChannelForCatalog" :platformId=platformId :platformName=platformName :defaultCatalogId=defaultCatalogId :catalogIdChange="catalogIdChange"></chooseChannelForCatalog>
                </el-main>
              </el-container>
            </el-tab-pane>
          </el-tabs>

        </el-col>
        <el-col :span="14">
          <el-tabs v-model="tabActiveName" @tab-click="tabClick">
            <el-tab-pane label="国标通道" name="gbChannel">
              <el-container>
                <el-main style="background-color: #FFF;">
                  <chooseChannelForGb ref="chooseChannelForGb" :platformId=platformId :updateChoosedCallback="updateChooseChannelCallback"></chooseChannelForGb>
                </el-main>
              </el-container>
            </el-tab-pane>
            <el-tab-pane label="直播流通道" name="streamchannel">
              <el-container>
                <el-main style="background-color: #FFF;">
                  <chooseChannelFoStream ref="chooseChannelFoStream" :platformId=platformId :updateChoosedCallback="updateChooseChannelCallback"></chooseChannelFoStream>
                </el-main>
              </el-container>
            </el-tab-pane>
          </el-tabs>
        </el-col>
      </el-row>

    </el-dialog>
</div>
</template>

<script>
import chooseChannelForGb from '../dialog/chooseChannelForGb.vue'
import chooseChannelFoStream from '../dialog/chooseChannelForStream.vue'
import chooseChannelForCatalog from '../dialog/chooseChannelForCatalog.vue'
export default {
    name: 'chooseChannel',
    props: {},
    components: {
        chooseChannelForGb,
        chooseChannelFoStream,
        chooseChannelForCatalog,
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
            catalogTabActiveName: "catalog",
            platformId: "",
            catalogId: "",
            platformName: "",
            defaultCatalogId: "",
            showDialog: false,
            chooseData: {},
            winHeight: window.innerHeight - 250,

        };
    },
    methods: {
        openDialog(platformId, platformName, defaultCatalogId, closeCallback) {
            this.platformId = platformId
            this.platformName = platformName
            this.defaultCatalogId = defaultCatalogId
            this.showDialog = true
            this.closeCallback = closeCallback
        },
        tabClick (tab, event){
          console.log(tab.label)
          if (tab.label === "gbChannel") {
            this.$refs.chooseChannelForGb.catalogIdChange(this.catalogId);
            this.$refs.chooseChannelForGb.initData();
          }else {
            this.$refs.chooseChannelFoStream.catalogIdChange(this.catalogId);
            this.$refs.chooseChannelFoStream.initData();
          }

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
        },
        catalogIdChange: function (id) {
            console.log("中间模块收到： " + id)
            this.catalogId = id;
            if (this.tabActiveName === "gbChannel") {
              this.$refs.chooseChannelForGb.catalogIdChange(id);
            }else {
              this.$refs.chooseChannelFoStream.catalogIdChange(id);
            }
        },
        updateChooseChannelCallback (id){
          console.log("中间模块收到选择通道变化： " + id)
          this.$refs.chooseChannelForCatalog.refreshCatalogById(id)
        }
    }
};
</script>

<style>

</style>
