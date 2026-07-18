<template>
  <div class="page">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Categories</h1>
        <p class="page-subtitle">All categories available in the bill form, including built-in defaults.</p>
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

    <!--
      Single unified grid that shows both:
        - the system defaults (read-only, no edit/delete buttons)
        - the user's custom categories (full CRUD)
      Each card is just an icon + name + actions, like a chip / tile.
    -->
    <div v-else class="card-grid">
      <div
        v-for="item in visibleItems"
        :key="item.key"
        :class="['cat-card', item.isDefault ? 'cat-card--default' : 'cat-card--custom', `cat-card--${activeType === 1 ? 'income' : 'expense'}`]"
      >
        <div class="cat-card__icon">
          <svg v-if="activeType === 1" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="19" x2="12" y2="5"/>
            <polyline points="5 12 12 5 19 12"/>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <polyline points="19 12 12 19 5 12"/>
          </svg>
        </div>
        <div class="cat-card__body">
          <span class="cat-card__name">{{ item.name }}</span>
          <span class="cat-card__tag">{{ item.isDefault ? 'DEFAULT' : 'CUSTOM' }}</span>
        </div>
        <div v-if="!item.isDefault" class="cat-card__actions">
          <button class="icon-btn icon-btn--edit" @click="openEditDialog(item)" title="Edit">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </button>
          <button class="icon-btn icon-btn--delete" @click="handleDelete(item)" title="Delete">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="visibleItems.length === 0" class="empty-state">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--ash)" stroke-width="1.5">
          <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
          <line x1="7" y1="7" x2="7.01" y2="7"/>
        </svg>
        <p>No categories in this section yet.</p>
        <p class="empty-sub">Click <strong>ADD CATEGORY</strong> to create your first one.</p>
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
  // Custom validator (not `required: true`) because the type is a number
  // (0 = expense, 1 = income) and `required` treats `0` as falsy and would
  // always reject the field.
  type: [
    {
      validator: (rule, value, callback) => {
        if (value !== 0 && value !== 1) {
          return callback(new Error('Please select a type'))
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  name: [
    { required: true, message: 'Category name is required', trigger: 'blur' },
    { min: 2, max: 64, message: 'Name must be 2-64 characters', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) return callback()
        const trimmed = String(value).trim()
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

/**
 * Unified list shown in the grid: system defaults first (read-only),
 * then the user's custom categories. Everything is sorted alphabetically.
 */
const visibleItems = computed(() => {
  const defaults = (DEFAULT_CATEGORIES[activeType.value] ?? []).map(name => ({
    key:        `default-${name}`,
    name,
    isDefault:  true,
    id:         null,
    type:       activeType.value,
  }))
  const customs = categoriesRef.value
    .filter(c => c.type === activeType.value)
    .map(c => ({
      key:        `custom-${c.id}`,
      name:       c.name,
      isDefault:  false,
      id:         c.id,
      type:       c.type,
      createdAt:  c.createdAt,
    }))
  // Sort each group alphabetically, then concatenate (defaults first).
  defaults.sort((a, b) => a.name.localeCompare(b.name))
  customs.sort((a, b) => a.name.localeCompare(b.name))
  return [...defaults, ...customs]
})

const getCountForType = (type) => {
  const defaultCount = (DEFAULT_CATEGORIES[type] ?? []).length
  const customCount  = categoriesRef.value.filter(c => c.type === type).length
  return defaultCount + customCount
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  formData.value   = createEmptyForm()
  formData.value.type = activeType.value
  dialogVisible.value = true
}

const openEditDialog = (item) => {
  if (item.isDefault) return            // built-ins can't be edited
  dialogMode.value = 'edit'
  formData.value   = { id: item.id, type: item.type, name: item.name }
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
      // createCategory already calls fetchCategories(userId, true) on
      // success, so the local cache is up-to-date by the time we get here.
      const created = await createCategory(userId, payload.type, payload.name)
      const newName = (created && created.name) || payload.name
      ElMessage.success(`Category "${newName}" created`)
      // Defensive: also trigger a manual refetch in case the cache inside
      // createCategory didn't pick up the new row (e.g. a race condition).
      await fetchCategories(userId, true)
      dialogVisible.value = false
    } else {
      const updated = await updateCategory(userId, {
        id:   formData.value.id,
        type: payload.type,
        name: payload.name,
      })
      const updName = (updated && updated.name) || payload.name
      ElMessage.success(`Category "${updName}" updated`)
      await fetchCategories(userId, true)
      dialogVisible.value = false
    }
  } catch (e) {
    // createCategory / updateCategory now throw an Error whose .message
    // contains the real server-side message (e.g. "Table 'db_bookkeeping.t_category'
    // doesn't exist", "Duplicate entry", etc.). Surface that to the user.
    console.error('handleSubmit error:', e)
    const serverMsg = e?.serverMsg || e?.response?.data?.message || e?.message || 'Failed to save category'
    ElMessage.error(serverMsg, { duration: 6000, showClose: true })
  } finally {
    saving.value = false
  }
}

const handleDelete = (item) => {
  if (!item || item.isDefault || !item.id) return
  ElMessageBox.confirm(
    `Delete the custom category "${item.name}"? Existing bills that use this name will not be affected.`,
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
    try {
      const ok = await deleteCategory(userId, item.id)
      if (ok) ElMessage.success('Category deleted')
    } catch (e) {
      // deleteCategory now throws on failure, so we get the real error here.
      console.error('handleDelete error:', e)
      const serverMsg = e?.serverMsg || e?.response?.data?.message || e?.message || 'Failed to delete category'
      ElMessage.error(serverMsg, { duration: 6000, showClose: true })
    }
  }).catch(() => {})
}

const navigateToBills = () => router.push('/bills')

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
  border-radius: var(--radius-sm);
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
  margin-bottom: 24px;
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
  border-radius: 6px;
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
  border: none;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.spinner {
  width: 30px; height: 30px;
  border: 3px solid var(--wire);
  border-top-color: var(--ember);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Card Grid ── */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.cat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--graphite);
  border: 1px solid var(--wire);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  transition: all 0.15s;
  position: relative;
}
.cat-card:hover {
  border-color: var(--muted);
  background: var(--slate);
}

.cat-card__icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
}
.cat-card--income .cat-card__icon {
  background: rgba(34, 197, 94, 0.12);
  color: var(--green);
}
.cat-card--expense .cat-card__icon {
  background: rgba(239, 68, 68, 0.12);
  color: var(--red);
}

