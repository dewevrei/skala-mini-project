<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <main class="placeholder-page">
    <h1>{{ route.meta.title }}</h1>
  </main>
</template>
