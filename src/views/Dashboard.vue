<template>
  <div class="page">

    <!-- Budget Alert -->
    <transition name="alert-slide">
      <div
        v-if="(monthlySummary.budgetExceeded || monthlySummary.budgetWarning) && !alertDismissed"
        :class="['alert-bar', monthlySummary.budgetExceeded ? 'alert-bar--exceeded' : 'alert-bar--warning']"
      >
        <div class="alert-bar__icon">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </div>
        <div class="alert-bar__body">
          <strong>{{ monthlySummary.budgetExceeded ? 'BUDGET EXCEEDED' : 'BUDGET WARNING' }}</strong>
          <span>Expenses {{ fmt(monthlySummary.expense).primary }} of {{ fmt(monthlySummary.monthlyBudget).primary }} ({{ (monthlySummary.budgetUsedPercent || 0).toFixed(1) }}%)</span>
        </div>
        <button class="alert-bar__close" @click="dismissAlert">✕</button>
      </div>
    </transition>

    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__left">
        <h1 class="page-title">Welcome, <span class="accent">{{ userName }}</span></h1>
        <p class="page-date">{{ currentDate }}</p>
      </div>
      <div class="page-header__actions">
        <button class="btn-ghost" @click="navigateToAnalytics">
          Analytics
        </button>
        <button class="btn-primary" @click="openBillDialog">
          + ADD BILL
        </button>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card">
        <p class="stat-card__label">TOTAL TRANSACTIONS</p>
        <p class="stat-card__value">{{ totalTransactions }}</p>
        <p class="stat-card__sub">all time</p>
      </div>
      <div class="stat-card">
        <p class="stat-card__label">THIS MONTH</p>
        <p class="stat-card__value">{{ currentMonthCount }}</p>
        <p class="stat-card__sub">transactions</p>
      </div>
      <div class="stat-card">
        <p class="stat-card__label">STATUS</p>
        <p class="stat-card__value" :class="monthlySummary.balance >= 0 ? 'value--green' : 'value--red'">
          {{ statusText.toUpperCase() }}
        </p>
        <p class="stat-card__sub">monthly</p>
      </div>
      <!--
        NET BALANCE card: shows the converted (selected-currency) value
        as the big primary text and the canonical USD value as a small
        gray line beneath. Same shape is reused everywhere a monetary
        amount is displayed in this app; see useCurrency.js.
      -->
      <div class="stat-card">
        <p class="stat-card__label">NET BALANCE</p>
        <p class="stat-card__value mono"
           :class="monthlySummary.balance >= 0 ? 'value--green' : 'value--red'">
          {{ fmtSigned(monthlySummary.balance, monthlySummary.balance >= 0 ? '' : '-').primary }}
        </p>
        <p v-if="fmt(monthlySummary.balance).secondary" class="stat-card__sub stat-card__sub--converted">
          ≈ {{ fmt(monthlySummary.balance).secondary }}
        </p>
        <p v-else class="stat-card__sub">{{ lastUpdate }}</p>
      </div>
    </div>

    <!-- Main Grid -->
    <div class="main-grid">
      <!-- Recent Bills -->
      <section class="panel panel--fill">
        <div class="panel__header">
          <span class="panel__label">RECENT TRANSACTIONS</span>
          <button class="btn-link" @click="openBillDialog">VIEW ALL →</button>
        </div>

        <el-table
          :data="recentBills"
          style="width: 100%"
          :header-cell-style="tableHeaderStyle"
          :cell-style="tableCellStyle"
        >
          <el-table-column prop="billDate" label="DATE" width="100" />
          <el-table-column label="TYPE" width="110">
            <template #default="{ row }">
              <span :class="['badge', row.type === 1 ? 'badge--income' : 'badge--expense']">
                {{ row.type === 1 ? 'INCOME' : 'EXPENSE' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="CATEGORY" width="130" />
          <el-table-column label="AMOUNT" width="180">
            <template #default="{ row }">
              <div class="amount-cell">
                <span :class="['mono', 'fw-600', 'amount-primary', row.type === 1 ? 'text-green' : 'text-red']">
                  {{ fmtSigned(row.amount, row.type === 1 ? '+' : '-').primary }}
                </span>
                <span v-if="fmt(row.amount).secondary" class="amount-secondary">
                  ≈ {{ fmt(row.amount).secondary }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="DESCRIPTION" min-width="160" show-overflow-tooltip />
        </el-table>

        <el-empty v-if="recentBills.length === 0" description="No transactions yet" class="empty" />
      </section>

      <!-- Sidebar -->
      <aside class="sidebar">
        <!-- Monthly Summary -->
        <section class="panel">
          <div class="panel__header">
            <span class="panel__label">MONTHLY SUMMARY</span>
          </div>

          <div class="kv-list">
            <div class="kv-row">
              <span class="kv-key">INCOME</span>
              <div class="kv-amount">
                <span class="kv-val mono text-green">{{ fmtSigned(monthlySummary.income, '+').primary }}</span>
                <span v-if="fmt(monthlySummary.income).secondary" class="kv-amount__sub">
                  ≈ {{ fmt(monthlySummary.income).secondary }}
                </span>
              </div>
            </div>
            <div class="kv-row">
              <span class="kv-key">EXPENSE</span>
              <div class="kv-amount">
                <span class="kv-val mono text-red">{{ fmtSigned(monthlySummary.expense, '-').primary }}</span>
                <span v-if="fmt(monthlySummary.expense).secondary" class="kv-amount__sub">
                  ≈ {{ fmt(monthlySummary.expense).secondary }}
                </span>
              </div>
            </div>
            <div class="kv-divider"></div>
            <div class="kv-row">
              <span class="kv-key">BALANCE</span>
              <div class="kv-amount">
                <span class="kv-val mono fw-700" :class="monthlySummary.balance >= 0 ? 'text-green' : 'text-red'">
                  {{ fmtSigned(monthlySummary.balance, monthlySummary.balance >= 0 ? '+' : '-').primary }}
                </span>
                <span v-if="fmt(monthlySummary.balance).secondary" class="kv-amount__sub">
                  ≈ {{ fmt(monthlySummary.balance).secondary }}
                </span>
              </div>
            </div>

            <!-- Budget -->
            <template v-if="monthlySummary.monthlyBudget">
              <div class="kv-divider"></div>
              <div class="budget-block">
                <div class="budget-block__header">
                  <span class="kv-key">BUDGET</span>
                  <div class="kv-amount">
                    <span class="kv-val mono">{{ fmt(monthlySummary.monthlyBudget).primary }}</span>
                    <span v-if="fmt(monthlySummary.monthlyBudget).secondary" class="kv-amount__sub">
                      ≈ {{ fmt(monthlySummary.monthlyBudget).secondary }}
                    </span>
                  </div>
                </div>
                <div class="budget-track">
                  <div
                    class="budget-fill"
                    :class="{
                      'budget-fill--exceeded': monthlySummary.budgetExceeded,
                      'budget-fill--warning':  monthlySummary.budgetWarning && !monthlySummary.budgetExceeded,
                    }"
                    :style="{ width: Math.min(monthlySummary.budgetUsedPercent || 0, 100) + '%' }"
                  ></div>
                </div>
                <div class="budget-meta">
                  <span :class="{
                    'text-red':   monthlySummary.budgetExceeded,
                    'text-amber': monthlySummary.budgetWarning && !monthlySummary.budgetExceeded,
                    'text-green': !monthlySummary.budgetWarning,
                  }">{{ (monthlySummary.budgetUsedPercent || 0).toFixed(1) }}% used</span>
                  <span class="text-ash mono">
                    <template v-if="!monthlySummary.budgetExceeded">
                      remaining {{ fmt(monthlySummary.monthlyBudget - monthlySummary.expense).primary }}
                    </template>
                    <template v-else>
                      over by {{ fmt(monthlySummary.expense - monthlySummary.monthlyBudget).primary }}
                    </template>
                  </span>
                </div>
              </div>
            </template>
          </div>

          <button class="btn-outline full-width mt-12" @click="openBudgetDialog">
            {{ monthlySummary.monthlyBudget ? 'EDIT BUDGET →' : 'SET BUDGET →' }}
          </button>
        </section>

        <!-- Upcoming Bills (next 7-14 days) -->
        <section class="panel">
          <div class="panel__header">
            <span class="panel__label">UPCOMING BILLS</span>
            <span class="panel__hint">NEXT 14 DAYS</span>
          </div>
          <div v-if="upcomingBills.length === 0" class="upcoming-empty">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
            <p>No bills due in the next 14 days.</p>
          </div>
          <ul v-else class="upcoming-list">
            <li
              v-for="b in upcomingBills"
              :key="b.id"
              class="upcoming-item"
            >
              <div :class="['upcoming-icon', b.type === 1 ? 'upcoming-icon--income' : 'upcoming-icon--expense']">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                </svg>
              </div>
              <div class="upcoming-body">
                <div class="upcoming-top">
                  <span class="upcoming-name">{{ b.category || b.description || 'Bill' }}</span>
                  <div class="upcoming-amount">
                    <span :class="['mono', 'fw-600', 'amount-primary', b.type === 1 ? 'text-green' : 'text-red']">
                      {{ fmtSigned(b.amount, b.type === 1 ? '+' : '-').primary }}
                    </span>
                    <span v-if="fmt(b.amount).secondary" class="amount-secondary">
                      ≈ {{ fmt(b.amount).secondary }}
                    </span>
                  </div>
                </div>
                <div class="upcoming-bottom">
                  <span class="upcoming-date">{{ formatDueDate(b.dueDate) }}</span>
                  <span :class="['upcoming-chip', urgencyClass(b.daysUntil)]">
                    {{ countdownLabel(b.daysUntil) }}
                  </span>
                </div>
              </div>
            </li>
          </ul>
        </section>

        <!-- Quick Actions -->
        <section class="panel">
          <div class="panel__header">
            <span class="panel__label">QUICK ACTIONS</span>
          </div>
          <div class="action-list">
            <button class="action-item" @click="openBillDialog">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              Add New Bill
            </button>
            <button class="action-item" @click="navigateToAnalytics">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
              </svg>
              View Analytics
            </button>
            <button class="action-item" @click="openBudgetDialog">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
              </svg>
              {{ monthlySummary.monthlyBudget ? 'Edit Budget' : 'Set Budget' }}
            </button>
          </div>
        </section>
      </aside>
    </div>

    <!-- Budget Dialog -->
    <el-dialog v-model="budgetDialogVisible" title="Set Monthly Budget" width="400px" class="forge-dialog">
      <div class="dialog-body">
        <p class="dialog-desc">Set the maximum monthly spending limit. An alert will appear when spending approaches or exceeds the limit.</p>
        <el-form label-position="top">
          <el-form-item label="TARGET BUDGET (USD)">
            <el-input-number
              v-model="budgetInput"
              :min="0"
              :step="10"
              :precision="0"
              style="width: 100%"
              size="large"
            />
          </el-form-item>
          <p v-if="budgetInput > 0" class="budget-preview">
            {{ fmt(budgetInput).primary }}
          </p>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="budgetDialogVisible = false">CANCEL</el-button>
        <el-button type="primary" :loading="budgetSaving" @click="saveBudget">SAVE BUDGET</el-button>
      </template>
    </el-dialog>

    <!-- Quick Add Bill dialog (in-page, no navigation needed) -->
    <BillDialog v-model:visible="billDialogVisible" @success="onBillDialogSuccess" />

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import BillDialog from '@/components/BillDialog.vue'
import { fmtAmount, fmtSigned, useCurrency } from '@/composables/useCurrency'
import { clearTokens, clearAllAuth, logoutOnServer } from '@/utils/auth'

const router = useRouter()

const bills = ref([])
// Raw list of recurring-bill templates from GET /api/recurring-bills. We
// derive the dashboard "Upcoming Bills" widget from this list on the fly.
const recurringBills      = ref([])
const monthlySummary = ref({
  income: 0, expense: 0, balance: 0,
  monthlyBudget: null, budgetUsedPercent: 0,
  budgetWarning: false, budgetExceeded: false,
})
const loading             = ref(false)
const alertDismissed      = ref(false)
const budgetDialogVisible = ref(false)
const budgetInput         = ref(0)
const budgetSaving        = ref(false)

const tableHeaderStyle = {
  background: 'var(--ink)', color: 'var(--ash)',
  borderBottom: '1px solid var(--wire)',
  fontSize: '11px', fontWeight: '600', letterSpacing: '1px',
}
const tableCellStyle = {
  background: 'var(--graphite)', color: 'var(--bone)',
  borderBottom: '1px solid var(--wire)',
  fontSize: '13px',
}

const userName = computed(() => localStorage.getItem('username') || 'user')

const currentDate = computed(() => {
  const today = new Date()
  return today.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
})

// Tie-breaker: when two bills share a billDate, the one with the more
// recent `createdAt` wins. `createdAt` is a LocalDateTime on the server
// (ISO string on the wire), so `new Date(...)` parses it correctly. The
// final `id DESC` keeps ordering fully deterministic for any legacy rows
// where `createdAt` happens to be null.
const toMillis = (v) => {
  if (!v) return 0
  const t = new Date(v).getTime()
  return Number.isFinite(t) ? t : 0
}
const compareBillsDesc = (a, b) => {
  const d = new Date(b.billDate) - new Date(a.billDate)
  if (d !== 0) return d
  const c = toMillis(b.createdAt) - toMillis(a.createdAt)
  if (c !== 0) return c
  return (b.id ?? 0) - (a.id ?? 0)
}

// Show the 14 most recent transactions, newest first.
// The `.slice()` before `.sort()` prevents mutating the original `bills` array
// (which other computeds/watches rely on). The panel uses flex stretching
// (see `.panel--fill` styles) so rows fill the available height.
const recentBills = computed(() =>
  bills.value
    .slice()
    .sort(compareBillsDesc) // newest first, with same-day tie-breaker
    .slice(0, 14)
)
const totalTransactions = computed(() => bills.value.length)

const currentMonthCount = computed(() => {
  const today = new Date()
  const m = today.getMonth() + 1, y = today.getFullYear()
  return bills.value.filter(b => {
    if (!b.billDate) return false
    const [yr, mo] = b.billDate.split('-').map(Number)
    return mo === m && yr === y
  }).length
})

const statusText = computed(() => {
  if (monthlySummary.value.balance > 0) return 'Surplus'
  if (monthlySummary.value.balance < 0) return 'Deficit'
  return 'Balanced'
})

const lastUpdate = computed(() => new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }))

