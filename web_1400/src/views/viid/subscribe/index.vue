<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <page-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <page-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import PageList from './SubscribeList'
import PageAdd from './SubscribeAdd'

export default {
  name: 'VIIDSubscribe',
  components: { PageList, PageAdd },
  data() {
    return {
      options: {
        data: {},
        showList: true,
        showAdd: false,
        showEdit: false,
        showDetail: false
      }
    }
  },
  methods: {
    showCard(data) {
      Object.assign(this.options, data)
    }
  }
}
</script>

<style lang="scss" scoped></style>
