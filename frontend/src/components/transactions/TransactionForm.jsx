import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Badge } from '@/components/ui/badge';
import { 
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
  Coffee,
  X
} from 'lucide-react';
import { formatCurrency } from '@/utils/formatters';
import { api } from '@/api/api';

const TransactionForm = ({ transaction = null, onClose, onSave }) => {
  const [formData, setFormData] = useState({
    amount: transaction?.amount || '',
    type: transaction?.type || 'EXPENSE',
    description: transaction?.description || '',
    category: transaction?.category || '',
    merchant: transaction?.merchant || '',
    paymentType: transaction?.paymentType || '',
    account: transaction?.account || '',
    date: transaction?.expenseDate || new Date().toISOString().split('T')[0],
    source: transaction?.source || 'manual',
    notes: transaction?.notes || '',
    tags: transaction?.tags || '',
    isRecurring: transaction?.isRecurring || false,
    recurrencePattern: transaction?.recurrencePattern || 'monthly',
    status: transaction?.status || 'COMPLETED',
    fromAccount: transaction?.fromAccount || '',
    toAccount: transaction?.toAccount || ''
  });

  const [loading, setLoading] = useState(false);

  const categories = [
    'Food & Dining', 'Transportation', 'Shopping', 'Entertainment',
    'Bills & Utilities', 'Healthcare', 'Education', 'Travel',
    'Investments', 'Savings', 'Salary', 'Freelance', 'Business',
    'Rental Income', 'Other'
  ];

  const accounts = [
    'Cash', 'Bank Account', 'Credit Card', 'Debit Card',
    'Digital Wallet', 'Investment Account', 'Savings Account',
    'Business Account', 'Emergency Fund'
  ];

  const paymentTypes = [
    'Cash', 'Credit Card', 'Debit Card', 'Bank Transfer',
    'UPI', 'Digital Wallet', 'Check', 'Other'
  ];

  const recurrencePatterns = [
    { value: 'daily', label: 'Daily' },
    { value: 'weekly', label: 'Weekly' },
    { value: 'monthly', label: 'Monthly' },
    { value: 'yearly', label: 'Yearly' }
  ];

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
        return 'border-green-200 bg-green-50';
      case 'EXPENSE':
        return 'border-red-200 bg-red-50';
      case 'TRANSFER':
        return 'border-blue-200 bg-blue-50';
      default:
        return 'border-gray-200 bg-gray-50';
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const payload = {
        amount: parseFloat(formData.amount),
        type: formData.type,
        description: formData.description,
        category: formData.category,
        merchant: formData.merchant,
        paymentType: formData.paymentType,
        account: formData.account,
        date: formData.date,
        source: formData.source,
        notes: formData.notes,
        tags: formData.tags,
        isRecurring: formData.isRecurring,
        recurrencePattern: formData.recurrencePattern,
        status: formData.status,
        fromAccount: formData.fromAccount,
        toAccount: formData.toAccount
      };

      let response;
      if (transaction) {
        response = await api.put(`/transactions/${transaction.id}`, payload);
      } else {
        if (formData.type === 'INCOME') {
          response = await api.post('/transactions/income', payload);
        } else if (formData.type === 'EXPENSE') {
          response = await api.post('/transactions/expense', payload);
        } else if (formData.type === 'TRANSFER') {
          response = await api.post('/transactions/transfer', payload);
        } else {
          response = await api.post('/transactions', payload);
        }
      }

      onSave(response.data);
      onClose();
    } catch (error) {
      console.error('Error saving transaction:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              {getTypeIcon(formData.type)}
              {transaction ? 'Edit Transaction' : 'Add Transaction'}
            </CardTitle>
            <Button variant="ghost" size="sm" onClick={onClose}>
              <X className="h-4 w-4" />
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Transaction Type */}
            <div className="space-y-2">
              <Label>Transaction Type</Label>
              <RadioGroup
                value={formData.type}
                onValueChange={(value) => handleInputChange('type', value)}
                className="flex gap-4"
              >
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="EXPENSE" id="expense" />
                  <Label htmlFor="expense" className="flex items-center gap-2">
                    <TrendingDown className="h-4 w-4 text-red-600" />
                    Expense
                  </Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="INCOME" id="income" />
                  <Label htmlFor="income" className="flex items-center gap-2">
                    <TrendingUp className="h-4 w-4 text-green-600" />
                    Income
                  </Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="TRANSFER" id="transfer" />
                  <Label htmlFor="transfer" className="flex items-center gap-2">
                    <ArrowRightLeft className="h-4 w-4 text-blue-600" />
                    Transfer
                  </Label>
                </div>
              </RadioGroup>
            </div>

            {/* Amount and Date */}
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="amount">Amount *</Label>
                <Input
                  id="amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  placeholder="0.00"
                  value={formData.amount}
                  onChange={(e) => handleInputChange('amount', e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="date">Date *</Label>
                <Input
                  id="date"
                  type="date"
                  value={formData.date}
                  onChange={(e) => handleInputChange('date', e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="description">Description *</Label>
              <Input
                id="description"
                placeholder="Enter transaction description"
                value={formData.description}
                onChange={(e) => handleInputChange('description', e.target.value)}
                required
              />
            </div>

            {/* Category and Merchant */}
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="category">Category</Label>
                <Select value={formData.category} onValueChange={(value) => handleInputChange('category', value)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map(category => (
                      <SelectItem key={category} value={category}>{category}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="merchant">Merchant/Payee</Label>
                <Input
                  id="merchant"
                  placeholder="Enter merchant or payee"
                  value={formData.merchant}
                  onChange={(e) => handleInputChange('merchant', e.target.value)}
                />
              </div>
            </div>

            {/* Account and Payment Type */}
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="account">Account</Label>
                <Select value={formData.account} onValueChange={(value) => handleInputChange('account', value)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select account" />
                  </SelectTrigger>
                  <SelectContent>
                    {accounts.map(account => (
                      <SelectItem key={account} value={account}>{account}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="paymentType">Payment Type</Label>
                <Select value={formData.paymentType} onValueChange={(value) => handleInputChange('paymentType', value)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select payment type" />
                  </SelectTrigger>
                  <SelectContent>
                    {paymentTypes.map(type => (
                      <SelectItem key={type} value={type}>{type}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Transfer Fields (only show for TRANSFER type) */}
            {formData.type === 'TRANSFER' && (
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="fromAccount">From Account *</Label>
                  <Select value={formData.fromAccount} onValueChange={(value) => handleInputChange('fromAccount', value)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select from account" />
                    </SelectTrigger>
                    <SelectContent>
                      {accounts.map(account => (
                        <SelectItem key={account} value={account}>{account}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="toAccount">To Account *</Label>
                  <Select value={formData.toAccount} onValueChange={(value) => handleInputChange('toAccount', value)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select to account" />
                    </SelectTrigger>
                    <SelectContent>
                      {accounts.map(account => (
                        <SelectItem key={account} value={account}>{account}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            )}

            {/* Notes and Tags */}
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="notes">Notes</Label>
                <Textarea
                  id="notes"
                  placeholder="Add any additional notes..."
                  value={formData.notes}
                  onChange={(e) => handleInputChange('notes', e.target.value)}
                  rows={3}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="tags">Tags</Label>
                <Input
                  id="tags"
                  placeholder="Enter tags separated by commas"
                  value={formData.tags}
                  onChange={(e) => handleInputChange('tags', e.target.value)}
                />
              </div>
            </div>

            {/* Recurring Options */}
            <div className="space-y-4">
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="isRecurring"
                  checked={formData.isRecurring}
                  onCheckedChange={(checked) => handleInputChange('isRecurring', checked)}
                />
                <Label htmlFor="isRecurring">Recurring Transaction</Label>
              </div>
              
              {formData.isRecurring && (
                <div className="space-y-2">
                  <Label>Recurrence Pattern</Label>
                  <Select value={formData.recurrencePattern} onValueChange={(value) => handleInputChange('recurrencePattern', value)}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select recurrence pattern" />
                    </SelectTrigger>
                    <SelectContent>
                      {recurrencePatterns.map(pattern => (
                        <SelectItem key={pattern.value} value={pattern.value}>{pattern.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
            </div>

            {/* Status */}
            <div className="space-y-2">
              <Label>Status</Label>
              <Select value={formData.status} onValueChange={(value) => handleInputChange('status', value)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="COMPLETED">Completed</SelectItem>
                  <SelectItem value="PENDING">Pending</SelectItem>
                  <SelectItem value="CANCELLED">Cancelled</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Preview */}
            {formData.amount && formData.type && (
              <div className={`p-4 rounded-lg border ${getTypeColor(formData.type)}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {getTypeIcon(formData.type)}
                    <span className="font-medium">{formData.description || 'Untitled Transaction'}</span>
                  </div>
                  <div className="text-right">
                    <p className={`font-bold text-lg ${
                      formData.type === 'INCOME' ? 'text-green-600' : 
                      formData.type === 'EXPENSE' ? 'text-red-600' : 
                      'text-blue-600'
                    }`}>
                      {formData.type === 'INCOME' ? '+' : 
                       formData.type === 'EXPENSE' ? '-' : ''}
                      {formatCurrency(parseFloat(formData.amount) || 0)}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {new Date(formData.date).toLocaleDateString()}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? 'Saving...' : (transaction ? 'Update' : 'Create')} Transaction
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default TransactionForm;