const formatCurrencyUSD = (amount) => {
  if (amount == null) return '$ 0'
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)
}

/* ── Multi-currency ────────────────────────────────────────────────
 * `fmt(usdAmount)` returns:
 *   { primary: 'Rp 15,842,312', secondary: '≈ $ 1,000.00', code: 'IDR', sign: '' }
 *   { primary: '+Rp 1,500,000', secondary: '≈ $ 100.00', code: 'IDR', sign: '+' }
 *   { primary: '$ 50.00',         secondary: null,        code: 'USD', sign: '' } (USD selected)
 * `secondary` is `null` when the user has already picked USD, so the
 * template can `<small v-if="fmt(x).secondary">…</small>` to suppress
 * the duplicate line. See composables/useCurrency.js for details.
 */
const { ensureRatesLoaded } = useCurrency()
const fmt = (usdAmount, opts) => fmtAmount(usdAmount, opts)

const billDialogVisible    = ref(false)
const openBillDialog        = () => { billDialogVisible.value = true }
const onBillDialogSuccess   = () => { billDialogVisible.value = false; fetchBills(); fetchMonthlySummary(); fetchRecurringBills() }

const dismissAlert         = () => { alertDismissed.value = true }
const navigateToBills      = () => router.push('/bills')
const navigateToAnalytics  = () => router.push('/analytics')

