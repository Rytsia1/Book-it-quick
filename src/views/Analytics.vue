<template>
  <div class="analytics-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">Financial Analytics</h1>
        <p class="page-subtitle">Click a pie chart slice to view transaction details</p>
      </div>

      <!-- Month Navigator -->
      <div class="month-nav">
        <button class="month-btn" @click="prevMonth">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="month-label">{{ MONTHS[selectedMonth - 1] }} {{ selectedYear }}</span>
        <button class="month-btn" @click="nextMonth" :disabled="isCurrentMonth">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
        <button class="refresh-btn" @click="refreshAnalytics" :class="{ loading: loading }">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="23 4 23 10 17 10"/>
            <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/>
          </svg>
          Refresh
        </button>
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-section">
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-icon income-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="19" x2="12" y2="5"/><polyline points="5 12 12 5 19 12"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-label">Total Income</span>
            <span class="stat-value income-value">{{ formatCurrency(stats.income) }}</span>
            <span class="stat-trend">
              <span class="trend-badge income-badge">{{ incomePercentage }}%</span>
              of total
            </span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon expense-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-label">Total Expenses</span>
            <span class="stat-value expense-value">{{ formatCurrency(stats.expense) }}</span>
            <span class="stat-trend">
              <span class="trend-badge expense-badge">{{ expensePercentage }}%</span>
              of total
            </span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon balance-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-label">Remaining Balance</span>
            <span class="stat-value" :class="stats.balance >= 0 ? 'balance-positive' : 'balance-negative'">
              {{ formatCurrency(stats.balance) }}
            </span>
            <span class="stat-trend">
              <span :class="['trend-badge', stats.balance >= 0 ? 'surplus-badge' : 'deficit-badge']">
                {{ stats.balance >= 0 ? 'Surplus' : 'Deficit' }}
              </span>
              financial status
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Section -->
    <div class="charts-section">
      <div class="charts-grid">
        <!-- Expense Distribution (clickable) -->
        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">Expense Distribution</h3>
              <span class="chart-subtitle">By category · click a slice for details</span>
            </div>
            <span class="click-hint">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5"/>
              </svg>
              Click slice
            </span>
          </div>
          <div class="chart-body">
            <PieChart
              :data="categoryData"
              title=""
              :clickable="true"
              @sliceClick="handleCategoryClick"
            />
          </div>
        </div>

        <!-- Income vs Expense (clickable) -->
        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">Transaction Summary</h3>
              <span class="chart-subtitle">Income vs Expenses · click a slice for details</span>
            </div>
            <span class="click-hint">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5"/>
              </svg>
              Click slice
            </span>
          </div>
          <div class="chart-body">
            <PieChart :data="typeData" title="" :clickable="true" @sliceClick="handleTypeClick" />
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!hasData" class="empty-state">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#475569" stroke-width="1.5">
        <path d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        <path d="M9 10h.01M15 10h.01M9.5 15.5a3.5 3.5 0 015 0"/>
      </svg>
      <h3 class="empty-title">No Data Yet</h3>
      <p class="empty-text">Start adding bills to see your financial analytics</p>
    </div>

    <!-- ── Category Transactions Drawer ── -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      :size="drawerSize"
      class="dark-drawer"
      :before-close="closeDrawer"
    >
      <template #header>
        <div class="drawer-header">
          <div class="drawer-title-group">
            <span class="drawer-category-badge" :style="{ background: drawerBadgeColor }">
              {{ selectedCategory }}
            </span>
            <span class="drawer-period">{{ MONTHS[selectedMonth - 1] }} {{ selectedYear }}</span>
          </div>
          <div class="drawer-summary">
            <span class="drawer-count">{{ categoryTransactions.length }} transactions</span>
            <span class="drawer-total" :class="isIncomeMode ? 'drawer-total--income' : 'drawer-total--expense'">
              {{ isIncomeMode ? '+' : '-' }}{{ formatCurrency(categoryTotal) }}
            </span>
          </div>
        </div>
      </template>

      <!-- Loading -->
      <div v-if="drawerLoading" class="drawer-loading">
        <div class="spinner"></div>
        <span>Loading transactions...</span>
      </div>

      <!-- Transaction List -->
      <div v-else-if="categoryTransactions.length > 0" class="transaction-list">
        <div
          v-for="(tx, index) in categoryTransactions"
          :key="tx.id"
          class="tx-item"
          :style="{ animationDelay: `${index * 40}ms` }"
        >
          <div class="tx-date-col">
            <span class="tx-day">{{ formatDay(tx.billDate) }}</span>
            <span class="tx-month-short">{{ formatMonthShort(tx.billDate) }}</span>
          </div>
          <div class="tx-info">
            <span class="tx-desc">{{ tx.description || '(No description)' }}</span>
            <span class="tx-category-tag">{{ tx.category }}</span>
          </div>
          <span class="tx-amount" :class="isIncomeMode ? 'tx-amount--income' : 'tx-amount--expense'">
            {{ isIncomeMode ? '+' : '-' }}{{ formatCurrency(tx.amount) }}
          </span>
        </div>
      </div>

      <!-- Empty -->
      <div v-else class="drawer-empty">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#475569" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/>
        </svg>
        <p>{{ drawerEmptyText }}</p>
      </div>
    </el-drawer>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import PieChart from '@/components/PieChart.vue'
