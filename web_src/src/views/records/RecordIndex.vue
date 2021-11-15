<template>
  <div>
    <component @goDetailPage="handleDetail" @goBack="handleGoBack" :record="record" :is="currentComponet"></component>
  </div>
</template>

<script>
// 动态切换组件
import RecordList from '@/views/records/RecordList'
import RecordDetail from '@/views/records/RecordDetail'

export default {
  name: 'RecordListWrapper',
  components: {
    RecordList,
    RecordDetail
  },
  data() {
    return {
      currentComponet: 'RecordList',
      record: ''
    }
  },
  created() {

  },
  methods: {
    handleDetail(record) {
      this.record = record || ''
      this.currentComponet = 'RecordDetail'
    },
    handleGoBack() {
      this.record = ''
      this.currentComponet = 'RecordList'
    }
  },
  watch: {
    '$route.path'() {
      this.record = ''
      this.currentComponet = 'RecordList'
    }
  }
}
</script>
