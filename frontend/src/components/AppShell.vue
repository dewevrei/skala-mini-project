<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

defineProps({ compact: Boolean })

const auth = useAuthStore()
const router = useRouter()
const loggingOut = ref(false)

async function logout() {
  loggingOut.value = true
  try {
    await auth.logout()
    await router.replace('/login')
  } catch (error) {
    ElMessage.error(error.message)
    if (error.code === 'SESSION_SERVICE_UNAVAILABLE') {
      await router.replace({ path: '/login', query: { error: 'session-service-unavailable' } })
    }
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--compact': compact }">
    <header class="app-header">
      <button class="app-brand" type="button" @click="router.push('/projects')">
        <span class="app-brand__mark" aria-hidden="true">AI</span>
        <span>AI Kanban</span>
      </button>
      <nav class="account-nav" aria-label="계정 메뉴">
        <el-button text @click="router.push('/profile')">{{ auth.user?.nickname ?? '회원정보' }}</el-button>
        <el-button :loading="loggingOut" @click="logout">로그아웃</el-button>
      </nav>
    </header>
    <slot />
  </div>
</template>
