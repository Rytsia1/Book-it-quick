<template>
  <div class="page">

    <!-- Page Header -->
    <div class="page-header">
      <div>
        <p class="page-eyebrow">BOOKKEEPING / BUDGET</p>
        <h1 class="page-title">Monthly Budget</h1>
        <p class="page-subtitle">Set a spending limit and track your progress each month.</p>
      </div>
      <div class="page-header__actions">
        <button class="btn-ghost" @click="navigateToDashboard">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="19" y1="12" x2="5" y2="12"/>
            <polyline points="12 19 5 12 12 5"/>
          </svg>
          BACK TO DASHBOARD
        </button>
        <button class="btn-primary" @click="focusBudgetInput">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          {{ savedBudget ? 'EDIT BUDGET' : 'SET BUDGET' }}
        </button>
      </div>
    </div>

    <!-- Month Navigator (same UX as Analytics.vue) -->
    <div class="month-nav">
      <button class="month-btn" @click="prevMonth" title="Previous month">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15 18 9 12 15 6"/>
        </svg>
      </button>
      <span class="month-label">{{ MONTHS[selectedMonth - 1] }} {{ selectedYear }}</span>
      <button class="month-btn" @click="nextMonth" :disabled="isCurrentMonth" title="Next month">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </button>
      <button class="refresh-btn" @click="refreshAll" :class="{ loading: loading }" title="Refresh">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="23 4 23 10 17 10"/>
          <path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/>
        </svg>
        REFRESH
      </button>
    </div>

    <!-- No-budget CTA: shown when the user hasn't set one yet. -->
    <div v-if="!loading && !savedBudget" class="empty-cta">
      <div class="empty-cta__icon">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
      </div>
      <div class="empty-cta__body">
        <h3 class="empty-cta__title">No budget set yet</h3>
        <p class="empty-cta__text">
          Set a monthly spending limit below. You'll get a warning when spending approaches the limit
          and an alert if you go over.
        </p>
      </div>
    </div>

    <!-- Main Grid: form on the left, status on the right -->
    <div class="main-grid">

      <!-- ── Set / Edit Budget form ── -->
      <section class="panel">
        <div class="panel__header">
          <span class="panel__label">{{ savedBudget ? 'EDIT MONTHLY BUDGET' : 'SET MONTHLY BUDGET' }}</span>
        </div>

        <div class="form-body">
          <p class="form-desc">
            Enter the maximum amount you want to spend in a month. This limit is the same for every
            month — change it any time to reflect a new goal.
          </p>

          <div class="form-row">
            <label class="form-label" for="budget-input">MONTHLY LIMIT (USD)</label>
            <el-input-number
              id="budget-input"
              ref="budgetInputRef"
              v-model="budgetInput"
              :min="0"
              :step="50"
              :precision="0"
              size="large"
              class="form-input"
              placeholder="0"
            />
          </div>

          <div v-if="budgetInput > 0" class="form-preview">
            <span class="form-preview__label">PREVIEW</span>
            <span class="form-preview__value mono">{{ formatCurrencyUSD(budgetInput) }}</span>
          </div>

          <div v-if="savedBudget" class="form-current">
            <span class="form-current__label">CURRENT SAVED</span>
            <span class="form-current__value mono">{{ formatCurrencyUSD(savedBudget) }}</span>
          </div>

          <div class="form-actions">
            <button
              v-if="savedBudget"
              class="btn-outline"
              :disabled="saving"
              @click="budgetInput = savedBudget"
            >
              RESET
            </button>
            <button
              class="btn-primary"
              :disabled="saving || budgetInput == null"
              @click="saveBudget"
            >
              {{ saving ? 'SAVING...' : (savedBudget ? 'UPDATE BUDGET' : 'SAVE BUDGET') }}
            </button>
          </div>
        </div>
      </section>

      <!-- ── This Month's Status ── -->
      <aside class="sidebar">
        <section class="panel">
          <div class="panel__header">
            <span class="panel__label">{{ MONTHS[selectedMonth - 1].toUpperCase() }} STATUS</span>
            <span
              v-if="summary.monthlyBudget"
              :class="['status-pill', statusPillClass]"
            >
              {{ statusPillText }}
            </span>
            <span v-else class="status-pill status-pill--neutral">NO BUDGET</span>
          </div>

          <div v-if="loading" class="loading">
            <div class="spinner"></div>
            <span>Loading summary...</span>
          </div>

          <div v-else-if="!summary.monthlyBudget" class="status-empty">
            <p>Set a budget to start tracking this month.</p>
          </div>

          <div v-else class="status-body">
            <!-- 3 mini stat tiles -->
            <div class="mini-stats">
              <div class="mini-stat">
                <span class="mini-stat__label">BUDGET</span>
                <span class="mini-stat__value mono">{{ formatCurrencyUSD(summary.monthlyBudget) }}</span>
              </div>
              <div class="mini-stat">
                <span class="mini-stat__label">SPENT</span>
                <span class="mini-stat__value mono text-red">{{ formatCurrencyUSD(summary.expense) }}</span>
              </div>
              <div class="mini-stat">
                <span class="mini-stat__label">
                  {{ summary.budgetExceeded ? 'OVER BY' : 'REMAINING' }}
                </span>
                <span
                  class="mini-stat__value mono"
                  :class="summary.budgetExceeded ? 'text-red' : 'text-green'"
                >
                  {{ formatCurrencyUSD(Math.abs(summary.monthlyBudget - summary.expense)) }}
                </span>
              </div>
            </div>

            <!-- Progress bar (matches Dashboard.vue's color thresholds). -->
            <div class="progress-block">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :class="{
                    'progress-fill--exceeded': summary.budgetExceeded,
                    'progress-fill--warning':  summary.budgetWarning && !summary.budgetExceeded,
                  }"
                  :style="{ width: Math.min(summary.budgetUsedPercent || 0, 100) + '%' }"
                ></div>
              </div>
              <div class="progress-meta">
                <span
                  :class="{
                    'text-red':   summary.budgetExceeded,
                    'text-amber': summary.budgetWarning && !summary.budgetExceeded,
                    'text-green': !summary.budgetWarning,
                  }"
                >
                  {{ (summary.budgetUsedPercent || 0).toFixed(1) }}% used
                </span>
                <span class="text-ash mono">
                  {{ summary.budgetExceeded ? 'over budget' : 'on track' }}
                </span>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </div>

    <!-- ── Budget History ── -->
    <section class="panel history-panel">
      <div class="panel__header">
        <span class="panel__label">BUDGET HISTORY (LAST 6 MONTHS)</span>
        <span class="panel__hint">
          Your saved budget applies to every month. Actual spending is fetched per month.
        </span>
      </div>

      <div v-if="historyLoading" class="loading">
        <div class="spinner"></div>
        <span>Loading history...</span>
      </div>

      <el-table
        v-else
        :data="historyRows"
        style="width: 100%"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column label="MONTH" min-width="160">
          <template #default="{ row }">
            <span class="month-cell">{{ row.label }}</span>
            <span v-if="row.isCurrent" class="month-tag">CURRENT</span>
          </template>
        </el-table-column>
        <el-table-column label="BUDGET" width="180">
          <template #default="{ row }">
            <span class="mono">{{ formatCurrencyUSD(row.budget) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="ACTUAL SPENT" width="180">
          <template #default="{ row }">
            <span class="mono" :class="row.usage > 100 ? 'text-red' : (row.usage > 80 ? 'text-amber' : 'text-green')">
              {{ formatCurrencyUSD(row.actual) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="USAGE" min-width="220">
          <template #default="{ row }">
            <div class="row-progress">
              <div class="row-progress__track">
                <div
                  class="row-progress__fill"
                  :class="{
                    'row-progress__fill--exceeded': row.usage > 100,
                    'row-progress__fill--warning':  row.usage > 80 && row.usage <= 100,
                  }"
                  :style="{ width: Math.min(row.usage, 100) + '%' }"
                ></div>
              </div>
              <span
                class="row-progress__pct mono"
                :class="row.usage > 100 ? 'text-red' : (row.usage > 80 ? 'text-amber' : 'text-green')"
              >
                {{ row.usage.toFixed(1) }}%
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="RESULT" width="140">
          <template #default="{ row }">
            <span :class="['badge', historyBadgeClass(row)]">
              {{ historyBadgeText(row) }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!historyLoading && historyRows.length === 0"
        description="No budget history yet"
        class="empty"
      />
    </section>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const today = new Date()
const selectedMonth = ref(today.getMonth() + 1)
const selectedYear  = ref(today.getFullYear())

const isCurrentMonth = computed(() =>
  selectedMonth.value === today.getMonth() + 1 &&
  selectedYear.value  === today.getFullYear()
)

const savedBudget  = ref(null)   // last value returned by GET /api/budget/{userId}
const budgetInput  = ref(0)      // bound to el-input-number
const saving       = ref(false)
const loading      = ref(false)  // current-month summary
const historyLoading = ref(false)
const budgetInputRef = ref(null)

// Shape returned by /api/stats/summary (matches Dashboard.vue).
const summary = ref({
  income: 0, expense: 0, balance: 0,
  monthlyBudget: null, budgetUsedPercent: 0,
  budgetWarning: false, budgetExceeded: false,
})

// History rows: 6 most recent months (oldest -> newest) plus the live selected month
// when it isn't already in the past 6.
const historyRows = ref([])

const tableHeaderStyle = {
  background: 'var(--ink)', color: 'var(--ash)',
  borderBottom: '1px solid var(--wire)',
  fontSize: '11px', fontWeight: '600', letterSpacing: '1px',
}
const tableCellStyle = {
  background: 'var(--graphite)', color: 'var(--bone)',
  borderBottom: '1px solid var(--wire)', fontSize: '13px',
}

// ── Computed UI helpers ─────────────────────────────────────────────
const statusPillClass = computed(() => {
  if (summary.value.budgetExceeded) return 'status-pill--exceeded'
  if (summary.value.budgetWarning)  return 'status-pill--warning'
  return 'status-pill--ok'
})
const statusPillText = computed(() => {
  if (summary.value.budgetExceeded) return 'EXCEEDED'
  if (summary.value.budgetWarning)  return 'WARNING'
  return 'ON TRACK'
})

// ── Currency helper (matches Dashboard.vue / Bills.vue). ────────────
const formatCurrencyUSD = (amount) => {
  if (amount == null) return '$ 0'
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD',
    minimumFractionDigits: 2, maximumFractionDigits: 2,
  }).format(amount)
}

// ── Month navigation ────────────────────────────────────────────────
const prevMonth = () => {
  if (selectedMonth.value === 1) { selectedMonth.value = 12; selectedYear.value-- }
  else selectedMonth.value--
  fetchMonthSummary()
}
const nextMonth = () => {
  if (isCurrentMonth.value) return
  if (selectedMonth.value === 12) { selectedMonth.value = 1; selectedYear.value++ }
  else selectedMonth.value++
  fetchMonthSummary()
}

// ── Routing helper ──────────────────────────────────────────────────
const navigateToDashboard = () => router.push('/dashboard')

// ── Focus the budget input when the user clicks the header CTA. ─────
const focusBudgetInput = async () => {
  await nextTick()
  // el-input-number wraps an inner <input>; focus it directly.
  const el = budgetInputRef.value?.$el?.querySelector?.('input')
  if (el) el.focus()
}

// ── API: load the saved monthly budget for the current user. ────────
const fetchSavedBudget = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('Session expired. Please log in again.')
    router.push('/login')
    return
  }
  try {
    const data = await request.get(`/budget/${userId}`)
    // Backend returns: { monthlyBudget: <BigDecimal|number|null> }
    const v = data?.monthlyBudget
    if (v == null || v === '') {
      savedBudget.value = null
    } else {
      savedBudget.value = Number(v)
    }
    // Pre-fill the form input with the saved value (or 0).
    budgetInput.value = savedBudget.value || 0
  } catch (e) {
    console.error('fetchSavedBudget error:', e)
    const msg = e?.response?.data?.message || e?.message || 'Failed to load saved budget'
    ElMessage.error(msg)
  }
}

