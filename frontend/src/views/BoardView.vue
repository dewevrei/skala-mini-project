<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VueDraggable } from 'vue-draggable-plus'
import TaskCreateModal from '../components/TaskCreateModal.vue'
import BoardTaskModal from '../components/BoardTaskModal.vue'
import { projectApi } from '../api/projects'
import { columnApi } from '../api/columns'
import { taskApi } from '../api/tasks'
import { useProjectRefresh } from '../composables/useProjectRefresh'

const route = useRoute()
const groups = ref([])
const loading = ref(true)
const errorMessage = ref('')
const createTaskOpen = ref(false)
const sourceColumnId = ref(null)
const editTaskOpen = ref(false)
const selectedTask = ref(null)
const columnDialogOpen = ref(false)
const columnMode = ref('create')
const selectedColumn = ref(null)
const columnName = ref('')
const columnPending = ref(false)
const movePending = ref(false)
const columnOrderBefore = ref([])
const projectId = computed(() => route.params.projectId)

async function fetchBoard({ quiet = false } = {}) {
  if (!quiet) loading.value = true
  errorMessage.value = ''
  try {
    const response = await projectApi.board(projectId.value)
    groups.value = response.data.columnGroups ?? []
  } catch (error) {
    errorMessage.value = error.message
    if (quiet) ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(fetchBoard)
watch(projectId, fetchBoard)
useProjectRefresh(() => fetchBoard({ quiet: true }))

function openTaskCreate(columnId) {
  sourceColumnId.value = columnId
  createTaskOpen.value = true
}

function appendCreated(task) {
  const group = groups.value.find(({ column }) => String(column.id) === String(task.columnId))
  if (group) {
    group.tasks.push(task)
    group.column.taskCount = group.tasks.length
  } else fetchBoard({ quiet: true })
}

function openTaskEdit(task) {
  selectedTask.value = task
  editTaskOpen.value = true
}

function replaceTask(task) {
  for (const group of groups.value) {
    const index = group.tasks.findIndex(({ id }) => String(id) === String(task.id))
    if (index >= 0) group.tasks.splice(index, 1, task)
  }
}

function removeTask(taskId) {
  for (const group of groups.value) {
    const index = group.tasks.findIndex(({ id }) => String(id) === String(taskId))
    if (index >= 0) {
      group.tasks.splice(index, 1)
      group.column.taskCount = group.tasks.length
    }
  }
}

function openColumnCreate() {
  columnMode.value = 'create'
  selectedColumn.value = null
  columnName.value = ''
  columnDialogOpen.value = true
}

function openColumnEdit(column) {
  columnMode.value = 'edit'
  selectedColumn.value = column
  columnName.value = column.name
  columnDialogOpen.value = true
}

async function saveColumn() {
  const name = columnName.value.trim()
  if (!name) {
    ElMessage.error('Column 이름을 입력해 주세요.')
    return
  }
  columnPending.value = true
  try {
    const response = columnMode.value === 'create'
      ? await columnApi.create(projectId.value, { name })
      : await columnApi.update(projectId.value, selectedColumn.value.id, { name })
    if (columnMode.value === 'create') {
      groups.value.push({ column: response.data.column, tasks: [] })
    } else {
      const group = groups.value.find(({ column }) => String(column.id) === String(response.data.column.id))
      if (group) group.column = response.data.column
    }
    ElMessage.success(response.message)
    columnDialogOpen.value = false
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    columnPending.value = false
  }
}

async function deleteColumn(group) {
  const count = group.tasks.length || group.column.taskCount || 0
  const detail = count
    ? `포함된 Task ${count}개도 함께 완전히 삭제됩니다.`
    : '삭제한 Column은 복구할 수 없습니다.'
  try {
    await ElMessageBox.confirm(
      `“${group.column.name}” Column을 삭제할까요? ${detail}`,
      'Column 삭제',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    const response = await columnApi.remove(projectId.value, group.column.id)
    groups.value = groups.value.filter(({ column }) => String(column.id) !== String(group.column.id))
    ElMessage.success(response.message)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function rememberColumnOrder() {
  columnOrderBefore.value = groups.value.map(({ column }) => String(column.id))
}

async function persistColumnOrder(event) {
  if (movePending.value || event.oldIndex === event.newIndex) return
  const currentOrder = groups.value.map(({ column }) => String(column.id))
  if (currentOrder.every((id, index) => id === columnOrderBefore.value[index])) return

  movePending.value = true
  try {
    const response = await columnApi.reorder(projectId.value, groups.value.map(({ column }) => column.id))
    const tasksById = new Map(groups.value.map(group => [String(group.column.id), group.tasks]))
    groups.value = response.data.columns.map(column => ({ column, tasks: tasksById.get(String(column.id)) ?? [] }))
    ElMessage.success(response.message)
  } catch (error) {
    ElMessage.error(error.message)
    await fetchBoard({ quiet: true })
  } finally {
    movePending.value = false
  }
}

async function persistTaskMove(event) {
  if (movePending.value || event.from === event.to) return
  await nextTick()
  const taskId = event.item?.dataset.taskId
  const targetColumnId = event.to?.dataset.columnId
  if (!taskId || !targetColumnId) {
    await fetchBoard({ quiet: true })
    return
  }
  const targetGroup = groups.value.find(({ column }) => String(column.id) === String(targetColumnId))
  const taskIndex = targetGroup?.tasks.findIndex(({ id }) => String(id) === String(taskId)) ?? -1
  const beforeTaskId = taskIndex >= 0 ? targetGroup.tasks[taskIndex + 1]?.id ?? null : null

  movePending.value = true
  try {
    const response = await taskApi.move(projectId.value, taskId, { targetColumnId, beforeTaskId })
    applyAffectedGroups(response.data.affectedColumnGroups)
    ElMessage.success(response.message)
  } catch (error) {
    ElMessage.error(error.message)
    await fetchBoard({ quiet: true })
  } finally {
    movePending.value = false
  }
}

function applyAffectedGroups(affected) {
  const byId = new Map((affected ?? []).map(group => [String(group.column.id), group]))
  groups.value = groups.value.map(group => byId.get(String(group.column.id)) ?? group)
}

</script>

<template>
  <section class="board-page" aria-label="프로젝트 Board">
    <div v-if="loading" class="board-state" aria-live="polite">
      <el-skeleton :rows="7" animated />
    </div>
    <el-result v-else-if="errorMessage" icon="error" title="Board를 불러오지 못했습니다" :sub-title="errorMessage">
      <template #extra><el-button type="primary" @click="fetchBoard()">다시 시도</el-button></template>
    </el-result>
    <div v-else class="board-scroll">
      <VueDraggable
        v-model="groups"
        class="board-columns"
        draggable=".board-column"
        handle=".column-menu-handle"
        direction="horizontal"
        :animation="150"
        :force-fallback="true"
        :fallback-on-body="true"
        :fallback-tolerance="4"
        fallback-class="column-drag-preview"
        ghost-class="column-sort-ghost"
        chosen-class="column-sort-chosen"
        :disabled="movePending"
        @start="rememberColumnOrder"
        @end="persistColumnOrder"
      >
        <article v-for="group in groups" :key="group.column.id" class="board-column">
            <header class="board-column__header">
              <span class="board-column__marker" />
              <h2>{{ group.column.name }}</h2>
              <span class="board-column__count" :aria-label="`${group.tasks.length}개 Task`">{{ group.tasks.length }}</span>
              <el-dropdown trigger="click">
                <button
                  class="icon-button column-menu-handle"
                  type="button"
                  :aria-label="`${group.column.name} Column 메뉴 또는 순서 이동`"
                  title="클릭하여 메뉴 열기 · 드래그하여 Column 순서 이동"
                >•••</button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openColumnEdit(group.column)">Rename</el-dropdown-item>
                    <el-dropdown-item class="danger-menu-item" @click="deleteColumn(group)">Delete</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <button class="icon-button" type="button" aria-label="Task 추가" @click="openTaskCreate(group.column.id)">＋</button>
            </header>
            <VueDraggable
              v-model="group.tasks"
              class="board-column__tasks"
              :data-column-id="group.column.id"
              group="board-tasks"
              draggable=".board-card"
              :sort="false"
              :animation="150"
              :force-fallback="true"
              :fallback-on-body="true"
              :fallback-tolerance="4"
              ghost-class="task-sort-ghost"
              chosen-class="task-sort-chosen"
              :disabled="movePending"
              @end="persistTaskMove"
            >
              <button
                v-for="task in group.tasks"
                :key="task.id"
                type="button"
                class="board-card"
                :data-task-id="task.id"
                :disabled="movePending"
                @click="openTaskEdit(task)"
              >
                <span class="board-card__topline">
                  <strong>{{ task.title }}</strong>
                  <span class="priority-badge">P{{ task.priority }}</span>
                </span>
                <span v-if="task.description" class="board-card__description">{{ task.description }}</span>
              </button>
              <button class="add-item-button" type="button" @click="openTaskCreate(group.column.id)">＋ Add item</button>
            </VueDraggable>
          </article>
        <button type="button" class="new-column-button" @click="openColumnCreate">＋ New column</button>
      </VueDraggable>
    </div>

    <TaskCreateModal
      v-if="sourceColumnId !== null"
      v-model="createTaskOpen"
      :project-id="projectId"
      :source-column-id="sourceColumnId"
      @created="appendCreated"
      @generated="fetchBoard({ quiet: true })"
    />
    <BoardTaskModal
      v-if="selectedTask"
      v-model="editTaskOpen"
      :project-id="projectId"
      :task="selectedTask"
      @updated="replaceTask"
      @deleted="removeTask"
    />
    <el-dialog
      v-model="columnDialogOpen"
      :title="columnMode === 'create' ? 'New column' : 'Rename column'"
      width="520px"
      :close-on-click-modal="false"
      :close-on-press-escape="!columnPending"
      :show-close="!columnPending"
    >
      <el-form label-position="top" @submit.prevent="saveColumn">
        <el-form-item label="Column name" required>
          <el-input v-model="columnName" maxlength="50" show-word-limit autofocus />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="columnPending" @click="columnDialogOpen = false">Cancel</el-button>
        <el-button type="primary" :loading="columnPending" @click="saveColumn">Save</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.board-page { min-height: calc(100vh - 190px); background: #fff; }
.board-state { padding: 40px; }
.board-scroll { min-height: calc(100vh - 190px); overflow-x: auto; padding: 24px 28px 36px; }
.board-columns { display: flex; align-items: stretch; gap: 16px; min-width: max-content; }
.board-column { width: 344px; height: calc(100vh - 250px); min-height: 420px; display: flex; flex: none; flex-direction: column; border: 1px solid #d0d7de; border-radius: 10px; background: #f6f8fa; }
.column-sort-chosen { opacity: .7; }
.column-sort-ghost { border-color: #7dd3fc; background: #f0f9ff; box-shadow: 0 0 0 1px #7dd3fc; opacity: .35; }
.column-drag-preview { width: 344px; border: 1px solid #7dd3fc; border-radius: 10px; background: #f6f8fa; box-shadow: 0 8px 20px rgba(31, 35, 40, .18); }
.board-column__header { min-height: 62px; padding: 14px 12px 12px 16px; display: flex; align-items: center; gap: 9px; }
.board-column__marker { width: 20px; height: 20px; border: 3px solid #43853d; border-radius: 50%; background: #dafbe1; }
.board-column__header h2 { flex: 1; min-width: 0; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 17px; }
.board-column__count { min-width: 27px; padding: 2px 8px; border-radius: 999px; background: #eaeef2; color: #57606a; text-align: center; font-size: 13px; }
.icon-button { min-width: 30px; min-height: 30px; padding: 2px; border: 0; border-radius: 6px; background: transparent; color: #57606a; cursor: pointer; font-size: 20px; }
.icon-button:hover { background: #eaeef2; color: #1f2328; }
.column-menu-handle { cursor: grab; }
.column-menu-handle:active { cursor: grabbing; }
.board-column__tasks { min-height: 180px; display: flex; flex: 1; flex-direction: column; gap: 8px; overflow-y: auto; overscroll-behavior: contain; padding: 0 12px 12px; border-radius: 0 0 10px 10px; }
.board-card { width: 100%; min-height: 108px; padding: 16px; display: flex; flex-direction: column; gap: 10px; border: 1px solid #d0d7de; border-radius: 8px; background: #fff; box-shadow: 0 1px 2px rgba(31,35,40,.08); color: #1f2328; cursor: grab; text-align: left; }
.task-sort-chosen { opacity: .7; }
.task-sort-ghost { border: 2px solid #7dd3fc; background: #f0f9ff; box-shadow: none; }
.board-card:hover { border-color: #8c959f; box-shadow: 0 3px 8px rgba(31,35,40,.12); }
.board-card:active { cursor: grabbing; }
.board-card__topline { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.board-card__topline strong { overflow-wrap: anywhere; font-size: 16px; line-height: 1.35; }
.priority-badge { flex: none; padding: 2px 7px; border: 1px solid #d0d7de; border-radius: 999px; color: #57606a; background: #f6f8fa; font-size: 12px; font-weight: 650; }
.board-card__description { display: -webkit-box; overflow: hidden; color: #57606a; line-height: 1.45; overflow-wrap: anywhere; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.add-item-button { width: 100%; min-height: 40px; border: 0; border-radius: 7px; background: transparent; color: #57606a; cursor: pointer; opacity: 0; pointer-events: none; text-align: left; transition: opacity .12s; font-size: 15px; }
.board-column:hover .add-item-button,
.board-column:focus-within .add-item-button { opacity: 1; pointer-events: auto; }
.add-item-button:hover { background: #eaeef2; color: #1f2328; }
.new-column-button { width: 164px; min-height: 46px; align-self: flex-start; flex: none; padding: 0 16px; border: 1px solid #d0d7de; border-radius: 8px; background: #f6f8fa; color: #1f2328; cursor: pointer; font-weight: 600; text-align: left; }
.new-column-button:hover { background: #eaeef2; }
@media (hover: none) { .add-item-button { opacity: 1; pointer-events: auto; } }
</style>
