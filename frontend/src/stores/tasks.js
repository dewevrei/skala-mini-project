import { defineStore } from 'pinia'
import { taskApi } from '../api/tasks'

export const useTaskStore = defineStore('tasks', {
  actions: {
    createTask(projectId, columnId, payload) {
      return taskApi.create(projectId, columnId, payload)
    },
    generateTasks(projectId, payload) {
      return taskApi.generate(projectId, payload)
    },
  },
})
