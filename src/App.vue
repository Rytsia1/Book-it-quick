<script setup>
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import NavBar from '@/components/NavBar.vue'

const route = useRoute()
const showNavBar = computed(() => route.path !== '/login' && route.path !== '/register')
</script>

<template>
  <NavBar v-if="showNavBar" />
  <router-view />
</template>

<style>
/* ── Design Tokens ── */
:root {
  --ink:      #090909;
  --graphite: #141414;
  --slate:    #181818;
  --wire:     #232323;
  --ash:      #5A5A5A;
  --muted:    #8A8A8A;
  --bone:     #DEDEDE;
  --white:    #FFFFFF;
  --ember:    #F05A14;
  --spark:    #FF7A3D;
  --green:    #22C55E;
  --red:      #EF4444;
  --amber:    #F59E0B;
  --font-display: 'Space Grotesk', 'Inter', sans-serif;
  --font-body:    'Inter', sans-serif;
  --font-mono:    'JetBrains Mono', 'Courier New', monospace;

  /* ── Radius & Elevation ── */
  --radius-sm:   8px;
  --radius-md:   12px;
  --radius-lg:   16px;
  --shadow-card: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  --shadow-elev: 0 8px 24px rgba(0, 0, 0, 0.35);
}

/* ── Reset & Base ── */
* { margin: 0; padding: 0; box-sizing: border-box; }

html { font-size: 14px; }

body {
  font-family: var(--font-body);
  background-color: var(--ink);
  color: var(--bone);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app { min-height: 100vh; background-color: var(--ink); }

/* ── Element Plus Dark Overrides ── */
.el-card {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-card) !important;
  color: var(--bone) !important;
}

.el-card__header {
  border-bottom: 1px solid var(--wire) !important;
  color: var(--bone) !important;
}

/* Table */
.el-table {
  background-color: var(--graphite) !important;
  color: var(--bone) !important;
  border-radius: var(--radius-md) !important;
}

.el-table tr { background-color: var(--graphite) !important; }

.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background-color: var(--slate) !important;
}

.el-table th.el-table__cell {
  background-color: var(--ink) !important;
  color: var(--ash) !important;
  font-family: var(--font-body) !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
  text-transform: uppercase !important;
}

.el-table td.el-table__cell { border-bottom: 1px solid var(--wire) !important; }

.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell {
  background-color: var(--slate) !important;
}

.el-table__empty-block { background-color: var(--graphite) !important; }
.el-table__empty-text  { color: var(--ash) !important; }

/* Dialog */
.el-dialog {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-card) !important;
}

.el-dialog__header { border-bottom: 1px solid var(--wire) !important; padding: 18px 24px !important; }
.el-dialog__title  { color: var(--white) !important; font-family: var(--font-display) !important; font-weight: 600 !important; }
.el-dialog__body   { padding: 24px !important; }
.el-dialog__footer { border-top: 1px solid var(--wire) !important; padding: 16px 24px !important; }
.el-dialog__headerbtn .el-dialog__close { color: var(--ash) !important; }
.el-dialog__headerbtn:hover .el-dialog__close { color: var(--bone) !important; }

/* Drawer */
.el-drawer {
  background-color: var(--graphite) !important;
  border-left: 1px solid var(--wire) !important;
}
.el-drawer__header { border-bottom: 1px solid var(--wire) !important; margin-bottom: 0 !important; }
.el-drawer__close-btn { color: var(--ash) !important; }
.el-drawer__close-btn:hover { color: var(--bone) !important; }

/* Form */
.el-form-item__label {
  color: var(--ash) !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
  text-transform: uppercase !important;
  font-family: var(--font-body) !important;
}

.el-input__wrapper {
  background-color: var(--ink) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-sm) !important;
  box-shadow: none !important;
}
.el-input__wrapper:hover { border-color: var(--muted) !important; box-shadow: none !important; }
.el-input__wrapper.is-focus { border-color: var(--ember) !important; box-shadow: none !important; }

.el-input__inner {
  color: var(--bone) !important;
  font-family: var(--font-body) !important;
  background-color: transparent !important;
}
.el-input__inner::placeholder { color: var(--ash) !important; }

.el-textarea__inner {
  background-color: var(--ink) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-sm) !important;
  color: var(--bone) !important;
  box-shadow: none !important;
  font-family: var(--font-body) !important;
}
.el-textarea__inner:hover  { border-color: var(--muted) !important; }
.el-textarea__inner:focus  { border-color: var(--ember) !important; box-shadow: none !important; }
.el-textarea__inner::placeholder { color: var(--ash) !important; }

/* Select - aggressive overrides to fight Element Plus white-on-dark leaks. */
.el-select,
.el-select-v2 {
  color: var(--bone) !important;
}

/* Wrapper background + border (covers both legacy and new variants). */
.el-select .el-input__wrapper,
.el-select-v2 .el-input__wrapper,
.el-select__wrapper {
  background-color: var(--ink) !important;
  box-shadow: 0 0 0 1px var(--wire) inset !important;
}
.el-select .el-input__wrapper:hover,
.el-select-v2 .el-input__wrapper:hover,
.el-select__wrapper.is-hovering { box-shadow: 0 0 0 1px var(--muted) inset !important; }
.el-select .el-input__wrapper.is-focus,
.el-select-v2 .el-input__wrapper.is-focus,
.el-select__wrapper.is-focused { box-shadow: 0 0 0 1px var(--ember) inset !important; }