const openBudgetDialog = () => {
  budgetInput.value = monthlySummary.value.monthlyBudget || 0
  budgetDialogVisible.value = true
}

const saveBudget = async () => {
  try {
    budgetSaving.value = true
    const userId = localStorage.getItem('userId')
    // Guard: ensure userId exists, otherwise backend will fail with 500.
    if (!userId || userId === 'null' || userId === 'undefined') {
      ElMessage.error('Session expired. Please log in again.')
      router.push('/login')
      return
    }
    // Guard: ensure the budget value is a valid non-negative number.
    const budgetValue = Number(budgetInput.value)
    if (Number.isNaN(budgetValue) || budgetValue < 0) {
      ElMessage.error('Please enter a valid budget amount (0 or greater)')
      return
    }
    const res = await request.put('/budget', {
      userId: Number(userId),
      monthlyBudget: budgetValue
    })
    if (res && res.message) {
      ElMessage.success(res.message)
    } else {
      ElMessage.success('Budget saved successfully')
    }
    budgetDialogVisible.value = false
    alertDismissed.value = false
    await fetchMonthlySummary()
  } catch (e) {
    // Show the actual server-side error message to make debugging easier.
    const serverMsg = e?.response?.data?.message || e?.message || 'Failed to save budget'
    console.error('saveBudget error:', e)
    ElMessage.error(serverMsg)
  } finally {
    budgetSaving.value = false
  }
}

