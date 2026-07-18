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

const categoryOptions = computed(() => {
  if (formData.type === 'income') {
    return ['Salary', 'Bonus', 'Freelance', 'Investment', 'Other']
  }
  return ['Food', 'Transport', 'Utilities', 'Shopping', 'Entertainment', 'Other']
})

const formData = ref({
  type: 'expense',
  category: '',
  amount: '',
  date: new Date().toISOString().split('T')[0],
  description: ''
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
  ]
}

const resetForm = () => {
  formData.value = {
    type: 'expense',
    category: '',
    amount: '',
    date: new Date().toISOString().split('T')[0],
    description: ''
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

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    loading.value = true

    const userId = Number(localStorage.getItem('userId'))
    if (!userId) {
      ElMessage.error('Session expired. Please log in again.')
      return
    }

    const payload = {
      userId: userId,
      type: formData.value.type,
      category: formData.value.category,
      amount: formData.value.amount,
      date: formData.value.date,
      description: formData.value.description || ''
    }
    if (isEditMode.value) {
      // Update existing bill
      await request.put(`/bills/${formData.value.id}`, payload)
      ElMessage.success('Bill updated successfully')
    } else {
      // Create new bill
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
</style>
