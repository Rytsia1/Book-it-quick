import { ref, watch } from 'vue'

/**
 * Reactive dark/light theme controller.
 *
 * - Persists to {@code localStorage} under the key {@code app.theme}.
 * - Mirrors the choice onto two HTML attributes:
 *      {@code <html data-theme="dark|light">}  — for our own CSS variables
 *      {@code <html class="dark">}            — for Element Plus's built-in dark mode
 * - Defaults to {@code 'dark'} when nothing is persisted.
 *
 * Usage:
 *   const { theme, toggle, isDark } = useTheme();
 */
const STORAGE_KEY = 'app.theme'

function readSaved() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    return v === 'light' || v === 'dark' ? v : 'dark'
  } catch (_) {
    return 'dark'
  }
}

function applyToDocument(next) {
  const html = document.documentElement
  html.dataset.theme = next
  html.classList.toggle('dark', next === 'dark')
  try { localStorage.setItem(STORAGE_KEY, next) } catch (_) { /* noop */ }
}

// Module-level singleton so every consumer sees the same value without
// each calling useState/useStorage independently.
const theme = ref(typeof document !== 'undefined' ? readSaved() : 'dark')

// Apply on init (covers the case where main.js didn't run for some
// reason, e.g. SSR / tests). Safe to call multiple times.
applyToDocument(theme.value)

watch(theme, (next) => applyToDocument(next))

export function useTheme() {
  function toggle() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }
  function set(next) {
    if (next !== 'light' && next !== 'dark') return
    theme.value = next
  }
  return {
    theme,
    isDark:  () => theme.value === 'dark',
    isLight: () => theme.value === 'light',
    toggle,
    set,
  }
}