// ── API: load /api/stats/summary for the currently selected month. ──
const fetchMonthSummary = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('Session expired. Please log in again.')
    router.push('/login')
    return
  }
  loading.value = true
  try {
    const data = await request.get('/stats/summary', {
      params: { userId, month: selectedMonth.value, year: selectedYear.value },
    })
    summary.value = {
      income:  data.totalIncome  || 0,
      expense: data.totalExpense || 0,
      balance: data.balance      || 0,
      monthlyBudget:     data.monthlyBudget     || null,
      budgetUsedPercent: data.budgetUsedPercent || 0,
      budgetWarning:     data.budgetWarning     || false,
      budgetExceeded:    data.budgetExceeded    || false,
    }
  } catch (e) {
    console.error('fetchMonthSummary error:', e)
    ElMessage.error('Failed to load monthly summary')
  } finally {
    loading.value = false
  }
}

// API: build the last-6-months history rows.
// Each row pulls the actual expense for that month from /api/stats/summary;
// the "budget" column shows the currently-saved monthly limit (the schema
// only stores a single value, so it applies to every month).
const HISTORY_MONTHS = 6

const buildHistoryRange = () => {
  const months = []
  // Oldest -> newest, including the current month.
  let m = selectedMonth.value
  let y = selectedYear.value
  for (let i = HISTORY_MONTHS - 1; i >= 0; i--) {
    const mm = ((m - 1 - i) % 12 + 12) % 12 + 1
    const yy = y + Math.floor((m - 1 - i - (m - 1)) / 12)
    // The math above simplifies to: walk backwards `i` months from (m, y).
    const target = new Date(y, m - 1, 1)
    target.setMonth(target.getMonth() - i)
    months.push({
      month: target.getMonth() + 1,
      year:  target.getFullYear(),
    })
  }
  return months
}

