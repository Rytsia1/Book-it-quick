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
 * Full sign-out: wipes tokens AND identity. Called only from the
 * explicit Logout button (NavBar.vue / Dashboard.vue), never from
 * the silent-refresh interceptor.
 */
export function clearAllAuth() {
    clearTokens()
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(USERID_KEY)
}

/**
 * Total nuclear option: wipe EVERYTHING in localStorage (and
 * sessionStorage, just in case). Used on logout so a stale 'token',
 * 'jwt', 'userToken', or any other legacy key from an older version
 * of the app can't accidentally keep the router guard or the
 * silent-refresh interceptor in an "authed" state. After this call,
 * the next hard reload (or page navigation) will see a completely
 * empty localStorage and treat the user as fully logged out.
 *
 * We use this instead of just clearAllAuth() because the previous
 * version only knew about the keys it itself wrote; if a key from
 * an older release or a hand-set value lingered, the router guard
 * would still see "authenticated" and bounce the user right back
 * to /dashboard right after the logout hard-reload completed.
 *
 * We do this in three layers, in order:
 *
 *  1. Explicit removeItem() for every key we KNOW about. This is
 *     the "belt-and-suspenders" layer — even if localStorage.clear()
 *     somehow missed a key (it shouldn't, but defense-in-depth),
 *     the explicit removals guarantee those specific keys are gone.
 *
 *  2. localStorage.clear() — nuke every remaining key. Covers any
 *     key we don't know about (older code paths, hand-set values,
 *     third-party libraries that stash auth tokens under their own
 *     key name).
 *
 *  3. sessionStorage.clear() — same reasoning, in case the JWT or
 *     user profile is being stored there by some other part of the
 *     app or a legacy code path.
 */
export function wipeAllLocalStorage() {
    // 1) Explicit removeItem for every key we know about, in BOTH
    //    storages. This is a no-op if the key doesn't exist, so it's
    //    safe to call even when localStorage is empty. removeItem
    //    throws on a disabled storage in some browsers, hence the
    //    try/catch wrappers.
    const keysToWipe = [
        // Keys this app writes (the canonical set).
        'accessToken',
        'token',
        'refreshToken',
        'accessTokenExpiresAt',
        'username',
        'userId',
        // Common legacy / alt names the router guard or Login.vue
        // might be reading. Listed explicitly so a stale entry
        // can't keep the user "logged in" after a hard reload.
        'user',
        'userToken',
        'jwt',
        'auth',
        'authToken',
        'access_token',
        'currentUser',
        'profile',
    ]
    for (const k of keysToWipe) {
        try { localStorage.removeItem(k) } catch (_) { /* disabled in some browsers */ }
        try { sessionStorage.removeItem(k) } catch (_) { /* same */ }
    }

    // 2) Nuclear: clear everything else in localStorage.
    try { localStorage.clear() } catch (_) { /* disabled in some browsers */ }

    // 3) Same in sessionStorage.
    try { sessionStorage.clear() } catch (_) { /* disabled in some browsers */ }
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
