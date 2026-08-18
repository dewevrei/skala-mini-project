<script setup>
import { computed, onActivated, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import TaskCreateModal from '../components/TaskCreateModal.vue'
import ItemsTaskEditModal from '../components/ItemsTaskEditModal.vue'
import { projectApi } from '../api/projects'
import { taskApi } from '../api/tasks'
import { useProjectRefresh } from '../composables/useProjectRefresh'

defineProps({
  project: { type: Object, default: null },
  refreshProject: { type: Function, default: null },
})

const route = useRoute()
const groups = ref([])
const searchTitle = ref('')
const loading = ref(false)
const loadError = ref('')
const createColumnId = ref(null)
const editingTask = ref(null)
const pendingTasks = ref(new Set())
const collapsedColumnIds = ref(new Set())
let searchTimer
let requestSequence = 0

const projectId = computed(() => route.params.projectId)
const hasTasks = computed(() => groups.value.some((group) => group.tasks.length > 0))
const statusOptions = computed(() => groups.value.map((group) => group.column))
const priorityOptions = [1, 2, 3, 4, 5]

function setTaskPending(taskId, pending) {
  const next = new Set(pendingTasks.value)
  if (pending) next.add(taskId)
  else next.delete(taskId)
  pendingTasks.value = next
}

function isTaskPending(taskId) {
  return pendingTasks.value.has(taskId)
}

function isColumnCollapsed(columnId) {
  return collapsedColumnIds.value.has(String(columnId))
}

function toggleColumn(columnId) {
  const id = String(columnId)
  const next = new Set(collapsedColumnIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  collapsedColumnIds.value = next
}

async function refreshItems({ quiet = false } = {}) {
  const sequence = ++requestSequence
  if (!quiet) loading.value = true
  loadError.value = ''
  try {
    const response = await projectApi.items(projectId.value, searchTitle.value)
    if (sequence !== requestSequence) return
    groups.value = response.data.columnGroups ?? []
  } catch (error) {
    if (sequence !== requestSequence) return
    loadError.value = error.message
    if (quiet) ElMessage.error(error.message)
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

function scheduleSearch() {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => refreshItems(), 350)
}

function openCreate(columnId) {
  createColumnId.value = columnId
}

function closeCreate(open) {
  if (!open) createColumnId.value = null
}

function openEdit(task) {
  editingTask.value = task
}

function closeEdit(open) {
  if (!open) editingTask.value = null
}

function replaceTask(canonicalTask) {
  groups.value = groups.value.map((group) => ({
    ...group,
    tasks: group.tasks.map((task) => task.id === canonicalTask.id ? canonicalTask : task),
  }))
  if (editingTask.value?.id === canonicalTask.id) editingTask.value = canonicalTask
}

function applyCanonicalGroups(affectedGroups) {
  const canonicalById = new Map(affectedGroups.map((group) => [String(group.column.id), group]))
  groups.value = groups.value.map((group) => canonicalById.get(String(group.column.id)) ?? group)
}

async function changeStatus(task, targetColumnId) {
  if (String(targetColumnId) === String(task.columnId) || isTaskPending(task.id)) return
  setTaskPending(task.id, true)
  try {
    const response = await taskApi.changeStatus(projectId.value, task.id, targetColumnId)
    applyCanonicalGroups(response.data.affectedColumnGroups ?? [])
    ElMessage.success(response.message)
  } catch (error) {
    ElMessage.error(error.message)
    await refreshItems({ quiet: true })
  } finally {
    setTaskPending(task.id, false)
  }
}

async function updatePriority(task, priority) {
  if (Number(priority) === task.priority || isTaskPending(task.id)) return
  setTaskPending(task.id, true)
  try {
    const response = await taskApi.updatePriority(projectId.value, task.id, Number(priority))
    replaceTask(response.data.task)
    ElMessage.success(response.message)
  } catch (error) {
    ElMessage.error(error.message)
    await refreshItems({ quiet: true })
  } finally {
    setTaskPending(task.id, false)
  }
}

async function updateDates(task, field, value) {
  if (isTaskPending(task.id)) return
  setTaskPending(task.id, true)
  const dates = {
    startDate: field === 'startDate' ? (value || null) : (task.startDate || null),
    endDate: field === 'endDate' ? (value || null) : (task.endDate || null),
  }
  try {
    const response = await taskApi.updateDates(projectId.value, task.id, dates)
    replaceTask(response.data.task)
    ElMessage.success(response.message)
  } catch (error) {
    ElMessage.error(error.message)
    await refreshItems({ quiet: true })
  } finally {
    setTaskPending(task.id, false)
  }
}

async function afterCreate() {
  await refreshItems({ quiet: true })
}

async function afterGenerated() {
  await refreshItems({ quiet: true })
}

function afterUpdate(task) {
  replaceTask(task)
}

async function afterDelete() {
  editingTask.value = null
  await refreshItems({ quiet: true })
}

watch(projectId, () => {
  searchTitle.value = ''
  refreshItems()
}, { immediate: true })

watch(searchTitle, scheduleSearch)
useProjectRefresh(() => refreshItems({ quiet: true }))
onActivated(() => refreshItems({ quiet: true }))
onBeforeUnmount(() => window.clearTimeout(searchTimer))
</script>

<template>
  <section class="items-view" aria-labelledby="items-view-heading">
    <h2 id="items-view-heading" class="visually-hidden">Items</h2>

    <div class="items-search">
      <el-input
        v-model="searchTitle"
        maxlength="200"
        clearable
        size="large"
        aria-label="Task 제목 검색"
        placeholder="Task 제목으로 검색"
      >
        <template #prefix><span aria-hidden="true">⌕</span></template>
      </el-input>
    </div>

    <div v-if="loadError && !loading" class="items-state items-state--error" role="alert">
      <p>{{ loadError }}</p>
      <el-button @click="refreshItems()">다시 시도</el-button>
    </div>

    <div v-else class="items-table-wrap" :aria-busy="loading">
      <div class="items-table-header" role="row">
        <div role="columnheader">Title</div>
        <div role="columnheader">Status</div>
        <div role="columnheader">Priority</div>
        <div role="columnheader">Start date</div>
        <div role="columnheader">End date</div>
      </div>

      <div v-if="loading" class="items-state" role="status">
        <span class="items-spinner" aria-hidden="true" />
        Items를 불러오는 중입니다.
      </div>

      <template v-else>
        <section
          v-for="(group, groupIndex) in groups"
          :key="group.column.id"
          class="items-group"
          :aria-labelledby="`items-group-${group.column.id}`"
        >
          <header class="items-group__header">
            <button
              class="items-group__toggle"
              type="button"
              :aria-controls="`items-group-content-${group.column.id}`"
              :aria-expanded="!isColumnCollapsed(group.column.id)"
              :aria-label="`${group.column.name} ${isColumnCollapsed(group.column.id) ? '펼치기' : '접기'}`"
              @click="toggleColumn(group.column.id)"
            >
              <span :class="{ 'items-group__chevron--collapsed': isColumnCollapsed(group.column.id) }" aria-hidden="true">∨</span>
            </button>
            <h3 :id="`items-group-${group.column.id}`">{{ group.column.name }}</h3>
            <span class="task-count" :aria-label="`${group.tasks.length}개 Task`">{{ group.tasks.length }}</span>
          </header>

          <div v-if="!isColumnCollapsed(group.column.id)" :id="`items-group-content-${group.column.id}`">
            <div
              v-for="task in group.tasks"
              :key="task.id"
              class="items-row"
              role="row"
              :aria-busy="isTaskPending(task.id)"
            >
              <div class="items-title-cell" role="cell">
                <button class="task-title-button" type="button" @click="openEdit(task)">
                  {{ task.title }}
                </button>
              </div>
              <div role="cell">
                <el-select
                  :model-value="task.columnId"
                  :disabled="isTaskPending(task.id)"
                  :aria-label="`${task.title} 상태`"
                  @change="changeStatus(task, $event)"
                >
                  <el-option
                    v-for="column in statusOptions"
                    :key="column.id"
                    :label="column.name"
                    :value="column.id"
                  />
                </el-select>
              </div>
              <div class="priority-cell" role="cell">
                <el-select
                  :model-value="task.priority"
                  :disabled="isTaskPending(task.id)"
                  :aria-label="`${task.title} 우선순위`"
                  @change="updatePriority(task, $event)"
                >
                  <el-option
                    v-for="priority in priorityOptions"
                    :key="priority"
                    :label="`P${priority}`"
                    :value="priority"
                  />
                </el-select>
              </div>
              <div role="cell">
                <el-date-picker
                  :model-value="task.startDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                  clearable
                  :disabled="isTaskPending(task.id)"
                  :aria-label="`${task.title} 시작일`"
                  placeholder="날짜 선택"
                  @change="updateDates(task, 'startDate', $event)"
                />
              </div>
              <div role="cell">
                <el-date-picker
                  :model-value="task.endDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                  clearable
                  :disabled="isTaskPending(task.id)"
                  :aria-label="`${task.title} 종료일`"
                  placeholder="날짜 선택"
                  @change="updateDates(task, 'endDate', $event)"
                />
              </div>
            </div>

            <button class="add-item-button" type="button" @click="openCreate(group.column.id)">
              <span aria-hidden="true">＋</span> Add item
            </button>
          </div>
        </section>

        <div v-if="groups.length === 0" class="items-state">
          표시할 Column이 없습니다.
        </div>
        <p v-else-if="searchTitle.trim() && !hasTasks" class="items-search-empty" role="status">
          검색 결과가 없습니다. Column은 그대로 표시됩니다.
        </p>
      </template>
    </div>

    <TaskCreateModal
      v-if="createColumnId !== null"
      :model-value="true"
      :project-id="projectId"
      :source-column-id="createColumnId"
      @update:model-value="closeCreate"
      @created="afterCreate"
      @generated="afterGenerated"
    />

    <ItemsTaskEditModal
      v-if="editingTask"
      :model-value="true"
      :project-id="projectId"
      :task="editingTask"
      @update:model-value="closeEdit"
      @updated="afterUpdate"
      @deleted="afterDelete"
    />
  </section>
</template>

<style scoped>
.items-view {
  min-width: 1280px;
  color: #1f2328;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.items-search {
  padding: 20px 24px;
  border-bottom: 1px solid #d0d7de;
}

.items-search :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d0d7de inset;
  font-size: 16px;
}

.items-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.items-table-header,
.items-row {
  display: grid;
  grid-template-columns: minmax(420px, 2.8fr) minmax(210px, 1fr) minmax(130px, .65fr) minmax(220px, 1fr) minmax(220px, 1fr);
  min-width: 1280px;
}

.items-table-header {
  min-height: 46px;
  align-items: center;
  border-bottom: 1px solid #8c959f;
  background: #fff;
  color: #57606a;
  font-weight: 600;
}

.items-table-header > div,
.items-row > div {
  height: 100%;
  padding: 8px 20px;
  display: flex;
  align-items: center;
  border-right: 1px solid #d8dee4;
}

.items-table-header > div:last-child,
.items-row > div:last-child {
  border-right: 0;
}

.items-group {
  border-bottom: 10px solid #f6f8fa;
}

.items-group__header {
  min-height: 54px;
  padding: 0 28px 0 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #d0d7de;
  background: #fff;
}

.items-group__toggle {
  width: 24px;
  height: 28px;
  padding: 0;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #57606a;
  cursor: pointer;
  font-size: 16px;
  margin-right: 8px;
}

.items-group__toggle:hover { background: #f6f8fa; color: #1f2328; }
.items-group__toggle span { display: block; line-height: 1; transition: transform .12s; }
.items-group__toggle .items-group__chevron--collapsed { transform: rotate(-90deg); }

.items-group__header h3 {
  margin: 0;
  color: #24292f;
  font-size: 17px;
  font-weight: 500;
}

.task-count {
  min-width: 28px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff2f5;
  color: #57606a;
  text-align: center;
  font-size: 14px;
}

.items-row {
  min-height: 44px;
  border-bottom: 1px solid #d8dee4;
  background: #fff;
}

.items-row > div { padding: 4px 16px; }

.items-row:hover {
  background: #f6f8fa;
}

.items-row :deep(.el-select),
.items-row :deep(.el-date-editor) {
  width: 100%;
}

.task-title-button {
  max-width: 100%;
  padding: 3px;
  overflow: hidden;
  border: 0;
  background: transparent;
  color: #0969da;
  font-weight: 600;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.task-title-button:hover {
  text-decoration: underline;
}

.priority-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.add-item-button {
  width: 100%;
  min-height: 46px;
  padding: 0 28px;
  border: 0;
  border-bottom: 1px solid #d0d7de;
  background: #fff;
  color: #57606a;
  text-align: left;
  cursor: pointer;
}

.add-item-button:hover {
  background: #f6f8fa;
  color: #1f2328;
}

.items-state,
.items-search-empty {
  min-height: 112px;
  margin: 0;
  padding: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #57606a;
  text-align: center;
}

.items-state--error {
  flex-direction: column;
  color: #cf222e;
}

.items-state--error p {
  margin: 0;
}

.items-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #d0d7de;
  border-top-color: #0969da;
  border-radius: 50%;
  animation: items-spin .8s linear infinite;
}

@keyframes items-spin {
  to { transform: rotate(360deg); }
}

.items-search-empty {
  min-height: 58px;
  border-bottom: 1px solid #d0d7de;
  background: #f6f8fa;
}
</style>
