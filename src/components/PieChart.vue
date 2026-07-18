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
const DEFAULT_PALETTE = [
  '#F05A14', '#22C55E', '#3B82F6', '#F59E0B', '#A855F7',
  '#EC4899', '#14B8A6', '#FF7A3D', '#6366F1', '#EF4444',
]

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

const buildOption = (data) => {
  const palette = activePalette()

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#111111',
      borderColor: '#F05A14',
      borderWidth: 1,
      textStyle: { color: '#DEDEDE', fontSize: 13 },
      formatter: (p) => {
        const val = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(p.value)
        return `<b>${p.name}</b><br/>${val} <span style="color:var(--muted)">(${p.percent.toFixed(1)}%)</span>`
      },
    },
    // No right-side legend: we now show category + percentage as outside labels
    // with connecting lines. ECharts' built-in algorithm arranges them so they
    // never overlap, regardless of how many categories there are.
    legend: { show: false },
    series: [
      {
        name: 'Category',
        type: 'pie',
        // Slightly smaller inner radius makes room for the outside label lines
        // without the donut becoming too thin.
        radius: ['38%', '60%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        minShowLabelAngle: 4, // hide the label for slices smaller than 4°
        color: palette,
        itemStyle: {
          borderRadius: 3,
          borderColor: '#090909',   // dark border so slices are clearly separated
          borderWidth: 2,
        },
        // Outside labels with connecting lines. ECharts' layout engine
        // automatically arranges them (left vs right, two columns) to prevent
        // any overlap — even with many categories.
        label: {
          show: true,
          position: 'outside',
          alignTo: 'labelLine',
          bleedMargin: 8,
          formatter: (p) => {
            const pct = (p.percent ?? 0).toFixed(0) + '%'
            return `${p.name}\n${pct}`
          },
          fontSize: 12,
          fontWeight: 600,
          color: '#DEDEDE',
          lineHeight: 15,
          // Solid dark text border so the label stays readable no matter what
          // colors appear near it on the canvas.
          textBorderColor: '#090909',
          textBorderWidth: 3,
        },
        labelLine: {
          show: true,
          length: 10,
          length2: 14,
          lineStyle: {
            // Slightly brighter than --muted so the connector line is visible
            // against the dark card background.
            color: '#5A5A5A',
            width: 1.4,
          },
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 18,
            shadowColor: 'rgba(240,90,20,0.6)',
            borderColor: '#F05A14',
            borderWidth: 2,
          },
          scaleSize: 8,
          label: {
            fontSize: 13,
            fontWeight: 700,
          },
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