const fetchHistory = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('Session expired. Please log in again.')
    router.push('/login')
    return
  }
  historyLoading.value = true
  const range = buildHistoryRange()
  try {
    const results = await Promise.all(
      range.map(({ month, year }) =>
        request
          .get('/stats/summary', { params: { userId, month, year } })
          .then(data => ({ month, year, data, error: null }))
          .catch(err => ({ month, year, data: null, error: err }))
      )
    )
    const currentM = today.getMonth() + 1
    const currentY = today.getFullYear()
    historyRows.value = results.map(({ month, year, data, error }) => {
      const actual    = error ? 0 : (data?.totalExpense || 0)
      const budgetNum = savedBudget.value || (data?.monthlyBudget ? Number(data.monthlyBudget) : 0)
      // Usage is computed client-side. Use 0 when no budget to avoid NaN%.
      const usage = budgetNum > 0 ? (actual / budgetNum) * 100 : 0
      return {
        month,
        year,
        label:    `${MONTHS[month - 1]} ${year}`,
        isCurrent: month === currentM && year === currentY,
        budget:   budgetNum,
        actual,
        usage,
      }
    })
  } catch (e) {
    console.error('fetchHistory error:', e)
    ElMessage.error('Failed to load budget history')
    historyRows.value = []
  } finally {
    historyLoading.value = false
  }
}

