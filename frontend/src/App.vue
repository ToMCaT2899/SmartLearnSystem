<template>
  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
</script>

<style>
/* === Global Reset & Base === */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #303133;
  background: #f0f2f5;
}

/* === Custom Scrollbar === */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

/* === Page Transition === */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* === Element Plus Global Overrides === */
.el-card {
  border: none !important;
  border-radius: 12px !important;
  transition: box-shadow 0.3s ease;
}

.el-card.is-hover-shadow:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08) !important;
}

.el-card__header {
  padding: 16px 20px !important;
  border-bottom: 1px solid #f2f3f5 !important;
}

.el-card__body {
  padding: 20px !important;
}

.el-button--primary {
  --el-button-bg-color: #667eea;
  --el-button-border-color: #667eea;
  --el-button-hover-bg-color: #768df0;
  --el-button-hover-border-color: #768df0;
  --el-button-active-bg-color: #5a6fd6;
  --el-button-active-border-color: #5a6fd6;
  border-radius: 8px;
  font-weight: 500;
}

.el-button--success {
  border-radius: 8px;
}

.el-button--danger {
  border-radius: 8px;
}

.el-button--default {
  border-radius: 8px;
}

.el-table {
  border-radius: 8px;
  overflow: hidden;
}

.el-table th.el-table__cell {
  background-color: #f7f8fa !important;
  color: #4e5969;
  font-weight: 600;
  border-bottom: 2px solid #e4e7ed !important;
}

.el-table .el-table__row:hover > td.el-table__cell {
  background-color: #f5f7ff !important;
}

.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background: #fafbfc;
}

.el-tabs--border-card {
  border: none !important;
  box-shadow: none !important;
}

.el-tabs--border-card > .el-tabs__header {
  background: #f7f8fa !important;
  border-bottom: 1px solid #e4e7ed !important;
}

.el-tabs--border-card > .el-tabs__header .el-tabs__nav {
  border: none !important;
}

.el-tabs--border-card > .el-tabs__header .el-tabs__item {
  border: none !important;
  color: #606266;
  font-weight: 500;
  transition: all 0.3s;
}

.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active {
  color: #667eea;
  background: #fff !important;
}

.el-tabs__active-bar {
  background-color: #667eea !important;
}

.el-dialog {
  border-radius: 16px !important;
}

.el-dialog__header {
  padding: 20px 24px !important;
  border-bottom: 1px solid #f2f3f5;
}

.el-dialog__body {
  padding: 24px !important;
}

.el-tag {
  border-radius: 6px;
  font-weight: 500;
}

.el-badge__content {
  border-radius: 10px;
}

.el-divider__text {
  background-color: #fff !important;
  font-weight: 600;
  color: #1d2129;
}

.el-timeline-item__node {
  border-radius: 50%;
}

/* === Form Styles === */
.el-input__wrapper {
  border-radius: 8px !important;
}

.el-select .el-input__wrapper {
  border-radius: 8px !important;
}

.el-textarea__inner {
  border-radius: 8px !important;
}

.el-input-number .el-input__wrapper {
  border-radius: 8px !important;
}

/* === Menu (Dropdown) === */
.el-popper.is-light {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
}

.el-dropdown-menu__item {
  border-radius: 6px;
  margin: 2px 6px;
  transition: all 0.2s;
}

.el-dropdown-menu__item:hover {
  background-color: #f5f7ff !important;
}

/* === Pagination === */
.el-pagination .el-pager li.is-active {
  background-color: #667eea !important;
  border-radius: 6px;
}

.el-pagination .btn-prev,
.el-pagination .btn-next {
  border-radius: 8px !important;
}

/* === Message box === */
.el-message-box {
  border-radius: 16px !important;
}

/* === Empty state === */
.el-empty__description {
  color: #c0c4cc !important;
}

/* === Alert === */
.el-alert {
  border-radius: 10px !important;
}
</style>
