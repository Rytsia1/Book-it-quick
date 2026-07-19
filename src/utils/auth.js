import axios from 'axios'

/**
 * Token-storage helpers + a single-flight silent refresh mechanism.
 *
 * <h2>Why this file exists</h2>
 * The auth flow now uses two tokens:
 *   - accessToken  (JWT, 15-minute lifetime)  \u2014 attached to every
 *     request as `Authorization: Bearer ...`.
 *   - refreshToken (opaque, 7-day lifetime)  \u2014 only sent to
 *     `/api/auth/refresh` when the access token expires.
 *
 * Both are kept in localStorage so the user stays signed in across
 * page reloads, and both are wiped on logout. We also keep a numeric
 * `accessTokenExpiresAt` (ms epoch) so the request layer can
 * proactively refresh ~1 minute before the access token dies,
 * avoiding the "user clicks a button right as the token expires"
 * race.
 *
 * <h2>Single-flight refresh</h2>
 * If 10 in-flight requests all see a 401 at the same time, they must
 * NOT all call `/refresh` independently \u2014 that would burn 10
 * refresh tokens (rotation) and log the user out. Instead, the first
 * request triggers the refresh and stores the in-flight promise on a
 * module-level variable; the other 9 await the same promise.
 */

const ACCESS_KEY  = 'accessToken'
const REFRESH_KEY = 'refreshToken'
const EXPIRES_KEY = 'accessTokenExpiresAt' // ms epoch

// Storage keys used elsewhere in the app (kept consistent with Login.vue
// and the router guard).
const USERNAME_KEY = 'username'
const USERID_KEY   = 'userId'

// ──────────────────────────── Token storage ────────────────────────────

export function getAccessToken() {
    return localStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken() {
    return localStorage.getItem(REFRESH_KEY)
}

export function getAccessTokenExpiresAt() {
    const v = localStorage.getItem(EXPIRES_KEY)
    return v ? Number(v) : 0
}

export function setTokens({ accessToken, refreshToken, accessTokenExpiresAt }) {
    if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken)
    if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
    if (accessTokenExpiresAt) localStorage.setItem(EXPIRES_KEY, String(accessTokenExpiresAt))
}

// ──────────────────────────── Token storage ────────────────────────────

/**
 * Wipe ONLY the auth tokens (access + refresh + expiry). Leaves the
 * user's identity (userId, username) in localStorage so a follow-up
 * login can pre-fill the form and the rest of the app can still
 * display the previous user's name in the navbar.
 *
 * Use this from the silent-refresh-on-401 fallback in request.js: we
 * are about to redirect to /login, but the user's identity is still
 * useful to remember across the bounce.
 *
 * For a full sign-out (including wiping username/userId) use
 * {@link clearAllAuth}.
 */
export function clearTokens() {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(EXPIRES_KEY)
}

/**
 * Full sign-out: wipes tokens AND user identity. Called only from the
 * explicit Logout button (NavBar.vue / Dashboard.vue), never from
 * the silent-refresh interceptor.
 */
export function clearAllAuth() {
    clearTokens()
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(USERID_KEY)
}

// ──────────────────────────── Refresh mechanism ────────────────────────────

// A single in-flight refresh promise, shared by every caller. Once
// set, every subsequent caller awaits the same promise. Cleared on
// success or failure.
let refreshInFlight = null

/**
 * Exchange the current refresh token for a new (access, refresh)
 * pair. Returns a Promise that resolves to the new access token on
 * success, or rejects on failure (expired refresh token, network
 * error, etc.). On rejection the caller should redirect to /login.
 *
 * Concurrent callers automatically share the same in-flight request
 * (single-flight) so a flurry of 401s only triggers one /refresh.
 */
export function refreshAccessToken() {
    if (refreshInFlight) return refreshInFlight

    const refreshToken = getRefreshToken()
    if (!refreshToken) {
        return Promise.reject(new Error('No refresh token available'))
    }

    // Use a fresh axios instance so the response interceptor below
    // (which would otherwise retry on 401) doesn't recursively loop.
    const http = axios.create({
        baseURL: 'http://localhost:8080/api',
        timeout: 10000,
    })

    refreshInFlight = http
        .post('/auth/refresh', {
            refreshToken,
            // Include the (possibly expired) access token so the server
            // can check its jti against the denylist at /refresh.
            accessToken: getAccessToken(),
        })
        .then((response) => {
            const data = response.data || response
            const accessToken         = data.token             || data.accessToken
            const newRefreshToken     = data.refreshToken
            const accessTokenExpiresAt = data.accessTokenExpiresAt
            if (!accessToken || !newRefreshToken) {
                throw new Error('Malformed refresh response')
            }
            setTokens({ accessToken, refreshToken: newRefreshToken, accessTokenExpiresAt })
            return accessToken
        })
        .finally(() => {
            // Always clear so a future 401 can start a fresh refresh.
            refreshInFlight = null
        })

    return refreshInFlight
}

/**
 * Compute how many ms remain until the access token expires. Negative
 * values mean the token has already expired. The schedule function in
 * `request.js` uses this to decide whether to refresh proactively.
 */
export function msUntilAccessExpiry() {
    return getAccessTokenExpiresAt() - Date.now()
}

// ──────────────────────────── Logout ────────────────────────────

/**
 * Tell the backend to revoke the tokens, then wipe them locally.
 * Always resolves (even on network error) so a flaky network can't
 * leave the user in a half-logged-in state.
 */
export async function logoutOnServer() {
    const accessToken  = getAccessToken()
    const refreshToken = getRefreshToken()
    try {
        await axios.post('http://localhost:8080/api/auth/logout',
            { accessToken, refreshToken },
            { timeout: 5000 }
        )
    } catch (_) {
        // best-effort
    }
}