// API: save the monthly budget (PUT /api/budget).
// Same call shape and error handling used in Dashboard.vue's saveBudget.
const saveBudget = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId || userId === 'null' || userId === 'undefined') {
    ElMessage.error('Session expired. Please log in again.')
    router.push('/login')
    return
  }
  const budgetValue = Number(budgetInput.value)
  if (Number.isNaN(budgetValue) || budgetValue < 0) {
    ElMessage.error('Please enter a valid budget amount (0 or greater)')
    return
  }
  saving.value = true
  try {
    const res = await request.put('/budget', {
      userId: Number(userId),
      monthlyBudget: budgetValue,
    })
    if (res && res.message) {
      ElMessage.success(res.message)
    } else {
      ElMessage.success('Budget saved successfully')
    }
    savedBudget.value = budgetValue
    // Refresh the current-month status panel and the history table so the
    // new budget value is reflected everywhere immediately.
    await Promise.all([fetchMonthSummary(), fetchHistory()])
  } catch (e) {
    console.error('saveBudget error:', e)
    const serverMsg = e?.response?.data?.message || e?.message || 'Failed to save budget'
    ElMessage.error(serverMsg)
  } finally {
    saving.value = false
  }
}

// Refresh button handler: re-fetch everything.
const refreshAll = async () => {
  ElMessage.info('Refreshing budget data...')
  await Promise.all([fetchSavedBudget(), fetchMonthSummary(), fetchHistory()])
  ElMessage.success('Budget data refreshed')
}

// History row badge helpers.
const historyBadgeClass = (row) => {
  if (row.usage > 100) return 'badge--expense'
  if (row.usage > 80)  return 'badge--warning'
  return 'badge--ok'
}
const historyBadgeText = (row) => {
  if (row.budget <= 0)  return 'NO BUDGET'
  if (row.usage > 100)  return 'OVER'
  if (row.usage > 80)   return 'WARNING'
  return 'ON TRACK'
}

