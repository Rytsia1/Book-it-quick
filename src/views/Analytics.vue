<template>
  <div class="analytics-container">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">Financial Analytics</h1>
        <p class="page-subtitle">Click a pie chart slice to view transaction details</p>
      </div>
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

    <div class="summary-section">
      <div class="stat-grid">
        <div class="stat-card stat-card--clickable" @click="openStatDrawer('income')" role="button" tabindex="0">
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
          <span class="stat-card-arrow" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
          </span>
        </div>

        <div class="stat-card stat-card--clickable" @click="openStatDrawer('expense')" role="button" tabindex="0">
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
          <span class="stat-card-arrow" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
          </span>
        </div>

        <div class="stat-card stat-card--clickable" @click="openStatDrawer('all')" role="button" tabindex="0">
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
          <span class="stat-card-arrow" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
          </span>
        </div>
      </div>
    </div>

    <div class="charts-section">
      <div class="charts-grid">
        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">Income Distribution</h3>
              <span class="chart-subtitle">By category · click a slice for details</span>
            </div>
          </div>
          <div class="chart-body">
            <PieChart
              :data="incomeCategoryData"
              title=""
              :clickable="true"
              :palette="INCOME_PALETTE"
              @sliceClick="(name) => handleCategoryClick(name, 'income')"
            />
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">Expense Distribution</h3>
              <span class="chart-subtitle">By category · click a slice for details</span>
            </div>
          </div>
          <div class="chart-body">
            <PieChart
              :data="categoryData"
              title=""
              :clickable="true"
              :palette="EXPENSE_PALETTE"
              @sliceClick="handleCategoryClick"
            />
          </div>
        </div>
      </div>

      <div class="charts-grid charts-grid--row">
        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">Transaction Summary</h3>
              <span class="chart-subtitle">Income vs Expenses · click a slice for details</span>
            </div>
          </div>
          <div class="chart-body">
            <PieChart :data="typeData" title="" :clickable="true" @sliceClick="handleTypeClick" />
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <div>
              <h3 class="chart-title">Income vs Expenses Trend</h3>
              <span class="chart-subtitle">Daily totals · {{ MONTHS[selectedMonth - 1] }} {{ selectedYear }}</span>
            </div>
          </div>
          <div class="chart-body">
            <LineChart :data="dailyData" />
          </div>
        </div>
      </div>
    </div>

    <div v-if="!hasData" class="empty-state">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#475569" stroke-width="1.5">
        <path d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        <path d="M9 10h.01M15 10h.01M9.5 15.5a3.5 3.5 0 015 0"/>
      </svg>
      <h3 class="empty-title">No Data Yet</h3>
      <p class="empty-text">Start adding bills to see your financial analytics</p>
    </div>

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
            <span
              class="drawer-total"
              :class="isIncomeMode === null
                ? (categoryTotal >= 0 ? 'drawer-total--income' : 'drawer-total--expense')
                : (isIncomeMode ? 'drawer-total--income' : 'drawer-total--expense')"
            >
              {{ isIncomeMode === null ? (categoryTotal >= 0 ? '+' : '-') : (isIncomeMode ? '+' : '-') }}{{ formatCurrency(Math.abs(categoryTotal)) }}
            </span>
          </div>
        </div>
      </template>

      <div v-if="drawerLoading" class="drawer-loading">
        <div class="spinner"></div>
        <span>Loading transactions...</span>
      </div>

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
            <span class="tx-category-tag">{{ tx.category }}</span>
            <span class="tx-desc">{{ tx.description || '(No description)' }}</span>
          </div>

          <span class="tx-amount" :class="txAmountClass(tx)">
            {{ txAmountPrefix(tx) }}{{ formatCurrency(tx.amount) }}
          </span>
        </div>
      </div>

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
import LineChart from '@/components/LineChart.vue'
import request from '@/utils/request'

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

const stats = ref({ income: 0, expense: 0, balance: 0 })
const categoryData = ref([])
const incomeCategoryData = ref([])
const typeData = ref([])
const dailyData = ref([])
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

