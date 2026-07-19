<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEditMode ? 'Edit Bill' : 'Add New Bill'"
    width="500px"
    @close="handleDialogClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      label-position="top"
    >
      <el-form-item label="Type" prop="type">
        <el-select v-model="formData.type" placeholder="Select type">
          <el-option label="Income" value="income" />
          <el-option label="Expense" value="expense" />
        </el-select>
      </el-form-item>

      <el-form-item label="Category" prop="category">
        <el-select v-model="formData.category" placeholder="Select category">
          <el-option
            v-for="cat in categoryOptions"
            :key="cat"
            :label="cat"
            :value="cat"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="Amount (USD)" prop="amount">
        <el-input
          v-model.number="formData.amount"
          type="number"
          placeholder="0"
          min="0"
        />
      </el-form-item>

      <el-form-item label="Date" prop="date">
        <el-date-picker
          v-model="formData.date"
          type="date"
          placeholder="Select date"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>

      <!--
        "Make Monthly Transaction" toggle. When checked, the form's
        "Date" field becomes irrelevant (the cron will fill it in) and
        a "Run on day of month" field appears. The submit handler
        branches to POST /api/recurring-bills instead of /bills.
      -->
      <el-form-item label="Recurring" class="recurring-toggle">
        <el-checkbox v-model="formData.isRecurring">
          Make Monthly Transaction
        </el-checkbox>
      </el-form-item>

      <el-form-item
        v-if="formData.isRecurring"
        label="Run on day of month"
        prop="dayOfMonth"
        class="recurring-day"
      >
        <el-input-number
          v-model="formData.dayOfMonth"
          :min="1"
          :max="28"
          :step="1"
          size="default"
          class="day-input"
        />
        <span class="recurring-hint">
          The bill will be auto-posted on this day of every month (1-28 to be valid every month).
        </span>
      </el-form-item>

      <el-form-item label="Description" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          placeholder="Enter description (optional)"
          :rows="3"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleDialogClose">Cancel</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEditMode ? 'Save Changes' : 'Add Bill' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  categories as categoriesRef,
  fetchCategories,
  getCategoryNamesForType,
} from '@/utils/categories'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  editData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'success'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formRef = ref(null)
const loading = ref(false)

const isEditMode = computed(() => !!props.editData)

/**
 * Category options shown in the dropdown.
 *
 * Previously this was a hard-coded list of system defaults, which meant
 * any new custom category added on the Categories page never appeared in
 * the Add / Edit bill form. Now we read from the shared `categories`
 * cache (kept in sync by the Categories page) and merge the system
 * defaults with the user's custom categories via
 * `getCategoryNamesForType`.
 *
 * `getCategoryNamesForType` expects the integer type used by the backend
 * (0 = expense, 1 = income), so we translate from the form's string
 * representation. Touching `categoriesRef.value` keeps this computed
 * reactive to updates from the shared cache.
 */
const categoryOptions = computed(() => {
  const typeInt = formData.value.type === 'income' ? 1 : 0
  // Read the ref so Vue tracks the dependency and re-runs when
  // categories are added / removed from the shared cache.
  // eslint-disable-next-line no-unused-expressions
  categoriesRef.value
  return getCategoryNamesForType(typeInt)
})

const formData = ref({
  type: 'expense',
  category: '',
  amount: '',
  date: new Date().toISOString().split('T')[0],
  description: '',
  // Recurring-bill template state. The cron job will auto-post a bill
  // for this template on the configured day of every month.
  isRecurring: false,
  dayOfMonth: 1,
})