onMounted(async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('Please log in to manage your budget')
    router.push('/login')
    return
  }
  await Promise.all([fetchSavedBudget(), fetchMonthSummary(), fetchHistory()])
})
</script>

<style scoped>
.page {
  padding: 28px 32px;
  max-width: 1440px;
  margin: 0 auto;
  min-height: 100vh;
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
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
.btn-ghost,
.btn-outline {
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
.btn-primary:hover:not(:disabled) { background: var(--spark); border-color: var(--spark); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-ghost {
  background: transparent;
  border: 1px solid var(--wire);
  color: var(--muted);
}
.btn-ghost:hover { border-color: var(--ember); color: var(--ember); background: rgba(240, 90, 20, 0.06); }

.btn-outline {
  background: transparent;
  border: 1px solid var(--wire);
  color: var(--muted);
}
.btn-outline:hover:not(:disabled) { border-color: var(--ember); color: var(--ember); background: rgba(240, 90, 20, 0.05); }
.btn-outline:disabled { opacity: 0.5; cursor: not-allowed; }

/* Month Navigator */
.month-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.month-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--bone);
  min-width: 160px;
  text-align: center;
  font-family: var(--font-display);
}

.month-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--graphite);
  border: 1px solid var(--wire);
  border-radius: 8px;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s;
}
.month-btn:hover:not(:disabled) { border-color: var(--ember); color: var(--ember); background: rgba(240, 90, 20, 0.08); }
.month-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--graphite);
  border: 1px solid var(--wire);
  border-radius: 8px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
  font-family: var(--font-body);
  letter-spacing: 0.8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 4px;
}
.refresh-btn:hover { border-color: var(--ember); color: var(--ember); }
.refresh-btn.loading svg { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Empty CTA */
.empty-cta {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 22px;
  margin-bottom: 20px;
  background: rgba(240, 90, 20, 0.06);
  border: 1px solid rgba(240, 90, 20, 0.25);
  border-left: 3px solid var(--ember);
  border-radius: var(--radius-md);
}
.empty-cta__icon {
  width: 48px; height: 48px;
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(240, 90, 20, 0.12);
  color: var(--ember);
  border-radius: 8px;
}
.empty-cta__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--white);
  margin: 0 0 4px;
  font-family: var(--font-body);
}
.empty-cta__text {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
  line-height: 1.5;
}

/* Main Grid */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 16px;
  margin-bottom: 24px;
}

/* Panel */
.panel {
  background: var(--graphite);
  border: none;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.panel :deep(.el-table) { border-radius: 0 !important; }
.panel :deep(.el-table__header-wrapper) { border-bottom: 1px solid var(--wire) !important; }
.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--wire);
  gap: 12px;
}
.panel__header--status { gap: 10px; }
.panel__label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: var(--ash);
}
.panel__hint {
  font-size: 11px;
  color: var(--muted);
  font-style: italic;
}

/* Form */
.form-body { padding: 20px 24px 24px; display: flex; flex-direction: column; gap: 16px; }
.form-desc { font-size: 13px; color: var(--muted); line-height: 1.6; margin: 0; }
.form-row { display: flex; flex-direction: column; gap: 8px; }
.form-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: var(--ash);
}
.form-input { width: 100%; }
.form-preview,
.form-current {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--ink);
  border: 1px solid var(--wire);
  border-radius: var(--radius-sm);
}
.form-preview__label,
.form-current__label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.2px;
  color: var(--ash);
}
.form-preview__value,
.form-current__value {
  font-size: 18px;
  font-weight: 600;
  color: var(--ember);
}
.form-current__value { color: var(--bone); }
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

/* Sidebar / status */
.sidebar { display: flex; flex-direction: column; gap: 16px; }

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  border-radius: 6px;
}
.status-pill--ok       { background: rgba(34, 197, 94, 0.12);  color: var(--green); }
.status-pill--warning  { background: rgba(245, 158, 11, 0.12); color: var(--amber); }
.status-pill--exceeded { background: rgba(239, 68, 68, 0.12);  color: var(--red); }
.status-pill--neutral  { background: var(--wire); color: var(--muted); }

.status-body { padding: 20px 24px 24px; display: flex; flex-direction: column; gap: 18px; }
.status-empty { padding: 30px 24px; color: var(--ash); font-size: 12px; text-align: center; }

