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

      <!--
        RBAC: the Admin link is only rendered when the current user
        has role === 'ADMIN'. The role is read from localStorage
        (set by Login.vue on a successful login). The server-side
        @PreAuthorize("hasRole('ADMIN')") on AdminController is
        the actual access-control boundary; this is purely a UI
        affordance so regular users don't see a link that would
        403 them.
      -->
      <router-link
        v-if="isAdmin"
        to="/admin"
        class="nav-item nav-item--admin"
        active-class="nav-item--active"
      >
        Admin
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
        <!--
          RBAC: small inline badge showing the user's role. Hidden
          for regular users (the 'Admin' nav link already tells them
          they're not an admin); visible for admins. Cheap visual
          confirmation that the JWT's role claim is what the server
          is using for @PreAuthorize checks.
        -->
        <span v-if="isAdmin" class="role-badge" title="You have the ADMIN role">ADMIN</span>
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
import { computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// Cross-module cleanup hooks. We import them here (not inside the
// handler) so the import cost is paid once at module load, not on
// every logout click.
import { cancelScheduledRefresh } from '@/utils/request'
import { cancelAutoRetry } from '@/composables/useCurrency'
import { resetRefreshState } from '@/utils/auth'
import CurrencySelector from '@/components/CurrencySelector.vue'
import {
    getAccessToken,
    getRefreshToken,
    wipeAllLocalStorage,
} from '@/utils/auth'

const router   = useRouter()
const username = computed(() => localStorage.getItem('username') || 'user')

// RBAC: read the role from localStorage and surface it as a boolean.
// auth.getRole() already defaults to 'USER' if the key is missing
// or has an unknown value, so this is safe on every page load.
import { getRole } from '@/utils/auth'
const isAdmin = computed(() => getRole() === 'ADMIN')

const handleLogout = () => {
  ElMessageBox.confirm('Are you sure you want to log out?', 'Confirmation', {
    confirmButtonText: 'Logout',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(() => {
    // WIPE-THEN-NAVIGATE. The previous version awaited a 2 s
    // logoutOnServer() race before navigating, which made the UI
    // feel frozen and — because the navbar's showNavBar is reactive
    // on route.path — left the navbar visible during the wait, so a
    // second click could fire a second router.push('/login') and the
    // Vue lifecycle would tear down the component while ElMessage
    // was still queuing. The user saw the toast but the page
    // didn't visibly change.
    //
    // The fix is to do everything synchronously inside the .then
    // callback: cancel the proactive-refresh + auto-retry timers,
    // cancel any in-flight silent refresh, wipe local state, queue
    // the navigation, schedule the toast for the next tick (so it
    // renders on the new /login view), and fire the server-side
    // revoke in the background.

    // 0) Cancel the cross-module timers and the in-flight refresh
    //    BEFORE wiping localStorage. This is the critical fix for
    //    the "token re-appears after logout" bug:
    //      - cancelScheduledRefresh() clears the proactive-refresh
    //        setTimeout (which, if it fired, would call
    //        refreshAccessToken() → setTokens() and silently
    //        re-populate the tokens).
    //      - resetRefreshState() drops the single-flight
    //        refreshInFlight promise so any silent refresh that's
    //        already mid-flight (and about to call setTokens()) is
    //        detached. (Even if it lands after the wipe, the
    //        loggedOut tombstone flag in auth.js blocks the write.)
    //      - cancelAutoRetry() clears the 60s exchange-rates
    //        retry interval. Hygiene, not security.
    //    Order matters: cancel first, then wipe, so the timers
    //    can't sneak in a setTokens() call between the cancel and
    //    the wipe.
    cancelScheduledRefresh()
    resetRefreshState()
    cancelAutoRetry()

    // 1) Capture tokens BEFORE wiping, so the background revoke
    //    request can still authenticate.
    const accessToken  = getAccessToken()
    const refreshToken = getRefreshToken()

    // 2) Wipe local state. We use wipeAllLocalStorage() (a
    //    superset of clearAllAuth) so every known key — including
    //    the legacy `token` key that the router guard checks for
    //    — is gone, AND the loggedOut tombstone flag in auth.js
    //    is raised so a racing setTokens() that somehow runs
    //    after this is a no-op.
    wipeAllLocalStorage()

    // 3) Fire the toast SYNCHRONOUSLY. `el-message` is appended to
    //    document.body in the same microtask, so the toast is in
    //    the DOM before any navigation tears the navbar down.
    //    The user explicitly asked for "redirect tepat setelah
    //    notifikasi" — we honour that by queuing the navigation
    //    in the next two steps, AFTER the toast is committed.
    ElMessage.success('Logged out successfully')

    // 4) Fire-and-forget server-side revoke. We deliberately use
    //    raw fetch (not the `request` instance) so the axios
    //    interceptors can't interfere with the cleanup. keepalive
    //    tells the browser to keep the request alive even if the
    //    page navigates away during it. Errors are swallowed
    //    because the local cleanup has already happened.
    if (accessToken || refreshToken) {
      try {
        fetch('http://localhost:8080/api/auth/logout', {
          method:  'POST',
          headers: { 'Content-Type': 'application/json' },
          body:    JSON.stringify({ accessToken, refreshToken }),
          keepalive: true,
        }).catch(() => { /* best-effort */ })
      } catch (_) { /* best-effort */ }
    }

    // 5) Hard browser-level redirect to /login. We deliberately
    //    skip the SPA `router.push('/login')` here because the
    //    previous attempts (router.push + 120 ms setTimeout
    //    fallback) still left the user on the dashboard in some
    //    edge cases. `window.location.href` is a plain browser
    //    API that cannot be blocked by any Vue Router state, any
    //    navigation guard, or any async race. The 50 ms delay
    //    is just to give the toast (step 3) one render frame
    //    before the hard reload destroys the page. The toast
    //    will be visible on the freshly-loaded /login page.
    setTimeout(() => {
      window.location.href = '/login'
    }, 50)
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

/* RBAC: visually distinguish the Admin link so admins can spot it
   at a glance. Slightly higher contrast than the regular links, and
   a thin ember underline even when inactive. */
.nav-item--admin {
  color: var(--ember);
}
.nav-item--admin:hover { color: var(--spark); }
.nav-item--admin.nav-item--active {
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

/* RBAC: small inline badge next to the username, visible only for
   admins. Confirms to the admin that the JWT's role claim is
   what the server is using for @PreAuthorize checks. */
.role-badge {
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 1.2px;
  color: var(--ember);
  background: rgba(240, 90, 20, 0.10);
  border: 1px solid rgba(240, 90, 20, 0.30);
  border-radius: 99px;
  padding: 2px 8px;
  margin-left: 4px;
  text-transform: uppercase;
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
