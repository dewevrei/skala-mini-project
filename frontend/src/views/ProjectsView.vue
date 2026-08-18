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
          <p>최근에 만든 프로젝트부터 표시됩니다.</p>
        </div>
        <el-button class="new-project-button" type="primary" size="large" @click="createProject">New project</el-button>
      </div>

      <section class="project-list" aria-label="프로젝트 목록" v-loading="projects.loading">
        <div v-if="!projects.loading && projects.projects.length === 0" class="project-list__empty">
          <h2>아직 프로젝트가 없습니다.</h2>
          <p>새 프로젝트를 만들면 Todo, In Progress, Done Column이 함께 준비됩니다.</p>
          <el-button type="primary" @click="createProject">첫 프로젝트 만들기</el-button>
        </div>
        <article
          v-for="project in projects.projects"
          :key="project.id"
          class="project-row"
        >
          <div class="project-row__icon" aria-hidden="true">▦</div>
          <div class="project-row__content">
            <h2>
              <button class="project-row__title-link" type="button" @click="router.push(`/projects/${project.id}/board`)">
                {{ project.name }}
              </button>
            </h2>
            <p v-if="project.description">{{ project.description }}</p>
            <p v-else class="project-row__muted">설명이 없습니다.</p>
          </div>
          <el-button text circle aria-label="프로젝트 수정" @click="editProject(project)">•••</el-button>
        </article>
      </section>
    </main>
    <ProjectFormModal v-model="modalOpen" :project="editingProject" />
  </AppShell>
</template>
