import { ref, reactive, readonly } from 'vue'
import request from '@/utils/request'

/**
 * useCurrency
 * -----------
 * Reactive multi-currency conversion helper for the entire frontend.
 *
 * What it gives you
 * -----------------
 * 1. A single source of truth for the user's preferred display
 *    currency (persisted to localStorage under the key
 *    `preferredCurrency`).
 * 2. A one-time-per-page-load fetch of `/api/exchange-rates`, cached
 *    in module scope so every component sees the same rates and we
 *    don't fire N HTTP requests.
 * 3. Two helpers used by every view that displays money:
 *      - `formatConverted(usdAmount)` → returns an object
 *        `{ primary, secondary, code, sign }` that drives the
 *        two-line UI ("big converted value on top, smaller `≈ $ 1.23`
 *        beneath, USD line hidden when the user has already picked
 *        USD").
 *      - `formatOnly(usdAmount)`       → returns just the primary
 *        string, for tight UI spots where a two-line cell would
 *        overflow (e.g. chart tooltips).
 *
 * What it does NOT do
 * -------------------
 * - It never mutates `Bill.amount`. The backend contract is "all
 *   monetary values returned by the API are USD"; the conversion is
 *   purely a display concern.
 * - It never blocks: if the rates fetch fails (e.g. backend down
 *   before the daily cron ran), the composable silently falls back
 *   to showing USD values, and the `ratesError` ref + the selector's
 *   "Retry" hint surface the problem to the user. A 60-second
 *   auto-retry then keeps trying in the background so a brief
 *   hiccup self-heals without a page refresh.
 */

// Per product decision on 2026-07-19: the preferred display currency
// is force-set to USD for ALL users (new and existing). Any value
// previously stored under `preferredCurrency` in localStorage is
// overwritten on module load, and the default for fresh sessions is
// also USD. Users can still change the dropdown at runtime — the
// choice is then persisted normally via setCurrency().
const STORAGE_KEY = 'preferredCurrency'
const DEFAULT_CURRENCY = 'USD'
const RETRY_INTERVAL_MS = 60_000   // 1 minute

// Module-scoped state (one fetch shared by every component on the page).
const rates         = reactive({})                  // { IDR: 15842.31, EUR: 0.85, ... }
const ratesLoaded   = ref(false)                    // true once a fetch has completed (success OR failure)
const ratesError    = ref(null)                     // non-null while a fetch is in the error state
const ratesLoading   = ref(false)                    // true only while a fetch is in flight

// Locale map for Intl.NumberFormat. Each code maps to the locale that
// formats it most naturally (e.g. `id-ID` for IDR uses the `Rp`
// symbol and `,` as the thousands separator; `ja-JP` for JPY uses
// `¥` and omits decimals — JPY has no sub-unit). The fallback `en-US`
// always produces a reasonable output.
const LOCALE_MAP = {
  USD: 'en-US',
  IDR: 'id-ID',
  CNY: 'zh-CN',
  RUB: 'ru-RU',
  EUR: 'de-DE',
  GBP: 'en-GB',
  SAR: 'ar-SA',
  JPY: 'ja-JP',
  KRW: 'ko-KR',
  PHP: 'en-PH',
  INR: 'en-IN',
  THB: 'th-TH',
  MYR: 'ms-MY',
  TWD: 'zh-TW',
  HKD: 'en-HK',
}

// The user's choice.
//
// Per the 2026-07-19 product decision documented at the top of
// this file, the preferred display currency is FORCE-set to USD
// on every page load — for new users AND for users who had
// previously picked something else. The dropdown at runtime is
// still functional (the user can change it back if they want, and
// the choice is then persisted via setCurrency()), but the next
// reload will reset it to USD again. That "force every reload"
// behaviour is intentional and matches the product spec.
//
// The previous (pre-force) behaviour was:
//
//   const stored = (() => {
//     try { return localStorage.getItem(STORAGE_KEY) } catch { return null }
//   })()
//   const selectedCurrency = ref(stored || DEFAULT_CURRENCY)
//
// If the product ever reverses this decision, restore the lines
// above and delete the two lines below.
try { localStorage.setItem(STORAGE_KEY, DEFAULT_CURRENCY) } catch { /* ignore quota errors */ }
const selectedCurrency = ref(DEFAULT_CURRENCY)

