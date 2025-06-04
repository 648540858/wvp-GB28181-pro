<template>
  <div class="app-container">
    <transition name="el-zoom-in-center">
      <page-list v-if="options.showList" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <page-add v-if="options.showAdd" :data="options.data" @showCard="showCard" />
    </transition>
    <transition name="el-zoom-in-top">
      <page-edit v-if="options.showEdit" :data="options.data" @showCard="showCard" />
    </transition>
  </div>
</template>

<script>
import PageList from './LaneList'
import PageAdd from './LaneAdd'
import PageEdit from './LaneEdit'

export default {
  name: 'VIIDLane',
  components: { PageList, PageAdd, PageEdit },
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
