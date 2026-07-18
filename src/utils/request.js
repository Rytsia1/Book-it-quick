import axios from 'axios'

const instance = axios.create({
    // Update this line with the local back-end address.
    baseURL: 'http://localhost:8080/api',
    timeout: 10000,
})

// Request Interceptor: Attach Authorization token
instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response Interceptor: Handle errors
instance.interceptors.response.use(
    (response) => {
        return response.data
    },
    (error) => {
        if (error.response?.status === 401) {
            // Token expired or invalid
            localStorage.removeItem('token')
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default instance
