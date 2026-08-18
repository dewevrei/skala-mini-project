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
const pendingAction = ref('')

watch(
  () => [props.modelValue, props.task],
  ([open, task]) => {
    if (!open || !task) return
    form.title = task.title ?? ''
    form.description = task.description ?? ''
  },
  { immediate: true },
)

function close() {
  if (pendingAction.value) return
  emit('update:modelValue', false)
}

async function save() {
  const title = form.title.trim()
  const description = form.description.trim()
  if (!title) {
    ElMessage.error('Task 제목을 입력해 주세요.')
    return
  }

  pendingAction.value = 'save'
  try {
    const response = await taskApi.update(props.projectId, props.task.id, {
      title,
      description: description || null,
    })
    ElMessage.success(response.message)
    emit('updated', response.data.task)
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    pendingAction.value = ''
  }
}

async function remove() {
  try {
    await ElMessageBox.confirm(
      `'${props.task.title}' Task를 삭제할까요? 삭제한 Task는 복구할 수 없습니다.`,
      'Task 삭제',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        confirmButtonClass: 'el-button--danger',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  pendingAction.value = 'delete'
  try {
    const response = await taskApi.remove(props.projectId, props.task.id)
    ElMessage.success(response.message)
    emit('deleted', props.task.id)
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    pendingAction.value = ''
  }
}
</script>

<template>
  <EntityModal
    :model-value="modelValue"
    title="Task 수정"
    :busy="Boolean(pendingAction)"
    @update:model-value="emit('update:modelValue', $event)"
    @cancel="close"
  >
    <el-form label-position="top" @submit.prevent="save">
      <el-form-item label="제목" required>
        <el-input v-model="form.title" maxlength="200" show-word-limit autofocus />
      </el-form-item>
      <el-form-item label="설명">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="15"
          maxlength="5000"
          show-word-limit
          resize="vertical"
          placeholder="Task 설명을 입력하세요."
        />
      </el-form-item>
    </el-form>
    <template #actions>
      <el-button
        type="danger"
        plain
        :loading="pendingAction === 'delete'"
        :disabled="Boolean(pendingAction)"
        @click="remove"
      >
        Delete
      </el-button>
      <span class="items-edit-spacer" />
      <el-button :disabled="Boolean(pendingAction)" @click="close">Cancel</el-button>
      <el-button
        type="primary"
        :loading="pendingAction === 'save'"
        :disabled="Boolean(pendingAction)"
        @click="save"
      >
        Save
      </el-button>
    </template>
  </EntityModal>
</template>

<style scoped>
.items-edit-spacer {
  flex: 1;
}
</style>
