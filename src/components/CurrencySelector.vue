<template>
  <div class="currency-selector">
    <el-select
      :model-value="selectedCurrency"
      size="default"
      class="currency-select"
      :teleported="false"
      @change="onChange"
    >
      <!--
        Decoupled from the API result on purpose: the dropdown lists
        every supported currency at all times, even before the rates
        fetch has returned (or if it failed). This means the user can
        always pick a non-USD currency, and the formatter will fall
        back to identity (USD) until the rates arrive. Without this,
        a failed fetch would leave the dropdown showing only USD
        (because the `Object.keys(rates)` would be empty), which is
        the original "I can only see USD" bug.
      -->
      <el-option
        v-for="opt in ALL_CODES"
        :key="opt.code"
        :value="opt.code"
        :label="`${opt.code} — ${opt.name}`"
      />
    </el-select>
    <!--
      Status hint: tiny grey text under the selector. Three states:
        - "Loading…" while the first fetch is in flight
        - "Couldn't load rates. Retry" if the fetch failed (clickable)
        - nothing once rates have loaded successfully
      The text is intentionally grey, small, and unobtrusive — the
      user already said USD values render correctly when rates fail,
      so we just want them to know why + give them a one-click fix.
    -->
    <div v-if="ratesLoading" class="currency-hint">Loading…</div>
    <div v-else-if="ratesError" class="currency-hint currency-hint--error">
      Couldn't load rates.
      <a href="#" class="currency-hint__retry" @click.prevent="onRetry">Retry</a>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useCurrency } from '@/composables/useCurrency'

const {
  selectedCurrency,
  setCurrency,
  ensureRatesLoaded,
  refreshRates,
  ratesLoading,
  ratesError,
} = useCurrency()

/**
 * Static list of every supported currency code + its human-friendly
 * name. This is the source of truth for the dropdown — the API only
 * supplies the numeric `rate`, not the display name, and the codes
 * are a known fixed set (they're a project constant on the backend
 * too, in `CurrencySyncService.SUPPORTED_CODES`).
 *
 * Keeping the list static here (rather than deriving it from the
 * rates API result) is what fixes the original "I can only see USD"
 * bug: the dropdown no longer goes blank when the fetch fails.
 */
const ALL_CODES = [
  { code: 'USD', name: 'US Dollar' },
  { code: 'IDR', name: 'Indonesian Rupiah' },
  { code: 'CNY', name: 'Chinese Yuan' },
  { code: 'RUB', name: 'Russian Ruble' },
  { code: 'EUR', name: 'Euro' },
  { code: 'GBP', name: 'British Pound' },
  { code: 'SAR', name: 'Saudi Riyal' },
  { code: 'JPY', name: 'Japanese Yen' },
  { code: 'KRW', name: 'South Korean Won' },
  { code: 'PHP', name: 'Philippine Peso' },
  { code: 'INR', name: 'Indian Rupee' },
  { code: 'THB', name: 'Thai Baht' },
  { code: 'MYR', name: 'Malaysian Ringgit' },
  { code: 'TWD', name: 'Taiwan Dollar' },
  { code: 'HKD', name: 'Hong Kong Dollar' },
]

const onChange = (code) => setCurrency(code)
const onRetry  = () => refreshRates()

// Fire the one-time rates fetch the first time the selector mounts.
onMounted(() => { ensureRatesLoaded() })
</script>

<style scoped>
/* Keep the selector compact — it's a global nav widget, not a form field. */
.currency-selector {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

/*
 * Element Plus's default <el-select> is sized for forms (240+ px wide
 * with 32px height). For the global nav we want something closer to
 * 100px so it doesn't fight the username / logout button for space.
 * Width 110px is enough for "USD — US Dollar" (the longest option in
 * the dropdown) on most browsers; the dropdown itself pops over the
 * navbar at its native width when opened.
 */
.currency-select {
  width: 110px;
}

.currency-select :deep(.el-select__wrapper) {
  background-color: var(--ink) !important;
  box-shadow: 0 0 0 1px var(--wire) inset !important;
  border-radius: var(--radius-sm) !important;
  min-height: 32px !important;
}
.currency-select :deep(.el-select__wrapper.is-hovering) {
  box-shadow: 0 0 0 1px var(--muted) inset !important;
}
.currency-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--ember) inset !important;
}
.currency-select :deep(.el-select__placeholder),
.currency-select :deep(.el-select__selected-item) {
  color: var(--bone) !important;
  font-size: 12px !important;
  font-weight: 600 !important;
  letter-spacing: 0.5px !important;
}
.currency-select :deep(.el-select__caret) {
  color: var(--ash) !important;
}

/*
 * Tiny grey status hint directly under the selector. The "Retry" link
 * is a normal anchor styled to match the rest of the app's muted
 * text; clicking it calls refreshRates() and the hint disappears
 * once the next attempt completes.
 */
.currency-hint {
  font-size: 10px;
  letter-spacing: 0.4px;
  color: var(--muted);
  line-height: 1;
  padding: 0 2px;
  font-family: var(--font-mono);
  user-select: none;
  white-space: nowrap;
}
.currency-hint--error { color: var(--ash); }
.currency-hint__retry {
  color: var(--ember);
  text-decoration: underline;
  text-underline-offset: 2px;
  margin-left: 4px;
}
.currency-hint__retry:hover { color: var(--spark); }
</style>
