import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { 
  TrendingUp, 
  TrendingDown, 
  Search, 
  Filter,
  Download,
  Calendar,
  DollarSign,
  ArrowUpRight,
  ArrowDownRight,
  ShoppingCart,
  GraduationCap,
  Car,
  Heart,
  ShoppingBag,
  Utensils,
  Film
} from 'lucide-react';
import { api } from '@/api/api';
import { formatCurrency } from '@/utils/formatters';

const TransactionsDashboard = () => {
  const [statistics, setStatistics] = useState({
    totalBalance: 0,
    balanceChange: 0,
    balanceChangePercent: 0,
    income: 0,
    incomeChange: 0,
    incomeChangePercent: 0,
    expenses: 0,
    expensesChange: 0,
    expensesChangePercent: 0
  });

  const [categoryData, setCategoryData] = useState([]);
  const [monthlyTrend, setMonthlyTrend] = useState([]);
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState('6months');

  useEffect(() => {
    fetchDashboardData();
  }, [timeRange]);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      
      // Fetch statistics
      const statsResponse = await api.get(`/transactions/statistics?timeRange=${timeRange}`);
      setStatistics(statsResponse.data);

      // Fetch category data
      const categoryResponse = await api.get(`/transactions/by-category?timeRange=${timeRange}`);
      setCategoryData(categoryResponse.data);

      // Fetch monthly trend
      const trendResponse = await api.get(`/transactions/monthly-trend?timeRange=${timeRange}`);
      setMonthlyTrend(trendResponse.data);
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getCategoryIcon = (category) => {
    const icons = {
      'groceries': <ShoppingCart className="h-4 w-4" />,
      'education': <GraduationCap className="h-4 w-4" />,
      'transport': <Car className="h-4 w-4" />,
      'personal care': <Heart className="h-4 w-4" />,
      'shopping': <ShoppingBag className="h-4 w-4" />,
      'food': <Utensils className="h-4 w-4" />,
      'entertainment': <Film className="h-4 w-4" />
    };
    return icons[category.toLowerCase()] || <ShoppingCart className="h-4 w-4" />;
  };

  const getCategoryColor = (category) => {
    const colors = {
      'groceries': 'bg-green-500',
      'education': 'bg-blue-500',
      'transport': 'bg-yellow-500',
      'personal care': 'bg-purple-500',
      'shopping': 'bg-pink-500',
      'food': 'bg-orange-500',
      'entertainment': 'bg-red-500'
    };
    return colors[category.toLowerCase()] || 'bg-gray-500';
  };

  const formatPercentage = (value) => {
    const sign = value >= 0 ? '+' : '';
    return `${sign}${value.toFixed(2)}%`;
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i} className="animate-pulse">
              <CardHeader className="pb-2">
                <div className="h-4 bg-gray-200 rounded w-20"></div>
              </CardHeader>
              <CardContent>
                <div className="h-8 bg-gray-200 rounded w-24 mb-2"></div>
                <div className="h-4 bg-gray-200 rounded w-16"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Transactions Dashboard</h1>
          <p className="text-gray-600">Track your income and expenses</p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={timeRange} onValueChange={setTimeRange}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1month">Last Month</SelectItem>
              <SelectItem value="3months">Last 3 Months</SelectItem>
              <SelectItem value="6months">Last 6 Months</SelectItem>
              <SelectItem value="1year">Last Year</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" size="sm">
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        {/* Total Balance Card */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">Total Balance</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-2xl font-bold">{formatCurrency(statistics.totalBalance)}</p>
                <div className="flex items-center text-sm">
                  {statistics.balanceChangePercent < 0 ? (
                    <>
                      <ArrowDownRight className="h-4 w-4 text-red-500 mr-1" />
                      <span className="text-red-500">{formatPercentage(statistics.balanceChangePercent)} this month</span>
                    </>
                  ) : (
                    <>
                      <ArrowUpRight className="h-4 w-4 text-green-500 mr-1" />
                      <span className="text-green-500">{formatPercentage(statistics.balanceChangePercent)} this month</span>
                    </>
                  )}
                </div>
              </div>
              <div className={`p-3 rounded-full ${statistics.totalBalance >= 0 ? 'bg-green-100' : 'bg-red-100'}`}>
                <DollarSign className={`h-6 w-6 ${statistics.totalBalance >= 0 ? 'text-green-600' : 'text-red-600'}`} />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Income Card */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">Income</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-2xl font-bold text-green-600">{formatCurrency(statistics.income)}</p>
                <div className="flex items-center text-sm">
                  <ArrowUpRight className="h-4 w-4 text-green-500 mr-1" />
                  <span className="text-green-500">{formatPercentage(statistics.incomeChangePercent)} from last month</span>
                </div>
              </div>
              <div className="p-3 rounded-full bg-green-100">
                <TrendingUp className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Expenses Card */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">Expenses</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-2xl font-bold text-red-600">{formatCurrency(statistics.expenses)}</p>
                <div className="flex items-center text-sm">
                  <ArrowUpRight className="h-4 w-4 text-red-500 mr-1" />
                  <span className="text-red-500">{formatPercentage(statistics.expensesChangePercent)} from last month</span>
                  <span className="text-yellow-600 ml-1">⚠️</span>
                </div>
              </div>
              <div className="p-3 rounded-full bg-red-100">
                <TrendingDown className="h-6 w-6 text-red-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Charts Row */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Spending by Category Chart */}
        <Card>
          <CardHeader>
            <CardTitle>Spending by Category</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {categoryData.map((category, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`p-2 rounded ${getCategoryColor(category.name)} bg-opacity-20`}>
                      {getCategoryIcon(category.name)}
                    </div>
                    <span className="text-sm font-medium">{category.name}</span>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold">{formatCurrency(category.amount)}</p>
                    <p className="text-xs text-gray-500">{category.percentage}%</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Monthly Trend Chart */}
        <Card>
          <CardHeader>
            <CardTitle>Monthly Trend</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 flex items-center justify-center text-gray-500">
              <div className="text-center">
                <TrendingUp className="h-12 w-12 mx-auto mb-2" />
                <p>Chart visualization will be implemented here</p>
                <p className="text-sm">Income, Expenses, and Savings over time</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <Button className="h-20 flex-col gap-2">
              <DollarSign className="h-6 w-6" />
              Add Transaction
            </Button>
            <Button variant="outline" className="h-20 flex-col gap-2">
              <ShoppingCart className="h-6 w-6" />
              Scan Receipt
            </Button>
            <Button variant="outline" className="h-20 flex-col gap-2">
              <Search className="h-6 w-6" />
              Voice Input
            </Button>
            <Button variant="outline" className="h-20 flex-col gap-2">
              <Calendar className="h-6 w-6" />
              View Calendar
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default TransactionsDashboard;
