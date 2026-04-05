import { useState, useEffect } from 'react'
import { useAuth } from '@/context/AuthContext'
import { dashboardService } from '@/services/dashboardService'
import { useToast } from '@/hooks/use-toast'
import { formatCurrency } from '@/utils/formatters'

// Dashboard Components
import MonthlyIncomeExpenseChart from '@/components/dashboard/MonthlyIncomeExpenseChart'
import CategoryPieChart from '@/components/dashboard/CategoryPieChart'
import RecentTransactions from '@/components/dashboard/RecentTransactions'
import UpcomingBills from '@/components/dashboard/UpcomingBills'
import StatCard from '@/components/dashboard/StatCard'
import LoadingSkeleton from '@/components/common/LoadingSkeleton'
import AIChatWidget from '@/components/ai/AIChatWidget'

// UI Components
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { TrendingUp, TrendingDown, Wallet, PiggyBank, DollarSign, Brain } from 'lucide-react'

const Dashboard = () => {
  const { user } = useAuth()
  const { toast } = useToast()
  
  const [summary, setSummary] = useState({
    totalBalance: 0,
    totalIncome: 0,
    totalExpense: 0,
    savings: 0,
    savingsRate: 0,
  })
  const [financialHealth, setFinancialHealth] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    try {
      setLoading(true)
      
      const response = await dashboardService.getDashboard()
      const data = response.data?.data || {}
      
      setSummary({
        totalBalance: data.totalBalance || 0,
        totalIncome: data.totalIncome || 0,
        totalExpense: data.totalExpense || 0,
        savings: data.savings || 0,
        savingsRate: data.savingsRate || 0,
      })
      
      setFinancialHealth(data.financialHealth || null)
    } catch (error) {
      console.error('Dashboard data fetch error:', error)
      toast({
        title: 'Error',
        description: 'Failed to load dashboard data. Please try again.',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const getFinancialHealthColor = (score) => {
    if (!score) return 'text-gray-600'
    if (score >= 80) return 'text-green-600'
    if (score >= 60) return 'text-yellow-600'
    return 'text-red-600'
  }

  const getFinancialHealthBg = (score) => {
    if (!score) return 'bg-gray-50'
    if (score >= 80) return 'bg-green-50'
    if (score >= 60) return 'bg-yellow-50'
    return 'bg-red-50'
  }

  if (loading) {
    return <LoadingSkeleton />
  }

  return (
    <div className="space-y-6 p-6">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Dashboard</h1>
          <p className="text-muted-foreground">
            Welcome back, {user?.name || user?.email}!
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline" className="bg-gradient-to-r from-blue-50 to-purple-50">
            <Brain className="w-4 h-4 mr-1" />
            AI Enhanced
          </Badge>
          {financialHealth && (
            <Badge className={`${getFinancialHealthColor(financialHealth.score)} ${getFinancialHealthBg(financialHealth.score)} border-0`}>
              Health: {financialHealth.score}/100
            </Badge>
          )}
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Balance"
          value={formatCurrency(summary.totalBalance)}
          icon={<Wallet className="w-5 h-5 text-blue-500" />}
          trend={summary.savings >= 0 ? 'up' : 'down'}
          subtitle="Net Balance"
        />
        <StatCard
          title="Monthly Income"
          value={formatCurrency(summary.totalIncome)}
          icon={<TrendingUp className="w-5 h-5 text-green-500" />}
          trend="up"
          subtitle="This month"
        />
        <StatCard
          title="Monthly Expenses"
          value={formatCurrency(summary.totalExpense)}
          icon={<TrendingDown className="w-5 h-5 text-red-500" />}
          trend="down"
          subtitle="This month"
        />
        <StatCard
          title="Savings Rate"
          value={`${Math.round(summary.savingsRate)}%`}
          icon={<PiggyBank className="w-5 h-5 text-violet-500" />}
          trend={summary.savingsRate > 0 ? 'up' : 'down'}
          subtitle={summary.savingsRate > 0 ? 'On Track' : 'Needs Attention'}
        />
      </div>

      {/* Charts Row - Monthly Income vs Expense and Category Pie Chart */}
      <div className="grid gap-6 md:grid-cols-2">
        <MonthlyIncomeExpenseChart />
        <CategoryPieChart />
      </div>

      {/* Transactions and Bills Row */}
      <div className="grid gap-6 md:grid-cols-2">
        <RecentTransactions limit={5} />
        <UpcomingBills daysAhead={30} />
      </div>

      {/* Financial Health Card */}
      {financialHealth && (
        <Card className={`${getFinancialHealthBg(financialHealth.score)} border-0`}>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-lg">
              <DollarSign className={`w-5 h-5 ${getFinancialHealthColor(financialHealth.score)}`} />
              Financial Health Score
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="text-center sm:text-left">
                <div className={`text-4xl font-bold ${getFinancialHealthColor(financialHealth.score)}`}>
                  {financialHealth.score}/100
                </div>
                <p className="text-sm text-muted-foreground mt-1">
                  {financialHealth.rating || 'Good'}
                </p>
              </div>
              {financialHealth.recommendations && financialHealth.recommendations.length > 0 && (
                <div className="flex-1 max-w-md">
                  <p className="text-sm font-medium mb-2">AI Recommendations:</p>
                  <ul className="text-sm text-muted-foreground space-y-1">
                    {financialHealth.recommendations.slice(0, 3).map((rec, index) => (
                      <li key={index} className="flex items-start gap-2">
                        <span className="text-blue-500 mt-0.5">•</span>
                        {rec}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* AI Chat Widget */}
      <AIChatWidget />
    </div>
  )
}

export default Dashboard
