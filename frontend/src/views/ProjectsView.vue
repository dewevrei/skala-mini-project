<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppShell from '../components/AppShell.vue'
import ProjectFormModal from '../components/ProjectFormModal.vue'
import { useProjectStore } from '../stores/projects'

const projects = useProjectStore()
const router = useRouter()
const modalOpen = ref(false)
const editingProject = ref(null)
const deletingId = ref(null)

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

async function deleteProject(project) {
  try {
    await ElMessageBox.confirm(
      `“${project.name}” 프로젝트와 모든 Column 및 Task가 완전히 삭제됩니다. 이 작업은 되돌릴 수 없습니다.`,
      '프로젝트 삭제',
      { confirmButtonText: '완전히 삭제', cancelButtonText: '취소', type: 'warning' },
    )
    deletingId.value = project.id
    const response = await projects.deleteProject(project.id)
    ElMessage.success(response.message)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message)
  } finally {
    deletingId.value = null
  }
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
          tabindex="0"
          @click="router.push(`/projects/${project.id}/board`)"
          @keydown.enter="router.push(`/projects/${project.id}/board`)"
        >
          <div class="project-row__icon" aria-hidden="true">▦</div>
          <div class="project-row__content">
            <h2>{{ project.name }}</h2>
            <p v-if="project.description">{{ project.description }}</p>
            <p v-else class="project-row__muted">설명이 없습니다.</p>
          </div>
          <el-dropdown trigger="click" @click.stop>
            <el-button text circle aria-label="프로젝트 메뉴">•••</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="editProject(project)">수정</el-dropdown-item>
                <el-dropdown-item divided class="danger-menu-item" :disabled="deletingId === project.id" @click="deleteProject(project)">
                  삭제
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </article>
      </section>
    </main>
    <ProjectFormModal v-model="modalOpen" :project="editingProject" />
  </AppShell>
</template>
