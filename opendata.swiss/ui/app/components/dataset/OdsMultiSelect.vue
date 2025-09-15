<template>
  <div class="form__group__select">
    <label
      v-if="label"
      :for="getUniqueId('multi-select')"
      :class="labelClasses"
    >
      {{ label }}
    </label>
    <div :class="selectWrapperClasses">
      <VSelect
        :id="getUniqueId('multi-select')"
        v-model="currentSelected"
        :multiple="multiple"
        :placeholder="placeholder"
        :class="selectClasses"
        :disabled="disabled"
        :components="{ Deselect, OpenIndicator }"
        :options="options"
        :name="name"
        label="title"
        :reduce="(option: Item) => option.id"

        :selectable="
          (option: Item) =>
            !excluded.includes(option.id) &&
            (selectLimit
              ? !currentSelected || currentSelected.length < selectLimit
              : true)
        "
        @update:model-value="emitNewValues"
      >
        <!-- Workaround for required validation -->
        <template #search="{ attributes, events }">
          <input
            class="vs__search"
            :required="
              required && (!currentSelected || currentSelected.length === 0)
            "
            v-bind="attributes as any"
            v-on="events"
          >
        </template>
       <template #option="option">
        <span>
          {{ option.title }}
          <span style="float: right; color: #888;">({{ option.count }})</span>
        </span>
      </template>
      </VSelect>
      <div class="select__icon">
        <svg role="presentation" aria-hidden="true" viewBox="0 0 24 24">
          <path
            d="m5.706 10.015 6.669 3.85 6.669-3.85.375.649-7.044 4.067-7.044-4.067z"
          />
        </svg>
      </div>
    </div>
    <div
      v-if="message"
      class="badge badge--sm"
      :class="`badge--${messageType}`"
    >
      {{ message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import VSelect from 'vue-select'
import { ref, computed, onMounted, h, watch } from 'vue'
import { v4 as uuidv4 } from 'uuid'

interface Item {
  id: string;
  title: string | undefined;
  count: number;
}

const selectId = ref('')
const currentSelected = ref([] as string[])
const Deselect = { render: () => h('span', '×') }
const OpenIndicator = { render: () => h('span') }

const emit =defineEmits<{
  (e: 'update:modelValue', value: string[]): void
}>()

const props = defineProps({
  bare: {
    type: Boolean,
    default: () => false,
  },
  variant: {
    type: String,
    validator: (prop) => ['outline', 'negative'].includes(prop as string),
    default: () => 'outline',
  },
  size: {
    type: String,
    validator: (prop) => ['sm', 'base', 'lg'].includes(prop as string),
    default: () => 'base',
  },
  disabled: {
    type: Boolean,
    default: () => false,
  },
  hideLabel: {
    type: Boolean,
    default: () => false,
  },
  required: {
    type: Boolean,
    default: () => false,
  },
  label: {
    type: String,
    default: () => undefined,
  },
  name: {
    type: String,
    default: () => undefined,
  },
  message: {
    type: String,
    default: () => undefined,
  },
  messageType: {
    type: String,
    validator: (prop) =>
      ['error', 'warning', 'success', 'info'].includes(prop as string),
    default: () => undefined,
  },
  options: {
    type: Array< Item>,
    default: () => [],
  },
  selected: {
    type: Array<string>,
    default: () => [],
  },
  multiple: {
    type: Boolean,
    default: () => true,
  },
  placeholder: {
    type: String,
    default: () => undefined,
  },
  excluded: {
    type: Array<string>,
    default: () => [],
  },
  selectLimit: {
    type: Number,
    default: () => undefined,
  },
})

const selectWrapperClasses = computed(() => {
  const classes = ['select', 'shadow-lg']
  if (props.bare) {
    classes.push('select--bare')
  }
  return classes.join(' ')
})

const selectClasses = computed(() => {
  const classes = []

  if (props.variant) {
    classes.push(`input--${props.variant}`)
  }
  if (props.size) {
    classes.push(`input--${props.size}`)
  }
  if (props.disabled) {
    classes.push('input--disabled')
  }
  if (props.messageType) {
    classes.push(`input--${props.messageType}`)
  }

  return classes.join(' ')
})

const labelClasses = computed(() => {
  const classes = []

  if (props.variant === 'negative') {
    classes.push('text--negative')
  }
  if (props.size) {
    classes.push(`text--${props.size}`)
  }
  if (props.hideLabel) {
    classes.push('sr-only')
  }
  if (props.required) {
    classes.push('text--asterisk')
  }

  return classes.join(' ')
})

const getUniqueId = function (text = '') {
  return `${text}-${selectId.value}`
}

function emitNewValues() {
  emit('update:modelValue', currentSelected.value)
}

onMounted(() => {
  // Set initial selected element
  currentSelected.value = props.selected
  selectId.value = uuidv4()
})



watch(
  () => props.selected,
  (newVal) => {
    currentSelected.value = newVal
  }
)
</script>
