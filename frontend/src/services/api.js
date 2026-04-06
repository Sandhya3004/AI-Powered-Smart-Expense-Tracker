import axios from 'axios'

// API Configuration - Uses environment variable with fallback
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://expense-tracker-api-1onn.onrender.com/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add token to every request if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export { api }
export default api
