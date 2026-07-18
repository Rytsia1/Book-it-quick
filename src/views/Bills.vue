<template>
  <div class="page">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <p class="page-eyebrow">BOOKKEEPING / BILLS</p>
        <h1 class="page-title">Transactions</h1>
      </div>
      <button class="btn-primary" @click="handleCreate">+ NEW BILL</button>
    </div>

    <!-- Filter Tabs -->
    <div class="filter-tabs">
      <button
        v-for="filter in filters"
        :key="filter.value"
        :class="['tab', { 'tab--active': activeFilter === filter.value }]"
        @click="activeFilter = filter.value"
      >
        {{ filter.label }}
        <span class="tab-count">{{ getFilterCount(filter.value) }}</span>
      </button>
    </div>

    <!-- Table -->
    <div class="table-panel">
      <el-table
        :data="filteredBills"
        style="width: 100%"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
        :default-sort="{ prop: 'billDate', order: 'descending' }"
      >
        <el-table-column prop="billDate" label="DATE" width="120" sortable />
        <el-table-column label="TYPE" width="120">
          <template #default="{ row }">
            <span :class="['badge', row.type === 1 ? 'badge--income' : 'badge--expense']">
              {{ row.type === 1 ? 'INCOME' : 'EXPENSE' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="CATEGORY" width="150" />
        <el-table-column label="AMOUNT" width="180">
          <template #default="{ row }">
            <span :class="['mono', 'fw-600', row.type === 1 ? 'text-green' : 'text-red']">
              {{ row.type === 1 ? '+' : '-' }}{{ formatCurrency(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="DESCRIPTION" min-width="200" show-overflow-tooltip />
        <el-table-column label="ACTIONS" width="120" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <button class="icon-btn icon-btn--edit" @click="handleEdit(row)" title="Edit">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
              </button>
              <button class="icon-btn icon-btn--delete" @click="handleDelete(row.id)" title="Delete">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                </svg>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="filteredBills.length === 0" description="No transactions found" class="empty" />
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? 'ADD NEW BILL' : 'EDIT BILL'"
      width="480px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
        <el-form-item label="TRANSACTION TYPE" prop="type">
          <el-select v-model="formData.type" placeholder="Select type" size="large" style="width: 100%">
            <el-option label="Income" :value="1" />
            <el-option label="Expense" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="CATEGORY" prop="category">
          <el-select v-model="formData.category" placeholder="Select category" size="large" style="width: 100%">
            <el-option
              v-for="category in availableCategories"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="AMOUNT (USD)" prop="amount">
          <el-input-number
            v-model="formData.amount"
            :min="1"
            :step="10"
            :precision="0"
            style="width: 100%"
            size="large"
          />
        </el-form-item>

        <el-form-item label="DATE" prop="billDate">
          <el-date-picker
            v-model="formData.billDate"
            type="date"
            placeholder="Select date"
            style="width: 100%"
            size="large"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="DESCRIPTION">
          <el-input
            v-model="formData.description"
            type="textarea"
            placeholder="Add a note..."
            :rows="3"
            size="large"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">CANCEL</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ dialogMode === 'create' ? 'ADD BILL' : 'SAVE CHANGES' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const bills         = ref([])
const activeFilter  = ref('all')
const dialogVisible = ref(false)
const dialogMode    = ref('create')
const formRef       = ref(null)
const loading       = ref(false)

const filters = [
  { label: 'ALL',     value: 'all' },
  { label: 'INCOME',  value: 'income' },
  { label: 'EXPENSE', value: 'expense' },
]

const fixedCategories = {
  1: ['Salary', 'Bonus', 'Freelance', 'Investment', 'Other Income'],
  0: ['Food', 'Transport', 'Utilities', 'Shopping', 'Entertainment', 'Other Expense'],
}

const getDefaultCategory = (type) => fixedCategories[type]?.[0] ?? ''

const createEmptyForm = () => ({
  type: 0,
  category: getDefaultCategory(0),
  amount: 1,
  billDate: new Date().toISOString().split('T')[0],
  description: '',
})

const formData = ref(createEmptyForm())

const rules = {
  type:     [{ required: true, message: 'Required', trigger: 'change' }],
  category: [{ required: true, message: 'Required', trigger: 'change' }],
  amount:   [
    { required: true, message: 'Required', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Amount must be greater than 0', trigger: 'blur' },
  ],
  billDate: [{ required: true, message: 'Required', trigger: 'change' }],
}

const availableCategories = computed(() => fixedCategories[formData.value.type] ?? [])

watch(
  () => formData.value.type,
  (type) => {
    if (!availableCategories.value.includes(formData.value.category)) {
      formData.value.category = getDefaultCategory(type)
    }
  }
)

const tableHeaderStyle = {
  background: 'var(--ink)', color: 'var(--ash)',
  borderBottom: '1px solid var(--wire)',
  fontSize: '11px', fontWeight: '600', letterSpacing: '1px',
}
const tableCellStyle = {
  background: 'var(--graphite)', color: 'var(--bone)',
  borderBottom: '1px solid var(--wire)', fontSize: '13px',
}

const filteredBills = computed(() => {
  if (activeFilter.value === 'all')     return bills.value
  if (activeFilter.value === 'income')  return bills.value.filter(b => b.type === 1)
  if (activeFilter.value === 'expense') return bills.value.filter(b => b.type === 0)
  return bills.value
})

const getFilterCount = (v) => {
  if (v === 'all')     return bills.value.length
  if (v === 'income')  return bills.value.filter(b => b.type === 1).length
  if (v === 'expense') return bills.value.filter(b => b.type === 0).length
  return 0
}

const formatCurrency = (amount) => {
  if (amount == null) return '$ 0'
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD',
    minimumFractionDigits: 0, maximumFractionDigits: 0,
  }).format(amount)
}

const fetchBills = async () => {
  try {
    loading.value = true
    const userId = localStorage.getItem('userId')
    const data = await request.get('/bills', { params: { userId } })
    bills.value = Array.isArray(data)
      ? data.sort((a, b) => new Date(b.billDate) - new Date(a.billDate))
      : []
  } catch (e) {
    ElMessage.error('Failed to load bills')
  } finally { loading.value = false }
}

const handleCreate = () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogMode.value = 'edit'
  const nextForm = { ...row }
  if (!fixedCategories[nextForm.type]?.includes(nextForm.category)) {
    nextForm.category = getDefaultCategory(nextForm.type)
  }
  formData.value = nextForm
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('Delete this transaction?', 'Confirm', {
    confirmButtonText: 'DELETE', cancelButtonText: 'Cancel', type: 'warning',
  }).then(async () => {
    try {
      await request.delete(`/bills/${id}`)
      ElMessage.success('Deleted')
      fetchBills()
    } catch (e) { ElMessage.error('Failed to delete') }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    if (dialogMode.value === 'create') {
      const userId = Number(localStorage.getItem('userId'))
      await request.post('/bills', { ...formData.value, userId })
      ElMessage.success('Bill added')
    } else {
      await request.put(`/bills/${formData.value.id}`, formData.value)
      ElMessage.success('Bill updated')
    }
    dialogVisible.value = false
    fetchBills()
  } catch (e) {
    if (e?.message) ElMessage.error('Failed to save')
  } finally { loading.value = false }
}

const resetForm = () => {
  formData.value = createEmptyForm()
  formRef.value?.clearValidate()
}

onMounted(fetchBills)
</script>

<style scoped>
.page {
  padding: 28px 32px;
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100vh;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--wire);
}

.page-eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--ash);
  margin-bottom: 6px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--white);
}

