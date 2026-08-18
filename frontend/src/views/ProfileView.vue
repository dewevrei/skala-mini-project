<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const nickname = ref(auth.user?.nickname ?? '')
const saving = ref(false)
const loggingOut = ref(false)

watch(() => auth.user?.nickname, (value) => { nickname.value = value ?? '' })

async function save() {
  const normalized = nickname.value.trim()
  if (!normalized || Array.from(normalized).length > 100 || /[\p{Cc}]/u.test(normalized)) {
    ElMessage.error('닉네임을 입력해 주세요.')
    return
  }
  saving.value = true
  try {
    await auth.updateNickname(normalized)
    ElMessage.success('닉네임을 변경했습니다.')
  } catch (error) {
    ElMessage.error(error.message)
    if (error.code === 'SESSION_SERVICE_UNAVAILABLE') {
      await router.replace({ path: '/login', query: { error: 'session-service-unavailable' } })
    }
  } finally {
    saving.value = false
  }
}

async function logout() {
  loggingOut.value = true
  try {
    await auth.logout()
    await router.replace('/login')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <header class="profile-header">
      <button class="wordmark" type="button" @click="router.push('/projects')">AI Kanban</button>
      <el-button :loading="loggingOut" @click="logout">로그아웃</el-button>
    </header>
    <main class="profile-content">
      <section class="profile-panel" aria-labelledby="profile-title">
        <div class="profile-panel__heading">
          <div>
            <h1 id="profile-title">회원정보</h1>
            <p>Google 계정 정보와 서비스에서 사용할 닉네임을 확인하세요.</p>
          </div>
        </div>
        <el-form label-position="top" @submit.prevent="save">
          <el-form-item label="이름">
            <el-input :model-value="auth.user?.name" disabled />
          </el-form-item>
          <el-form-item label="이메일">
            <el-input :model-value="auth.user?.email" disabled />
          </el-form-item>
          <el-form-item label="닉네임" required>
            <el-input v-model="nickname" autocomplete="nickname" />
          </el-form-item>
          <div class="profile-actions">
            <el-button @click="router.push('/projects')">취소</el-button>
            <el-button type="primary" native-type="submit" :loading="saving">저장</el-button>
          </div>
        </el-form>
      </section>
    </main>
  </div>
</template>
