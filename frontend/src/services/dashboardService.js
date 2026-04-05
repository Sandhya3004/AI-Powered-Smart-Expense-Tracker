import api from './api'

const dashboardService = {
  // Get full dashboard data
  getDashboard: () => api.get('/dashboard'),

  // Get total balance
  getBalance: () => api.get('/dashboard/balance'),

  // Get recent transactions
  getRecentTransactions: (limit = 5) => api.get(`/dashboard/recent-transactions?limit=${limit}`),

  // Get monthly trends
  getMonthlyTrends: (months = 6) => api.get(`/dashboard/monthly-trends?months=${months}`),

  // Get category breakdown
  getCategoryBreakdown: () => api.get('/dashboard/category-breakdown'),

  // Get budget status
  getBudgetStatus: () => api.get('/dashboard/budget-status'),

  // Get upcoming bills
  getUpcomingBills: (daysAhead = 30) => api.get(`/dashboard/upcoming-bills?daysAhead=${daysAhead}`),

  // Get savings goals
  getSavingsGoals: () => api.get('/dashboard/savings-goals'),

  // Get financial health
  getFinancialHealth: () => api.get('/dashboard/financial-health'),
}

export { dashboardService }
export default dashboardService
