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
          <span>Expenses {{ formatCurrencyUSD(monthlySummary.expense) }} of {{ formatCurrencyUSD(monthlySummary.monthlyBudget) }} ({{ (monthlySummary.budgetUsedPercent || 0).toFixed(1) }}%)</span>
        </div>
        <button class="alert-bar__close" @click="dismissAlert">✕</button>
      </div>
    </transition>

    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__left">
        <p class="page-eyebrow">BOOKKEEPING / DASHBOARD</p>
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
      <div class="stat-card">
        <p class="stat-card__label">NET BALANCE</p>
        <p class="stat-card__value mono" :class="monthlySummary.balance >= 0 ? 'value--green' : 'value--red'">
          {{ formatCurrencyUSD(monthlySummary.balance) }}
        </p>
        <p class="stat-card__sub">{{ lastUpdate }}</p>
      </div>
    </div>

    <!-- Main Grid -->
    <div class="main-grid">
      <!-- Recent Bills -->
      <section class="panel">
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
          <el-table-column label="AMOUNT" width="160">
            <template #default="{ row }">
              <span :class="['mono', 'fw-600', row.type === 1 ? 'text-green' : 'text-red']">
                {{ row.type === 1 ? '+' : '-' }}{{ formatCurrencyUSD(row.amount) }}
              </span>
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
              <span class="kv-val mono text-green">+{{ formatCurrencyUSD(monthlySummary.income) }}</span>
            </div>
            <div class="kv-row">
              <span class="kv-key">EXPENSE</span>
              <span class="kv-val mono text-red">-{{ formatCurrencyUSD(monthlySummary.expense) }}</span>
            </div>
            <div class="kv-divider"></div>
            <div class="kv-row">
              <span class="kv-key">BALANCE</span>
              <span class="kv-val mono fw-700" :class="monthlySummary.balance >= 0 ? 'text-green' : 'text-red'">
                {{ formatCurrencyUSD(monthlySummary.balance) }}
              </span>
            </div>

            <!-- Budget -->
            <template v-if="monthlySummary.monthlyBudget">
              <div class="kv-divider"></div>
              <div class="budget-block">
                <div class="budget-block__header">
                  <span class="kv-key">BUDGET</span>
                  <span class="kv-val mono">{{ formatCurrencyUSD(monthlySummary.monthlyBudget) }}</span>
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
                      remaining {{ formatCurrencyUSD(monthlySummary.monthlyBudget - monthlySummary.expense) }}
                    </template>
                    <template v-else>
                      over by {{ formatCurrencyUSD(monthlySummary.expense - monthlySummary.monthlyBudget) }}
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
            {{ formatCurrencyUSD(budgetInput) }}
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

const router = useRouter()

const bills = ref([])
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

const recentBills = computed(() => bills.value.slice(0, 6))
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

const billDialogVisible    = ref(false)
const openBillDialog        = () => { billDialogVisible.value = true }
const onBillDialogSuccess   = () => { billDialogVisible.value = false; fetchBills(); fetchMonthlySummary() }

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

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
    router.push('/login')
    ElMessage.success('Logged out')
  } else if (cmd === 'settings') {
    openBudgetDialog()
  }
}

const fetchBills = async () => {
  try {
    loading.value = true
    const userId = localStorage.getItem('userId')
    const data = await request.get('/bills', { params: { userId } })
    bills.value = Array.isArray(data) ? data.sort((a, b) => new Date(b.billDate) - new Date(a.billDate)) : []
  } catch (e) {
    ElMessage.error('Failed to load bills')
  } finally {
    loading.value = false
  }
}

const fetchMonthlySummary = async () => {
  try {
    const today = new Date()
    const userId = localStorage.getItem('userId')
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
  } catch (e) { console.error(e) }
}

onMounted(() => { fetchBills(); fetchMonthlySummary() })
</script>

<style scoped>
/* ── Page Layout ── */
.page {
  padding: 28px 32px;
  max-width: 1440px;
  margin: 0 auto;
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
