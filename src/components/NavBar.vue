<template>
  <nav class="navbar">
    <!-- Brand -->
    <router-link to="/dashboard" class="brand">
      <div class="brand-icon">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <rect width="20" height="20" rx="2" fill="#F05A14"/>
          <path d="M4 10h12M10 4v12" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      <span class="brand-text">BOOKKEEPING</span>
    </router-link>

    <!-- Navigation -->
    <div class="nav-links">
      <router-link to="/dashboard" class="nav-item" active-class="nav-item--active">
        Dashboard
      </router-link>
      <router-link to="/bills" class="nav-item" active-class="nav-item--active">
        Bills
      </router-link>
      <router-link to="/budget" class="nav-item" active-class="nav-item--active">
        Budget
      </router-link>
      <router-link to="/analytics" class="nav-item" active-class="nav-item--active">
        Analytics
      </router-link>
      <router-link to="/categories" class="nav-item" active-class="nav-item--active">
        Categories
      </router-link>
    </div>

    <!-- Right -->
    <div class="nav-right">
      <!--
        Global multi-currency selector. Sits in the nav so the chosen
        display currency is consistent across every page; the
        composable stores the choice in localStorage and shares the
        rates cache with all components.
      -->
      <CurrencySelector />

      <span class="nav-user">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/>
        </svg>
        {{ username }}
      </span>
      <button class="logout-btn" @click="handleLogout">
        Logout
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
          <polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
        </svg>
      </button>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CurrencySelector from '@/components/CurrencySelector.vue'

const router   = useRouter()
const username = computed(() => localStorage.getItem('username') || 'user')

const handleLogout = () => {
  ElMessageBox.confirm('Are you sure you want to log out?', 'Confirmation', {
    confirmButtonText: 'Logout',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
    router.push('/login')
    ElMessage.success('Logged out successfully')
  }).catch(() => {})
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 24px;
  background: var(--ink);
  border-bottom: 1px solid var(--wire);
  position: sticky;
  top: 0;
  z-index: 100;
  gap: 32px;
}

/* Brand */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.brand-icon { display: flex; align-items: center; }

.brand-text {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--white);
}

/* Nav links */
.nav-links {
  display: flex;
  align-items: stretch;
  gap: 0;
  flex: 1;
  height: 100%;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ash);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: color 0.15s, border-color 0.15s;
  white-space: nowrap;
}

.nav-item:hover { color: var(--bone); }

.nav-item--active {
  color: var(--white) !important;
  border-bottom-color: var(--ember) !important;
}

/* Right */
.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ash);
  font-weight: 500;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--wire);
  border-radius: var(--radius-sm);
  color: var(--ash);
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font-body);
  cursor: pointer;
  transition: all 0.15s;
  letter-spacing: 0.5px;
}

.logout-btn:hover {
  border-color: var(--red);
  color: var(--red);
  background: rgba(239,68,68,0.05);
}

/* Responsive */
@media (max-width: 768px) {
  .navbar { padding: 0 16px; gap: 16px; }
  .brand-text { display: none; }
  .nav-item { padding: 0 10px; font-size: 12px; }
  .nav-user { display: none; }
  .logout-btn span { display: none; }
}
</style>
