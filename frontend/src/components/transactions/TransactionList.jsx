import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { 
  Search, 
  Filter, 
  Plus, 
  Download, 
  Upload,
  TrendingUp,
  TrendingDown,
  ArrowRightLeft,
  Calendar,
  CreditCard,
  Banknote,
  PiggyBank,
  ShoppingCart,
  Home,
  Car,
  Heart,
  Zap,
  Coffee
} from 'lucide-react';
import { formatCurrency } from '@/utils/formatters';
import { api } from '@/api/api';

const TransactionList = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    search: '',
    type: '',
    category: '',
    account: '',
    startDate: '',
    endDate: ''
  });
  const [pagination, setPagination] = useState({
    page: 0,
    size: 20,
    total: 0
  });

  const categories = [
    'Food & Dining', 'Transportation', 'Shopping', 'Entertainment',
    'Bills & Utilities', 'Healthcare', 'Education', 'Travel',
    'Investments', 'Savings', 'Other'
  ];

  const accounts = [
    'Cash', 'Bank Account', 'Credit Card', 'Debit Card',
    'Digital Wallet', 'Investment Account', 'Savings Account'
  ];

  const getCategoryIcon = (category) => {
    const icons = {
      'Food & Dining': <Coffee className="h-4 w-4" />,
      'Transportation': <Car className="h-4 w-4" />,
      'Shopping': <ShoppingCart className="h-4 w-4" />,
      'Entertainment': <Zap className="h-4 w-4" />,
      'Bills & Utilities': <Home className="h-4 w-4" />,
      'Healthcare': <Heart className="h-4 w-4" />,
      'Investments': <TrendingUp className="h-4 w-4" />,
      'Savings': <PiggyBank className="h-4 w-4" />,
      'Other': <CreditCard className="h-4 w-4" />
    };
    return icons[category] || icons['Other'];
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'INCOME':
        return <TrendingUp className="h-4 w-4 text-green-600" />;
      case 'EXPENSE':
        return <TrendingDown className="h-4 w-4 text-red-600" />;
      case 'TRANSFER':
        return <ArrowRightLeft className="h-4 w-4 text-blue-600" />;
      default:
        return <CreditCard className="h-4 w-4" />;
    }
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'INCOME':
        return 'text-green-600 bg-green-50 border-green-200';
      case 'EXPENSE':
        return 'text-red-600 bg-red-50 border-red-200';
      case 'TRANSFER':
        return 'text-blue-600 bg-blue-50 border-blue-200';
      default:
        return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const response = await api.post('/transactions/search', {
        ...filters,
        page: pagination.page,
        size: pagination.size,
        sortBy: 'date',
        sortDirection: 'desc'
      });
      
      setTransactions(response.data.content || []);
      setPagination(prev => ({
        ...prev,
        total: response.data.totalElements || 0
      }));
    } catch (error) {
      console.error('Error fetching transactions:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [filters, pagination.page]);

  const handleSearch = (value) => {
    setFilters(prev => ({ ...prev, search: value }));
    setPagination(prev => ({ ...prev, page: 0 }));
  };

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPagination(prev => ({ ...prev, page: 0 }));
  };

  const handleExport = async () => {
    try {
      const response = await api.get('/transactions/export', {
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'transactions.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error exporting transactions:', error);
    }
  };

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="h-8 bg-gray-200 rounded animate-pulse"></div>
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 bg-gray-200 rounded animate-pulse"></div>
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
          <h2 className="text-2xl font-bold">Transactions</h2>
          <p className="text-muted-foreground">
            Manage and track all your financial transactions
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={handleExport}>
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
          <Button variant="outline">
            <Upload className="h-4 w-4 mr-2" />
            Import
          </Button>
          <Button>
            <Plus className="h-4 w-4 mr-2" />
            Add Transaction
          </Button>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Filter className="h-5 w-5" />
            Filters
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-6">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search transactions..."
                value={filters.search}
                onChange={(e) => handleSearch(e.target.value)}
                className="pl-10"
              />
            </div>

            {/* Type Filter */}
            <Select value={filters.type} onValueChange={(value) => handleFilterChange('type', value)}>
              <SelectTrigger>
                <SelectValue placeholder="All Types" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Types</SelectItem>
                <SelectItem value="EXPENSE">Expense</SelectItem>
                <SelectItem value="INCOME">Income</SelectItem>
                <SelectItem value="TRANSFER">Transfer</SelectItem>
              </SelectContent>
            </Select>

            {/* Category Filter */}
            <Select value={filters.category} onValueChange={(value) => handleFilterChange('category', value)}>
              <SelectTrigger>
                <SelectValue placeholder="All Categories" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Categories</SelectItem>
                {categories.map(category => (
                  <SelectItem key={category} value={category}>{category}</SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Account Filter */}
            <Select value={filters.account} onValueChange={(value) => handleFilterChange('account', value)}>
              <SelectTrigger>
                <SelectValue placeholder="All Accounts" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Accounts</SelectItem>
                {accounts.map(account => (
                  <SelectItem key={account} value={account}>{account}</SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Start Date */}
            <Input
              type="date"
              value={filters.startDate}
              onChange={(e) => handleFilterChange('startDate', e.target.value)}
              placeholder="Start Date"
            />

            {/* End Date */}
            <Input
              type="date"
              value={filters.endDate}
              onChange={(e) => handleFilterChange('endDate', e.target.value)}
              placeholder="End Date"
            />
          </div>
        </CardContent>
      </Card>

      {/* Transactions List */}
      <Card>
        <CardHeader>
          <CardTitle>
            Transactions ({pagination.total} total)
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {transactions.map((transaction) => (
              <div
                key={transaction.id}
                className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center gap-4">
                  {/* Type Icon */}
                  <div className="flex items-center justify-center w-10 h-10 rounded-full bg-gray-100">
                    {getTypeIcon(transaction.type)}
                  </div>

                  {/* Transaction Details */}
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="font-medium">{transaction.description}</p>
                      <Badge className={getTypeColor(transaction.type)}>
                        {transaction.type}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-4 text-sm text-muted-foreground">
                      <div className="flex items-center gap-1">
                        {getCategoryIcon(transaction.category)}
                        <span>{transaction.category}</span>
                      </div>
                      {transaction.account && (
                        <div className="flex items-center gap-1">
                          <Banknote className="h-3 w-3" />
                          <span>{transaction.account}</span>
                        </div>
                      )}
                      <div className="flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        <span>{new Date(transaction.expenseDate).toLocaleDateString()}</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Amount and Actions */}
                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className={`font-semibold ${
                      transaction.type === 'INCOME' ? 'text-green-600' : 
                      transaction.type === 'EXPENSE' ? 'text-red-600' : 
                      'text-blue-600'
                    }`}>
                      {transaction.type === 'INCOME' ? '+' : 
                       transaction.type === 'EXPENSE' ? '-' : ''}
                      {formatCurrency(transaction.amount)}
                    </p>
                    {transaction.merchant && (
                      <p className="text-sm text-muted-foreground">{transaction.merchant}</p>
                    )}
                  </div>
                  
                  <div className="flex items-center gap-2">
                    <Button variant="ghost" size="sm">
                      Edit
                    </Button>
                    <Button variant="ghost" size="sm" className="text-red-600">
                      Delete
                    </Button>
                  </div>
                </div>
              </div>
            ))}

            {transactions.length === 0 && (
              <div className="text-center py-8">
                <CreditCard className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                <p className="text-muted-foreground">No transactions found</p>
                <Button className="mt-4">
                  <Plus className="h-4 w-4 mr-2" />
                  Add Your First Transaction
                </Button>
              </div>
            )}
          </div>

          {/* Pagination */}
          {pagination.total > 0 && (
            <div className="flex items-center justify-between mt-4">
              <div className="text-sm text-muted-foreground">
                Showing {pagination.page * pagination.size + 1} to{' '}
                {Math.min((pagination.page + 1) * pagination.size, pagination.total)} of{' '}
                {pagination.total} transactions
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={pagination.page === 0}
                  onClick={() => setPagination(prev => ({ ...prev, page: prev.page - 1 }))}
                >
                  Previous
                </Button>
                <span className="text-sm">
                  Page {pagination.page + 1} of {Math.ceil(pagination.total / pagination.size)}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={(pagination.page + 1) * pagination.size >= pagination.total}
                  onClick={() => setPagination(prev => ({ ...prev, page: prev.page + 1 }))}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default TransactionList;
