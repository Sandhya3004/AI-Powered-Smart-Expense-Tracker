import { useState, useEffect } from 'react'
import { useToast } from '@/hooks/use-toast'
import { dashboardService } from '@/services/dashboardService'
import { formatCurrency } from '@/utils/formatters'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { TrendingUp, TrendingDown } from 'lucide-react'

const MonthlyIncomeExpenseChart = () => {
  const { toast } = useToast()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [totals, setTotals] = useState({ income: 0, expense: 0, savings: 0 })

  useEffect(() => {
    fetchMonthlyTrends()
  }, [])

  const fetchMonthlyTrends = async () => {
    try {
      setLoading(true)
      const response = await dashboardService.getMonthlyTrends(6)
      const trends = response.data?.data || []

      // Transform data for the chart
      const chartData = trends.map(trend => ({
        month: trend.month?.substring(0, 3) || trend.monthName?.substring(0, 3) || '',
        fullMonth: trend.month || '',
        income: Number(trend.income || 0),
        expense: Number(trend.expense || 0),
      }))

      setData(chartData)

      // Calculate totals for the last month
      if (chartData.length > 0) {
        const lastMonth = chartData[chartData.length - 1]
        const totalIncome = chartData.reduce((sum, item) => sum + item.income, 0)
        const totalExpense = chartData.reduce((sum, item) => sum + item.expense, 0)
        setTotals({
          income: lastMonth.income,
          expense: lastMonth.expense,
          savings: lastMonth.income - lastMonth.expense
        })
      }
    } catch (error) {
      console.error('Error fetching monthly trends:', error)
      toast({
        title: 'Error',
        description: 'Failed to load monthly trends data',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white p-3 border rounded-lg shadow-lg">
          <p className="font-semibold text-sm mb-2">{label}</p>
          <p className="text-sm text-green-600">
            Income: {formatCurrency(payload[0].value)}
          </p>
          <p className="text-sm text-red-600">
            Expense: {formatCurrency(payload[1].value)}
          </p>
          <p className="text-sm text-blue-600 mt-1">
            Savings: {formatCurrency(payload[0].value - payload[1].value)}
          </p>
        </div>
      )
    }
    return null
  }

  if (loading) {
    return (
      <Card className="h-[400px]">
        <CardContent className="flex items-center justify-center h-full">
          <div className="animate-pulse text-muted-foreground">Loading chart data...</div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="h-[400px]">
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg">
          <TrendingUp className="w-5 h-5 text-blue-500" />
          Monthly Income vs Expense
        </CardTitle>
        <CardDescription>
          Last 6 months comparison
        </CardDescription>
      </CardHeader>
      <CardContent>
        {/* Summary Stats */}
        <div className="grid grid-cols-3 gap-4 mb-4">
          <div className="text-center p-2 bg-green-50 rounded-lg">
            <p className="text-xs text-green-600 mb-1">Income</p>
            <p className="font-semibold text-green-700">{formatCurrency(totals.income)}</p>
          </div>
          <div className="text-center p-2 bg-red-50 rounded-lg">
            <p className="text-xs text-red-600 mb-1">Expenses</p>
            <p className="font-semibold text-red-700">{formatCurrency(totals.expense)}</p>
          </div>
          <div className="text-center p-2 bg-blue-50 rounded-lg">
            <p className="text-xs text-blue-600 mb-1">Savings</p>
            <p className="font-semibold text-blue-700">{formatCurrency(totals.savings)}</p>
          </div>
        </div>

        {/* Chart */}
        <ResponsiveContainer width="100%" height={250}>
          <BarChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis 
              dataKey="month" 
              tick={{ fontSize: 12 }}
              axisLine={{ stroke: '#e0e0e0' }}
            />
            <YAxis 
              tick={{ fontSize: 12 }}
              axisLine={{ stroke: '#e0e0e0' }}
              tickFormatter={(value) => `₹${value >= 1000 ? (value/1000).toFixed(0) + 'k' : value}`}
            />
            <Tooltip content={<CustomTooltip />} />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Bar 
              dataKey="income" 
              name="Income" 
              fill="#22c55e" 
              radius={[4, 4, 0, 0]}
              maxBarSize={40}
            />
            <Bar 
              dataKey="expense" 
              name="Expense" 
              fill="#ef4444" 
              radius={[4, 4, 0, 0]}
              maxBarSize={40}
            />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  )
}

export default MonthlyIncomeExpenseChart