/**
 * Returns the current user's id from localStorage, or null if it's
 * missing / corrupted. Used as a guard by every fetch* function so a
 * stale localStorage state can't fire a /api/bills?userId=null
 * request that would 400 and confuse the user.
 */
const getUserId = () => {
  const v = localStorage.getItem('userId')
  if (!v || v === 'null' || v === 'undefined' || v === '') return null
  return v
}

/**
 * Centralised \"you're not signed in\" recovery. Wipes the auth
 * tokens, surfaces a friendly toast, and routes to /login. Used by
 * every fetch function that detects a missing userId.
 */
const redirectToLogin = (reason) => {
  if (reason) ElMessage.warning(reason)
  clearTokens()
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login').catch(() => { /* ignore duplicate-nav */ })
  }
}

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    // Tell the backend to revoke BOTH the access token (jti -> denylist)
    // and the refresh token (hash -> revoked=1) before we wipe them
    // locally. logoutOnServer() is best-effort: a network failure
    // still proceeds to local cleanup so the UI is never stuck.
    // The await is wrapped so a slow /auth/logout can never block the
    // user from being redirected to /login.
    try {
      await Promise.race([
        logoutOnServer(),
        new Promise((resolve) => setTimeout(resolve, 2000)),
      ])
    } catch (_) { /* best-effort */ }
    clearAllAuth()
    router.push('/login').catch(() => { /* ignore duplicate-nav */ })
    ElMessage.success('Logged out')
  } else if (cmd === 'settings') {
    openBudgetDialog()
  }
}

