import { defineStore } from 'pinia'
import { apiClient, clearCsrfToken, onAuthenticationFailure } from '../api/client'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    initialized: false,
    loading: false,
  }),
  getters: {
    authenticated: (state) => Boolean(state.user),
  },
  actions: {
    bindAuthenticationFailureHandler() {
      onAuthenticationFailure(() => this.invalidate())
    },
    invalidate() {
      this.user = null
      this.initialized = true
      clearCsrfToken()
    },
    async initialize(force = false) {
      this.bindAuthenticationFailureHandler()
      if (this.initialized && !force) return this.user
      this.loading = true
      try {
        const data = await apiClient.get('/users/me')
        this.user = data.user
      } catch (error) {
        if (error.code === 'AUTHENTICATION_REQUIRED') {
          this.invalidate()
        } else {
          this.user = null
          throw error
        }
      } finally {
        this.initialized = true
        this.loading = false
      }
      return this.user
    },
    async updateNickname(nickname) {
      this.bindAuthenticationFailureHandler()
      const data = await apiClient.patch('/users/me/nickname', { nickname })
      this.user = data.user
      return this.user
    },
    async logout() {
      this.bindAuthenticationFailureHandler()
      try {
        await apiClient.post('/auth/logout')
      } finally {
        this.invalidate()
      }
    },
  },
})
