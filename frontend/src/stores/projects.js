import { defineStore } from 'pinia'
import { projectApi } from '../api/projects'

export const useProjectStore = defineStore('projects', {
  state: () => ({
    projects: [],
    currentProject: null,
    loading: false,
  }),
  actions: {
    async fetchProjects() {
      this.loading = true
      try {
        const response = await projectApi.list()
        this.projects = response.data.projects
        return response
      } finally {
        this.loading = false
      }
    },
    async fetchProject(projectId) {
      const response = await projectApi.get(projectId)
      this.currentProject = response.data.project
      return response
    },
    async createProject(payload) {
      const response = await projectApi.create(payload)
      this.projects.unshift(response.data.project)
      return response
    },
    async updateProject(projectId, payload) {
      const response = await projectApi.update(projectId, payload)
      const index = this.projects.findIndex((project) => String(project.id) === String(projectId))
      if (index >= 0) this.projects[index] = response.data.project
      if (String(this.currentProject?.id) === String(projectId)) this.currentProject = response.data.project
      return response
    },
    async deleteProject(projectId) {
      const response = await projectApi.remove(projectId)
      this.projects = this.projects.filter((project) => String(project.id) !== String(projectId))
      if (String(this.currentProject?.id) === String(projectId)) this.currentProject = null
      return response
    },
  },
})
