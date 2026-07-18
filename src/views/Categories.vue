<template>
  <div class="page">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <p class="page-eyebrow">BOOKKEEPING / CATEGORIES</p>
        <h1 class="page-title">Custom Categories</h1>
        <p class="page-subtitle">Manage the income & expense categories that appear in the bill form.</p>
      </div>
      <div class="page-header__actions">
        <button class="btn-ghost" @click="navigateToBills">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="19" y1="12" x2="5" y2="12"/>
            <polyline points="12 19 5 12 12 5"/>
          </svg>
          BACK TO BILLS
        </button>
        <button class="btn-primary" @click="openCreateDialog">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          ADD CATEGORY
        </button>
      </div>
    </div>

    <!-- Type Tabs -->
    <div class="type-tabs">
      <button
        v-for="tab in typeTabs"
        :key="tab.value"
        :class="['tab', { 'tab--active': activeType === tab.value }]"
        @click="activeType = tab.value"
      >
        {{ tab.label }}
        <span class="tab-count">{{ getCountForType(tab.value) }}</span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <span>Loading categories...</span>
    </div>

    <!-- Custom Categories Table -->
    <div v-else class="table-panel">
      <el-table
        :data="filteredCustomCategories"
        style="width: 100%"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="name" label="CATEGORY NAME" min-width="200">
          <template #default="{ row }">
            <span class="category-name">
              <span :class="['category-tag', row.type === 1 ? 'category-tag--income' : 'category-tag--expense']">
                {{ row.name }}
              </span>
            </span>
          </template>
        </el-table-column>

        <el-table-column label="TYPE" width="140">
          <template #default="{ row }">
            <span :class="['badge', row.type === 1 ? 'badge--income' : 'badge--expense']">
              {{ row.type === 1 ? 'INCOME' : 'EXPENSE' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="CREATED" width="180">
          <template #default="{ row }">
            <span class="date-cell">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="ACTIONS" width="120" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <button class="icon-btn icon-btn--edit" @click="openEditDialog(row)" title="Edit">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
              </button>
              <button class="icon-btn icon-btn--delete" @click="handleDelete(row)" title="Delete">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                </svg>
              </button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--ash)" stroke-width="1.5">
              <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
              <line x1="7" y1="7" x2="7.01" y2="7"/>
            </svg>
            <p>No custom categories yet.</p>
            <p class="empty-sub">Click <strong>ADD CATEGORY</strong> to create your first one.</p>
          </div>
        </template>
      </el-table>
    </div>

    <!-- Default Categories Info Box -->
    <div class="info-box">
      <div class="info-box__icon">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="16" x2="12" y2="12"/>
          <line x1="12" y1="8" x2="12.01" y2="8"/>
        </svg>
      </div>
      <div class="info-box__body">
        <strong>Default categories</strong>
        <p>
          In addition to your custom categories, every user can also pick from these built-in defaults:
          <span class="info-box__list">
            <span v-for="(cat, i) in defaultsForActiveType" :key="`d-${i}`">
              <span :class="['mini-tag', activeType === 1 ? 'mini-tag--income' : 'mini-tag--expense']">{{ cat }}</span>
              <span v-if="i < defaultsForActiveType.length - 1" class="mini-sep">·</span>
            </span>
          </span>
        </p>
      </div>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? 'ADD CATEGORY' : 'EDIT CATEGORY'"
      width="440px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top">
        <el-form-item label="TYPE" prop="type">
          <el-select v-model="formData.type" placeholder="Select type" size="large" style="width: 100%">
            <el-option label="Income"  :value="1" />
            <el-option label="Expense" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="CATEGORY NAME" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="e.g. Coffee, Freelance, Side Hustle"
            size="large"
            maxlength="64"
            show-word-limit
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">CANCEL</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ dialogMode === 'create' ? 'CREATE' : 'SAVE' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  categories as categoriesRef,
  fetchCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  DEFAULT_CATEGORIES,
} from '@/utils/categories'

const router = useRouter()

const activeType = ref(0)             // 0 = expense, 1 = income
const loading    = ref(false)
const saving     = ref(false)

const dialogVisible = ref(false)
const dialogMode    = ref('create')    // 'create' | 'edit'
const formRef       = ref(null)

const createEmptyForm = () => ({
  id:   null,
  type: 0,
  name: '',
})

const formData = ref(createEmptyForm())

