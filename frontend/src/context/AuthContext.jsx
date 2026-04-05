import { createContext, useContext, useState, useEffect } from 'react'
import axios from 'axios'

// API Configuration - Uses environment variable with fallback
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5050/api'

// Create API instance with correct backend URL
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
})

// Add token interceptor
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const AuthContext = createContext(undefined)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [session, setSession] = useState(null)
  const [loading, setLoading] = useState(true)

  // Check if session is expired
  const isSessionExpired = () => {
    if (!session?.expiresAt) return true
    return new Date() > new Date(session.expiresAt)
  }

  // Update session activity
  const updateSessionActivity = async () => {
    if (!session || isSessionExpired()) return
    
    try {
      await api.post('/auth/update-activity', { sessionId: session.sessionId })
      // Update local session expiry
      const newExpiry = new Date()
      newExpiry.setHours(newExpiry.getHours() + 1) // Extend by 1 hour
      setSession(prev => prev ? { ...prev, expiresAt: newExpiry.toISOString() } : null)
    } catch (error) {
      console.error('Failed to update session activity:', error)
      // Force logout if session update fails
      logout()
    }
  }

  // Refresh token
  const refreshToken = async () => {
    try {
      const response = await api.post('/auth/refresh-token')
      const { token, newSession } = response.data
      
      localStorage.setItem('token', token)
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`
      
      if (newSession) {
        setSession(newSession)
      }
    } catch (error) {
      console.error('Failed to refresh token:', error)
      logout()
    }
  }

  // Auto-refresh token and update activity
  useEffect(() => {
    if (!session || !user) return

    const interval = setInterval(() => {
      if (isSessionExpired()) {
        logout()
      } else {
        console.log('Session active, checking expiry...')
      }
    }, 15 * 60 * 1000) // 15 minutes

    return () => clearInterval(interval)
  }, [session, user])

  // Validate session on mount - only if user exists in localStorage
  useEffect(() => {
    const token = localStorage.getItem('token')
    const sessionId = localStorage.getItem('sessionId')
    
    if (token && sessionId) {
      // Only validate if we have existing session data
      const validateSession = async () => {
        try {
          api.defaults.headers.common['Authorization'] = `Bearer ${token}`
          
          // First try to get user info to validate token
          const userResponse = await api.get('/auth/profile')
          const userData = userResponse.data
          
          setUser(userData)
          
          // Set a minimal session object if validation endpoint doesn't exist
          setSession({
            sessionId: sessionId,
            createdAt: new Date().toISOString(),
            expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString() // 1 hour
          })
          
        } catch (error) {
          // Session invalid or expired - clear storage and redirect to login
          console.error('Session validation failed:', error)
          localStorage.removeItem('token')
          localStorage.removeItem('sessionId')
          delete api.defaults.headers.common['Authorization']
          setUser(null)
          setSession(null)
          
          // Redirect to login page if token was invalid
          if (error.response?.status === 401) {
            window.location.href = '/login'
          }
        } finally {
          setLoading(false)
        }
      }
      validateSession()
    } else {
      setLoading(false)
    }
  }, [])

  const login = async (email, password) => {
    try {
      const res = await api.post('/auth/login', { 
        email, 
        password,
        deviceInfo: navigator.userAgent,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
      })
      
      const { token, user: userData, session: sessionData } = res.data
      
      localStorage.setItem('token', token)
      localStorage.setItem('sessionId', sessionData.sessionId)
      
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`
      
      setUser(userData)
      setSession(sessionData)
      
      return { success: true, message: 'Login successful' }
      
    } catch (error) {
      console.error('Login error:', error)
      if (error.response?.status === 401) {
        throw new Error('Invalid email or password')
      } else if (error.response?.status === 400) {
        throw new Error('Invalid credentials')
      } else if (error.response?.data?.message) {
        throw new Error(error.response.data.message)
      } else {
        throw new Error('Login failed. Please try again.')
      }
    }
  }

  const logout = async () => {
    try {
      // Notify server about logout
      if (session) {
        await api.post('/auth/logout', { sessionId: session.sessionId })
      }
    } catch (error) {
      console.error('Logout notification failed:', error)
    } finally {
      // Clear local storage and state
      localStorage.removeItem('token')
      localStorage.removeItem('sessionId')
      delete api.defaults.headers.common['Authorization']
      setUser(null)
      setSession(null)
    }
  }

  const register = async (name, email, password) => {
    try {
      console.log('Register API Call:', { name, email, password: password ? '[PRESENT]' : '[MISSING]' });
      const response = await api.post('/auth/register', { 
        name, 
        email, 
        password,
        deviceInfo: navigator.userAgent,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
      });
      console.log('Register API Response:', response.data);
      return response.data;
    } catch (error) {
      console.error('Register API Error:', error.response?.data || error.message);
      // Provide better error messages
      if (error.response?.status === 400) {
        const errorMessage = error.response.data?.message || 'Registration failed'
        throw new Error(errorMessage)
      } else if (error.response?.status === 409) {
        throw new Error('Email already exists. Please use a different email.')
      } else if (error.response?.data?.message?.includes('duplicate key')) {
        throw new Error('Email already exists. Please use a different email.')
      } else {
        throw new Error('Registration failed. Please try again.')
      }
    }
  }

  return (
    <AuthContext.Provider value={{ 
      user, 
      setUser,
      session, 
      loading, 
      login, 
      logout, 
      register, 
      refreshToken,
      updateSessionActivity,
      isSessionExpired
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
