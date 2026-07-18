<template>
  <div ref="chartContainer" class="line-chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart as LineChartSeries } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChartSeries, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps({
  // Array of { date: 'YYYY-MM-DD', income: number, expense: number }
  data: {
    type: Array,
    default: () => [],
  },
})

const chartContainer = ref(null)
let chartInstance = null

const formatCurrencyShort = (v) => {
  const n = Number(v) || 0
  if (n >= 1000) return `$${(n / 1000).toFixed(1)}k`
  return `$${n.toFixed(0)}`
}

const buildOption = (data) => {
  const hasData = data && data.length > 0
  const dates = hasData ? data.map(d => d.date) : []
  const incomeValues = hasData ? data.map(d => Number(d.income) || 0) : []
  const expenseValues = hasData ? data.map(d => Number(d.expense) || 0) : []

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#111111',
      borderColor: '#232323',
      textStyle: { color: '#DEDEDE', fontSize: 13 },
      formatter: (params) => {
        if (!params || params.length === 0) return ''
        let html = `<div style="margin-bottom:6px;font-weight:600;color:#f1f5f9">${params[0].axisValue}</div>`
        params.forEach(p => {
          const val = new Intl.NumberFormat('en-US', {
            style: 'currency', currency: 'USD', minimumFractionDigits: 0,
          }).format(p.value)
          html += `<div style="display:flex;align-items:center;gap:6px;margin-top:4px">
            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
            <span>${p.seriesName}: <b style="color:#f1f5f9">${val}</b></span>
          </div>`
        })
        return html
      },
    },
    legend: {
      data: ['Income', 'Expenses'],
      textStyle: { color: '#94a3b8', fontSize: 12 },
      icon: 'roundRect',
      itemWidth: 14,
      itemHeight: 8,
      top: 0,
      right: 8,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '6%',
      top: '18%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: hasData ? dates : ['No data'],
      axisLine: { lineStyle: { color: '#2a2a3a' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#1e1e2e', type: 'dashed' } },
      axisLabel: {
        color: '#64748b',
        fontSize: 11,
        formatter: (v) => formatCurrencyShort(v),
      },
    },
    series: [
      {
        name: 'Income',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        lineStyle: { width: 2.5, color: '#22C55E' },
        itemStyle: { color: '#22C55E', borderColor: '#0a1a0f', borderWidth: 2 },
        emphasis: {
          focus: 'series',
          lineStyle: { width: 3 },
          scale: 1.6,
        },
        areaStyle: {
          color: 'rgba(34,197,94,0.12)',
        },
        data: hasData ? incomeValues : [0],
      },
      {
        name: 'Expenses',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        lineStyle: { width: 2.5, color: '#EF4444' },
        itemStyle: { color: '#EF4444', borderColor: '#1a0a0a', borderWidth: 2 },
        emphasis: {
          focus: 'series',
          lineStyle: { width: 3 },
          scale: 1.6,
        },
        areaStyle: {
          color: 'rgba(239,68,68,0.12)',
        },
        data: hasData ? expenseValues : [0],
      },
    ],
  }
}

const initChart = () => {
  if (!chartContainer.value) return
  chartInstance = echarts.init(chartContainer.value)
  chartInstance.setOption(buildOption(props.data))
}

let resizeTimer = null
const handleResize = () => {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => chartInstance?.resize(), 200)
}

watch(
  () => props.data,
  (newData) => {
    if (!chartInstance) return
    chartInstance.setOption(buildOption(newData), true)
  },
  { deep: true }
)

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.line-chart-container {
  width: 100%;
  height: 360px;
  min-height: 300px;
}
</style>
