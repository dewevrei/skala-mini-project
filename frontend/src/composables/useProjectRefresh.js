import { onBeforeUnmount, onMounted } from 'vue'

export function useProjectRefresh(refresh) {
  const onFocus = () => refresh()
  onMounted(() => window.addEventListener('focus', onFocus))
  onBeforeUnmount(() => window.removeEventListener('focus', onFocus))
  return { refresh }
}
