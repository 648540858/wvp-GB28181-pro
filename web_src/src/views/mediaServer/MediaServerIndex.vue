<template>
  <div>
    <component @goEditPage="goEditPage" @goAddPage="goAddPage" @goDetailPage="goDetailPage" @goBack="goBack" :record="record" :is="currentComponent"></component>
  </div>
</template>

<script>
import MediaServerList from "@/views/mediaServer/MediaServerList";
import MediaServerEdit from "@/views/mediaServer/MediaServerEdit";
import MediaServerStepForm from "@/views/mediaServer/stepForm/MediaServerStepForm";
import MediaServerDetail from "@/views/mediaServer/MediaServerDetail";

export default {
  components: {
    MediaServerList,
    MediaServerEdit,
    MediaServerStepForm,
    MediaServerDetail
  },
  data() {
    return {
      currentComponent: 'MediaServerList',
      record: {}
    }
  },
  methods: {
    goAddPage(){
      this.record = {}
      this.currentComponent = 'MediaServerStepForm'
    },
    goEditPage(record){
      this.record = record || {}
      this.currentComponent = 'MediaServerEdit'
    },
    goDetailPage(record){
      this.record = record || {}
      this.currentComponent = 'MediaServerDetail'
    },
    goBack() {
      this.currentComponent = {}
      this.currentComponent = 'MediaServerList'
    }
  }
}

</script>

<style scoped>

</style>