// 60-second auto-retry bookkeeping. The timer is created on the
// first failure and cleared on the next success, so a recovered
// session has zero background polling. setInterval (not a setTimeout
// chain) is used so multiple component instances calling
// `useCurrency()` don't all start their own timer.
let retryTimerId = null
function ensureRetryTimer() {
  if (retryTimerId !== null) return
  retryTimerId = setInterval(() => {
    if (ratesError.value) {
      // eslint-disable-next-line no-console
      console.debug('useCurrency: auto-retrying rates fetch...')
      refreshRates()
    }
  }, RETRY_INTERVAL_MS)
}
function clearRetryTimer() {
  if (retryTimerId !== null) {
    clearInterval(retryTimerId)
    retryTimerId = null
  }
}

/**
 * Cancel the 60-second background auto-retry. Called from the
 * logout flow (NavBar.handleLogout) so a long-lived timer on a
 * wiped session can't keep firing /exchange-rates fetches every
 * minute.
 *
 * Safe to call when no timer is scheduled: clearRetryTimer is
 * a no-op in that case.
 *
 * Note: this is hygiene, not security. /exchange-rates is
 * whitelisted in request.js's isAuthEndpoint() so a 401 there
 * cannot trigger the silent-refresh / wipe / redirect path.
 * Cancelling the timer is just a "stop doing pointless work"
 * cleanup.
 */
export function cancelAutoRetry() {
  clearRetryTimer()
}

/**
 * Run a single fetch attempt. The caller is responsible for
 * throttling; this function just makes the request and updates
 * module state. Returns `true` on success, `false` on failure.
 *
 * On either outcome it:
 *   - sets `ratesLoaded = true` (so the in-flight dedup in
 *     `ensureRatesLoaded` knows the first attempt completed)
 *   - sets/clears `ratesError` (drives the UI's retry hint)
 *   - starts/stops the 60s auto-retry timer
 *
 * The `rates` dict is intentionally NOT cleared on failure, so the
 * fallback `getRate(code) === 1` still produces sensible (USD)
 * output instead of NaN while we wait for the next successful fetch.
 */
async function attemptFetch() {
  ratesLoading.value = true
  try {
    const data = await request.get('/exchange-rates')
    // Defensive: backend returns an array of { currencyCode, rate, ... }.
    const list = Array.isArray(data) ? data : []
    // Reset and refill the reactive dict.
    for (const k of Object.keys(rates)) delete rates[k]
    for (const row of list) {
      if (row && row.currencyCode && row.rate != null) {
        rates[row.currencyCode] = Number(row.rate)
      }
    }
    // USD is the base currency; it always converts at 1:1.
    rates.USD = 1

    // Mark as loaded AND clear any previous error. Every template
    // reading `ratesError` is reactive, so the "Retry" hint
    // disappears automatically.
    ratesLoaded.value = true
    ratesError.value  = null
    clearRetryTimer()
    return true
  } catch (e) {
    // Leave `rates` populated from any prior success so the
    // fallback `getRate(code) === 1` still produces sensible (USD)
    // output instead of NaN while we wait for the next successful
    // fetch.
    ratesLoaded.value = true
    ratesError.value  = e
    // Start the 60-second background retry. The interval is a no-op
    // while `ratesError` is null (the success path clears it), so a
    // recovered session has zero polling overhead.
    ensureRetryTimer()
    // eslint-disable-next-line no-console
    console.warn('useCurrency: failed to load exchange rates; falling back to USD. Will auto-retry in 60s.', e)
    return false
  } finally {
    ratesLoading.value = false
  }
}

/**
 * Promise-based dedup: every caller within the same tick gets the
 * same in-flight Promise. After the first attempt completes,
 * subsequent calls return immediately (success or failure both
 * count as "completed") unless the user explicitly hits "Retry".
 */
let inflight = null

function ensureRatesLoaded() {
  // If a fetch is already in flight, share that Promise with every
  // concurrent caller (the dashboard, the bills page, the selector,
  // and the view's onMounted can all fire at once on first paint).
  if (inflight) return inflight

  // If we've already loaded successfully, do nothing — the rates
  // are in the reactive dict and every template re-renders when
  // the user changes the selected currency.
  if (ratesLoaded.value && !ratesError.value) {
    return Promise.resolve(true)
  }

  // First attempt (or post-error retry). Wire the dedup so
  // concurrent callers share the same Promise; clear it in finally
  // so the next retry can re-enter.
  inflight = attemptFetch().finally(() => {
    inflight = null
  })
  return inflight
}

