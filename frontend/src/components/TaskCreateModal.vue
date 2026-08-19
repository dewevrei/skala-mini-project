<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import EntityModal from './EntityModal.vue'
import { useTaskStore } from '../stores/tasks'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  projectId: { type: [String, Number], required: true },
  sourceColumnId: { type: [String, Number], required: true },
})
const emit = defineEmits(['update:modelValue', 'created', 'generated'])
const tasks = useTaskStore()
const form = reactive({ title: '', description: '' })
const pendingAction = ref('')

watch(() => props.modelValue, (open) => {
  if (open) reset()
})

function reset() {
  form.title = ''
  form.description = ''
}

function cancel() {
  if (pendingAction.value) return
  reset()
  emit('update:modelValue', false)
}

function payload(requireDescription) {
  const title = form.title.trim()
  const description = form.description.trim()
  if (!title) {
    ElMessage.error('Task 제목을 입력해 주세요.')
    return null
  }
  if (requireDescription && !description) {
    ElMessage.error('AI Generate에는 설명이 필요합니다.')
    return null
  }
  return { title, description: description || null }
}

async function create() {
  const body = payload(false)
  if (!body) return
  pendingAction.value = 'create'
  try {
    const response = await tasks.createTask(props.projectId, props.sourceColumnId, body)
    ElMessage.success(response.message)
    emit('created', response.data.task)
    cancelAfterSuccess()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    pendingAction.value = ''
  }
}

async function generate() {
  const body = payload(true)
  if (!body) return
  pendingAction.value = 'generate'
  try {
    const response = await tasks.generateTasks(props.projectId, body)
    ElMessage.success(response.message)
    emit('generated', response.data.tasks)
    cancelAfterSuccess()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    pendingAction.value = ''
  }
}

function cancelAfterSuccess() {
  reset()
  emit('update:modelValue', false)
}
</script>

<template>
  <EntityModal
    :model-value="modelValue"
    title="새 Task 추가"
    :busy="Boolean(pendingAction)"
    @update:model-value="emit('update:modelValue', $event)"
    @cancel="cancel"
  >
    <el-form label-position="top" @submit.prevent="create">
      <el-form-item label="제목" required>
        <el-input v-model="form.title" maxlength="200" show-word-limit autofocus />
      </el-form-item>
      <el-form-item label="설명">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="15"
          maxlength="5000"
          resize="vertical"
          placeholder="Task 설명을 입력하세요. AI Generate 사용 시 필수입니다."
        />
      </el-form-item>
    </el-form>
    <template #actions>
      <el-button class="task-cancel-button" :disabled="Boolean(pendingAction)" @click="cancel">Cancel</el-button>
      <el-button class="task-create-button" :loading="pendingAction === 'create'" :disabled="Boolean(pendingAction)" @click="create">
        Create
      </el-button>
      <el-button class="ai-generate-button" :loading="pendingAction === 'generate'" :disabled="Boolean(pendingAction)" @click="generate">
        AI Generate
      </el-button>
    </template>
  </EntityModal>
</template>