const formRules = {
  type: [{ required: true, message: 'Please select a type', trigger: 'change' }],
  name: [
    { required: true, message: 'Category name is required', trigger: 'blur' },
    { min: 2, max: 64, message: 'Name must be 2-64 characters', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) return callback()
        const trimmed = value.trim()
        if (trimmed.length < 2) {
          return callback(new Error('Name must be at least 2 characters'))
        }
        const lower = trimmed.toLowerCase()
        const otherType = formData.value.type === 1 ? 0 : 1
        const section = formData.value.type === 1 ? 'income' : 'expense'

        // 1) Cannot collide with a system default in the SAME type.
        const isDefaultDuplicate = (DEFAULT_CATEGORIES[formData.value.type] ?? [])
          .some(d => d.toLowerCase() === lower)
        if (isDefaultDuplicate) {
          return callback(new Error(`"${trimmed}" is already a default ${section} category`))
        }
        // 2) Cannot collide with a system default in the OPPOSITE type.
        const isOppositeDefault = (DEFAULT_CATEGORIES[otherType] ?? [])
          .some(d => d.toLowerCase() === lower)
        if (isOppositeDefault) {
          const otherSection = otherType === 1 ? 'income' : 'expense'
          return callback(new Error(`"${trimmed}" is a default ${otherSection} category`))
        }
        // 3) Cannot collide with a custom category in the SAME type (skipping self when editing).
        const isCustomDuplicate = categoriesRef.value
          .filter(c => c.type === formData.value.type && c.id !== formData.value.id)
          .some(c => c.name && c.name.toLowerCase() === lower)
        if (isCustomDuplicate) {
          return callback(new Error(`A custom ${section} category with this name already exists`))
        }
        // 4) Cannot collide with a custom category in the OPPOSITE type.
        const isOppositeCustom = categoriesRef.value
          .filter(c => c.type === otherType)
          .some(c => c.name && c.name.toLowerCase() === lower)
        if (isOppositeCustom) {
          const otherSection = otherType === 1 ? 'income' : 'expense'
          return callback(new Error(`A custom ${otherSection} category with this name already exists`))
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const typeTabs = [
  { label: 'EXPENSE', value: 0 },
  { label: 'INCOME',  value: 1 },
]

// Show only the custom (user-defined) categories in the table.
// Default categories are shown in the info box below.
const filteredCustomCategories = computed(() => {
  return categoriesRef.value
    .filter(c => c.type === activeType.value)
    .sort((a, b) => (a.name || '').localeCompare(b.name || ''))
})

const getCountForType = (type) => {
  return categoriesRef.value.filter(c => c.type === type).length
}

const defaultsForActiveType = computed(() => DEFAULT_CATEGORIES[activeType.value] ?? [])

const tableHeaderStyle = {
  background: 'var(--ink)', color: 'var(--ash)',
  borderBottom: '1px solid var(--wire)',
  fontSize: '11px', fontWeight: '600', letterSpacing: '1px',
}
const tableCellStyle = {
  background: 'var(--graphite)', color: 'var(--bone)',
  borderBottom: '1px solid var(--wire)', fontSize: '13px',
}

const formatDate = (value) => {
  if (!value) return '—'
  try {
    return new Date(value).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: '2-digit',
    })
  } catch (e) {
    return value
  }
}

const navigateToBills = () => router.push('/bills')

const openCreateDialog = () => {
  dialogMode.value = 'create'
  formData.value   = createEmptyForm()
  formData.value.type = activeType.value
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  formData.value   = { id: row.id, type: row.type, name: row.name }
  dialogVisible.value = true
}

const resetForm = () => {
  formData.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return // validation errors are already shown
  }

  const userId = Number(localStorage.getItem('userId'))
  if (!userId) {
    ElMessage.error('Session expired. Please log in again.')
    router.push('/login')
    return
  }

  saving.value = true
  try {
    const payload = {
      name: formData.value.name.trim(),
      type: formData.value.type,
    }
    if (dialogMode.value === 'create') {
      const created = await createCategory(userId, payload.type, payload.name)
      if (created) {
        ElMessage.success(`Category "${created.name}" created`)
      } else {
        ElMessage.error('Failed to create category')
        return
      }
    } else {
      const updated = await updateCategory(userId, {
        id:   formData.value.id,
        type: payload.type,
        name: payload.name,
      })
      if (updated) {
        ElMessage.success(`Category "${updated.name}" updated`)
      } else {
        ElMessage.error('Failed to update category')
        return
      }
    }
    dialogVisible.value = false
  } catch (e) {
    const serverMsg = e?.response?.data?.message || e?.message || 'Failed to save category'
    ElMessage.error(serverMsg)
  } finally {
    saving.value = false
  }
}