const fetchBills = async () => {
  const userId = getUserId()
  if (!userId) {
    // No userId means the session is broken; send the user back to
    // /login instead of firing a request that's guaranteed to 400.
    redirectToLogin('Session expired. Please log in again.')
    return
  }
  try {
    loading.value = true
    const data = await request.get('/bills', { params: { userId } })
    // Use the shared `compareBillsDesc` so the underlying `bills` list
    // is also newest-first within the same day, not just `recentBills`.
    bills.value = Array.isArray(data) ? data.sort(compareBillsDesc) : []
  } catch (e) {
    // 401: the silent-refresh interceptor in request.js has already
    // tried to recover. If we get here, the session is gone.
    if (e?.response?.status === 401) {
      redirectToLogin('Session expired. Please log in again.')
      return
    }
    ElMessage.error('Failed to load bills')
  } finally {
    loading.value = false
  }
}

const fetchMonthlySummary = async () => {
  const userId = getUserId()
  if (!userId) {
    redirectToLogin('Session expired. Please log in again.')
    return
  }
  try {
    const today = new Date()
    const data = await request.get('/stats/summary', {
      params: { userId, month: today.getMonth() + 1, year: today.getFullYear() }
    })
    monthlySummary.value = {
      income:  data.totalIncome  || 0,
      expense: data.totalExpense || 0,
      balance: data.balance      || 0,
      monthlyBudget:     data.monthlyBudget     || null,
      budgetUsedPercent: data.budgetUsedPercent || 0,
      budgetWarning:     data.budgetWarning     || false,
      budgetExceeded:    data.budgetExceeded    || false,
    }
  } catch (e) {
    if (e?.response?.status === 401) {
      redirectToLogin('Session expired. Please log in again.')
      return
    }
    console.error(e)
  }
}

/* ── Upcoming Bills widget ───────────────────────────────────────────
 * Fetches the user's recurring-bill templates from GET /api/recurring-bills
 * and projects each one to its next firing date. Templates that fire
 * within the next 7-14 days (window chosen to match the task spec:
 * "anticipation" of upcoming bills) are surfaced in a compact card on
 * the dashboard, sorted by proximity, with a color-coded urgency chip.
 */

// "Today" as a midnight-anchored Date so day-diff math is stable.
const startOfToday = () => {
  const t = new Date()
  t.setHours(0, 0, 0, 0)
  return t
}

// Build a Date for the next time `dayOfMonth` falls on or after `from`.
// `dayOfMonth` is 1-28 (validated server-side), so this is safe in every month
// including February. If the template has a `lastRunYearMonth` matching the
// current month and the day has already passed, we skip to next month.
const nextOccurrence = (tpl, from) => {
  const day = Number(tpl?.dayOfMonth)
  if (!day || day < 1 || day > 28) return null
  const today = new Date(from)
  const y = today.getFullYear()
  const m = today.getMonth()
  // First candidate: this month on `day`.
  let candidate = new Date(y, m, day)
  // If the scheduler has already fired this template this month
  // (lastRunYearMonth === 'YYYY-MM' for the current month), advance to next.
  const lastRun = tpl.lastRunYearMonth
  const currentYM = `${y}-${String(m + 1).padStart(2, '0')}`
  if (lastRun === currentYM) {
    candidate = new Date(y, m + 1, day)
  }
  // If the candidate is in the past (day-of-month already passed and the
  // template hasn't fired this month yet, e.g. we're on the 5th and the
  // day-of-month is the 1st), advance one month.
  if (candidate < from) {
    candidate = new Date(y, m + 1, day)
  }
  return candidate
}