/*
 * Three small stat tiles (BUDGET / SPENT / REMAINING) laid out in a
 * single row when there's room, wrapping to a new line when there isn't.
 *
 * Earlier this used `display: grid; grid-template-columns: repeat(3, 1fr)`
 * which (a) made the third tile's right edge clip against the sidebar at
 * common widths (so "REMAINING" rendered as "REMAININ…"), and (b) let a
 * long currency value push the whole row wider than the panel. Switching
 * to flex+wrap + min-width:0 + ellipsis on the value fixes both.
 */
.mini-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  /* flex: 1 1 0 + min-width: 0 is the canonical flexbox pattern that lets
     all three children share the row equally and shrink below their
     intrinsic content width so none of them overflows the panel. */
  flex: 1 1 0;
  min-width: 0;
  padding: 10px 10px;
  background: var(--ink);
  border: 1px solid var(--wire);
  border-radius: var(--radius-sm);
}
.mini-stat__label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.2px;
  color: var(--ash);
  /* Never wrap the label so words like "REMAINING" stay whole. */
  white-space: nowrap;
}
.mini-stat__value {
  font-size: 14px;
  font-weight: 600;
  color: var(--white);
  /* Long currency values (e.g. $1,234,567.89) now ellipsise instead of
     pushing the box wider than its share of the row. */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.progress-block { display: flex; flex-direction: column; gap: 8px; }
.progress-track {
  height: 4px;
  background: var(--wire);
  border-radius: 99px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--green);
  border-radius: 99px;
  transition: width 0.5s ease;
}
.progress-fill--warning  { background: var(--amber); }
.progress-fill--exceeded { background: var(--red); }
.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
}

/* Loading spinner */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 60px 20px;
  color: var(--ash);
  font-size: 13px;
}
.spinner {
  width: 30px; height: 30px;
  border: 3px solid var(--wire);
  border-top-color: var(--ember);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* History table */
.history-panel { margin-bottom: 24px; }

.month-cell { color: var(--bone); font-weight: 500; font-size: 13px; }
.month-tag {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.8px;
  background: rgba(240, 90, 20, 0.12);
  color: var(--ember);
  border-radius: 6px;
  vertical-align: middle;
}

.row-progress { display: flex; align-items: center; gap: 10px; }
.row-progress__track {
  flex: 1;
  height: 4px;
  background: var(--wire);
  border-radius: 99px;
  overflow: hidden;
}
.row-progress__fill {
  height: 100%;
  background: var(--green);
  border-radius: 99px;
  transition: width 0.4s ease;
}
.row-progress__fill--warning

.row-progress__fill--warning  { background: var(--amber); }
.row-progress__fill--exceeded { background: var(--red); }
.row-progress__pct {
  font-size: 12px;
  font-weight: 600;
  min-width: 50px;
  text-align: right;
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
.badge--ok      { background: rgba(34, 197, 94, 0.12);  color: var(--green); }
.badge--warning { background: rgba(245, 158, 11, 0.12); color: var(--amber); }
.badge--expense { background: rgba(239, 68, 68, 0.12);  color: var(--red); }

/* Empty state inside the history panel */
.empty { padding: 40px 0; }
.empty :deep(.el-empty__description p) { color: var(--ash); font-size: 12px; }

/* Utilities */
.mono       { font-family: var(--font-mono) !important; }
.text-red   { color: var(--red) !important; }
.text-amber { color: var(--amber) !important; }
.text-green { color: var(--green) !important; }
.text-ash   { color: var(--ash) !important; }

/* Responsive */
@media (max-width: 1200px) {
  .main-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
  .page-title { font-size: 22px; }
  .page-header__actions { width: 100%; }
  .page-header__actions .btn-ghost,
  .page-header__actions .btn-primary { flex: 1; justify-content: center; }
  /* Phone-sized viewports: stack the three tiles vertically so each one
     has the full panel width to itself. */
  .mini-stats { flex-direction: column; }
  .mini-stat  { flex: 1 1 100%; }
  .month-label { min-width: 130px; font-size: 14px; }
}
@media (max-width: 480px) {
  .page { padding: 12px; }
  .month-label { min-width: 110px; font-size: 13px; }
  .month-btn, .refresh-btn { padding: 6px 10px; }
}
</style>
