<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import EntityModal from './EntityModal.vue'
import { useProjectStore } from '../stores/projects'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  project: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])
const projects = useProjectStore()
const saving = ref(false)
const form = reactive({ name: '', description: '' })

watch(() => props.modelValue, (open) => {
  if (open) {
    form.name = props.project?.name ?? ''
    form.description = props.project?.description ?? ''
  }
})

function close() {
  if (saving.value) return
  form.name = ''
  form.description = ''
  emit('update:modelValue', false)
}

async function save() {
  const name = form.name.trim()
  if (!name) {
    ElMessage.error('프로젝트 이름을 입력해 주세요.')
    return
  }
  saving.value = true
  try {
    const payload = { name, description: form.description.trim() || null }
    const response = props.project
      ? await projects.updateProject(props.project.id, payload)
      : await projects.createProject(payload)
    ElMessage.success(response.message)
    emit('saved', response.data.project)
    form.name = ''
    form.description = ''
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <EntityModal
    :model-value="modelValue"
    :title="project ? '프로젝트 수정' : '새 프로젝트'"
    :busy="saving"
    @update:model-value="emit('update:modelValue', $event)"
    @cancel="close"
  >
    <el-form label-position="top" @submit.prevent="save">
      <el-form-item label="프로젝트 이름" required>
        <el-input v-model="form.name" maxlength="255" show-word-limit autofocus />
      </el-form-item>
      <el-form-item label="설명">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="12"
          maxlength="2000"
          resize="vertical"
          placeholder="프로젝트에 대한 설명을 입력하세요."
        />
      </el-form-item>
    </el-form>
    <template #actions>
      <el-button :disabled="saving" @click="close">Cancel</el-button>
      <el-button type="primary" :loading="saving" @click="save">{{ project ? 'Save' : 'Create' }}</el-button>
    </template>
  </EntityModal>
</template>
