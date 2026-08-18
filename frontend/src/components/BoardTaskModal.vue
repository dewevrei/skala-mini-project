<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EntityModal from './EntityModal.vue'
import { taskApi } from '../api/tasks'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  projectId: { type: [String, Number], required: true },
  task: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'updated', 'deleted'])
const form = reactive({ title: '', description: '' })
const pending = ref('')

watch(() => props.modelValue, (open) => {
  if (!open || !props.task) return
  form.title = props.task.title ?? ''
  form.description = props.task.description ?? ''
})

function close() {
  if (pending.value) return
  emit('update:modelValue', false)
}

async function save() {
  const title = form.title.trim()
  if (!title) {
    ElMessage.error('Task 제목을 입력해 주세요.')
    return
  }
  pending.value = 'save'
  try {
    const response = await taskApi.update(props.projectId, props.task.id, {
      title,
      description: form.description.trim() || null,
    })
    ElMessage.success(response.message)
    emit('updated', response.data.task)
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    pending.value = ''
  }
}

async function remove() {
  try {
    await ElMessageBox.confirm(
      `“${props.task.title}” Task를 삭제할까요? 삭제한 Task는 복구할 수 없습니다.`,
      'Task 삭제',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' },
    )
  } catch {
    return
  }
  pending.value = 'delete'
  try {
    const response = await taskApi.remove(props.projectId, props.task.id)
    ElMessage.success(response.message)
    emit('deleted', props.task.id)
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    pending.value = ''
  }
}
</script>

<template>
  <EntityModal
    :model-value="modelValue"
    title="Task 수정"
    :busy="Boolean(pending)"
    @update:model-value="emit('update:modelValue', $event)"
    @cancel="close"
  >
    <el-form label-position="top" @submit.prevent="save">
      <el-form-item label="제목" required>
        <el-input v-model="form.title" maxlength="200" show-word-limit autofocus />
      </el-form-item>
      <el-form-item label="설명">
        <el-input v-model="form.description" type="textarea" :rows="15" maxlength="5000" resize="vertical" />
      </el-form-item>
      <el-form-item label="우선순위">
        <span class="board-task-priority-readonly">P{{ task?.priority ?? 0 }} · 읽기 전용</span>
      </el-form-item>
    </el-form>
    <template #actions>
      <el-button type="danger" plain :loading="pending === 'delete'" :disabled="Boolean(pending)" @click="remove">
        Delete
      </el-button>
      <span class="board-task-modal-spacer" />
      <el-button :disabled="Boolean(pending)" @click="close">Cancel</el-button>
      <el-button type="primary" :loading="pending === 'save'" :disabled="Boolean(pending)" @click="save">
        Save
      </el-button>
    </template>
  </EntityModal>
</template>

<style scoped>
.board-task-priority-readonly { color: #57606a; }
.board-task-modal-spacer { flex: 1; }
</style>
