//loading效果组件

<template>
<div class="loadEffect" :style="{marginTop: marginTop? marginTop : '50%'}">
    <span class="ld-span"></span>
    <span class="ld-span"></span>
    <span class="ld-span"></span>
    <span class="ld-span"></span>
    <span class="ld-span"></span>
    <span class="ld-span"></span>
    <span class="ld-span"></span>
    <span class="ld-span"></span>
</div>
</template>

<script>
export default {
  name: 'Loading',
  props: ["marginTop"]

}
</script>

<style scoped>
.loadEffect{
    width: 100px;
    height: 100px;
    position: relative;
    margin: 0 auto;
    position: relative;
    top:-50px;
    margin-top:50%;
    transform: scale(.5)
}
.loadEffect .ld-span{
    display: inline-block;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #67e7d5;
    position: absolute;
    -webkit-animation: load 1.04s ease infinite;
}
@-webkit-keyframes load{
    0%{
    -webkit-transform: scale(1.2);
    opacity: 1;
    }
    100%{
    -webkit-transform: scale(.3);
    opacity: 0.5;
    }
}
.loadEffect .ld-span:nth-child(1){
    left: 0;
    top: 50%;
    margin-top:-10px;
    -webkit-animation-delay:0.13s;
}
.loadEffect .ld-span:nth-child(2){
    left: 14px;
    top: 14px;
    -webkit-animation-delay:0.26s;
}
.loadEffect .ld-span:nth-child(3){
    left: 50%;
    top: 0;
    margin-left: -10px;
    -webkit-animation-delay:0.39s;
}
.loadEffect .ld-span:nth-child(4){
    top: 14px;
    right:14px;
    -webkit-animation-delay:0.52s;
}
.loadEffect .ld-span:nth-child(5){
    right: 0;
    top: 50%;
    margin-top:-10px;
    -webkit-animation-delay:0.65s;
}
.loadEffect .ld-span:nth-child(6){
    right: 14px;
    bottom:14px;
    -webkit-animation-delay:0.78s;
}
.loadEffect .ld-span:nth-child(7){
    bottom: 0;
    left: 50%;
    margin-left: -10px;
    -webkit-animation-delay:0.91s;
}
.loadEffect .ld-span:nth-child(8){
    bottom: 14px;
    left: 14px;
    -webkit-animation-delay:1.04s;
}
</style>
