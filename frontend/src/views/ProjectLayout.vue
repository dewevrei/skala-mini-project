<script setup>
import { onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppShell from '../components/AppShell.vue'
import { useProjectStore } from '../stores/projects'
import { useProjectRefresh } from '../composables/useProjectRefresh'

const route = useRoute()
const router = useRouter()
const projects = useProjectStore()

async function refreshProject() {
  try {
    await projects.fetchProject(route.params.projectId)
  } catch (error) {
    ElMessage.error(error.message)
    if (error.status === 404) await router.replace('/projects')
  }
}

onMounted(refreshProject)
watch(() => route.params.projectId, refreshProject)
useProjectRefresh(refreshProject)
</script>

<template>
  <AppShell compact>
    <section class="project-heading">
      <div>
        <h1>{{ projects.currentProject?.name ?? '프로젝트' }}</h1>
        <p v-if="projects.currentProject?.description">{{ projects.currentProject.description }}</p>
      </div>
    </section>
    <nav class="project-tabs" aria-label="프로젝트 보기">
      <RouterLink :to="`/projects/${route.params.projectId}/board`">Board</RouterLink>
      <RouterLink :to="`/projects/${route.params.projectId}/items`">Items</RouterLink>
    </nav>
    <main class="project-view-content">
      <RouterView :project="projects.currentProject" :refresh-project="refreshProject" />
    </main>
  </AppShell>
</template>