const handleDelete = (row) => {
  if (!row || !row.id) return
  ElMessageBox.confirm(
    `Delete the custom category "${row.name}"? Existing bills that use this name will not be affected.`,
    'Delete category',
    {
      confirmButtonText: 'DELETE',
      cancelButtonText:  'Cancel',
      type: 'warning',
    }
  ).then(async () => {
    const userId = Number(localStorage.getItem('userId'))
    if (!userId) {
      ElMessage.error('Session expired. Please log in again.')
      return
    }
    const ok = await deleteCategory(userId, row.id)
    if (ok) ElMessage.success('Category deleted')
    else    ElMessage.error('Failed to delete category')
  }).catch(() => {})
}

onMounted(async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('Please log in to manage your categories')
    router.push('/login')
    return
  }
  loading.value = true
  await fetchCategories(Number(userId))
  loading.value = false
})
</script>

<style scoped>
.page {
  padding: 28px 32px;
  max-width: 1100px;
  margin: 0 auto;
  min-height: 100vh;
}

/* ── Header ── */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--wire);
  gap: 16px;
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
  line-height: 1.1;
}

.page-subtitle {
  font-size: 12px;
  color: var(--ash);
  margin-top: 6px;
}

.page-header__actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-shrink: 0;
}

.btn-primary,
.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 12px;
  font-weight: 700;
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary {
  background: var(--ember);
  border: 1px solid var(--ember);
  color: #fff;
}
.btn-primary:hover { background: var(--spark); border-color: var(--spark); }

.btn-ghost {
  background: transparent;
  border: 1px solid var(--wire);
  color: var(--muted);
}
.btn-ghost:hover { border-color: var(--ember); color: var(--ember); background: rgba(240, 90, 20, 0.06); }

/* ── Type Tabs ── */
.type-tabs {
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

/* ── Loading ── */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 80px 20px;
  color: var(--ash);
  font-size: 13px;
  background: var(--graphite);
  border: 1px solid var(--wire);
  border-radius: 3px;
}
.spinner {
  width: 30px; height: 30px;
  border: 3px solid var(--wire);
  border-top-color: var(--ember);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Table Panel ── */
.table-panel {
  background: var(--graphite);
  border: 1px solid var(--wire);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 20px;
}
.table-panel :deep(.el-table) { border-radius: 0 !important; }

/* Category names */
.category-name { display: inline-flex; }
.category-tag {
  display: inline-block;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.6px;
  border-radius: 2px;
  text-transform: uppercase;
}
.category-tag--income  { background: rgba(34, 197, 94, 0.12); color: var(--green); }
.category-tag--expense { background: rgba(239, 68, 68, 0.12); color: var(--red); }

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

.date-cell {
  font-size: 12px;
  color: var(--muted);
  font-family: var(--font-mono);
}

/* ── Empty state inside the table ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 50px 20px;
  color: var(--ash);
  font-size: 13px;
  text-align: center;
}
.empty-state p { margin: 0; }
.empty-sub { font-size: 12px; color: var(--muted); }
.empty-sub strong { color: var(--ember); }

/* ── Info box (default categories) ── */
.info-box {
  display: flex;
  gap: 14px;
  padding: 14px 18px;
  background: var(--slate);
  border: 1px solid var(--wire);
  border-left: 3px solid var(--ember);
  border-radius: 3px;
  color: var(--muted);
}
.info-box__icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(240, 90, 20, 0.1);
  color: var(--ember);
  border-radius: 3px;
}
.info-box__body { flex: 1; min-width: 0; }
.info-box__body strong {
  display: block;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--white);
  margin-bottom: 6px;
}
.info-box__body p {
  font-size: 12px;
  line-height: 1.7;
  margin: 0;
}
.info-box__list {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-left: 4px;
}
.mini-tag {
  display: inline-block;
  padding: 1px 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.6px;
  border-radius: 2px;
  text-transform: uppercase;
}
.mini-tag--income  { background: rgba(34, 197, 94, 0.1);  color: var(--green); }
.mini-tag--expense { background: rgba(239, 68, 68, 0.1);  color: var(--red); }
.mini-sep {
  color: var(--ash);
  margin: 0 2px;
}

/* ── Action buttons ── */
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

/* ── Responsive ── */
@media (max-width: 768px) {
  .page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
  .page-title { font-size: 22px; }
  .page-header__actions { width: 100%; }
  .page-header__actions .btn-ghost,
  .page-header__actions .btn-primary { flex: 1; justify-content: center; }
  .info-box { flex-direction: column; }
}
@media (max-width: 480px) { .page { padding: 12px; } }
</style>
