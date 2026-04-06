import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { api } from '@/api/api';
import { useToast } from '@/hooks/use-toast';
import { 
  TrendingUp, 
  PieChart, 
  DollarSign, 
  Sparkles,
  Loader2,
  Lightbulb
} from 'lucide-react';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  PieChart as RePieChart,
  Pie,
  Cell,
  Legend
} from 'recharts';

const COLORS = ['#7B6FC9', '#9C90E8', '#4ADE80', '#F472B6', '#60A5FA', '#FBBF24', '#F87171'];

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

const Insights = () => {
  const [loading, setLoading] = useState(true);
  const [insights, setInsights] = useState({
    totalSpent: 0,
    categoryBreakdown: {},
    monthlyTrend: [],
    topCategory: null,
    aiSuggestion: ''
  });
  const { toast } = useToast();

  useEffect(() => {
    fetchInsights();
  }, []);

  const fetchInsights = async () => {
    try {
      setLoading(true);
      
      // Fetch all required data in parallel
      const [trendsRes, categoriesRes, aiRes] = await Promise.all([
        api.get('/insights/monthly-trends').catch(() => ({ data: { data: [] } })),
        api.get('/insights/top-categories').catch(() => ({ data: { data: [] } })),
        api.post('/ai/chat', { message: 'Give me a personalized savings tip based on my spending' }).catch(() => null)
      ]);

      const monthlyTrends = trendsRes.data?.data || [];
      const categoryData = categoriesRes.data?.data || [];
      const aiSuggestion = aiRes?.data?.data?.reply || 'Track your expenses regularly to identify saving opportunities!';

      // Calculate total spent from current month
      const currentMonth = monthlyTrends[monthlyTrends.length - 1] || { amount: 0 };
      const totalSpent = currentMonth.amount || 0;

      // Format category breakdown for pie chart
      const categoryBreakdown = {};
      let topCategory = null;
      let maxAmount = 0;
      
      categoryData.forEach(cat => {
        const amount = cat.amount || 0;
        categoryBreakdown[cat.category] = amount;
        if (amount > maxAmount) {
          maxAmount = amount;
          topCategory = cat.category;
        }
      });

      // Format monthly trend for line chart
      const formattedTrends = monthlyTrends.map(trend => ({
        month: trend.month || '',
        amount: trend.amount || 0
      }));

      setInsights({
        totalSpent,
        categoryBreakdown,
        monthlyTrend: formattedTrends,
        topCategory,
        aiSuggestion
      });
    } catch (error) {
      console.error('Failed to fetch insights:', error);
      toast({
        title: 'Error',
        description: 'Failed to load insights. Please try again.',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  // Prepare data for pie chart
  const pieData = Object.entries(insights.categoryBreakdown).map(([name, value]) => ({
    name,
    value
  }));

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center gap-3 text-[#7B6FC9]">
          <Loader2 className="w-8 h-8 animate-spin" />
          <span className="text-lg">Loading insights...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Financial Insights</h1>
          <p className="text-gray-400 mt-1">Analyze your spending patterns and get AI-powered advice</p>
        </div>
      </div>

      {/* AI Suggestion Card */}
      <Card className="bg-gradient-to-br from-[#7B6FC9]/20 to-[#9C90E8]/10 border-[#7B6FC9]/30">
        <CardContent className="p-6">
          <div className="flex items-start gap-4">
            <div className="w-12 h-12 rounded-full bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] flex items-center justify-center flex-shrink-0">
              <Lightbulb className="w-6 h-6 text-white" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-white mb-2 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-[#9C90E8]" />
                AI Savings Suggestion
              </h3>
              <p className="text-gray-300 leading-relaxed">{insights.aiSuggestion}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Stats Overview */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <DollarSign className="w-4 h-4" />
              Total Spent This Month
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">{formatCurrency(insights.totalSpent)}</p>
            <p className="text-sm text-gray-400 mt-1">Based on your expenses</p>
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <PieChart className="w-4 h-4" />
              Top Spending Category
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold text-white">{insights.topCategory || 'N/A'}</p>
            <p className="text-sm text-gray-400 mt-1">
              {insights.topCategory ? formatCurrency(insights.categoryBreakdown[insights.topCategory]) : 'No data'}
            </p>
          </CardContent>
        </Card>

        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <TrendingUp className="w-4 h-4" />
              Categories Tracked
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">{Object.keys(insights.categoryBreakdown).length}</p>
            <p className="text-sm text-gray-400 mt-1">Different expense categories</p>
          </CardContent>
        </Card>
      </div>

      {/* Charts Grid */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Monthly Trend Chart */}
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader>
            <CardTitle className="text-white flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-[#7B6FC9]" />
              Monthly Spending Trend
            </CardTitle>
          </CardHeader>
          <CardContent>
            {insights.monthlyTrend.length > 0 ? (
              <div className="h-[300px] w-full min-h-[300px] min-w-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={insights.monthlyTrend}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#3A3560" />
                    <XAxis 
                      dataKey="month" 
                      stroke="#9CA3AF"
                      fontSize={12}
                    />
                    <YAxis 
                      stroke="#9CA3AF"
                      fontSize={12}
                      tickFormatter={(value) => `₹${value}`}
                    />
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: '#1E1E2A', 
                        border: '1px solid #3A3560',
                        borderRadius: '8px'
                      }}
                      formatter={(value) => [formatCurrency(value), 'Amount']}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="amount" 
                      stroke="#7B6FC9" 
                      strokeWidth={3}
                      dot={{ fill: '#7B6FC9', strokeWidth: 2 }}
                      activeDot={{ r: 8, fill: '#9C90E8' }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <div className="h-[300px] flex items-center justify-center text-gray-400">
                <p>No spending data available</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Category Breakdown Pie Chart */}
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader>
            <CardTitle className="text-white flex items-center gap-2">
              <PieChart className="w-5 h-5 text-[#7B6FC9]" />
              Spending by Category
            </CardTitle>
          </CardHeader>
          <CardContent>
            {pieData.length > 0 ? (
              <div className="h-[300px] w-full min-h-[300px] min-w-[300px]">
                <ResponsiveContainer width="100%" height="100%">
                  <RePieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={100}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {pieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: '#1E1E2A', 
                        border: '1px solid #3A3560',
                        borderRadius: '8px'
                      }}
                      formatter={(value) => formatCurrency(value)}
                    />
                    <Legend 
                      verticalAlign="bottom" 
                      height={36}
                      iconType="circle"
                      wrapperStyle={{ color: '#9CA3AF' }}
                    />
                  </RePieChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <div className="h-[300px] flex items-center justify-center text-gray-400">
                <p>No category data available</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Category Breakdown List */}
      <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
        <CardHeader>
          <CardTitle className="text-white">Category Breakdown</CardTitle>
        </CardHeader>
        <CardContent>
          {Object.entries(insights.categoryBreakdown).length > 0 ? (
            <div className="space-y-4">
              {Object.entries(insights.categoryBreakdown)
                .sort(([, a], [, b]) => b - a)
                .map(([category, amount], index) => (
                  <div key={category} className="flex items-center justify-between p-3 bg-[#1E1E2A]/50 rounded-lg">
                    <div className="flex items-center gap-3">
                      <div 
                        className="w-4 h-4 rounded-full"
                        style={{ backgroundColor: COLORS[index % COLORS.length] }}
                      />
                      <span className="text-white font-medium">{category}</span>
                    </div>
                    <div className="text-right">
                      <p className="text-white font-semibold">{formatCurrency(amount)}</p>
                      <p className="text-sm text-gray-400">
                        {((amount / insights.totalSpent) * 100).toFixed(1)}% of total
                      </p>
                    </div>
                  </div>
                ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-400">
              <p>No expense categories found</p>
              <p className="text-sm mt-2">Add expenses to see your spending breakdown</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default Insights;
