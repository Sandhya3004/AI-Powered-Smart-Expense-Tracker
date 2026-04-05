import { useState, useEffect } from 'react'
import { useToast } from '@/hooks/use-toast'
import { dashboardService } from '@/services/dashboardService'
import { formatCurrency } from '@/utils/formatters'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { PieChartIcon } from 'lucide-react'

const COLORS = [
  '#3b82f6', // blue
  '#22c55e', // green
  '#f59e0b', // amber
  '#ef4444', // red
  '#8b5cf6', // violet
  '#06b6d4', // cyan
  '#f97316', // orange
  '#84cc16', // lime
  '#ec4899', // pink
  '#6b7280', // gray
]

const CategoryPieChart = () => {
  const { toast } = useToast()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(true)
  const [totalAmount, setTotalAmount] = useState(0)

  useEffect(() => {
    fetchCategoryBreakdown()
  }, [])

  const fetchCategoryBreakdown = async () => {
    try {
      setLoading(true)
      const response = await dashboardService.getCategoryBreakdown()
      const breakdown = response.data?.data || {}

      // Transform object to array for Recharts
      const chartData = Object.entries(breakdown).map(([name, value]) => ({
        name,
        value: Number(value || 0),
      }))

      // Sort by value descending and take top 8
      const sortedData = chartData.sort((a, b) => b.value - a.value).slice(0, 8)

      // Group remaining as "Others" if there are more categories
      if (chartData.length > 8) {
        const othersValue = chartData.slice(8).reduce((sum, item) => sum + item.value, 0)
        if (othersValue > 0) {
          sortedData.push({ name: 'Others', value: othersValue })
        }
      }

      setData(sortedData)
      setTotalAmount(sortedData.reduce((sum, item) => sum + item.value, 0))
    } catch (error) {
      console.error('Error fetching category breakdown:', error)
      toast({
        title: 'Error',
        description: 'Failed to load category breakdown data',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      const percentage = totalAmount > 0 ? ((data.value / totalAmount) * 100).toFixed(1) : 0
      return (
        <div className="bg-white p-3 border rounded-lg shadow-lg">
          <p className="font-semibold text-sm">{data.name}</p>
          <p className="text-sm text-muted-foreground">
            {formatCurrency(data.value)} ({percentage}%)
          </p>
        </div>
      )
    }
    return null
  }

  const renderCustomLabel = ({ name, value, percent }) => {
    if (percent < 0.05) return null // Don't show label for very small slices
    return `${(percent * 100).toFixed(0)}%`
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

  if (data.length === 0) {
    return (
      <Card className="h-[400px]">
        <CardHeader className="pb-2">
          <CardTitle className="flex items-center gap-2 text-lg">
            <PieChartIcon className="w-5 h-5 text-violet-500" />
            Spending by Category
          </CardTitle>
          <CardDescription>
            Current month breakdown
          </CardDescription>
        </CardHeader>
        <CardContent className="flex items-center justify-center h-[300px]">
          <div className="text-center text-muted-foreground">
            <PieChartIcon className="w-12 h-12 mx-auto mb-2 text-gray-300" />
            <p>No expense data available</p>
            <p className="text-sm mt-1">Add expenses to see category breakdown</p>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="h-[400px]">
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg">
          <PieChartIcon className="w-5 h-5 text-violet-500" />
          Spending by Category
        </CardTitle>
        <CardDescription>
          Current month breakdown
        </CardDescription>
      </CardHeader>
      <CardContent>
        {/* Total Amount */}
        <div className="text-center mb-4">
          <p className="text-sm text-muted-foreground">Total Spent</p>
          <p className="text-2xl font-bold text-gray-900">{formatCurrency(totalAmount)}</p>
        </div>

        {/* Pie Chart */}
        <ResponsiveContainer width="100%" height={220}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={renderCustomLabel}
              outerRadius={80}
              innerRadius={40}
              fill="#8884d8"
              dataKey="value"
              paddingAngle={2}
            >
              {data.map((entry, index) => (
                <Cell 
                  key={`cell-${index}`} 
                  fill={COLORS[index % COLORS.length]}
                  stroke="#fff"
                  strokeWidth={2}
                />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
          </PieChart>
        </ResponsiveContainer>

        {/* Legend */}
        <div className="grid grid-cols-2 gap-2 mt-2">
          {data.slice(0, 6).map((item, index) => (
            <div key={item.name} className="flex items-center gap-2 text-xs">
              <div 
                className="w-3 h-3 rounded-full flex-shrink-0"
                style={{ backgroundColor: COLORS[index % COLORS.length] }}
              />
              <span className="truncate">{item.name}</span>
              <span className="text-muted-foreground flex-shrink-0">
                {totalAmount > 0 ? ((item.value / totalAmount) * 100).toFixed(0) : 0}%
              </span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

export default CategoryPieChart