// Two distinct color palettes so the same category on the Income and Expense pie
// charts never shares a color shade. Cool tones for Income; warm tones for Expense.
const INCOME_PALETTE = [
  '#14B8A6', '#22C55E', '#84CC16', '#6366F1', '#10B981',
  '#06B6D4', '#84CC16', '#A855F7', '#3B82F6', '#FBBF24',
]
const EXPENSE_PALETTE = [
  '#F05A14', '#EF4444', '#F97316', '#DC2626', '#F59E0B',
  '#EC4899', '#FB923C', '#A855F7', '#E11D48', '#FF7A3D',
]

const drawerVisible        = ref(false)
const drawerLoading        = ref(false)
const drawerMode           = ref('category')
const selectedCategory     = ref('')
const selectedTxType       = ref(0)
const categoryTransactions = ref([])

const drawerBadgeColor = computed(() => {
  if (drawerMode.value === 'type') {
    return selectedCategory.value === 'Income' ? '#22C55E' : '#EF4444'
  }
  if (drawerMode.value === 'stat') {
    if (selectedCategory.value === 'All Transactions') return '#F05A14'
    return selectedCategory.value === 'Income' ? '#22C55E' : '#EF4444'
  }
  const palette = selectedTxType.value === 1 ? INCOME_PALETTE : EXPENSE_PALETTE
  const list    = selectedTxType.value === 1 ? incomeCategoryData.value : categoryData.value
  const idx = list.findIndex(c => c.name === selectedCategory.value)
  const fallback = selectedTxType.value === 1 ? '#22C55E' : '#F05A14'
  return idx >= 0 ? palette[idx % palette.length] : fallback
})

const isIncomeMode = computed(() => {
  if (drawerMode.value === 'stat') {
    if (selectedCategory.value === 'All Transactions') return null
    return selectedCategory.value === 'Income'
  }
  return selectedTxType.value === 1
})

const categoryTotal = computed(() => {
  if (drawerMode.value === 'stat' && selectedCategory.value === 'All Transactions') {
    return categoryTransactions.value.reduce(
      (sum, tx) => sum + (Number(tx.type) === 1 ? Number(tx.amount) : -Number(tx.amount)),
      0
    )
  }
  return categoryTransactions.value.reduce((sum, tx) => sum + Number(tx.amount), 0)
})

const drawerEmptyText = computed(() => {
  if (drawerMode.value === 'stat') {
    if (selectedCategory.value === 'All Transactions') return 'No transactions this month'
    if (selectedCategory.value === 'Income')           return 'No income transactions this month'
    if (selectedCategory.value === 'Expenses')         return 'No expense transactions this month'
  }
  if (isIncomeMode.value) return 'No income transactions this month'
  if (drawerMode.value === 'type') return 'No expense transactions this month'
  return 'No transactions in this category'
})

const txAmountClass = (tx) => {
  if (isIncomeMode.value === null) {
    return Number(tx.type) === 1 ? 'tx-amount--income' : 'tx-amount--expense'
  }
  return isIncomeMode.value ? 'tx-amount--income' : 'tx-amount--expense'
}

const txAmountPrefix = (tx) => {
  if (isIncomeMode.value === null) {
    return Number(tx.type) === 1 ? '+' : '-'
  }
  return isIncomeMode.value ? '+' : '-'
}

const drawerSize = computed(() => window.innerWidth <= 600 ? '100%' : '460px')

const formatCurrency = (amount) => {
  if (amount == null) return '$ 0'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)
}

const formatDay = (dateStr) => {
  if (!dateStr) return '--'
  return new Date(dateStr).getDate().toString().padStart(2, '0')
}

const formatMonthShort = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short' })
}

const fetchSummary = async () => {
  try {
    loading.value = true
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/summary', { params: { userId, month: selectedMonth.value, year: selectedYear.value } })
    stats.value = { income: data.totalIncome || 0, expense: data.totalExpense || 0, balance: data.balance || 0 }
  } catch (e) { ElMessage.error('Failed to load summary') } finally { loading.value = false }
}