/**
 * Manually re-fetch rates. Called by the selector's "Retry" button.
 * Always fires regardless of current state, so the user can force
 * a retry even if the auto-retry is in progress.
 */
function refreshRates() {
  inflight = attemptFetch().finally(() => {
    inflight = null
  })
  return inflight
}

/**
 * Persist a new display currency. Setting it re-renders every
 * component that reads `selectedCurrency` reactively.
 */
function setCurrency(code) {
  if (!code) return
  selectedCurrency.value = code
  try { localStorage.setItem(STORAGE_KEY, code) } catch { /* ignore quota errors */ }
}

/**
 * Look up the rate for a given code. Returns 1 for USD (or anything
 * unknown / not yet loaded) so we never crash on a stale
 * localStorage value or a code the API hasn't responded with yet.
 */
function getRate(code) {
  if (!code) return 1
  const r = rates[code]
  return typeof r === 'number' && Number.isFinite(r) && r > 0 ? r : 1
}

/**
 * Format a USD amount as a localised currency string in the
 * `selectedCurrency`. Negative numbers are formatted normally (e.g.
 * "-Rp 1.234,56") — callers control the surrounding sign with
 * `sign: 'positive' | 'negative' | 'auto'`.
 */
function formatInSelectedCurrency(usdAmount, opts = {}) {
  const { sign = 'auto', hideSymbol = false } = opts
  const code = selectedCurrency.value || 'USD'
  const n = Number(usdAmount)
  if (!Number.isFinite(n)) return { primary: '—', code, secondary: null }

  const locale = LOCALE_MAP[code] || 'en-US'
  // JPY/KRW/IDR conventionally render without decimals.
  const fractionDigits = (code === 'JPY' || code === 'KRW' || code === 'IDR') ? 0 : 2

  let primary
  try {
    primary = new Intl.NumberFormat(locale, {
      style: hideSymbol ? 'decimal' : 'currency',
      currency: code,
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    }).format(Math.abs(n) * getRate(code))
  } catch {
    // Intl can throw on exotic codes in very old runtimes; fall back.
    primary = `${(Math.abs(n) * getRate(code)).toFixed(fractionDigits)} ${code}`
  }

  let signChar = ''
  if (sign === 'positive' || (sign === 'auto' && n > 0)) signChar = '+'
  else if (sign === 'negative' || (sign === 'auto' && n < 0)) signChar = '-'
  // 'never' (used by formatSigned) emits no leading sign at all —
  // the caller is going to prepend its own. Any other value is
  // treated like 'auto' and falls through to no sign.

  return { primary: signChar + primary, code }
}

/**
 * Format the USD secondary line ("≈ $ 1,000.00"). Returns `null`
 * when the user has already picked USD, so the caller can
 * `v-if`-out the secondary line cleanly.
 */
function formatUsdSecondary(usdAmount) {
  if (!Number.isFinite(Number(usdAmount))) return null
  if ((selectedCurrency.value || 'USD') === 'USD') return null
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(usdAmount)
  } catch {
    return `$ ${Number(usdAmount).toFixed(2)}`
  }
}

/**
 * The two-line helper every view should use.
 *
 *   const formatted = formatConverted(bill.amount, { sign: 'negative' })
 *   → { primary: '-Rp 158.423', secondary: '≈ $ 10.00', code: 'IDR', sign: '-' }
 *
 * `secondary` is `null` when the user has already picked USD, so
 * the template can do `<small v-if="formatted.secondary">…</small>`.
 */
function formatConverted(usdAmount, opts = {}) {
  const primaryInfo = formatInSelectedCurrency(usdAmount, opts)
  return {
    primary:    primaryInfo.primary,
    secondary:  formatUsdSecondary(usdAmount),
    code:       primaryInfo.code,
    sign:       primaryInfo.primary.startsWith('-') ? '-' : (primaryInfo.primary.startsWith('+') ? '+' : ''),
  }
}

/**
 * Single-line helper for tight UIs (chart tooltips, sparklines).
 * Returns just the primary value.
 */
function formatOnly(usdAmount, opts = {}) {
  return formatInSelectedCurrency(usdAmount, opts).primary
}

