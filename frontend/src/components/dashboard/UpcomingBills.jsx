import { useState, useEffect } from 'react'
import { useToast } from '@/hooks/use-toast'
import { dashboardService } from '@/services/dashboardService'
import { formatCurrency, formatDate } from '@/utils/formatters'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Calendar, AlertCircle, CheckCircle, Clock, Zap, Home, Smartphone, Wifi, CreditCard, MoreHorizontal } from 'lucide-react'

const getBillIcon = (category) => {
  const icons = {
    'Utilities': Zap,
    'Electricity': Zap,
    'Internet': Wifi,
    'Mobile': Smartphone,
    'Phone': Smartphone,
    'Rent': Home,
    'Housing': Home,
    'Credit Card': CreditCard,
    'Entertainment': Wifi,
    'Insurance': Shield,
    'Other': MoreHorizontal,
  }
  
  const IconComponent = icons[category] || CreditCard
  return <IconComponent className="w-4 h-4" />
}

const Shield = ({ className }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
  </svg>
)

const getStatusBadge = (daysUntil, isPaid) => {
  if (isPaid) {
    return (
      <Badge className="bg-green-100 text-green-700 border-green-200">
        <CheckCircle className="w-3 h-3 mr-1" />
        Paid
      </Badge>
    )
  }
  
  if (daysUntil < 0) {
    return (
      <Badge className="bg-red-100 text-red-700 border-red-200">
        <AlertCircle className="w-3 h-3 mr-1" />
        Overdue
      </Badge>
    )
  }
  
  if (daysUntil <= 3) {
    return (
      <Badge className="bg-orange-100 text-orange-700 border-orange-200">
        <Clock className="w-3 h-3 mr-1" />
        Due Soon
      </Badge>
    )
  }
  
  return (
    <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
      <Clock className="w-3 h-3 mr-1" />
      {daysUntil} days
    </Badge>
  )
}

const getCategoryColor = (category) => {
  const colors = {
    'Utilities': 'bg-yellow-100 text-yellow-700',
    'Electricity': 'bg-yellow-100 text-yellow-700',
    'Internet': 'bg-blue-100 text-blue-700',
    'Mobile': 'bg-green-100 text-green-700',
    'Phone': 'bg-green-100 text-green-700',
    'Rent': 'bg-purple-100 text-purple-700',
    'Housing': 'bg-purple-100 text-purple-700',
    'Credit Card': 'bg-red-100 text-red-700',
    'Entertainment': 'bg-pink-100 text-pink-700',
    'Insurance': 'bg-cyan-100 text-cyan-700',
  }
  return colors[category] || 'bg-gray-100 text-gray-700'
}

const UpcomingBills = ({ daysAhead = 30 }) => {
  const { toast } = useToast()
  const [bills, setBills] = useState([])
  const [loading, setLoading] = useState(true)
  const [totalDue, setTotalDue] = useState(0)

  useEffect(() => {
    fetchUpcomingBills()
  }, [daysAhead])

  const fetchUpcomingBills = async () => {
    try {
      setLoading(true)
      const response = await dashboardService.getUpcomingBills(daysAhead)
      const data = response.data?.data || []
      
      // Sort by days until due (ascending)
      const sortedBills = data.sort((a, b) => (a.daysUntilDue || 0) - (b.daysUntilDue || 0))
      
      setBills(sortedBills)
      
      // Calculate total unpaid amount
      const unpaidTotal = sortedBills
        .filter(bill => !bill.isPaid && (bill.daysUntilDue || 0) >= 0)
        .reduce((sum, bill) => sum + Number(bill.amount || 0), 0)
      setTotalDue(unpaidTotal)
    } catch (error) {
      console.error('Error fetching upcoming bills:', error)
      toast({
        title: 'Error',
        description: 'Failed to load upcoming bills',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  const formatDaysUntil = (days) => {
    if (days === 0) return 'Today'
    if (days === 1) return 'Tomorrow'
    if (days < 0) return `${Math.abs(days)} days overdue`
    return `Due in ${days} days`
  }

  if (loading) {
    return (
      <Card className="h-[400px]">
        <CardContent className="flex items-center justify-center h-full">
          <div className="animate-pulse text-muted-foreground">Loading bills...</div>
        </CardContent>
      </Card>
    )
  }

  const unpaidCount = bills.filter(b => !b.isPaid && (b.daysUntilDue || 0) >= 0).length

  return (
    <Card className="h-[400px]">
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-lg">
          <Calendar className="w-5 h-5 text-orange-500" />
          Upcoming Bills
        </CardTitle>
        <CardDescription>
          Bills due in next {daysAhead} days
        </CardDescription>
      </CardHeader>
      <CardContent>
        {/* Summary */}
        {unpaidCount > 0 && (
          <div className="mb-4 p-3 bg-orange-50 rounded-lg border border-orange-100">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-orange-700">
                  <span className="font-semibold">{unpaidCount}</span> bills upcoming
                </p>
                <p className="text-xs text-orange-600">
                  Total due: <span className="font-semibold">{formatCurrency(totalDue)}</span>
                </p>
              </div>
              <AlertCircle className="w-5 h-5 text-orange-500" />
            </div>
          </div>
        )}

        {/* Bills List */}
        <div className="space-y-3 max-h-[280px] overflow-y-auto">
          {bills.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <Calendar className="w-12 h-12 mx-auto mb-2 text-gray-300" />
              <p>No upcoming bills</p>
              <p className="text-sm mt-1">You're all caught up for the next {daysAhead} days!</p>
            </div>
          ) : (
            bills.map((bill) => {
              const amount = Number(bill.amount || 0)
              const daysUntil = bill.daysUntilDue || 0
              const isPaid = bill.isPaid || bill.status === 'PAID'
              const title = bill.title || bill.billName || 'Untitled Bill'
              const category = bill.category || 'Other'
              const dueDate = bill.dueDate

              return (
                <div 
                  key={bill.id} 
                  className={`flex items-center justify-between p-3 rounded-lg transition-colors ${
                    isPaid ? 'bg-green-50' : daysUntil < 0 ? 'bg-red-50' : daysUntil <= 3 ? 'bg-orange-50' : 'bg-gray-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`p-2 rounded-full ${getCategoryColor(category)}`}>
                      {getBillIcon(category)}
                    </div>
                    <div className="min-w-0">
                      <p className="font-medium text-sm truncate">{title}</p>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-xs text-muted-foreground">
                          {dueDate ? formatDate(dueDate) : formatDaysUntil(daysUntil)}
                        </span>
                        {getStatusBadge(daysUntil, isPaid)}
                      </div>
                    </div>
                  </div>
                  <span className={`font-semibold text-sm ${isPaid ? 'text-green-600 line-through' : 'text-gray-900'}`}>
                    {formatCurrency(amount)}
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

export default UpcomingBills
