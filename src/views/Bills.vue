<template>
  <div class="page">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <p class="page-eyebrow">BOOKKEEPING / BILLS</p>
        <h1 class="page-title">Transactions</h1>
      </div>
      <div class="page-header__actions">
        <button class="btn-ghost" @click="navigateToCategories">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
            <line x1="7" y1="7" x2="7.01" y2="7"/>
          </svg>
          MANAGE CATEGORIES
        </button>
        <button class="btn-primary" @click="handleCreate">+ NEW BILL</button>
      </div>
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
        <el-table-column prop="category" label="CATEGORY" width="160">
          <template #default="{ row }">
            <span class="category-name">{{ row.category }}</span>
          </template>
        </el-table-column>
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

    <!-- Pagination (server-driven). Below the table so the controls don't
         compete with the filter tabs. Driven by /api/bills/page?page=&size=&type=. -->
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        :pager-count="5"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Single shared Add/Edit bill pop-up (same window used on the Dashboard too). -->
    <BillDialog
      v-model:visible="dialogVisible"
      :edit-data="editTarget"
      @success="onBillSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import BillDialog from '@/components/BillDialog.vue'

const router = useRouter()

const bills         = ref([])      // the current page of rows (already filtered server-side)
const activeFilter  = ref('all')   // 'all' | 'income' | 'expense'
const dialogVisible = ref(false)
const editTarget    = ref(null)    // null = create mode, object = edit mode

// ── Pagination state ────────────────────────────────────────────────
const currentPage = ref(1)         // 1-based, sent to the server as ?page=
const pageSize    = ref(20)        // rows per page, sent as ?size=
const total       = ref(0)         // total rows matching the current filter, used by <el-pagination :total="...">
const loading     = ref(false)     // shown while a page is being fetched

// Per-filter totals so the tab badges (ALL / INCOME / EXPENSE) stay
// accurate regardless of which page is currently displayed. Populated by
// /api/bills/counts on mount and after every successful mutation.
const tabCounts = ref({ all: 0, income: 0, expense: 0 })

const filters = [
  { label: 'ALL',     value: 'all' },
  { label: 'INCOME',  value: 'income' },
  { label: 'EXPENSE', value: 'expense' },
]

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

// Tab badge counts come from /api/bills/counts, NOT from the current page,
// so the user always sees the real total for each filter even when they're
// on page 7 of 50.
const getFilterCount = (v) => {
  if (v === 'all')     return tabCounts.value.all
  if (v === 'income')  return tabCounts.value.income
  if (v === 'expense') return tabCounts.value.expense
  return 0
}

// Map the active tab to the optional `type` query param the server expects.
// 'all'       → no type filter
// 'income'    → type=1
// 'expense'   → type=0
const typeParamForCurrentFilter = () => {
  if (activeFilter.value === 'income')  return 1
  if (activeFilter.value === 'expense') return 0
  return null
}

const formatCurrency = (amount) => {
  if (amount == null) return '$ 0'
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD',
    minimumFractionDigits: 2, maximumFractionDigits: 2,
  }).format(amount)
}

const navigateToCategories = () => router.push('/categories')

// Fetch a single page of bills from the server, plus refresh the per-filter
// totals. Called on mount, after every mutation, and on every pagination
// event. Skips the request when userId is missing (the watcher in
// onMounted will already have redirected to /login).
const fetchBills = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('Session expired. Please log in again.')
    router.push('/login')
    return
  }
  loading.value = true
  try {
    // The list page and the per-type counts are independent — fire them
    // in parallel so the page paints in one round-trip.
    const type = typeParamForCurrentFilter()
    const [page, counts] = await Promise.all([
      request.get('/bills/page', {
        params: { userId, page: currentPage.value, size: pageSize.value, type },
      }),
      request.get('/bills/counts', { params: { userId } }),
    ])
    bills.value = Array.isArray(page?.items) ? page.items : []
    total.value = page?.total ?? 0
    // Defensive: server returns { all, income, expense } but we only
    // trust the shape we asked for.
    tabCounts.value = {
      all:     Number(counts?.all     ?? 0),
      income:  Number(counts?.income  ?? 0),
      expense: Number(counts?.expense ?? 0),
    }
  } catch (e) {
    // Global axios interceptor will have already handled 401, so any
    // error here is a real failure (network, 500, etc.).
    const serverMsg = e?.response?.data?.message || e?.message || 'Failed to load bills'
    console.error('fetchBills error:', e)
    ElMessage.error(serverMsg)
  } finally {
    loading.value = false
  }
}

