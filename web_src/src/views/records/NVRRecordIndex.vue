<template>
  <div>
    <component @goDetailPage="handleDetail" @goBack="handleGoBack" :record="record" :is="currentComponet"></component>
  </div>
</template>

<script>
// 动态切换组件
import NVRRecords from "@/views/records/NVRRecords";
import NVRRecordDetail from "@/views/records/NVRRecordDetail";

export default {
  name: 'NVRRecordListWrapper',
  components: {
    NVRRecords,
    NVRRecordDetail
  },
  data() {
    return {
      currentComponet: 'NVRRecords',
      record: ''
    }
  },
  created() {

  },
  methods: {
    handleDetail(record) {
      this.record = record || ''
      this.currentComponet = 'NVRRecordDetail'
    },
    handleGoBack() {
      this.record = ''
      this.currentComponet = 'NVRRecords'
    }
  },
  watch: {
    '$route.path'() {
      this.record = ''
      this.currentComponet = 'NVRRecords'
    }
  }
}
</script>