const formRules = {
  type: [
    { required: true, message: 'Type is required', trigger: 'change' }
  ],
  category: [
    { required: true, message: 'Category is required', trigger: 'change' }
  ],
  amount: [
    { required: true, message: 'Amount is required', trigger: 'blur' },
    {
      type: 'number',
      min: 1,
      message: 'Amount must be greater than 0',
      trigger: 'blur'
    }
  ],
  date: [
    { required: true, message: 'Date is required', trigger: 'change' }
  ],
  // Only validated when the recurring checkbox is on. The 1-28 range is
  // also enforced by el-input-number's :min/:max, so this is a belt-and-
  // suspenders check.
  dayOfMonth: [
    {
      validator: (rule, value, callback) => {
        if (!formData.value.isRecurring) return callback()
        const n = Number(value)
        if (!Number.isInteger(n) || n < 1 || n > 28) {
          return callback(new Error('Day must be an integer between 1 and 28'))
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

const resetForm = () => {
  formData.value = {
    type: 'expense',
    category: '',
    amount: '',
    date: new Date().toISOString().split('T')[0],
    description: '',
    isRecurring: false,
    dayOfMonth: 1,
  }
  formRef.value?.clearValidate()
}

// Watch to reset form when mode changes
watch(
  () => props.editData,
  (newVal) => {
    if (newVal) {
      formData.value = { ...newVal }
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

// When the user changes the bill type, clear the previously selected
// category so we don't keep a value that doesn't belong to the new
// type's list (e.g. an expense category still selected after switching
// to income).
watch(
  () => formData.value.type,
  () => {
    formData.value.category = ''
  }
)

// When the dialog becomes visible, make sure the shared categories
// cache is loaded so custom categories show up even if the user never
// visited the Categories page first. `fetchCategories` is a no-op when
// the cache is already populated for the same user, so this is cheap.
watch(
  () => props.visible,
  async (isVisible) => {
    if (!isVisible) return
    const userId = Number(localStorage.getItem('userId'))
    if (!userId) return
    try {
      await fetchCategories(userId)
    } catch (e) {
      // Non-fatal: defaults will still render.
      console.warn('Failed to refresh categories for bill dialog:', e)
    }
  }
)

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    loading.value = true

    const userId = Number(localStorage.getItem('userId'))
    if (!userId) {
      ElMessage.error('Session expired. Please log in again.')
      return
    }

    // Convert the UI's "income"/"expense" string to the Integer (1/0)
    // that the backend's Bill.type field expects. Also map the form's
    // "date" field to "billDate" so Jackson can bind it to the entity.
    const typeInt = formData.value.type === 'income' ? 1 : 0

    // Two distinct submission paths. Edit + new one-off go to /bills.
    // New recurring goes to /api/recurring-bills so the backend can
    // persist the template that the @Scheduled cron will fire monthly.
    if (isEditMode.value) {
      // Edit mode is one-off only — recurring templates are managed
      // through the dedicated endpoints, not the bill editor.
      const payload = {
        userId: userId,
        type: typeInt,
        category: formData.value.category,
        amount: formData.value.amount,
        billDate: formData.value.date,
        description: formData.value.description || ''
      }
      await request.put(`/bills/${formData.value.id}`, payload)
      ElMessage.success('Bill updated successfully')
    } else if (formData.value.isRecurring) {
      // Recurring template. The backend clamps dayOfMonth to 1-28 and
      // derives startYearMonth from "now" if not provided.
      const payload = {
        userId: userId,
        amount: formData.value.amount,
        type: typeInt,
        category: formData.value.category,
        description: formData.value.description || '',
        dayOfMonth: formData.value.dayOfMonth,
      }
      // The path is relative to request.js's baseURL ('/api'), so the
      // effective URL is /api/recurring-bills — the route declared on
      // RecurringBillController. Do NOT prefix with /api here.
      await request.post('/recurring-bills', payload)
      ElMessage.success(
        `Recurring bill created — will be auto-posted on the ${formData.value.dayOfMonth} of every month.`
      )
    } else {
      // Standard one-off bill.
      const payload = {
        userId: userId,
        type: typeInt,
        category: formData.value.category,
        amount: formData.value.amount,
        billDate: formData.value.date,
        description: formData.value.description || ''
      }
      await request.post('/bills', payload)
      ElMessage.success('Bill added successfully')
    }

    emit('success')
    handleDialogClose()
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('Failed to save bill')
    }
  } finally {
    loading.value = false
  }
}

const handleDialogClose = () => {
  dialogVisible.value = false
  resetForm()
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Recurring toggle row: keep the checkbox aligned with the form labels. */
.recurring-toggle :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}

/* Day-of-month input + hint laid out side-by-side. */
.recurring-day :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.day-input { width: 120px; }
.recurring-hint {
  font-size: 11px;
  color: var(--ash);
  letter-spacing: 0.3px;
}
</style>
