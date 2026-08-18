<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { backendLoginUrl } from '../api/client'

const route = useRoute()
const errorMessage = computed(() => {
  if (route.query.error === 'session-service-unavailable') {
    return '로그인 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.'
  }
  if (route.query.error === 'oauth') {
    return 'Google 로그인에 실패했습니다. 다시 시도해 주세요.'
  }
  return ''
})
</script>

<template>
  <main class="auth-page">
    <section class="auth-card" aria-labelledby="login-title">
      <div class="brand-mark" aria-hidden="true">AI</div>
      <h1 id="login-title">AI Kanban</h1>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
      <el-button class="google-login" type="primary" size="large" tag="a" :href="backendLoginUrl">
        Google로 계속하기
      </el-button>
    </section>
  </main>
</template>
