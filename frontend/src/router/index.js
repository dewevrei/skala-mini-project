import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const PlaceholderView = () => import('../views/PlaceholderView.vue')
const LoginView = () => import('../views/LoginView.vue')
const ProfileView = () => import('../views/ProfileView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    { path: '/login', name: 'login', component: LoginView, meta: { title: '로그인', public: true } },
    { path: '/projects', name: 'projects', component: PlaceholderView, meta: { title: '프로젝트' } },
    { path: '/projects/:projectId/board', name: 'board', component: PlaceholderView, meta: { title: '보드' } },
    { path: '/projects/:projectId/items', name: 'items', component: PlaceholderView, meta: { title: '작업 목록' } },
    { path: '/profile', name: 'profile', component: ProfileView, meta: { title: '회원정보' } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    try {
      await auth.initialize(auth.authenticated)
    } catch (error) {
      if (error.code === 'SESSION_SERVICE_UNAVAILABLE' && to.query.error !== 'session-service-unavailable') {
        return { name: 'login', query: { ...to.query, error: 'session-service-unavailable' } }
      }
      return true
    }
    if (to.name === 'login' && auth.authenticated) return { name: 'projects' }
    return true
  }
  try {
    await auth.initialize()
  } catch (error) {
    if (error.code === 'SESSION_SERVICE_UNAVAILABLE') {
      return { name: 'login', query: { error: 'session-service-unavailable' } }
    }
    return { name: 'login' }
  }
  return auth.authenticated ? true : { name: 'login', query: { redirect: to.fullPath } }
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? 'AI Kanban'} · AI Kanban`
})

export default router
