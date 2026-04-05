import { useState, useEffect } from 'react'
import { useToast } from '@/hooks/use-toast'
import { dashboardService } from '@/services/dashboardService'
import { formatCurrency, formatDate } from '@/utils/formatters'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { CreditCard, ArrowUpRight, ArrowDownRight, ShoppingCart, Home, Car, Heart, Zap, Coffee, Briefcase, Gift, MoreHorizontal } from 'lucide-react'

const getCategoryIcon = (category) => {
  const icons = {
    'Food': Coffee,
    'Groceries': ShoppingCart,
    'Housing': Home,
    'Rent': Home,
    'Transportation': Car,
    'Transport': Car,
    'Health': Heart,
    'Healthcare': Heart,
    'Utilities': Zap,
    'Entertainment': Gift,
    'Salary': Briefcase,
    'Income': Briefcase,
    'Freelance': Briefcase,
    'Shopping': ShoppingCart,
    'Other': MoreHorizontal,
  }
  
  const IconComponent = icons[category] || CreditCard
  return <IconComponent className="w-4 h-4" />
}

const getCategoryColor = (category) => {
  const colors = {
    'Food': 'bg-orange-100 text-orange-700',
    'Groceries': 'bg-green-100 text-green-700',
    'Housing': 'bg-blue-100 text-blue-700',
    'Rent': 'bg-blue-100 text-blue-700',
    'Transportation': 'bg-yellow-100 text-yellow-700',
    'Transport': 'bg-yellow-100 text-yellow-700',
    'Health': 'bg-red-100 text-red-700',
    'Healthcare': 'bg-red-100 text-red-700',
    'Utilities': 'bg-purple-100 text-purple-700',
    'Entertainment': 'bg-pink-100 text-pink-700',
    'Salary': 'bg-green-100 text-green-700',
    'Income': 'bg-green-100 text-green-700',
    'Freelance': 'bg-green-100 text-green-700',
    'Shopping': 'bg-cyan-100 text-cyan-700',
  }
  return colors[category] || 'bg-gray-100 text-gray-700'
}

const RecentTransactions = ({ limit = 5 }) => {
  const { toast } = useToast()
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchRecentTransactions()
  }, [limit])

  const fetchRecentTransactions = async () => {
    try {
      setLoading(true)
      const response = await dashboardService.getRecentTransactions(limit)
      const data = response.data?.data || []
      setTransactions(data)
    } catch (error) {
      console.error('Error fetching recent transactions:', error)
      toast({
        title: 'Error',
        description: 'Failed to load recent transactions',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <Card className="h-[400px]">
        <CardContent className="flex items-center justify-center h-full">
          <div className="animate-pulse text-muted-foreground">Loading transactions...</div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="h-[400px]">
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg">
          <CreditCard className="w-5 h-5 text-blue-500" />
          Recent Transactions
        </CardTitle>
        <CardDescription>
          Your latest {limit} transactions
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-3 max-h-[300px] overflow-y-auto">
          {transactions.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <CreditCard className="w-12 h-12 mx-auto mb-2 text-gray-300" />
              <p>No transactions found</p>
              <p className="text-sm mt-1">Add your first expense to get started</p>
            </div>
          ) : (
            transactions.map((transaction) => {
              const isIncome = transaction.type === 'INCOME' || transaction.type === 'income'
              const amount = Number(transaction.amount || 0)
              const date = transaction.date || transaction.expenseDate || transaction.createdAt

              return (
                <div 
                  key={transaction.id} 
                  className="flex items-center justify-between p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className={`p-2 rounded-full ${getCategoryColor(transaction.category)}`}>
                      {getCategoryIcon(transaction.category)}
                    </div>
                    <div className="min-w-0">
                      <p className="font-medium text-sm truncate">
                        {transaction.description || transaction.title || 'Untitled'}
                      </p>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-xs text-muted-foreground">
                          {formatDate(date)}
                        </span>
                        <Badge 
                          variant="outline" 
                          className={`text-xs px-1.5 py-0 ${isIncome ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`}
                        >
                          {isIncome ? (
                            <ArrowUpRight className="w-3 h-3 mr-1" />
                          ) : (
                            <ArrowDownRight className="w-3 h-3 mr-1" />
                          )}
                          {transaction.type || 'EXPENSE'}
                        </Badge>
                      </div>
                    </div>
                  </div>
                  <span className={`font-semibold text-sm ${isIncome ? 'text-green-600' : 'text-gray-900'}`}>
                    {isIncome ? '+' : '-'}{formatCurrency(amount)}
                  </span>
                </div>
              )
            })
          )}
        </div>
      </CardContent>
    </Card>
  )
}

export default RecentTransactions