import request from '@/utils/request'

// ── Month state ──
const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June',
                 'July', 'August', 'September', 'October', 'November', 'December']
const today = new Date()
const selectedMonth = ref(today.getMonth() + 1)
const selectedYear  = ref(today.getFullYear())

const isCurrentMonth = computed(() =>
  selectedMonth.value === today.getMonth() + 1 &&
  selectedYear.value  === today.getFullYear()
)

const prevMonth = () => {
  if (selectedMonth.value === 1) { selectedMonth.value = 12; selectedYear.value-- }
  else selectedMonth.value--
  fetchAnalytics()
}

const nextMonth = () => {
  if (isCurrentMonth.value) return
  if (selectedMonth.value === 12) { selectedMonth.value = 1; selectedYear.value++ }
  else selectedMonth.value++
  fetchAnalytics()
}

// ── Stats state ──
const stats = ref({ income: 0, expense: 0, balance: 0 })
const categoryData = ref([])
const typeData = ref([])
const loading = ref(false)

const hasData = computed(() => stats.value.income > 0 || stats.value.expense > 0)

const incomePercentage = computed(() => {
  const total = stats.value.income + stats.value.expense
  return total > 0 ? Math.round((stats.value.income / total) * 100) : 0
})

const expensePercentage = computed(() => {
  const total = stats.value.income + stats.value.expense
  return total > 0 ? Math.round((stats.value.expense / total) * 100) : 0
})

// ── Drawer state ──
const drawerVisible        = ref(false)
const drawerLoading        = ref(false)
const drawerMode           = ref('category') // 'category' | 'type'
const selectedCategory     = ref('')
const categoryTransactions = ref([])

const PALETTE = ['#F05A14','#22C55E','#3B82F6','#F59E0B','#A855F7','#EC4899','#14B8A6','#FF7A3D','#6366F1','#EF4444']

// Badge color: orange palette for categories, green/red for transaction type
const drawerBadgeColor = computed(() => {
  if (drawerMode.value === 'type') {
    return selectedCategory.value === 'Income' ? '#22C55E' : '#EF4444'
  }
  const idx = categoryData.value.findIndex(c => c.name === selectedCategory.value)
  return PALETTE[idx % PALETTE.length] ?? '#F05A14'
})

// Whether the drawer is currently showing income transactions.
const isIncomeMode = computed(() =>
  drawerMode.value === 'type' && selectedCategory.value === 'Income'
)

const categoryTotal = computed(() =>
  categoryTransactions.value.reduce((sum, tx) => sum + Number(tx.amount), 0)
)

const drawerEmptyText = computed(() => {
  if (isIncomeMode.value) return 'No income transactions this month'
  if (drawerMode.value === 'type') return 'No expense transactions this month'
  return 'No transactions in this category'
})

// Responsive drawer size
const drawerSize = computed(() => window.innerWidth <= 600 ? '100%' : '460px')

// ── Helpers ──
const formatCurrency = (amount) => {
  if (amount == null) return '$ 0'
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD',
    minimumFractionDigits: 0, maximumFractionDigits: 0,
  }).format(amount)
}

const formatDay = (dateStr) => {
  if (!dateStr) return '--'
  return new Date(dateStr).getDate().toString().padStart(2, '0')
}

const formatMonthShort = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short' })
}

// ── API calls ──
const fetchSummary = async () => {
  try {
    loading.value = true
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/summary', {
      params: { userId, month: selectedMonth.value, year: selectedYear.value }
    })
    stats.value = {
      income:  data.totalIncome  || 0,
      expense: data.totalExpense || 0,
      balance: data.balance      || 0,
    }
  } catch (e) {
    ElMessage.error('Failed to load summary')
  } finally {
    loading.value = false
  }
}

