<template>
	<div id="UiHeader">
		<el-menu router :default-active="this.$route.path" background-color="#545c64" text-color="#fff" active-text-color="#ffd04b" mode="horizontal">
            <el-menu-item index="/">控制台</el-menu-item>
            <el-menu-item index="/videoList">设备列表</el-menu-item>
            <!-- <el-menu-item index="/videoReplay">录像回看</el-menu-item> -->
            <!-- <el-menu-item index="4">级联设置</el-menu-item> -->
            <el-menu-item style="float: right;" @click="loginout">退出</el-menu-item>
        </el-menu>
	</div>
</template>

<script>
export default {
    name: "UiHeader",
    methods:{

  	    loginout(){
            // 删除cookie，回到登录页面
            this.$cookies.remove("session");
            this.$router.push('/login');
        },
    }
}

</script>
