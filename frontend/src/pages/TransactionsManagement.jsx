import React, { useState } from 'react';
import { Button } from '@/components/ui/button';
import TransactionsDashboard from '@/components/dashboard/TransactionsDashboard';
import TransactionsList from '@/components/transactions/TransactionsList';
import AddTransactionForm from '@/components/transactions/AddTransactionForm';
import ReceiptUploader from '@/components/expenses/ReceiptUploader';
import { 
  LayoutDashboard, 
  List, 
  Plus, 
  ShoppingCart, 
  TrendingUp,
  Menu
} from 'lucide-react';

const TransactionsManagement = () => {
  const [activeView, setActiveView] = useState('dashboard');
  const [showAddForm, setShowAddForm] = useState(false);
  const [showReceiptUploader, setShowReceiptUploader] = useState(false);

  const handleExpenseCreated = async (expenseData) => {
    try {
      // Create transaction from receipt data
      const response = await api.post('/transactions/expense', expenseData);
      console.log('Expense created from receipt:', response.data);
      setShowReceiptUploader(false);
      
      // Show success message (you can integrate with toast)
      alert('Expense created successfully from receipt!');
      
      // Refresh the current view
      if (activeView === 'list') {
        // Trigger refresh of transactions list
        window.dispatchEvent(new CustomEvent('refreshTransactions'));
      }
    } catch (error) {
      console.error('Error creating expense from receipt:', error);
      alert('Failed to create expense from receipt');
    }
  };

  const handleTransactionSaved = (transaction) => {
    setShowAddForm(false);
    
    // Refresh the current view
    if (activeView === 'list') {
      window.dispatchEvent(new CustomEvent('refreshTransactions'));
    }
  };

  const renderActiveView = () => {
    switch (activeView) {
      case 'dashboard':
        return <TransactionsDashboard />;
      case 'list':
        return <TransactionsList />;
      default:
        return <TransactionsDashboard />;
    }
  };

  const navigationItems = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: <LayoutDashboard className="h-4 w-4" />,
      description: 'Overview and statistics'
    },
    {
      id: 'list',
      label: 'Transactions',
      icon: <List className="h-4 w-4" />,
      description: 'View and manage all transactions'
    }
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-8">
              <h1 className="text-xl font-bold text-gray-900">Smart Expense Tracker</h1>
              
              {/* Navigation */}
              <nav className="hidden md:flex space-x-8">
                {navigationItems.map((item) => (
                  <button
                    key={item.id}
                    onClick={() => setActiveView(item.id)}
                    className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                      activeView === item.id
                        ? 'bg-blue-100 text-blue-700'
                        : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                    }`}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </button>
                ))}
              </nav>
            </div>

            {/* Action Buttons */}
            <div className="flex items-center gap-3">
              <Button
                onClick={() => setShowReceiptUploader(true)}
                variant="outline"
                className="flex items-center gap-2"
              >
                <ShoppingCart className="h-4 w-4" />
                Scan Receipt
              </Button>
              
              <Button
                onClick={() => setShowAddForm(true)}
                className="flex items-center gap-2"
              >
                <Plus className="h-4 w-4" />
                Add Transaction
              </Button>

              {/* Mobile Menu */}
              <div className="md:hidden">
                <Button variant="ghost" size="sm">
                  <Menu className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Mobile Navigation */}
      <div className="md:hidden border-b bg-white">
        <div className="px-4 py-3 space-x-4 flex overflow-x-auto">
          {navigationItems.map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveView(item.id)}
              className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium whitespace-nowrap transition-colors ${
                activeView === item.id
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {renderActiveView()}
      </main>

      {/* Add Transaction Modal */}
      {showAddForm && (
        <AddTransactionForm
          onClose={() => setShowAddForm(false)}
          onSave={handleTransactionSaved}
        />
      )}

      {/* Receipt Upload Modal */}
      {showReceiptUploader && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="relative w-full max-w-4xl">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowReceiptUploader(false)}
              className="absolute -top-2 -right-2 z-10 bg-white rounded-full p-2 shadow-lg"
            >
              ×
            </Button>
            <ReceiptUploader onExpenseCreated={handleExpenseCreated} />
          </div>
        </div>
      )}
    </div>
  );
};

export default TransactionsManagement;
