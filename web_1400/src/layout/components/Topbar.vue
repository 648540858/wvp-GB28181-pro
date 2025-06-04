<template>
  <div class="top-nav">
    <div class="log">VIID视图库平台</div>
    <el-menu
      :active-text-color="variables.menuActiveText"
      :default-active="activeMenu"
      mode="horizontal"
      @select="handleSelect"
    >
      <div v-for="item in routes" :key="item.path" class="nav-item">
        <app-link :to="resolvePath(item)">
          <el-menu-item
            v-if="!item.hidden"
            :index="item.path"
          >
            <!--            <i class="el-icon-menu"></i>-->
            <span slot="title">{{ item.meta ? item.meta.title : item.children[0].meta.title }}</span>
          </el-menu-item>
        </app-link>
      </div>
    </el-menu>

    <div class="right-menu">
      <screenfull id="screenfull" class="right-menu-btn" />
      <span class="right-menu-btn" @click="handleLock">
        <i class="el-icon-lock" />
      </span>
      <!--      <span class="right-menu-btn">-->
      <!--        <el-badge is-dot class="badge">-->
      <!--          <i class="el-icon-bell"></i>-->
      <!--        </el-badge>-->
      <!--      </span>-->
      <el-dropdown>
        <span class="right-menu-btn">
          {{ user.nickname }}
          <i class="el-icon-arrow-down el-icon--right" />
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item @click.native="logout">
            <i style="padding-right: 8px" class="el-icon-turn-off" />退出系统
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import AppLink from './Sidebar/Link'
import variables from '@/styles/variables.scss'
import { isExternal } from '@/utils/validate'
import Screenfull from '@/components/Screenfull'
import { mapGetters } from 'vuex'

export default {
  name: 'Topbar',
  components: {
    AppLink,
    Screenfull
  },
  data() {
    return {
      routes: this.$store.state.permission.routes,
      dialogHandlePasswordVisible: false
    }
  },
  computed: {
    ...mapGetters([
      'user'
    ]),
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      // if set path, the sidebar will highlight the path you set
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      // 如果是首页，首页高亮
      if (path === '/dashboard') {
        return '/'
      }
      // 如果不是首页，高亮一级菜单
      const activeMenu = '/' + path.split('/')[1]
      return activeMenu
    },
    variables() {
      return variables
    },
    sidebar() {
      return this.$store.state.app.sidebar
    }
  },
  mounted() {
    this.initCurrentRoutes()
  },
  methods: {
    // 通过当前路径找到二级菜单对应项，存到store，用来渲染左侧菜单
    initCurrentRoutes() {
      const { path } = this.$route
      let route = this.routes.find(
        item => item.path === '/' + path.split('/')[1]
      )
      // 如果找不到这个路由，说明是首页
      if (!route) {
        route = this.routes.find(item => item.path === '/')
      }
      this.$store.commit('permission/SET_CURRENT_ROUTE', route)
      this.setSidebarHide(route)
    },
    // 判断该路由是否只有一个子项或者没有子项，如果是，则在一级菜单添加跳转路由
    isOnlyOneChild(item) {
      if (item.children && item.children.length === 1) {
        return true
      }
      return false
    },
    resolvePath(item) {
      // 如果是个完成的url直接返回
      if (isExternal(item.path)) {
        return item.path
      }
      // 如果是首页，就返回重定向路由
      if (item.path === '/') {
        const path = item.redirect
        return path
      }

      // 如果有子项，默认跳转第一个子项路由
      let path = ''
      /**
       * item 路由子项
       * parent 路由父项
       */
      const getDefaultPath = (item, parent) => {
        // 如果path是个外部链接（不建议），直接返回链接，存在个问题：如果是外部链接点击跳转后当前页内容还是上一个路由内容
        if (isExternal(item.path)) {
          path = item.path
          return
        }
        // 第一次需要父项路由拼接，所以只是第一个传parent
        if (parent) {
          path += (parent.path + '/' + item.path)
        } else {
          path += ('/' + item.path)
        }
        // 如果还有子项，继续递归
        if (item.children) {
          getDefaultPath(item.children[0])
        }
      }

      if (item.children) {
        getDefaultPath(item.children[0], item)
        return path
      }

      return item.path
    },
    handleSelect(key, keyPath) {
      // 把选中路由的子路由保存store
      const route = this.routes.find(item => item.path === key)
      this.$store.commit('permission/SET_CURRENT_ROUTE', route)
      this.setSidebarHide(route)
    },
    // 设置侧边栏的显示和隐藏
    setSidebarHide(route) {
      if (!route.children || route.children.length === 1) {
        this.$store.dispatch('app/toggleSideBarHide', true)
      } else {
        this.$store.dispatch('app/toggleSideBarHide', false)
      }
    },
    async logout() {
      await this.$store.dispatch('user/logout')
      // this.$router.push(`/login?redirect=${this.$route.fullPath}`)
      this.$router.push('/login')
    },
    handlePassword() {
      this.dialogHandlePasswordVisible = true
    },
    handleLock() {
      this.$prompt('请输入锁屏密码', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        showClose: false,
        closeOnClickModal: false,
        inputPattern: /^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{4,}$/,
        inputErrorMessage: '锁屏密码长度要大于4位，由数字和字母组成'
      }).then(({ value }) => {
        this.$store.commit('user/SET_LOCK', value)
        setTimeout(() => {
          this.$router.push({ path: '/lock' })
        }, 100)
      }).catch(() => {
      })
    }
  }
}
</script>