// <el-pagination> event handlers.
const handlePageChange = (page) => {
  currentPage.value = page
  fetchBills()
}
const handleSizeChange = (size) => {
  // Page size changed — reset to page 1 so the user doesn't end up on a
  // page that no longer exists (e.g. page 5 of 10 with size=10 → page 5
  // might not exist with size=50).
  pageSize.value = size
  currentPage.value = 1
  fetchBills()
}

// Watch the active filter tab: re-fetch from page 1 whenever it changes.
watch(activeFilter, () => {
  currentPage.value = 1
  fetchBills()
})

const handleCreate = () => {
  editTarget.value = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editTarget.value = { ...row }
  dialogVisible.value = true
}

const onBillSaved = () => {
  dialogVisible.value = false
  fetchBills()
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

onMounted(() => { fetchBills() })
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

.page-header__actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.btn-primary {
  padding: 8px 16px;
  background: var(--ember);
  border: none;
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary:hover { background: var(--spark); }

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: transparent;
  border: 1px solid var(--wire);
  border-radius: var(--radius-sm);
  color: var(--muted);
  font-size: 11px;
  font-weight: 700;
  font-family: var(--font-body);
  letter-spacing: 0.8px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-ghost:hover { border-color: var(--ember); color: var(--ember); background: rgba(240, 90, 20, 0.06); }

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
  border-radius: 6px;
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
  border: none;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.table-panel :deep(.el-table) { border-radius: 0 !important; }

/* Modern table: no vertical grid lines, only soft horizontal hairlines. */
.table-panel :deep(.el-table__cell) {
  border-right: none !important;
}
.table-panel :deep(.el-table__row) td.el-table__cell {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06) !important;
}
.table-panel :deep(th.el-table__cell) {
  border-right: none !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.10) !important;
}
.table-panel :deep(.el-table__inner-wrapper::before),
.table-panel :deep(.el-table__border-left-patch) {
  background: transparent !important;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  border-radius: 6px;
}
.badge--income  { background: rgba(34, 197, 94, 0.1);  color: var(--green); }
.badge--expense { background: rgba(239, 68, 68, 0.1);  color: var(--red); }

/* Plain category name in the table (no badge/tag). */
.category-name {
  color: var(--bone);
  font-size: 13px;
}

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
  border-radius: var(--radius-sm);
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

/* Pagination bar (Element Plus defaults to white-on-light; retheme it
   to match the dark app palette). The :deep() selectors reach into
   <el-pagination>'s generated child elements. */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--graphite);
  border: none;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.pagination-bar :deep(.el-pagination) { color: var(--ash); }
.pagination-bar :deep(.el-pager li),
.pagination-bar :deep(.btn-prev),
.pagination-bar :deep(.btn-next) {
  background: var(--ink) !important;
  color: var(--muted) !important;
  border: 1px solid var(--wire) !important;
  font-size: 12px;
  font-weight: 600;
}
.pagination-bar :deep(.el-pager li.is-active) {
  background: var(--ember) !important;
  color: #fff !important;
  border-color: var(--ember) !important;
}
.pagination-bar :deep(.el-pagination__total),
.pagination-bar :deep(.el-pagination__sizes .el-select .el-input__wrapper) {
  color: var(--ash);
  background: var(--ink);
  box-shadow: 0 0 0 1px var(--wire) inset;
}
.pagination-bar :deep(.el-pagination__jump) { color: var(--ash); }
.pagination-bar :deep(.el-pagination__jump .el-input__inner) {
  background: var(--ink);
  color: var(--bone);
  border-color: var(--wire);
}

/* Responsive */
@media (max-width: 768px) {
  .page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 16px; }
  .page-title { font-size: 22px; }
  .page-header__actions { width: 100%; }
  .page-header__actions .btn-ghost,
  .page-header__actions .btn-primary { flex: 1; justify-content: center; }
}
@media (max-width: 480px) { .page { padding: 12px; } }
</style>