.cat-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.cat-card__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--white);
  font-family: var(--font-body);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cat-card__tag {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: var(--muted);
  text-transform: uppercase;
}
.cat-card--custom .cat-card__tag { color: var(--ember); }

.cat-card__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  max-width: 0;            /* claim no flex space when hidden */
  opacity: 0;
  transform: translateX(10px);
  overflow: hidden;        /* clip the slid-out buttons cleanly */
  transition: max-width 0.2s ease, opacity 0.18s ease, transform 0.2s ease;
}

.cat-card:hover .cat-card__actions,
.cat-card:focus-within .cat-card__actions {
  max-width: 80px;         /* enough for two 28px buttons + 4px gap + buffer */
  opacity: 1;
  transform: translateX(0);
}

/* ── Action buttons ── */
.icon-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--wire);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
}
.icon-btn--edit   { color: var(--muted); }
.icon-btn--edit:hover  { border-color: var(--ember); color: var(--ember); background: rgba(240, 90, 20, 0.06); }
.icon-btn--delete { color: var(--ash); }
.icon-btn--delete:hover { border-color: var(--red); color: var(--red); background: rgba(239, 68, 68, 0.06); }

/* ── Empty state inside the grid ── */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 60px 20px;
  color: var(--ash);
  font-size: 13px;
  text-align: center;
}
.empty-state p { margin: 0; }
.empty-sub { font-size: 12px; color: var(--muted); }
.empty-sub strong { color: var(--ember); }

/* ── Responsive ── */
@media (max-width: 768px) {
  .page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
  .page-title { font-size: 22px; }
  .page-header__actions { width: 100%; }
  .page-header__actions .btn-ghost,
  .page-header__actions .btn-primary { flex: 1; justify-content: center; }
  .card-grid { grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); }
}
@media (max-width: 480px) {
  .page { padding: 12px; }
  .card-grid { grid-template-columns: 1fr; }
}
</style>
