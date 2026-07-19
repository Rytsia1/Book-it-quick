<template>
  <div class="auth-page">
    <!-- Left: Marketing -->
    <div class="auth-left">
      <div class="auth-brand">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <rect width="24" height="24" rx="3" fill="#F05A14"/>
          <path d="M6 12h12M12 6v12" stroke="#fff" stroke-width="2.5" stroke-linecap="round"/>
        </svg>
        <span>BOOKKEEPING</span>
      </div>
      <div class="auth-hero">
        <h1>Financial<br/>clarity<br/><span class="accent">starts here.</span></h1>
        <p>Track every dollar. Set monthly budgets. Understand where your money goes — all in one place.</p>
      </div>
      <div class="auth-stats">
        <div class="auth-stat">
          <span class="auth-stat__num">100%</span>
          <span class="auth-stat__label">Private</span>
        </div>
        <div class="auth-stat">
          <span class="auth-stat__num">0ms</span>
          <span class="auth-stat__label">Latency</span>
        </div>
        <div class="auth-stat">
          <span class="auth-stat__num">&#8734;</span>
          <span class="auth-stat__label">Entries</span>
        </div>
      </div>
    </div>

    <!-- Right: Form -->
    <div class="auth-right">
      <div class="auth-form-wrapper">
        <div class="form-header">
          <p class="form-eyebrow">AUTHENTICATION</p>
          <h2 class="form-title">Sign In</h2>
          <p class="form-desc">Enter your credentials to continue</p>
        </div>

        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="handleLogin">
          <el-form-item label="USERNAME" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="your username"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item label="PASSWORD" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="••••••••"
              show-password
              size="large"
              clearable
            />
          </el-form-item>

          <button type="button" class="submit-btn" :disabled="isLoading" @click="handleLogin">
            <span v-if="!isLoading">SIGN IN &rarr;</span>
            <span v-else>SIGNING IN...</span>
          </button>
        </el-form>

        <p class="form-footer">
          Don't have an account?
          <router-link to="/register" class="form-link">Create one &rarr;</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router      = useRouter()
const loginFormRef = ref()
const isLoading   = ref(false)

const loginForm = reactive({ username: '', password: '' })

const loginRules = {
  username: [
    { required: true, message: 'Username is required', trigger: 'blur' },
    { min: 3, max: 20, message: '3-20 characters', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 6, message: 'Min 6 characters', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
  } catch { return }

  isLoading.value = true
  try {
    const response = await request.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
    })
    // The backend returns BOTH an access token (15 min, JWT) and a
    // refresh token (7 days, opaque). The response interceptor in
    // request.js unwraps `response.data`, so the fields live at the
    // top level — but we still fall back to `response.data.*` for
    // older code paths / defensive coverage.
    const data = response && response.data ? response.data : response
    const accessToken          = data?.token         || response?.token
    const refreshToken         = data?.refreshToken  || response?.refreshToken
    const accessTokenExpiresAt = data?.accessTokenExpiresAt || response?.accessTokenExpiresAt
    const userId               = data?.userId        || response?.userId
    const username             = data?.username      || response?.username || loginForm.username
    if (accessToken) {
      // Persist the access token under both `accessToken` (new) and
      // `token` (legacy) so any older call site that still reads
      // `localStorage.getItem('token')` keeps working unchanged.
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('token', accessToken)
      if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken)
      }
      if (accessTokenExpiresAt) {
        localStorage.setItem('accessTokenExpiresAt', String(accessTokenExpiresAt))
      }
      // Identity: always set username; only set userId if the
      // backend actually returned a numeric one. Without userId the
      // Dashboard's /api/bills?userId=... requests will 400, so this
      // is a hard requirement.
      if (username) localStorage.setItem('username', username)
      if (userId != null && userId !== '' && userId !== 'null' && userId !== 'undefined') {
        localStorage.setItem('userId', String(userId))
      } else {
        // Clean up any stale 'null'/'undefined' string the old code
        // may have written. The router guard and request interceptor
        // will route the user to /login if userId is missing.
        localStorage.removeItem('userId')
      }
      ElMessage.success('Welcome back!')
      setTimeout(() => router.push('/dashboard'), 400)
    } else {
      ElMessage.error('Login failed: No token received')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'Login failed. Please try again.')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
  background: var(--ink);
}

/* Left panel */
.auth-left {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px;
  background: var(--graphite);
  border-right: 1px solid var(--wire);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--white);
}

.auth-hero {
  padding: 40px 0;
}

.auth-hero h1 {
  font-family: var(--font-display);
  font-size: 52px;
  font-weight: 700;
  line-height: 1.05;
  color: var(--white);
  margin-bottom: 20px;
}

.accent { color: var(--ember); }

.auth-hero p {
  font-size: 15px;
  color: var(--muted);
  line-height: 1.7;
  max-width: 360px;
}

.auth-stats {
  display: flex;
  gap: 40px;
}

.auth-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-stat__num {
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 600;
  color: var(--white);
}

.auth-stat__label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.2px;
  color: var(--ash);
  text-transform: uppercase;
}

/* Right panel */
.auth-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.auth-form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 32px;
}

.form-eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--ember);
  margin-bottom: 10px;
}

.form-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--white);
  margin-bottom: 6px;
}

.form-desc {
  font-size: 13px;
  color: var(--ash);
}

.auth-form-wrapper :deep(.el-form-item) { margin-bottom: 20px; }

.submit-btn {
  width: 100%;
  padding: 13px;
  margin-top: 8px;
  background: var(--ember);
  border: none;
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  font-family: var(--font-body);
  letter-spacing: 1px;
  cursor: pointer;
  transition: background 0.15s;
}
.submit-btn:hover:not(:disabled) { background: var(--spark); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.form-footer {
  margin-top: 24px;
  font-size: 13px;
  color: var(--ash);
  text-align: center;
}

.form-link {
  margin-left: 6px;
  color: var(--ember);
  text-decoration: none;
  font-weight: 600;
  transition: color 0.15s;
}
.form-link:hover { color: var(--spark); }

/* Responsive */
@media (max-width: 900px) {
  .auth-page { grid-template-columns: 1fr; }
  .auth-left { display: none; }
  .auth-right { padding: 40px 24px; align-items: flex-start; padding-top: 80px; }
}
</style>
