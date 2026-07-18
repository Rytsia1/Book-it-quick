import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

// Element Plus - import only what's needed for smaller bundle
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

const app = createApp(App)

app.use(router)
app.use(ElementPlus, { size: 'default' })

app.mount('#app')
