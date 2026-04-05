import axios from 'axios'

// API Configuration - Uses environment variable with fallback
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5050/api'

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 second timeout
})

// Request interceptor - Add auth token
api.interceptors.request.use(
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

// Response interceptor - Handle errors globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      localStorage.removeItem('token')
      localStorage.removeItem('sessionId')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth Service
export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', {
      email,
      password,
      deviceInfo: navigator.userAgent,
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
    })
    return response.data
  },

  register: async (name, email, password) => {
    const response = await api.post('/auth/register', {
      name,
      email,
      password,
      deviceInfo: navigator.userAgent,
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
    })
    return response.data
  },

  logout: async (sessionId) => {
    if (sessionId) {
      await api.post('/auth/logout', { sessionId })
    }
  },

  getProfile: async () => {
    const response = await api.get('/auth/profile')
    return response.data
  }
}

// Expense Service
export const expenseService = {
  getAll: async (page = 0, size = 50) => {
    const response = await api.get(`/expenses?page=${page}&size=${size}`)
    return response.data
  },

  create: async (expenseData) => {
    const response = await api.post('/expenses', expenseData)
    return response.data
  },

  update: async (id, expenseData) => {
    const response = await api.put(`/expenses/${id}`, expenseData)
    return response.data
  },

  delete: async (id) => {
    await api.delete(`/expenses/${id}`)
  },

  getSummary: async () => {
    const response = await api.get('/expenses/summary')
    return response.data
  }
}

// Bill Service
export const billService = {
  getAll: async () => {
    const response = await api.get('/bill-reminders')
    return response.data
  },

  create: async (billData) => {
    const response = await api.post('/bill-reminders', billData)
    return response.data
  },

  delete: async (id) => {
    await api.delete(`/bill-reminders/${id}`)
  },

  update: async (id, billData) => {
    const response = await api.put(`/bill-reminders/${id}`, billData)
    return response.data
  },

  getUpcoming: async (daysAhead = 30) => {
    const response = await api.get(`/bill-reminders/upcoming?daysAhead=${daysAhead}`)
    return response.data
  }
}

// Alert Service
export const alertService = {
  getAll: async () => {
    const response = await api.get('/alerts')
    return response.data
  },

  markAsRead: async (id) => {
    const response = await api.put(`/alerts/${id}/read`)
    return response.data
  }
}

// Dashboard Service
export const dashboardService = {
  getDashboard: async () => {
    const response = await api.get('/dashboard')
    // Extract data from ApiResponse wrapper: { success, message, data }
    return response.data?.data || response.data || {}
  },

  getMonthlyTrends: async (months = 6) => {
    const response = await api.get(`/insights/monthly-trends?months=${months}`)
    return response.data?.data || response.data || []
  },

  getFinancialHealth: async () => {
    const response = await api.get('/dashboard/financial-health')
    return response.data?.data || response.data || {}
  }
}

// Group Service
export const groupService = {
  getAll: async () => {
    const response = await api.get('/groups')
    return response.data
  },

  getById: async (id) => {
    const response = await api.get(`/groups/${id}`)
    return response.data
  },

  create: async (groupData) => {
    const response = await api.post('/groups', groupData)
    return response.data
  },

  update: async (id, groupData) => {
    const response = await api.put(`/groups/${id}`, groupData)
    return response.data
  },

  delete: async (id) => {
    const response = await api.delete(`/groups/${id}`)
    return response.data
  },

  // Members
  getMembers: async (groupId) => {
    const response = await api.get(`/groups/${groupId}/members`)
    return response.data
  },

  addMember: async (groupId, email) => {
    const response = await api.post(`/groups/${groupId}/members`, { email })
    return response.data
  },

  removeMember: async (groupId, memberId) => {
    const response = await api.delete(`/groups/${groupId}/members/${memberId}`)
    return response.data
  },

  // Expenses
  getExpenses: async (groupId) => {
    const response = await api.get(`/groups/${groupId}/expenses`)
    return response.data
  },

  addExpense: async (groupId, expenseData) => {
    const response = await api.post(`/groups/${groupId}/expenses`, expenseData)
    return response.data
  },

  // Settlements
  getSettlement: async (groupId) => {
    const response = await api.post(`/groups/${groupId}/settle`)
    return response.data
  }
}

export { api }
export default api