// Whole-day diff between two midnight-anchored Dates. Positive when `a` is
// after `b`. Used to drive the countdown chip.
const dayDiff = (a, b) => {
  const ms = a.getTime() - b.getTime()
  return Math.round(ms / 86400000)
}

// Project the raw templates into a flat list of "upcoming" rows.
// Filters to active templates whose next firing date is in the next
// 0-14 days (so a bill due *today* still shows up). Paused / soft-deleted
// templates are excluded.
const upcomingBills = computed(() => {
  const today = startOfToday()
  const horizon = 14
  const rows = []
  for (const tpl of recurringBills.value) {
    if (!tpl) continue
    // Backend uses `active` (0/1) and `isDeleted` (0/1). Treat both falsy
    // values as "not active".
    if (tpl.active === 0 || tpl.active === false) continue
    if (tpl.isDeleted === 1 || tpl.isDeleted === true) continue
    const due = nextOccurrence(tpl, today)
    if (!due) continue
    const diff = dayDiff(due, today)
    if (diff < 0 || diff > horizon) continue
    rows.push({
      id:        tpl.id,
      type:      tpl.type ?? 0,
      amount:    tpl.amount,
      category:  tpl.category,
      description: tpl.description,
      dayOfMonth: tpl.dayOfMonth,
      dueDate:   due,
      daysUntil: diff,
    })
  }
  return rows.sort((a, b) => a.daysUntil - b.daysUntil)
})

// Color-code the urgency chip. Red = today / tomorrow, amber = 2-3 days,
// yellow = 4-7 days, muted ash = 8-14 days.
const urgencyClass = (days) => {
  if (days <= 1) return 'upcoming-chip--red'
  if (days <= 3) return 'upcoming-chip--amber'
  if (days <= 7) return 'upcoming-chip--yellow'
  return 'upcoming-chip--ash'
}

const countdownLabel = (days) => {
  if (days === 0) return 'Due today'
  if (days === 1) return 'Due tomorrow'
  return `Due in ${days} days`
}

// "Aug 12" style short date. Avoids locale issues by building from the
// Date directly.
const formatDueDate = (d) => {
  if (!d) return ''
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

const fetchRecurringBills = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId || userId === 'null' || userId === 'undefined') return
  try {
    // The path is relative to request.js's baseURL ('/api'), so the
    // effective URL is /api/recurring-bills — the route declared on
    // RecurringBillController. Do NOT prefix with /api here, or
    // axios will produce /api/api/recurring-bills (404).
    const data = await request.get('/recurring-bills', { params: { userId } })
    recurringBills.value = Array.isArray(data) ? data : []
  } catch (e) {
    // Non-fatal: the widget will just render the empty state.
    console.warn('Failed to load recurring bills:', e)
    recurringBills.value = []
  }
}

onMounted(() => {
  // Make sure the rates are loaded so the multi-currency formatter can
  // render right away on first paint. Safe to call multiple times — the
  // composable de-duplicates the fetch.
  ensureRatesLoaded()
  fetchBills(); fetchMonthlySummary(); fetchRecurringBills()
})
</script>

<style scoped>
/* ── Page Layout ── */
.page {
  padding: 28px 32px;
  min-height: 100vh;
}

/* ── Alert Bar ── */
.alert-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 20px;
  border-left: 3px solid;
  border-radius: 2px;
}

.alert-bar--exceeded {
  background: rgba(239,68,68,0.07);
  border-left-color: var(--red);
  color: var(--red);
}

.alert-bar--warning {
  background: rgba(245,158,11,0.07);
  border-left-color: var(--amber);
  color: var(--amber);
}

.alert-bar__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
  font-size: 12px;
}

.alert-bar__body strong {
  font-size: 11px;
  letter-spacing: 1px;
  font-weight: 700;
}

.alert-bar__close {
  background: none;
  border: none;
  cursor: pointer;
  color: inherit;
  font-size: 14px;
  opacity: 0.5;
  padding: 2px 4px;
  transition: opacity 0.15s;
}
.alert-bar__close:hover { opacity: 1; }

.alert-slide-enter-active, .alert-slide-leave-active { transition: all 0.25s ease; }
.alert-slide-enter-from, .alert-slide-leave-to { opacity: 0; transform: translateY(-8px); }