/* The actual text inside the closed select. Cover all the candidates. */
.el-select .el-input__inner,
.el-select-v2 .el-input__inner,
.el-select__placeholder,
.el-select__placeholder.is-transparent,
.el-select__selected-item,
.el-select__input,
.el-select__wrapper .el-select__placeholder,
.el-select-dropdown__item {
  color: var(--bone) !important;
  font-family: var(--font-body) !important;
  -webkit-text-fill-color: var(--bone) !important; /* defeat WebKit autofill */
}
.el-select__placeholder,
.el-select__placeholder.is-transparent,
.el-select__input,
.el-select .el-input__inner::placeholder {
  color: var(--ash) !important;
  -webkit-text-fill-color: var(--ash) !important;
}
.el-select__selected-item { font-weight: 500 !important; }
.el-select__caret { color: var(--ash) !important; }

.el-select-dropdown {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-sm) !important;
}
.el-select-dropdown__item { font-family: var(--font-body) !important; border-radius: 6px !important; margin: 2px 0 !important; }
.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover { background-color: var(--slate) !important; color: var(--white) !important; }
.el-select-dropdown__item.selected { color: var(--ember) !important; }
.el-select-dropdown__item.selected:hover { color: var(--ember) !important; }

/* InputNumber - explicit text color so the value is readable. */
.el-input-number .el-input__wrapper { background-color: var(--ink) !important; }
.el-input-number .el-input__inner {
  color: var(--bone) !important;
  background-color: transparent !important;
  -webkit-text-fill-color: var(--bone) !important;
}
.el-input-number__decrease,
.el-input-number__increase {
  background: transparent !important;
  color: var(--muted) !important;
  border-color: var(--wire) !important;
}
.el-input-number__decrease:hover,
.el-input-number__increase:hover {
  background: var(--slate) !important;
  color: var(--ember) !important;
}

/* Date picker - the displayed date inside the input. */
.el-date-editor .el-input__inner {
  color: var(--bone) !important;
  background-color: transparent !important;
  -webkit-text-fill-color: var(--bone) !important;
}
.el-date-editor .el-input__inner::placeholder { color: var(--ash) !important; }
.el-date-editor.el-input,
.el-date-editor.el-input__wrapper {
  background-color: var(--ink) !important;
  box-shadow: 0 0 0 1px var(--wire) inset !important;
}
.el-date-editor.el-input:hover,
.el-date-editor.el-input__wrapper:hover { box-shadow: 0 0 0 1px var(--muted) inset !important; }
.el-date-editor.el-input.is-focus,
.el-date-editor.el-input__wrapper.is-focus { box-shadow: 0 0 0 1px var(--ember) inset !important; }
.el-date-editor .el-input__prefix,
.el-date-editor .el-input__suffix { color: var(--ash) !important; }
.el-date-editor .el-range-input { color: var(--bone) !important; background-color: transparent !important; }
.el-date-editor .el-range-input::placeholder { color: var(--ash) !important; }
.el-date-editor .el-range-separator { color: var(--ash) !important; }
.el-date-editor .el-input__icon,
.el-date-editor .el-icon { color: var(--ash) !important; }

/* Date picker */
.el-date-picker, .el-picker-panel {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-md) !important;
  color: var(--bone) !important;
}
.el-date-table td.in-range .el-date-table-cell { background-color: rgba(240,90,20,0.1) !important; }
.el-date-table td.current:not(.disabled) .el-date-table-cell__text {
  background-color: var(--ember) !important;
  color: white !important;
}

/* Buttons — primary = ember */
.el-button--primary {
  background-color: var(--ember) !important;
  border-color: var(--ember) !important;
  color: #fff !important;
  border-radius: var(--radius-sm) !important;
  font-weight: 600 !important;
  letter-spacing: 0.5px !important;
  font-family: var(--font-body) !important;
}
.el-button--primary:hover {
  background-color: var(--spark) !important;
  border-color: var(--spark) !important;
}
.el-button--default {
  background-color: transparent !important;
  border: 1px solid var(--wire) !important;
  color: var(--bone) !important;
  border-radius: var(--radius-sm) !important;
  font-family: var(--font-body) !important;
}
.el-button--default:hover {
  border-color: var(--muted) !important;
  color: var(--white) !important;
  background-color: var(--slate) !important;
}

/* Message box */
.el-message-box {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-md) !important;
}
.el-message-box__title   { color: var(--white) !important; font-family: var(--font-display) !important; }
.el-message-box__content { color: var(--muted) !important; }

/* Dropdown menu */
.el-dropdown-menu {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: var(--radius-sm) !important;
  padding: 4px !important;
}
.el-dropdown-menu__item { color: var(--bone) !important; border-radius: 6px !important; font-family: var(--font-body) !important; font-size: 13px !important; }
.el-dropdown-menu__item:hover { background-color: var(--slate) !important; color: var(--white) !important; }
.el-dropdown-menu__item--divided { border-top: 1px solid var(--wire) !important; margin: 4px 0 !important; }

/* Empty */
.el-empty__description p { color: var(--ash) !important; }

/* Notifications */
.el-message { border-radius: var(--radius-sm) !important; border-color: var(--wire) !important; background-color: var(--graphite) !important; }

/* Scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--ink); }
::-webkit-scrollbar-thumb { background: var(--wire); border-radius: 99px; }
::-webkit-scrollbar-thumb:hover { background: var(--ash); }
</style>
