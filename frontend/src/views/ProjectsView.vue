<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppShell from '../components/AppShell.vue'
import ProjectFormModal from '../components/ProjectFormModal.vue'
import { useProjectStore } from '../stores/projects'

const projects = useProjectStore()
const router = useRouter()
const modalOpen = ref(false)
const editingProject = ref(null)

onMounted(load)

async function load() {
  try {
    await projects.fetchProjects()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function createProject() {
  editingProject.value = null
  modalOpen.value = true
}

function editProject(project) {
  editingProject.value = project
  modalOpen.value = true
}
</script>

<template>
  <AppShell>
    <main class="projects-page">
      <div class="projects-toolbar">
        <div>
          <h1>프로젝트</h1>
        </div>
        <el-button class="new-project-button" type="primary" size="large" @click="createProject">New project</el-button>
      </div>

      <section class="project-list" aria-label="프로젝트 목록" v-loading="projects.loading">
        <div v-if="!projects.loading && projects.projects.length === 0" class="project-list__empty">
          <h2>첫 번째 프로젝트를 만들어보세요.</h2>
        </div>
        <article
          v-for="project in projects.projects"
          :key="project.id"
          class="project-row"
        >
          <div class="project-row__content">
            <h2>
              <button class="project-row__title-link" type="button" @click="router.push(`/projects/${project.id}/board`)">
                {{ project.name }}
              </button>
            </h2>
            <p v-if="project.description">{{ project.description }}</p>
          </div>
          <el-button text circle aria-label="프로젝트 수정" @click="editProject(project)">•••</el-button>
        </article>
      </section>
    </main>
    <ProjectFormModal v-model="modalOpen" :project="editingProject" />
  </AppShell>
</template>