/* ── Page Header ── */
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
  line-height: 1.1;
}

.accent { color: var(--ember); }

.page-date {
  font-size: 12px;
  color: var(--ash);
  margin-top: 4px;
}

.page-header__actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* ── Buttons ── */
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
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--wire);
  border-radius: var(--radius-sm);
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-ghost:hover { border-color: var(--muted); color: var(--bone); }

.btn-outline {
  padding: 8px 16px;
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
  text-align: center;
}
.btn-outline:hover { border-color: var(--ember); color: var(--ember); background: rgba(240,90,20,0.05); }

.btn-link {
  background: none;
  border: none;
  color: var(--ash);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.8px;
  cursor: pointer;
  transition: color 0.15s;
}
.btn-link:hover { color: var(--ember); }

/* ── Stats Row ── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 24px 28px;
  background: var(--graphite);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}

.stat-card__label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--ash);
  margin-bottom: 12px;
}

.stat-card__value {
  font-family: var(--font-display);
  font-size: 40px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: var(--white);
  line-height: 1;
  margin-bottom: 8px;
  /* Keep the sign glued to the number. The default browser line-break
     rules treat U+002D (the ASCII hyphen in "-$169,648.00") as a
     soft wrap point, so at 40px the string can break between the "-"
     and the value, producing a "-" on one line and "$169,648.00" on
     the next. nowrap prevents that without affecting the other
     (short) stat-card values (22, 21, DEFICIT). */
  white-space: nowrap;
}

.stat-card__sub {
  font-size: 11px;
  color: var(--ash);
}

/* ── Main Grid ── */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
}

/* ── Panel ── */
.panel {
  background: var(--graphite);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

/* When set on a panel, the panel becomes a flex column and the inner
   el-table is stretched to fill the remaining vertical space. Used on
   the Dashboard's RECENT TRANSACTIONS panel so it matches the height
   of the right-hand sidebar. */
.panel--fill {
  display: flex;
  flex-direction: column;
}
.panel--fill :deep(.el-table) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.panel--fill :deep(.el-table__inner-wrapper) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.panel--fill :deep(.el-table__body-wrapper) {
  flex: 1;
  min-height: 0;
}
/* Make the table body a flex column and stretch each row so the
   visible rows evenly fill the panel's remaining vertical space.
   min-height keeps a row from collapsing when the panel is very
   small (mobile). */
.panel--fill :deep(.el-table__body) {
  display: flex;
  flex-direction: column;
  width: 100%;
}
.panel--fill :deep(.el-table__row) {
  display: flex;
  flex: 1 1 auto;
  min-height: 48px;
}
.panel--fill :deep(.el-table__row td.el-table__cell) {
  flex: 1;
}

.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--wire);
}

.panel__label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: var(--ash);
}

/* Table overrides */
.panel :deep(.el-table) { border-radius: 0 !important; }
.panel :deep(.el-table__header-wrapper) { border-bottom: 1px solid var(--wire) !important; }

/* ── Empty ── */
.empty { padding: 40px 0; }
.empty :deep(.el-empty__description p) { color: var(--ash); font-size: 12px; }

/* ── Sidebar ── */
.sidebar { display: flex; flex-direction: column; gap: 16px; }

/* ── KV List ── */
.kv-list { padding: 16px 20px; display: flex; flex-direction: column; gap: 10px; }

.kv-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kv-key {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.2px;
  color: var(--ash);
}

.kv-val {
  font-size: 13px;
  color: var(--bone);
  font-weight: 500;
}

.kv-divider { height: 1px; background: var(--wire); margin: 4px 0; }

/* Budget block */
.budget-block { display: flex; flex-direction: column; gap: 8px; }
.budget-block__header { display: flex; justify-content: space-between; align-items: center; }

.budget-track {
  height: 3px;
  background: var(--wire);
  border-radius: 99px;
  overflow: hidden;
}

.budget-fill {
  height: 100%;
  background: var(--green);
  border-radius: 99px;
  transition: width 0.5s ease;
}
.budget-fill--warning  { background: var(--amber); }
.budget-fill--exceeded { background: var(--red); }

.budget-meta { display: flex; justify-content: space-between; font-size: 11px; }

/* ── Action List ── */
.action-list { padding: 12px 16px; display: flex; flex-direction: column; gap: 4px; }

.action-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
  font-family: var(--font-body);
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
  width: 100%;
}
.action-item:hover {
  border-color: var(--wire);
  color: var(--bone);
  background: var(--slate);
}

