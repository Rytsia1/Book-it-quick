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
        <h1>Start your<br/>financial<br/><span class="accent">journey.</span></h1>
        <p>Create an account and take control of your personal finances — budgets, insights, and full history.</p>
      </div>
      <div class="auth-steps">
        <div class="step">
          <span class="step-num">01</span>
          <span class="step-text">Create your account</span>
        </div>
        <div class="step">
          <span class="step-num">02</span>
          <span class="step-text">Add your first bill</span>
        </div>
        <div class="step">
          <span class="step-num">03</span>
          <span class="step-text">Set a monthly budget</span>
        </div>
      </div>
    </div>

    <!-- Right: Form -->
    <div class="auth-right">
      <div class="auth-form-wrapper">
        <div class="form-header">
          <p class="form-eyebrow">NEW ACCOUNT</p>
          <h2 class="form-title">Create Account</h2>
          <p class="form-desc">Fill in the details below to get started</p>
        </div>

        <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" @keyup.enter="handleRegister">
          <el-form-item label="USERNAME" prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="3-20 characters"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item label="PASSWORD" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="Min 6 characters"
              show-password
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item label="CONFIRM PASSWORD" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="Re-enter password"
              show-password
              size="large"
              clearable
            />
          </el-form-item>

          <button type="button" class="submit-btn" :disabled="isLoading" @click="handleRegister">
            <span v-if="!isLoading">CREATE ACCOUNT &rarr;</span>
            <span v-else>CREATING...</span>
          </button>
        </el-form>

        <p class="form-footer">
          Already have an account?
          <router-link to="/login" class="form-link">Sign in &rarr;</router-link>
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

const router          = useRouter()
const registerFormRef = ref()
const isLoading       = ref(false)

const registerForm = reactive({ username: '', password: '', confirmPassword: '' })

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('Passwords do not match'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: 'Username is required', trigger: 'blur' },
    { min: 3, max: 20, message: '3-20 characters', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 6, message: 'Min 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()
  } catch { return }

  isLoading.value = true
  try {
    await request.post('/auth/register', {
      username: registerForm.username,
      password: registerForm.password,
    })
    ElMessage.success('Account created! Please sign in.')
    setTimeout(() => router.push('/login'), 600)
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'Registration failed.')
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

.auth-hero { padding: 40px 0; }

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

.auth-steps { display: flex; flex-direction: column; gap: 16px; }

.step {
  display: flex;
  align-items: center;
  gap: 16px;
}

.step-num {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--ember);
  min-width: 24px;
}

.step-text {
  font-size: 13px;
  color: var(--muted);
}

.auth-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.auth-form-wrapper { width: 100%; max-width: 360px; }

.form-header { margin-bottom: 32px; }

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

.form-desc { font-size: 13px; color: var(--ash); }

.auth-form-wrapper :deep(.el-form-item) { margin-bottom: 20px; }

.submit-btn {
  width: 100%;
  padding: 13px;
  margin-top: 8px;
  background: var(--ember);
  border: none;
  border-radius: 3px;
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

@media (max-width: 900px) {
  .auth-page { grid-template-columns: 1fr; }
  .auth-left { display: none; }
  .auth-right { padding: 40px 24px; align-items: flex-start; padding-top: 80px; }
}
</style>
