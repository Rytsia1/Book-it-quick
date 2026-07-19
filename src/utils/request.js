import axios from 'axios'
import {
    getAccessToken,
    refreshAccessToken,
    msUntilAccessExpiry,
    clearTokens,
} from '@/utils/auth'

/**
 * Token-bearing axios instance used by every API call in the app.
 *
 * <h2>Why this file does NOT import the Vue router</h2>
 * Importing `@/router` here would create a circular import: the
 * router file imports the views, and every view imports this file.
 * Under ES-module rules a circular import can return a partially
 * evaluated module, so by the time the 401 interceptor runs,
 * {@code router.currentRoute} / {@code router.push} may be
 * {@code undefined}. Worse, on some bundlers the partial evaluation
 * cascades and {@code request.js} itself fails to load \u2014 which in
 * turn breaks {@code Login.vue} (it can't even call {@code /auth/login}).
 * The user sees a blank /login page.
 *
 * Instead, this module exposes {@link onAuthFailure}, a tiny event
 * hook. {@code App.vue} subscribes once at mount and translates the
 * event into an actual {@code router.push('/login')}. The 401
 * interceptor just dispatches the event. No router import, no cycle.
 */

const instance = axios.create({
    // Update this line with the local back-end address.
    baseURL: 'http://localhost:8080/api',
    timeout: 10000,
})

// ──────────────────────────── Request interceptor ────────────────────────────
// Attach the (short-lived) access token as a Bearer header. We do NOT
// attach the refresh token here \u2014 it only travels to /refresh.
instance.interceptors.request.use(
    (config) => {
        const token = getAccessToken()
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// ──────────────────────────── Auth-failure event bus ────────────────────────────
//
// 401 events that need a redirect-to-/login flow through here.
// App.vue subscribes once on mount and turns the event into a real
// router navigation. Using a window CustomEvent (rather than a
// direct router.push) keeps request.js free of any @/router import.

const AUTH_FAILURE_EVENT = 'app:auth-failure'

/**
 * Register a handler for the global auth-failure event.
 * Returns an unsubscribe function. Safe to call from any setup().
 */
export function onAuthFailure(handler) {
    if (typeof window === 'undefined') return () => {}
    window.addEventListener(AUTH_FAILURE_EVENT, handler)
    return () => window.removeEventListener(AUTH_FAILURE_EVENT, handler)
}

/** Fire the auth-failure event. The App-level listener does the redirect. */
function emitAuthFailure(detail) {
    if (typeof window === 'undefined') return
    window.dispatchEvent(new CustomEvent(AUTH_FAILURE_EVENT, { detail }))
}

/**
 * Decide whether a given request URL is one of the auth endpoints
 * that should NOT trigger a silent refresh on 401 (we'd just recurse
 * into /refresh forever, or fight the Login.vue flow).
 */
function isAuthEndpoint(url) {
    if (!url) return false
    return url.includes('/auth/refresh')
        || url.includes('/auth/login')
        || url.includes('/auth/register')
        || url.includes('/auth/logout')
}

// ──────────────────────────── Response interceptor ────────────────────────────
// 401 handling:
//   1. The 401 came from /auth/* (login/register/refresh/logout) \u2014
//      pass the error through; the caller (Login.vue or the logout
//      handler) already deals with it.
//   2. The 401 came from a regular protected endpoint \u2014 try one
//      silent refresh, then retry the original request. If the
//      refresh fails, clear tokens and dispatch the auth-failure
//      event so App.vue can route to /login.
instance.interceptors.response.use(
    (response) => response.data,
    async (error) => {
        const original = error.config || {}
        const status = error.response?.status
        const url = typeof original.url === 'string' ? original.url : ''

        if (status === 401 && !isAuthEndpoint(url) && !original._retried) {
            try {
                const newToken = await refreshAccessToken()
                original._retried = true
                original.headers = original.headers || {}
                original.headers.Authorization = `Bearer ${newToken}`
                return instance(original)
            } catch (_) {
                // Refresh failed. Wipe the tokens (identity stays \u2014 see
                // clearTokens in auth.js) and ask App.vue to route to
                // /login. The event is decoupled so request.js doesn't
                // have to import the router (which would create a
                // circular import through the views).
                clearTokens()
                emitAuthFailure({ reason: 'refresh-failed' })
                return Promise.reject(error)
            }
        }

        // Non-401, or the request was an auth endpoint \u2014 pass through.
        // The view layer surfaces e.response.data.message via
        // ElMessage.error.
        return Promise.reject(error)
    }
)

// ──────────────────────────── Proactive refresh scheduler ────────────────────────────
// Refresh the access token ~60 s before it would naturally expire.
// Without this, a user actively using the app right as the access
// token dies would see a flash of 401-retry on every click.
const REFRESH_LEAD_MS = 60 * 1000
let proactiveTimer = null

function scheduleProactiveRefresh() {
    if (proactiveTimer) {
        clearTimeout(proactiveTimer)
        proactiveTimer = null
    }
    const ms = msUntilAccessExpiry()
    if (ms <= 0) return
    const delay = Math.max(ms - REFRESH_LEAD_MS, 0)
    proactiveTimer = setTimeout(() => {
        refreshAccessToken()
            .then(scheduleProactiveRefresh)   // chain to schedule the next one
            .catch(() => { /* request interceptor handles the redirect */ })
    }, delay)
}

// Re-schedule whenever the user comes back to the tab. The browser
// may have throttled setTimeout while the tab was hidden, so the
// scheduled fire could be in the past; the recompute in
// scheduleProactiveRefresh handles that.
if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) scheduleProactiveRefresh()
    })
    // Initial schedule on module load so a page refresh mid-session
    // still gets a proactive refresh.
    scheduleProactiveRefresh()
}

export default instance