/* ── Dialog ── */
.dialog-body { display: flex; flex-direction: column; gap: 16px; }

.dialog-desc {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
}

.budget-preview {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 600;
  color: var(--ember);
  margin-top: 4px;
}

/* ── Multi-currency: two-line amount cell ─────────────────────────
 * Reused everywhere a monetary amount is shown. The primary line is
 * the user's selected currency (big), the secondary line is the
 * canonical USD value (small, gray). When the user has already picked
 * USD the secondary line is removed by the template (v-if), so it
 * never reads "$ 10.00 / $ 10.00".
 */
.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
  line-height: 1.15;
}
.amount-primary { font-size: 13px; }
.amount-secondary {
  font-size: 10px;
  color: var(--muted);
  font-family: var(--font-mono);
  letter-spacing: 0.2px;
}

.kv-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
  line-height: 1.15;
}
.kv-amount__sub {
  font-size: 10px;
  color: var(--muted);
  font-family: var(--font-mono);
}

.stat-card__sub--converted {
  font-family: var(--font-mono);
  letter-spacing: 0.2px;
}

.upcoming-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
  line-height: 1.15;
  flex-shrink: 0;
}

/* ── Utilities ── */
.mono   { font-family: var(--font-mono) !important; }
.fw-600 { font-weight: 600 !important; }
.fw-700 { font-weight: 700 !important; }
.text-green { color: var(--green) !important; }
.text-red   { color: var(--red) !important; }
.text-amber { color: var(--amber) !important; }
.text-ash   { color: var(--ash) !important; }
.value--green { color: var(--green) !important; }
.value--red   { color: var(--red) !important; }
.full-width { width: 100%; }
.mt-12 { margin-top: 12px; }

/* Badge */
.badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  border-radius: 6px;
}
.badge--income  { background: rgba(34,197,94,0.1);  color: var(--green); }
.badge--expense { background: rgba(239,68,68,0.1);  color: var(--red); }

/* ── Upcoming Bills Widget ──
 * Compact list rendered in the right sidebar. Each row shows a colored
 * category icon, the bill name + amount (in USD), and a color-coded
 * countdown chip ("Due in N days"). Color tiers:
 *   red    = today / tomorrow
 *   amber  = 2-3 days
 *   yellow = 4-7 days
 *   ash    = 8-14 days
 */
.panel__hint {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.2px;
  color: var(--ash);
  padding: 2px 6px;
  border: 1px solid var(--wire);
  border-radius: 4px;
}

.upcoming-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 20px;
  color: var(--ash);
  text-align: center;
}
.upcoming-empty svg { opacity: 0.5; }
.upcoming-empty p {
  font-size: 12px;
  line-height: 1.4;
  max-width: 220px;
}

.upcoming-list {
  list-style: none;
  margin: 0;
  padding: 8px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 360px;
  overflow-y: auto;
}

.upcoming-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 8px;
  border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.upcoming-item:hover { background: var(--slate); }

.upcoming-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
}
.upcoming-icon--expense { color: var(--red);   background: rgba(239, 68, 68, 0.10); }
.upcoming-icon--income  { color: var(--green); background: rgba(34, 197, 94, 0.10); }

.upcoming-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0; /* allow the name to truncate inside flex */
}

.upcoming-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}
.upcoming-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--bone);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}
.upcoming-amount { font-size: 13px; }

.upcoming-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.upcoming-date {
  font-size: 11px;
  color: var(--ash);
  font-family: var(--font-mono);
}

.upcoming-chip {
  display: inline-block;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-radius: 99px;
  white-space: nowrap;
}
.upcoming-chip--red    { background: rgba(239, 68, 68, 0.14);  color: var(--red);   }
.upcoming-chip--amber  { background: rgba(245, 158, 11, 0.14); color: var(--amber); }
.upcoming-chip--yellow { background: rgba(234, 179, 8, 0.14);  color: #eab308;      }
.upcoming-chip--ash    { background: var(--wire);              color: var(--ash);   }

/* ── Responsive ── */
@media (max-width: 1200px) {
  .main-grid { grid-template-columns: 1fr; }
  .sidebar { display: grid; grid-template-columns: 1fr 1fr; }
}

@media (max-width: 1024px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .page { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 16px; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .sidebar { grid-template-columns: 1fr; }
  .page-title { font-size: 22px; }
}

@media (max-width: 480px) {
  .stats-row { grid-template-columns: 1fr; }
  .page { padding: 12px; }
}
</style>