/**
 * "Signed" variant for bill rows. The caller passes an explicit
 * sign ('+' or '-') and we format `Math.abs(usdAmount)` — never
 * letting the formatter emit its own negative sign.
 *
 * Why this exists
 * ---------------
 * `formatConverted` / `formatOnly` are sign-agnostic; they let
 * `Intl.NumberFormat` decide whether to render a leading '-'. For
 * bill tables the *sign* is conveyed by `row.type` (1 = income,
 * 0 = expense), not by the value of `amount`. If the caller writes
 *
 *     {{ row.type === 1 ? '+' : '-' }}{{ fmt(x).primary }}
 *
 * and `amount` happens to be negative (a refund, or legacy data
 * that was entered as `-600`), `Intl` will emit `-$600.00` and
 * the template will prepend `+` on top, producing the
 * double-sign `++$600.00` bug.
 *
 * `fmtSigned` is the single-line, single-purpose fix: the formatter
 * is told the sign from the outside, and the value is always
 * positive. One helper, no surprises.
 *
 * @param {number} usdAmount  The amount, may be negative or positive.
 *                            Internally converted to `Math.abs()`.
 * @param {'+'|'-'|''} sign  The leading sign to display. Empty
 *                            string means "no sign".
 * @returns {{
 *   primary: string,    // e.g. '+$600.00' or '-Rp 9.000.000'
 *   secondary: string|null,  // USD secondary line (null when user picked USD)
 *   code: string,        // ISO 4217 code, e.g. 'IDR'
 *   sign: string,        // echoes the input sign, e.g. '+' or '-'
 * }}
 */
function formatSigned(usdAmount, sign) {
  const safeSign = (sign === '+' || sign === '-') ? sign : ''
  // Math.abs() so the underlying value is always non-negative, AND
  // { sign: 'never' } so the inner formatter doesn't emit its own
  // leading '+' / '-'. Together, these two lines mean the only sign
  // in the output is the one we prepend below — fixing the
  // `++$600.00` / `-$500.00` double-sign bug.
  const absAmount = Math.abs(Number(usdAmount) || 0)
  if (typeof console !== 'undefined' && console.assert) {
    // Dev-only guard: catches future regressions where someone calls
    // formatSigned with a negative number. Stripped from prod builds
    // because Vite's esbuild drops console.assert calls under the
    // default production config.
    console.assert(absAmount >= 0,
      'formatSigned: amount should be Math.abs() before reaching the formatter')
  }
  const primaryInfo = formatInSelectedCurrency(absAmount, { sign: 'never' })
  return {
    primary:    safeSign + primaryInfo.primary,
    secondary:  formatUsdSecondary(usdAmount),
    code:       primaryInfo.code,
    sign:       safeSign,
  }
}

/**
 * Composable entry point. Returns a stable, reactive object so
 * components can destructure safely. Backed by module-scope state,
 * so multiple components calling useCurrency() share the same data.
 */
export function useCurrency() {
  // Side-effect: if a failure was already recorded (e.g. the first
  // view loaded, errored, then a second view mounts later), make
  // sure the retry timer is alive. ensureRetryTimer is idempotent.
  if (ratesError.value) ensureRetryTimer()
  return {
    // state (reactive)
    rates:          readonly(rates),
    ratesLoaded:    readonly(ratesLoaded),
    ratesLoading:   readonly(ratesLoading),
    ratesError:     readonly(ratesError),
    selectedCurrency,

    // actions
    ensureRatesLoaded,
    refreshRates,
    setCurrency,
    getRate,

    // formatters
    formatConverted,
    formatOnly,
  }
}

/**
 * Convenience export: pre-bound `formatConverted` so callers can do
 * `fmtAmount(1234.5)` without destructuring the composable.
 * Mirrors the `formatCurrencyUSD` style that the rest of the app
 * already uses.
 *
 *   import { fmtAmount } from '@/composables/useCurrency'
 *   fmtAmount(1234.5)  // → { primary: 'Rp 19,564,000', secondary: '≈ $ 1,234.50', code: 'IDR' }
 */
export const fmtAmount = (usdAmount, opts) => formatConverted(usdAmount, opts)

/**
 * Pre-bound "signed" variant for bill rows. Fixes the `++$` / `-$`
 * double-sign bug by always formatting `Math.abs(usdAmount)` and
 * prepending the caller-supplied sign (typically derived from
 * `row.type`: income → '+', expense → '-').
 *
 *   import { fmtSigned } from '@/composables/useCurrency'
 *   fmtSigned(bill.amount, bill.type === 1 ? '+' : '-')
 *   // → { primary: '+$500.00' or '-$600.00', code: 'USD', sign: '+' or '-' }
 */
export const fmtSigned = (usdAmount, sign) => formatSigned(usdAmount, sign)
