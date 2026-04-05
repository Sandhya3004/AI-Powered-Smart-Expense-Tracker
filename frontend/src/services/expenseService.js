import { api } from '@/api/api';

export const getCurrentBudget = () => api.get('/budgets/current');

export const setBudget = (amount, month) =>
  api.post('/budgets', { amount, month })

// Monthly summary and category breakdown
export const getMonthlySummary = (month) =>
  api.get(`/insights/monthly-summary?month=${month}`)

export const getCategoryBreakdown = (month) =>
  api.get(`/insights/category-breakdown?month=${month}`)

// Expense CRUD
export const getExpenses = (page = 0, size = 10) =>
  api.get(`/expenses?page=${page}&size=${size}`)

export const createExpense = (data) =>
  api.post('/expenses', data)

export const updateExpense = (id, data) =>
  api.put(`/expenses/${id}`, data)

export const deleteExpense = (id) =>
  api.delete(`/expenses/${id}`)

// AI‑powered endpoints
export const getAnomalies = () =>
  api.get('/insights/anomalies')

export const getPrediction = () =>
  api.get('/insights/prediction')

export const categorizeExpense = (description) =>
  api.post('/ai/categorize', { description })
