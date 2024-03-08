<template>
	<div id="mediaServerManger" style="width: 100%">
    <div class="page-header">
      <div class="page-title">节点列表</div>
      <div class="page-header-btn">
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">添加节点</el-button>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="num" v-for="item in mediaServerList" :key="item.id">
        <el-card shadow="hover" :body-style="{ padding: '0px'}" class="server-card">
          <div class="card-img-zlm"></div>
          <div style="padding: 14px;text-align: left">
            <span style="font-size: 16px">{{item.id}}</span>
            <el-button v-if="!item.defaultServer" icon="el-icon-edit" style="padding: 0;float: right;" type="text" @click="edit(item)">编辑</el-button>
            <el-button v-if="item.defaultServer" icon="el-icon-edit" style="padding: 0;float: right;" type="text" @click="edit(item)">查看</el-button>
            <el-button v-if="!item.defaultServer" icon="el-icon-delete" style="margin-right: 10px;padding: 0;float: right;" type="text" @click="del(item)">移除</el-button>
            <div style="margin-top: 13px; line-height: 12px; ">
              <span style="font-size: 14px; color: #999; margin-top: 5px; ">{{item.ip}}</span>
              <span style="font-size: 14px; color: #999; margin-top: 5px; float: right;">{{item.createTime}}</span>
            </div>
          </div>
          <i v-if="item.status" class="iconfont icon-online server-card-status-online" title="在线"></i>
          <i v-if="!item.status" class="iconfont icon-online server-card-status-offline" title="离线"></i>
          <i v-if="item.defaultServer" class="server-card-default" >默认</i>
        </el-card>
      </el-col>
    </el-row>
    <mediaServerEdit ref="mediaServerEdit" ></mediaServerEdit>
	</div>
</template>

<script>
	import uiHeader from '../layout/UiHeader.vue'
  import MediaServer from './service/MediaServer'
  import mediaServerEdit from './dialog/MediaServerEdit'
	export default {
		name: 'mediaServerManger',
		components: {
			uiHeader,mediaServerEdit
		},
		data() {
			return {
        mediaServerObj : new MediaServer(),
        mediaServerList: [], //设备列表
        winHeight: window.innerHeight - 200,
        updateLooper: false,
        currentPage:1,
        count:15,
        num: this.getNumberByWidth(),
        total:0,
			};
		},
		computed: {

		},
		mounted() {
			this.initData();
			this.updateLooper = setInterval(this.initData, 2000);
		},
		destroyed() {
			clearTimeout(this.updateLooper);
		},
		methods: {
			initData: function() {
        this.getServerList()
			},
      currentChange: function(val){
        this.currentPage = val;
        this.getServerList();
      },
      handleSizeChange: function(val){
        this.count = val;
        this.getServerList();
      },
      getServerList: function(){
        this.mediaServerObj.getMediaServerList((data)=>{
          this.mediaServerList = data.data;
        })
      },
      add: function (){
        this.$refs.mediaServerEdit.openDialog(null, this.initData)
      },
      edit: function (row){
        this.$refs.mediaServerEdit.openDialog(row, this.initData)
      },
      del: function (row){
        this.$confirm('确认删除此节点？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.mediaServerObj.delete(row.id, (data)=>{
            if (data.code === 0) {
              this.$message({
                type: 'success',
                message: '删除成功!'
              });
            }
          })

        }).catch(() => {
        });

      },
      getNumberByWidth(){
        let candidateNums = [1, 2, 3, 4, 6, 8, 12, 24]
        let clientWidth = window.innerWidth - 30;
        let interval = 20;
        let itemWidth = 360;
        let num = (clientWidth + interval)/(itemWidth + interval)
        let result = Math.ceil(24/num);
        let resultVal = 24;
        for (let i = 0; i < candidateNums.length; i++) {
          let value = candidateNums[i]
          if (i + 1 >= candidateNums.length) {
            return  24;
          }
          if (value <= result && candidateNums[i + 1] > result ) {
            return  value;
          }
        }

        return resultVal;
      },
			dateFormat: function(/** timestamp=0 **/) {
				var ts = arguments[0] || 0;
				var t,y,m,d,h,i,s;
				t = ts ? new Date(ts*1000) : new Date();
				y = t.getFullYear();
				m = t.getMonth()+1;
				d = t.getDate();
				h = t.getHours();
				i = t.getMinutes();
				s = t.getSeconds();
				// 可根据需要在这里定义时间格式
				return y+'-'+(m<10?'0'+m:m)+'-'+(d<10?'0'+d:d)+' '+(h<10?'0'+h:h)+':'+(i<10?'0'+i:i)+':'+(s<10?'0'+s:s);
			}

		}
	};
</script>

<style>
  .server-card{
    position: relative;
    margin-bottom: 20px;
  }
  .card-img-zlm{
    width: 200px; height: 200px;
    background: url('~@static/images/zlm-logo.png') no-repeat center;
    background-position: center;
    background-size: contain;
    margin: 0 auto;
  }
  .server-card-status-online{
    position: absolute;
    right: 20px;
    top: 20px;
    color: #3caf36;
    font-size: 18px;
  }
  .server-card-status-offline{
    position: absolute;
    right: 20px;
    top: 20px;
    color: #808080;
    font-size: 18px;
  }
  .server-card-default{
    position: absolute;
    left: 20px;
    top: 20px;
    color: #808080;
    font-size: 18px;
  }
	.server-card:hover {
    border: 1px solid #adadad;
  }
</style>
