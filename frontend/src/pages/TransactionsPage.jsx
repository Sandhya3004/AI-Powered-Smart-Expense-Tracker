import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import TransactionList from '@/components/transactions/TransactionList';
import TransactionForm from '@/components/transactions/TransactionForm';
import { 
  TrendingUp,
  TrendingDown,
  ArrowRightLeft,
  PiggyBank,
  CreditCard,
  Banknote,
  Calendar,
  Filter,
  Download,
  Upload,
  Plus,
  BarChart3,
  PieChart,
  Target
} from 'lucide-react';
import { formatCurrency } from '@/utils/formatters';
import { api } from '@/api/api';

const TransactionsPage = () => {
  const [showForm, setShowForm] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSummary = async () => {
    try {
      setLoading(true);
      const response = await api.get('/transactions/summary');
      setSummary(response.data);
    } catch (error) {
      console.error('Error fetching summary:', error);
    } finally {
      setLoading(false);
    }
  };

  React.useEffect(() => {
    fetchSummary();
  }, []);

  const handleAddTransaction = () => {
    setEditingTransaction(null);
    setShowForm(true);
  };

  const handleEditTransaction = (transaction) => {
    setEditingTransaction(transaction);
    setShowForm(true);
  };

  const handleSaveTransaction = () => {
    setShowForm(false);
    setEditingTransaction(null);
    fetchSummary(); // Refresh summary
  };

  const handleCloseForm = () => {
    setShowForm(false);
    setEditingTransaction(null);
  };

  const getQuickStats = () => {
    if (!summary) return [];

    return [
      {
        title: 'Total Income',
        value: formatCurrency(summary.totalIncome || 0),
        icon: <TrendingUp className="h-5 w-5 text-green-600" />,
        color: 'bg-green-50 border-green-200'
      },
      {
        title: 'Total Expenses',
        value: formatCurrency(summary.totalExpenses || 0),
        icon: <TrendingDown className="h-5 w-5 text-red-600" />,
        color: 'bg-red-50 border-red-200'
      },
      {
        title: 'Net Amount',
        value: formatCurrency(summary.netAmount || 0),
        icon: <Target className="h-5 w-5 text-blue-600" />,
        color: 'bg-blue-50 border-blue-200'
      },
      {
        title: 'Transactions',
        value: `${summary.categoryBreakdown?.length || 0} Categories`,
        icon: <BarChart3 className="h-5 w-5 text-purple-600" />,
        color: 'bg-purple-50 border-purple-200'
      }
    ];
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Transactions</h1>
          <p className="text-muted-foreground">
            Manage your income, expenses, and transfers
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline">
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
          <Button variant="outline">
            <Upload className="h-4 w-4 mr-2" />
            Import
          </Button>
          <Button onClick={handleAddTransaction}>
            <Plus className="h-4 w-4 mr-2" />
            Add Transaction
          </Button>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {getQuickStats().map((stat, index) => (
          <Card key={index} className={stat.color}>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-muted-foreground">{stat.title}</p>
                  <p className="text-2xl font-bold">{stat.value}</p>
                </div>
                <div className="p-2 rounded-full bg-white">
                  {stat.icon}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Main Content */}
      <Tabs defaultValue="all" className="space-y-6">
        <TabsList>
          <TabsTrigger value="all">All Transactions</TabsTrigger>
          <TabsTrigger value="income">Income</TabsTrigger>
          <TabsTrigger value="expenses">Expenses</TabsTrigger>
          <TabsTrigger value="transfers">Transfers</TabsTrigger>
          <TabsTrigger value="recurring">Recurring</TabsTrigger>
          <TabsTrigger value="analytics">Analytics</TabsTrigger>
        </TabsList>

        <TabsContent value="all">
          <TransactionList onEdit={handleEditTransaction} />
        </TabsContent>

        <TabsContent value="income">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="h-5 w-5 text-green-600" />
                Income Transactions
              </CardTitle>
            </CardHeader>
            <CardContent>
              <TransactionList 
                onEdit={handleEditTransaction} 
                defaultFilters={{ type: 'INCOME' }}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="expenses">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingDown className="h-5 w-5 text-red-600" />
                Expense Transactions
              </CardTitle>
            </CardHeader>
            <CardContent>
              <TransactionList 
                onEdit={handleEditTransaction} 
                defaultFilters={{ type: 'EXPENSE' }}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="transfers">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ArrowRightLeft className="h-5 w-5 text-blue-600" />
                Transfer Transactions
              </CardTitle>
            </CardHeader>
            <CardContent>
              <TransactionList 
                onEdit={handleEditTransaction} 
                defaultFilters={{ type: 'TRANSFER' }}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="recurring">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calendar className="h-5 w-5 text-purple-600" />
                Recurring Transactions
              </CardTitle>
            </CardHeader>
            <CardContent>
              <TransactionList 
                onEdit={handleEditTransaction} 
                defaultFilters={{ isRecurring: true }}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="analytics">
          <div className="grid gap-6 md:grid-cols-2">
            {/* Category Breakdown */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <PieChart className="h-5 w-5" />
                  Category Breakdown
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {summary?.categoryBreakdown?.map((category, index) => (
                    <div key={index} className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                        <span className="text-sm font-medium">{category.category}</span>
                      </div>
                      <span className="text-sm font-semibold">{formatCurrency(category.amount)}</span>
                    </div>
                  ))}
                  {(!summary?.categoryBreakdown || summary.categoryBreakdown.length === 0) && (
                    <p className="text-muted-foreground text-center py-4">No data available</p>
                  )}
                </div>
              </CardContent>
            </Card>

            {/* Account Balances */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Banknote className="h-5 w-5" />
                  Account Balances
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {summary?.accountBalances?.map((account, index) => (
                    <div key={index} className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <PiggyBank className="h-4 w-4 text-muted-foreground" />
                        <span className="text-sm font-medium">{account.account}</span>
                      </div>
                      <span className={`text-sm font-semibold ${
                        account.balance >= 0 ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {formatCurrency(account.balance)}
                      </span>
                    </div>
                  ))}
                  {(!summary?.accountBalances || summary.accountBalances.length === 0) && (
                    <p className="text-muted-foreground text-center py-4">No data available</p>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Monthly Summary */}
          <Card className="mt-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="h-5 w-5" />
                Monthly Summary
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 md:grid-cols-3">
                <div className="text-center p-4 bg-green-50 rounded-lg">
                  <p className="text-sm text-muted-foreground">Total Income</p>
                  <p className="text-2xl font-bold text-green-600">
                    {formatCurrency(summary?.totalIncome || 0)}
                  </p>
                </div>
                <div className="text-center p-4 bg-red-50 rounded-lg">
                  <p className="text-sm text-muted-foreground">Total Expenses</p>
                  <p className="text-2xl font-bold text-red-600">
                    {formatCurrency(summary?.totalExpenses || 0)}
                  </p>
                </div>
                <div className="text-center p-4 bg-blue-50 rounded-lg">
                  <p className="text-sm text-muted-foreground">Net Amount</p>
                  <p className="text-2xl font-bold text-blue-600">
                    {formatCurrency(summary?.netAmount || 0)}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Transaction Form Modal */}
      {showForm && (
        <TransactionForm
          transaction={editingTransaction}
          onClose={handleCloseForm}
          onSave={handleSaveTransaction}
        />
      )}
    </div>
  );
};

export default TransactionsPage;
