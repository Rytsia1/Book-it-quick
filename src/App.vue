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
  --graphite: #111111;
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
  border-radius: 3px !important;
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
  border-radius: 3px !important;
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
  border-radius: 4px !important;
  box-shadow: 0 24px 48px rgba(0,0,0,0.6) !important;
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
  border-radius: 3px !important;
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
  border-radius: 3px !important;
  color: var(--bone) !important;
  box-shadow: none !important;
  font-family: var(--font-body) !important;
}
.el-textarea__inner:hover  { border-color: var(--muted) !important; }
.el-textarea__inner:focus  { border-color: var(--ember) !important; box-shadow: none !important; }
.el-textarea__inner::placeholder { color: var(--ash) !important; }

/* Select */
.el-select .el-input__wrapper { background-color: var(--ink) !important; }

.el-select-dropdown {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: 3px !important;
}
.el-select-dropdown__item { color: var(--bone) !important; font-family: var(--font-body) !important; }
.el-select-dropdown__item.hover,
.el-select-dropdown__item:hover { background-color: var(--slate) !important; }
.el-select-dropdown__item.selected { color: var(--ember) !important; }

/* InputNumber */
.el-input-number .el-input__wrapper { background-color: var(--ink) !important; }

/* Date picker */
.el-date-picker, .el-picker-panel {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: 3px !important;
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
  border-radius: 3px !important;
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
  border-radius: 3px !important;
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
  border-radius: 4px !important;
}
.el-message-box__title   { color: var(--white) !important; font-family: var(--font-display) !important; }
.el-message-box__content { color: var(--muted) !important; }

/* Dropdown menu */
.el-dropdown-menu {
  background-color: var(--graphite) !important;
  border: 1px solid var(--wire) !important;
  border-radius: 3px !important;
  padding: 4px !important;
}
.el-dropdown-menu__item { color: var(--bone) !important; border-radius: 2px !important; font-family: var(--font-body) !important; font-size: 13px !important; }
.el-dropdown-menu__item:hover { background-color: var(--slate) !important; color: var(--white) !important; }
.el-dropdown-menu__item--divided { border-top: 1px solid var(--wire) !important; margin: 4px 0 !important; }

/* Empty */
.el-empty__description p { color: var(--ash) !important; }

/* Notifications */
.el-message { border-radius: 3px !important; border-color: var(--wire) !important; background-color: var(--graphite) !important; }

/* Scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--ink); }
::-webkit-scrollbar-thumb { background: var(--wire); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--ash); }
</style>