.btn-primary {
  padding: 8px 16px;
  background: var(--ember);
  border: none;
  border-radius: 3px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover { background: var(--spark); }

/* Filter Tabs */
.filter-tabs {
  display: flex;
  align-items: stretch;
  gap: 0;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--wire);
}

.tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--ash);
  font-size: 11px;
  font-weight: 700;
  font-family: var(--font-body);
  letter-spacing: 1px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: -1px;
}

.tab:hover { color: var(--bone); }

.tab--active {
  color: var(--white) !important;
  border-bottom-color: var(--ember) !important;
}

.tab-count {
  padding: 1px 6px;
  background: var(--wire);
  border-radius: 2px;
  font-size: 10px;
  color: var(--muted);
}

.tab--active .tab-count {
  background: rgba(240, 90, 20, 0.15);
  color: var(--ember);
}

/* Table Panel */
.table-panel {
  background: var(--graphite);
  border: 1px solid var(--wire);
  border-radius: 3px;
  overflow: hidden;
}

.table-panel :deep(.el-table) { border-radius: 0 !important; }

/* Badges */
.badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  border-radius: 2px;
}
.badge--income  { background: rgba(34, 197, 94, 0.1);  color: var(--green); }
.badge--expense { background: rgba(239, 68, 68, 0.1);  color: var(--red); }

/* Utilities */
.mono   { font-family: var(--font-mono) !important; }
.fw-600 { font-weight: 600 !important; }
.text-green { color: var(--green) !important; }
.text-red   { color: var(--red) !important; }

/* Action cell */
.actions-cell { display: flex; gap: 6px; }

.icon-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--wire);
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.15s;
}
.icon-btn--edit   { color: var(--muted); }
.icon-btn--edit:hover  { border-color: var(--ember); color: var(--ember); background: rgba(240, 90, 20, 0.06); }
.icon-btn--delete { color: var(--ash); }
.icon-btn--delete:hover { border-color: var(--red); color: var(--red); background: rgba(239, 68, 68, 0.06); }

/* Empty */
.empty { padding: 60px 0; }
.empty :deep(.el-empty__description p) { color: var(--ash); font-size: 12px; }

/* Responsive */
@media (max-width: 768px) {
  .page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 16px; }
  .page-title { font-size: 22px; }
}
@media (max-width: 480px) { .page { padding: 12px; } }
</style>