const fetchCategoryData = async () => {
  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/categories', {
      params: { userId, month: selectedMonth.value, year: selectedYear.value }
    })
    categoryData.value = Array.isArray(data)
      ? data.map(item => ({ name: item.name || item.category, value: item.value || item.amount }))
      : []
  } catch (e) {
    ElMessage.error('Failed to load category data')
    categoryData.value = []
  }
}

const fetchTypeData = () => {
  typeData.value = [
    { name: 'Income',   value: stats.value.income  },
    { name: 'Expenses', value: stats.value.expense },
  ].filter(item => item.value > 0)
}

const fetchAnalytics = async () => {
  await Promise.all([fetchSummary(), fetchCategoryData()])
  fetchTypeData()
}

// ── Chart click handlers ──

// Click handler for "Expense Distribution" slices by category.
const handleCategoryClick = async (categoryName) => {
  selectedCategory.value     = categoryName
  drawerMode.value           = 'category'
  drawerVisible.value        = true
  drawerLoading.value        = true
  categoryTransactions.value = []

  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/transactions', {
      params: { userId, category: categoryName, month: selectedMonth.value, year: selectedYear.value }
    })
    categoryTransactions.value = Array.isArray(data) ? data : []
  } catch (e) {
    ElMessage.error('Failed to load transaction details')
  } finally {
    drawerLoading.value = false
  }
}

// Click handler for "Transaction Summary" slices by transaction type.
const handleTypeClick = async (typeName) => {
  if (typeName === 'No data') return
  const typeValue = typeName === 'Income' ? 1 : 0

  selectedCategory.value     = typeName
  drawerMode.value           = 'type'
  drawerVisible.value        = true
  drawerLoading.value        = true
  categoryTransactions.value = []

  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/bills-by-type', {
      params: { userId, type: typeValue, month: selectedMonth.value, year: selectedYear.value }
    })
    categoryTransactions.value = Array.isArray(data) ? data : []
  } catch (e) {
    ElMessage.error('Failed to load transactions')
  } finally {
    drawerLoading.value = false
  }
}

const closeDrawer = () => {
  drawerVisible.value        = false
  categoryTransactions.value = []
  drawerMode.value           = 'category'
}

const refreshAnalytics = async () => {
  ElMessage.info('Refreshing data...')
  await fetchAnalytics()
  ElMessage.success('Data refreshed successfully')
}

onMounted(fetchAnalytics)

defineExpose({ refreshAnalytics })
</script>

<style scoped>
.analytics-container {
  padding: 32px;
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100vh;
}

/* ── Page Header ── */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 4px 0 0;
}

/* ── Month Navigator ── */
.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.month-label {
  font-size: 15px;
  font-weight: 600;
  color: #e2e8f0;
  min-width: 140px;
  text-align: center;
}

.month-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a24;
  border: 1px solid #2a2a3a;
  border-radius: 8px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.month-btn:hover:not(:disabled) {
  border-color: var(--ember);
  color: var(--ember);
  background: rgba(240,90,20,0.08);
}

.month-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #1a1a24;
  border: 1px solid #2a2a3a;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 4px;
}