const fetchCategoryData = async () => {
  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/categories', { params: { userId, month: selectedMonth.value, year: selectedYear.value } })
    categoryData.value = Array.isArray(data) ? data.map(item => ({ name: item.name || item.category, value: item.value || item.amount })) : []
  } catch (e) { ElMessage.error('Failed to load category data'); categoryData.value = [] }
}

const fetchIncomeCategoryData = async () => {
  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/income-categories', { params: { userId, month: selectedMonth.value, year: selectedYear.value } })
    incomeCategoryData.value = Array.isArray(data) ? data.map(item => ({ name: item.name || item.category, value: item.value || item.amount })) : []
  } catch (e) { ElMessage.error('Failed to load income category data'); incomeCategoryData.value = [] }
}

const fetchDailyData = async () => {
  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/daily', { params: { userId, month: selectedMonth.value, year: selectedYear.value } })
    dailyData.value = Array.isArray(data) ? data.map(item => ({
      date: typeof item.date === 'string' ? item.date : new Date(item.date).toISOString().slice(0, 10),
      income: Number(item.income ?? 0),
      expense: Number(item.expense ?? 0),
    })) : []
  } catch (e) { ElMessage.error('Failed to load daily trend data'); dailyData.value = [] }
}

const fetchTypeData = () => {
  typeData.value = [{ name: 'Income', value: stats.value.income }, { name: 'Expenses', value: stats.value.expense }].filter(item => item.value > 0)
}

const fetchAnalytics = async () => {
  await Promise.all([fetchSummary(), fetchCategoryData(), fetchIncomeCategoryData(), fetchDailyData()])
  fetchTypeData()
}

const handleCategoryClick = async (categoryName, type = 'expense') => {
  const typeValue = type === 'income' ? 1 : 0
  selectedCategory.value = categoryName
  selectedTxType.value = typeValue
  drawerMode.value = 'category'
  drawerVisible.value = true
  drawerLoading.value = true
  categoryTransactions.value = []
  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/transactions', { params: { userId, category: categoryName, type: typeValue, month: selectedMonth.value, year: selectedYear.value } })
    categoryTransactions.value = Array.isArray(data) ? data : []
  } catch (e) { ElMessage.error('Failed to load transaction details') } finally { drawerLoading.value = false }
}

const openStatDrawer = async (kind) => {
  drawerMode.value = 'stat'
  drawerLoading.value = true
  categoryTransactions.value = []
  if (kind === 'income') { selectedCategory.value = 'Income'; selectedTxType.value = 1 }
  else if (kind === 'expense') { selectedCategory.value = 'Expenses'; selectedTxType.value = 0 }
  else { selectedCategory.value = 'All Transactions'; selectedTxType.value = 0 }
  drawerVisible.value = true
  try {
    const userId = localStorage.getItem('userId')
    if (kind === 'all') {
      const data = await request.get('/stats/all-bills', { params: { userId, month: selectedMonth.value, year: selectedYear.value } })
      categoryTransactions.value = Array.isArray(data) ? data : []
    } else {
      const typeValue = kind === 'income' ? 1 : 0
      const data = await request.get('/stats/bills-by-type', { params: { userId, type: typeValue, month: selectedMonth.value, year: selectedYear.value } })
      categoryTransactions.value = Array.isArray(data) ? data : []
    }
  } catch (e) { ElMessage.error('Failed to load transactions') } finally { drawerLoading.value = false }
}

const handleTypeClick = async (typeName) => {
  if (typeName === 'No data') return
  const typeValue = typeName === 'Income' ? 1 : 0
  selectedCategory.value = typeName
  selectedTxType.value = typeValue
  drawerMode.value = 'type'
  drawerVisible.value = true
  drawerLoading.value = true
  categoryTransactions.value = []
  try {
    const userId = localStorage.getItem('userId')
    const data = await request.get('/stats/bills-by-type', { params: { userId, type: typeValue, month: selectedMonth.value, year: selectedYear.value } })
    categoryTransactions.value = Array.isArray(data) ? data : []
  } catch (e) { ElMessage.error('Failed to load transactions') } finally { drawerLoading.value = false }
}

