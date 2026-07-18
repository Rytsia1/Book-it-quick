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
})

const emit = defineEmits(['sliceClick'])

const chartContainer = ref(null)
let chartInstance = null

const PALETTE = ['#F05A14', '#22C55E', '#3B82F6', '#F59E0B', '#A855F7', '#EC4899', '#14B8A6', '#FF7A3D', '#6366F1', '#EF4444']

const buildOption = (data) => ({
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
    textStyle: { color: '#8A8A8A', fontSize: 12 },
    itemWidth: 12,
    itemHeight: 12,
  },
  series: [
    {
      name: 'Category',
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      color: PALETTE,
      itemStyle: {
        borderRadius: 3,
        borderColor: '#090909',
        borderWidth: 2,
      },
      label: { show: false },
      emphasis: {
        itemStyle: {
          shadowBlur: 12,
          shadowColor: 'rgba(240,90,20,0.35)',
        },
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold',
          color: '#f1f5f9',
          formatter: '{b}',
        },
      },
      labelLine: { show: false },
      cursor: props.clickable ? 'pointer' : 'default',
      data: data.length
        ? data
        : [{ value: 0, name: 'No data', itemStyle: { color: '#232323' } }],
    },
  ],
})

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
    chartInstance?.setOption({ series: [{ data: newData.length ? newData : [{ value: 0, name: 'No data', itemStyle: { color: '#2a2a3a' } }] }] })
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
.pie-chart-container {
  width: 100%;
  height: 360px;
  min-height: 300px;
}

.clickable-chart {
  cursor: pointer;
}
</style>