.refresh-btn:hover { border-color: var(--ember); color: var(--ember); }
.refresh-btn.loading svg { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Summary Cards ── */
.summary-section { margin-bottom: 32px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  background: #1a1a24;
  border: 1px solid #2a2a3a;
  border-radius: 14px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  border-color: #3a3a4a;
  box-shadow: 0 8px 30px rgba(0,0,0,0.3);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.income-icon  { background: rgba(16,185,129,0.12); color: #10b981; }
.expense-icon { background: rgba(239,68,68,0.12);  color: #ef4444; }
.balance-icon { background: rgba(240,90,20,0.12); color: var(--ember); }

.stat-info { display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: 13px; color: #64748b; font-weight: 500; }
.stat-value { font-size: 24px; font-weight: 700; font-family: 'JetBrains Mono', monospace; }
.income-value    { color: #10b981; }
.expense-value   { color: #ef4444; }
.balance-positive { color: #10b981; }
.balance-negative { color: #ef4444; }

.stat-trend { font-size: 12px; color: #64748b; display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.trend-badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.income-badge  { background: rgba(16,185,129,0.12); color: #10b981; }
.expense-badge { background: rgba(239,68,68,0.12);  color: #ef4444; }
.surplus-badge { background: rgba(16,185,129,0.12); color: #10b981; }
.deficit-badge { background: rgba(239,68,68,0.12);  color: #ef4444; }

/* ── Charts ── */
.charts-section { margin-bottom: 32px; }

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.chart-card {
  background: #1a1a24;
  border: 1px solid #2a2a3a;
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.2s;
}

.chart-header {
  padding: 20px 24px 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.chart-title { font-size: 16px; font-weight: 600; color: #f1f5f9; margin: 0; }
.chart-subtitle { font-size: 13px; color: #64748b; }

.click-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--ember);
  background: rgba(240,90,20,0.08);
  border: 1px solid rgba(240,90,20,0.2);
  padding: 4px 10px;
  border-radius: 20px;
  white-space: nowrap;
  animation: pulse-hint 2.5s ease-in-out infinite;
}

@keyframes pulse-hint {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.55; }
}

.chart-body { padding: 12px 24px 24px; }

/* ── Empty State ── */
.empty-state {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 80px 20px; text-align: center;
}
.empty-title { font-size: 18px; font-weight: 600; color: #94a3b8; margin: 20px 0 8px; }
.empty-text  { font-size: 14px; color: #64748b; margin: 0; }

/* ── Drawer ── */
.dark-drawer :deep(.el-drawer) {
  background: #13131b;
  border-left: 1px solid #2a2a3a;
}

.dark-drawer :deep(.el-drawer__header) {
  margin: 0;
  padding: 20px 24px;
  border-bottom: 1px solid #2a2a3a;
}

.dark-drawer :deep(.el-drawer__body) {
  padding: 0;
  overflow-y: auto;
}

.dark-drawer :deep(.el-drawer__close-btn) {
  color: #64748b;
}
.dark-drawer :deep(.el-drawer__close-btn:hover) {
  color: #f1f5f9;
}

.drawer-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.drawer-title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-category-badge {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  padding: 4px 14px;
  border-radius: 20px;
  letter-spacing: 0.3px;
}

.drawer-period {
  font-size: 13px;
  color: #64748b;
}

.drawer-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-count {
  font-size: 13px;
  color: #64748b;
}

.drawer-total {
  font-size: 20px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}
.drawer-total--expense { color: var(--red); }
.drawer-total--income  { color: var(--green); }

/* ── Loading ── */
.drawer-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px 20px;
  color: #64748b;
  font-size: 14px;
}

.spinner {
  width: 32px; height: 32px;
  border: 3px solid var(--wire);
  border-top-color: var(--ember);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* ── Transaction List ── */
.transaction-list {
  padding: 8px 0;
}

.tx-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 24px;
  border-bottom: 1px solid #1e1e2e;
  transition: background 0.15s;
  animation: fadeSlideIn 0.3s ease both;
}

.tx-item:hover { background: rgba(255,255,255,0.03); }
.tx-item:last-child { border-bottom: none; }

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateX(12px); }
  to   { opacity: 1; transform: translateX(0); }
}

.tx-date-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 36px;
  flex-shrink: 0;
}

.tx-day {
  font-size: 18px;
  font-weight: 700;
  color: #e2e8f0;
  line-height: 1;
}

.tx-month-short {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tx-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.tx-desc {
  font-size: 14px;
  color: #e2e8f0;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tx-category-tag {
  font-size: 11px;
  color: var(--ember);
  background: rgba(240,90,20,0.1);
  padding: 2px 8px;
  border-radius: 2px;
  align-self: flex-start;
}

.tx-amount {
  font-size: 14px;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  flex-shrink: 0;
}
.tx-amount--expense { color: var(--red); }
.tx-amount--income  { color: var(--green); }

/* ── Drawer empty ── */
.drawer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 20px;
  color: #64748b;
  font-size: 14px;
  text-align: center;
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .stat-grid .stat-card:last-child { grid-column: 1 / -1; }
}

@media (max-width: 768px) {
  .analytics-container { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
  .stat-grid { grid-template-columns: 1fr; }
  .stat-grid .stat-card:last-child { grid-column: auto; }
  .charts-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 20px; }
  .stat-value { font-size: 20px; }
  .month-label { min-width: 110px; font-size: 14px; }
}

@media (max-width: 480px) {
  .analytics-container { padding: 12px; }
  .stat-card { padding: 16px; }
  .stat-icon { width: 40px; height: 40px; }
  .stat-value { font-size: 18px; }
}
</style>