const closeDrawer = () => {
  drawerVisible.value = false
  categoryTransactions.value = []
  drawerMode.value = 'category'
  selectedTxType.value = 0
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
.analytics-container { padding: 32px; min-height: 100vh; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; flex-wrap: wrap; gap: 16px; }
.page-title { font-size: 24px; font-weight: 700; color: var(--bone); margin: 0; }
.page-subtitle { font-size: 14px; color: var(--muted); margin: 4px 0 0; }
.month-nav { display: flex; align-items: center; gap: 8px; }
.month-label { font-size: 15px; font-weight: 600; color: var(--bone); min-width: 140px; text-align: center; }
.month-btn { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; background: var(--graphite); border: 1px solid var(--wire); border-radius: 8px; color: var(--muted); cursor: pointer; transition: all 0.2s; }
.month-btn:hover:not(:disabled) { border-color: var(--ember); color: var(--ember); background: rgba(240,90,20,0.08); }
.month-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.refresh-btn { display: flex; align-items: center; gap: 6px; padding: 8px 14px; background: var(--graphite); border: 1px solid var(--wire); border-radius: 8px; color: var(--muted); font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s; margin-left: 4px; }
.refresh-btn:hover { border-color: var(--ember); color: var(--ember); }
.refresh-btn.loading svg { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.summary-section { margin-bottom: 32px; }
.stat-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.stat-card { display: flex; align-items: flex-start; gap: 16px; padding: 24px; background: var(--graphite); border: 1px solid var(--wire); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); transition: all 0.3s ease; position: relative; }
.stat-card:hover { border-color: var(--slate); box-shadow: var(--shadow-elev); transform: translateY(-2px); }
.stat-card--clickable { cursor: pointer; outline: none; user-select: none; }
.stat-card--clickable:hover { border-color: var(--ember); box-shadow: var(--shadow-elev); }
.stat-card--clickable:focus-visible { border-color: var(--ember); box-shadow: 0 0 0 3px rgba(240, 90, 20, 0.25); }
.stat-card--clickable:active { transform: translateY(0) scale(0.99); }
.stat-card-arrow { margin-left: auto; align-self: center; display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 8px; background: rgba(240, 90, 20, 0.08); color: var(--ember); opacity: 0; transform: translateX(-4px); transition: opacity 0.2s, transform 0.2s, background 0.2s; flex-shrink: 0; }
.stat-card--clickable:hover .stat-card-arrow, .stat-card--clickable:focus-visible .stat-card-arrow { opacity: 1; transform: translateX(0); background: rgba(240, 90, 20, 0.18); }
.stat-icon { width: 48px; height: 48px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.income-icon { background: rgba(16,185,129,0.12); color: var(--green); }
.expense-icon { background: rgba(239,68,68,0.12); color: var(--red); }
.balance-icon { background: rgba(240,90,20,0.12); color: var(--ember); }
.stat-info { display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: 13px; color: var(--muted); font-weight: 500; }
.stat-value { font-size: 24px; font-weight: 700; font-family: 'JetBrains Mono', monospace; }
.income-value { color: var(--green); }
.expense-value { color: var(--red); }
.balance-positive { color: var(--green); }
.balance-negative { color: var(--red); }
.stat-trend { font-size: 12px; color: var(--muted); display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.trend-badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.income-badge { background: rgba(16,185,129,0.12); color: var(--green); }
.expense-badge { background: rgba(239,68,68,0.12); color: var(--red); }
.surplus-badge { background: rgba(16,185,129,0.12); color: var(--green); }
.deficit-badge { background: rgba(239,68,68,0.12); color: var(--red); }
.charts-section { margin-bottom: 32px; }
.charts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.charts-grid--row { margin-top: 20px; }
.chart-card { background: var(--graphite); border: 1px solid var(--wire); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); overflow: hidden; transition: border-color 0.2s; }
.chart-header { padding: 20px 24px 0; display: flex; justify-content: space-between; align-items: flex-start; }
.chart-title { font-size: 16px; font-weight: 600; color: var(--bone); margin: 0; }
.chart-subtitle { font-size: 13px; color: var(--muted); }
.chart-body { padding: 12px 24px 24px; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 20px; text-align: center; }
.empty-title { font-size: 18px; font-weight: 600; color: var(--muted); margin: 20px 0 8px; }
.empty-text { font-size: 14px; color: var(--muted); margin: 0; }
.dark-drawer :deep(.el-drawer) { background: var(--graphite); border-left: 1px solid var(--wire); }
.dark-drawer :deep(.el-drawer__header) { margin: 0; padding: 20px 24px; border-bottom: 1px solid var(--wire); }
.dark-drawer :deep(.el-drawer__body) { padding: 0; overflow-y: auto; }
.dark-drawer :deep(.el-drawer__close-btn) { color: var(--muted); }
.dark-drawer :deep(.el-drawer__close-btn:hover) { color: var(--bone); }
.drawer-header { display: flex; flex-direction: column; gap: 10px; width: 100%; }
.drawer-title-group { display: flex; align-items: center; gap: 10px; }
.drawer-category-badge { font-size: 14px; font-weight: 700; color: #fff; padding: 4px 14px; border-radius: 20px; letter-spacing: 0.3px; }
.drawer-period { font-size: 13px; color: var(--muted); }
.drawer-summary { display: flex; align-items: center; justify-content: space-between; }
.drawer-count { font-size: 13px; color: var(--muted); }
.drawer-total { font-size: 20px; font-weight: 700; font-family: 'JetBrains Mono', monospace; }
.drawer-total--expense { color: var(--red); }
.drawer-total--income { color: var(--green); }
.drawer-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; padding: 60px 20px; color: var(--muted); font-size: 14px; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--wire); border-top-color: var(--ember); border-radius: 50%; animation: spin 0.8s linear infinite; }
.transaction-list { padding: 8px 0; }
.tx-item { display: flex; align-items: center; gap: 14px; padding: 14px 24px; border-bottom: 1px solid var(--wire); transition: background 0.15s; animation: fadeSlideIn 0.3s ease both; }
.tx-item:hover { background: rgba(255,255,255,0.03); }
.tx-item:last-child { border-bottom: none; }
@keyframes fadeSlideIn { from { opacity: 0; transform: translateX(12px); } to { opacity: 1; transform: translateX(0); } }
.tx-date-col { display: flex; flex-direction: column; align-items: center; min-width: 36px; flex-shrink: 0; }
.tx-day { font-size: 18px; font-weight: 700; color: var(--bone); line-height: 1; }
.tx-month-short { font-size: 11px; color: var(--muted); text-transform: uppercase; letter-spacing: 0.5px; }
.tx-info { flex: 1; display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.tx-desc { font-size: 14px; color: var(--bone); font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tx-category-tag { font-size: 11px; color: var(--ember); background: rgba(240,90,20,0.1); padding: 2px 8px; border-radius: 6px; align-self: flex-start; }
.tx-amount { font-size: 14px; font-weight: 700; font-family: 'JetBrains Mono', monospace; flex-shrink: 0; }
.tx-amount--expense { color: var(--red); }
.tx-amount--income { color: var(--green); }
.drawer-empty { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 60px 20px; color: var(--muted); font-size: 14px; text-align: center; }
@media (max-width: 1024px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } .stat-grid .stat-card:last-child { grid-column: 1 / -1; } }
@media (max-width: 768px) { .analytics-container { padding: 16px; } .page-header { flex-direction: column; align-items: flex-start; } .stat-grid { grid-template-columns: 1fr; } .stat-grid .stat-card:last-child { grid-column: auto; } .charts-grid { grid-template-columns: 1fr; } .page-title { font-size: 20px; } .stat-value { font-size: 20px; } .month-label { min-width: 110px; font-size: 14px; } }
@media (max-width: 480px) { .analytics-container { padding: 12px; } .stat-card { padding: 16px; } .stat-icon { width: 40px; height: 40px; } .stat-value { font-size: 18px; } }
</style>
