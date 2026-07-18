<template>
  <div ref="chartContainer" class="pie-chart-container" :class="{ 'clickable-chart': clickable }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart as PieChartSeries } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChartSeries, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// Default fallback palette (mixed) — used when no `palette` prop is provided.
const DEFAULT_PALETTE = ['#F05A14', '#22C55E', '#3B82F6', '#F59E0B', '#A855F7', '#EC4899', '#14B8A6', '#FF7A3D', '#6366F1', '#EF4444']

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    default: 'Expense Distribution',
  },
  clickable: {
    type: Boolean,
    default: false,
  },
  // Optional color palette. Pass an array of hex colors to override the default.
  // Use this to assign different palettes to e.g. income vs expense pie charts so
  // viewers don't confuse categories across charts.
  palette: {
    type: Array,
    default: null,
  },
})

const emit = defineEmits(['sliceClick'])

const chartContainer = ref(null)
let chartInstance = null

// Resolve the active palette, falling back to the default if none was provided.
const activePalette = () => (props.palette && props.palette.length ? props.palette : DEFAULT_PALETTE)

const formatCurrency = (v) => {
  const n = Number(v) || 0
  if (n >= 1000) return `$${(n / 1000).toFixed(1)}k`
  return `$${n.toFixed(0)}`
}

const buildOption = (data) => {
  const hasData = data && data.length > 0
  const palette = activePalette()
  const total = hasData ? data.reduce((sum, d) => sum + (Number(d.value) || 0), 0) : 0

  // Build legend entries with "Name — XX%" text so users can read proportions at a glance.
  const legendData = hasData
    ? data.map((d, i) => {
        const pct = total > 0 ? Math.round((Number(d.value) || 0) / total * 100) : 0
        return {
          name: `${d.name}  ·  ${pct}%`,
          // ECharts uses the legend `name` field to map colors to the series data.
          icon: 'roundRect',
        }
      })
    : [{ name: 'No data' }]

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#111111',
      borderColor: '#232323',
      textStyle: { color: '#DEDEDE', fontSize: 13 },
      formatter: (p) => {
        const val = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0 }).format(p.value)
        return `<b>${p.name}</b><br/>${val} <span style="color:#94a3b8">(${p.percent.toFixed(1)}%)</span>`
      },
    },
    legend: {
      orient: 'vertical',
      right: '2%',
      top: 'center',
      data: legendData,
      textStyle: {
        color: '#DEDEDE',
        fontSize: 12,
        rich: {},
      },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 14,
      formatter: (name) => {
        // Already includes "Name  ·  XX%" in `name`; just return as-is.
        return name
      },
    },
    series: [
      {
        name: 'Category',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        color: palette,
        itemStyle: {
          borderRadius: 3,
          borderColor: '#090909',
          borderWidth: 2,
        },
        // Always-visible labels: show category name + percentage on the slice.
        label: {
          show: true,
          position: 'inside',
          formatter: (p) => {
            const pct = (p.percent ?? 0).toFixed(0) + '%'
            // Shorten the category name if it's long
            const name = p.name.length > 10 ? p.name.slice(0, 9) + '…' : p.name
            return `${name}\n${pct}`
          },
          fontSize: 11,
          fontWeight: 600,
          color: '#0a0a0a',
          lineHeight: 14,
          textShadowColor: 'rgba(255, 255, 255, 0.55)',
          textShadowBlur: 3,
        },
        labelLine: { show: false },
        emphasis: {
          itemStyle: {
            shadowBlur: 12,
            shadowColor: 'rgba(240,90,20,0.35)',
          },
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold',
            color: '#0a0a0a',
          },
          scaleSize: 6,
        },
        cursor: props.clickable ? 'pointer' : 'default',
        data: data.length
          ? data
          : [{ value: 0, name: 'No data', itemStyle: { color: '#232323' } }],
      },
    ],
  }
}

const initChart = () => {
  if (!chartContainer.value) return
  chartInstance = echarts.init(chartContainer.value)
  chartInstance.setOption(buildOption(props.data))

  if (props.clickable) {
    chartInstance.on('click', (params) => {
      if (params.data?.name && params.data.name !== 'No data') {
        emit('sliceClick', params.data.name)
      }
    })
  }
}

let resizeTimer = null
const handleResize = () => {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => chartInstance?.resize(), 200)
}

watch(
  () => props.data,
  (newData) => {
    if (chartInstance) {
      chartInstance.setOption(buildOption(newData), true)
    }
  },
  { deep: true }
)

// Re-render the chart if the palette prop changes (so switching palettes works).
watch(
  () => props.palette,
  () => {
    if (chartInstance) {
      chartInstance.setOption(buildOption(props.data), true)
    }
  }
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
.pie-chart-container {
  width: 100%;
  height: 360px;
  min-height: 300px;
}

.clickable-chart {
  cursor: pointer;
}
</style>
