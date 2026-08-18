import { createRouter, createWebHistory } from 'vue-router'

const PlaceholderView = () => import('../views/PlaceholderView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/projects' },
    { path: '/login', name: 'login', component: PlaceholderView, meta: { title: '로그인', public: true } },
    { path: '/projects', name: 'projects', component: PlaceholderView, meta: { title: '프로젝트' } },
    { path: '/projects/:projectId/board', name: 'board', component: PlaceholderView, meta: { title: '보드' } },
    { path: '/projects/:projectId/items', name: 'items', component: PlaceholderView, meta: { title: '작업 목록' } },
    { path: '/profile', name: 'profile', component: PlaceholderView, meta: { title: '회원정보' } },
  ],
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? 'AI Kanban'} · AI Kanban`
})

export default router
