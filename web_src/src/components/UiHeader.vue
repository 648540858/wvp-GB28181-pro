<template>
	<div id="UiHeader">
		<el-menu router :default-active="this.$route.path" background-color="#545c64" text-color="#fff" active-text-color="#ffd04b" mode="horizontal">
            <el-menu-item index="/">控制台</el-menu-item>
            <el-menu-item index="/videoList">设备列表</el-menu-item>
            <!-- <el-menu-item index="/videoReplay">录像回看</el-menu-item> -->
            <el-switch v-model="alarmNotify"  active-text="报警信息推送" style="display: block float: right" @change="sseControl"></el-switch>
            <el-menu-item style="float: right;" @click="loginout">退出</el-menu-item>
        </el-menu>
	</div>
</template>

<script>
export default {
    name: "UiHeader",
    components: { Notification },
    data() {
        return {
            alarmNotify: true,
            sseSource: null,
        };
    },
    methods:{

  	    loginout(){
            // 删除cookie，回到登录页面
            this.$cookies.remove("session");
            this.$router.push('/login');
            this.sseSource.close();
        },
        beforeunloadHandler() {
            this.sseSource.close();
        },
        sseControl() {
            let that = this;
            if (this.alarmNotify) {
                console.log("申请SSE推送API调用，浏览器ID: " + this.$browserId);
                this.sseSource = new EventSource('/api/emit?browserId=' + this.$browserId); 
        	    this.sseSource.addEventListener('message', function(evt) {
                    that.$notify({
                        title: '收到报警信息',
                        dangerouslyUseHTMLString: true,
                        message: evt.data,
                        type: 'warning'
                    });
	                console.log("收到信息：" + evt.data);
        	    });
	            this.sseSource.addEventListener('open', function(e) {
        	        console.log("SSE连接打开.");
	            }, false);
        	    this.sseSource.addEventListener('error', function(e) {
	                if (e.target.readyState == EventSource.CLOSED) {
	                    console.log("SSE连接关闭");
        	        } else {
	                    console.log(e.target.readyState);
        	        }
	            }, false);
            } else {
                this.sseSource.removeEventListener('open', null);
                this.sseSource.removeEventListener('message', null);
                this.sseSource.removeEventListener('error', null);
                this.sseSource.close();
            } 
        }
    },
    mounted() {
        window.addEventListener('beforeunload', e => this.beforeunloadHandler(e))
        // window.addEventListener('unload', e => this.unloadHandler(e))
        this.sseControl();
    },
    destroyed() {
        window.removeEventListener('beforeunload', e => this.beforeunloadHandler(e))
        // window.removeEventListener('unload', e => this.unloadHandler(e))
    },
 }

</script>
