<script setup>
defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, required: true },
  busy: Boolean,
})

const emit = defineEmits(['update:modelValue', 'cancel'])

function close(done) {
  if (done) done()
  else emit('update:modelValue', false)
  emit('cancel')
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    class="entity-dialog"
    width="min(1040px, calc(100vw - 64px))"
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    :show-close="!busy"
    :before-close="close"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="entity-dialog__body">
      <slot />
    </div>
    <template #footer>
      <div class="entity-dialog__actions">
        <slot name="actions">
          <el-button :disabled="busy" @click="close()">Cancel</el-button>
        </slot>
      </div>
    </template>
  </el-dialog>
</template>
