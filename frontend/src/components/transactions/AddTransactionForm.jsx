import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Badge } from '@/components/ui/badge';
import { 
  TrendingUp,
  TrendingDown,
  ArrowRightLeft,
  Calendar,
  CreditCard,
  Banknote,
  Mic,
  X
} from 'lucide-react';
import { formatCurrency } from '@/utils/formatters';

const AddTransactionForm = ({ transaction = null, onClose, onSave }) => {
  const [formData, setFormData] = useState({
    type: transaction?.type || 'EXPENSE',
    amount: transaction?.amount || '',
    description: transaction?.description || '',
    category: transaction?.category || '',
    merchant: transaction?.merchant || '',
    date: transaction?.date || new Date().toISOString().split('T')[0],
    currency: transaction?.currency || 'INR',
    notes: transaction?.notes || '',
    paymentType: transaction?.paymentType || '',
    account: transaction?.account || ''
  });

  const [loading, setLoading] = useState(false);
  const [showVoiceInput, setShowVoiceInput] = useState(false);

  const categories = [
    'Food & Dining', 'Transportation', 'Shopping', 'Entertainment',
    'Bills & Utilities', 'Healthcare', 'Education', 'Travel',
    'Investments', 'Savings', 'Personal Care', 'Other'
  ];

  const currencies = [
    { value: 'INR', label: '₹ Indian Rupee', symbol: '₹' },
    { value: 'USD', label: '$ US Dollar', symbol: '$' },
    { value: 'EUR', label: '€ Euro', symbol: '€' },
    { value: 'GBP', label: '£ British Pound', symbol: '£' }
  ];

  const accounts = [
    'Cash', 'Bank Account', 'Credit Card', 'Debit Card',
    'Digital Wallet', 'Investment Account', 'Savings Account'
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

  const getCurrentCurrency = () => {
    return currencies.find(c => c.value === formData.currency) || currencies[0];
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const payload = {
        ...formData,
        amount: parseFloat(formData.amount),
        type: formData.type,
        source: 'manual'
      };

      let response;
      if (transaction) {
        response = await api.put(`/transactions/${transaction.id}`, payload);
      } else {
        if (formData.type === 'INCOME') {
          response = await api.post('/transactions/income', payload);
        } else if (formData.type === 'EXPENSE') {
          response = await api.post('/transactions/expense', payload);
        } else {
          response = await api.post('/transactions/transfer', payload);
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

  const handleVoiceInput = (voiceData) => {
    setFormData(prev => ({
      ...prev,
      amount: voiceData.amount || prev.amount,
      description: voiceData.description || prev.description,
      category: voiceData.category || prev.category,
      merchant: voiceData.merchant || prev.merchant
    }));
    setShowVoiceInput(false);
  };

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
        <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                {getTypeIcon(formData.type)}
                {transaction ? 'Edit Transaction' : 'Add Transaction'}
              </CardTitle>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setShowVoiceInput(true)}
                  className="flex items-center gap-2"
                >
                  <Mic className="h-4 w-4" />
                  Voice Input
                </Button>
                <Button variant="ghost" size="sm" onClick={onClose}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
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

              {/* Amount and Currency */}
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="amount">Amount *</Label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500 text-lg">
                      {getCurrentCurrency().symbol}
                    </span>
                    <Input
                      id="amount"
                      type="number"
                      step="0.01"
                      min="0.01"
                      placeholder="Enter amount"
                      value={formData.amount}
                      onChange={(e) => handleInputChange('amount', e.target.value)}
                      className="pl-8 text-lg font-medium"
                      required
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="currency">Currency</Label>
                  <Select value={formData.currency} onValueChange={(value) => handleInputChange('currency', value)}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {currencies.map(currency => (
                        <SelectItem key={currency.value} value={currency.value}>
                          {currency.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* Description and Merchant */}
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="description">Description *</Label>
                  <Input
                    id="description"
                    placeholder="Enter description"
                    value={formData.description}
                    onChange={(e) => handleInputChange('description', e.target.value)}
                    required
                  />
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

              {/* Category and Date */}
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
                      <SelectItem value="Cash">Cash</SelectItem>
                      <SelectItem value="Credit Card">Credit Card</SelectItem>
                      <SelectItem value="Debit Card">Debit Card</SelectItem>
                      <SelectItem value="Bank Transfer">Bank Transfer</SelectItem>
                      <SelectItem value="UPI">UPI</SelectItem>
                      <SelectItem value="Digital Wallet">Digital Wallet</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* Notes */}
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
                        {getCurrentCurrency().symbol}{parseFloat(formData.amount || 0).toFixed(2)}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {new Date(formData.date).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                  {formData.category && (
                    <div className="mt-2">
                      <Badge variant="secondary">{formData.category}</Badge>
                    </div>
                  )}
                </div>
              )}

              {/* Actions */}
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={onClose}>
                  Cancel
                </Button>
                <Button type="submit" disabled={loading}>
                  {loading ? 'Saving...' : (transaction ? 'Update' : 'Save')} Transaction
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>

      {/* Voice Input Modal */}
      {showVoiceInput && (
        <VoiceInputModal
          onClose={() => setShowVoiceInput(false)}
          onTranscript={handleVoiceInput}
        />
      )}
    </>
  );
};

// Voice Input Modal Component
const VoiceInputModal = ({ onClose, onTranscript }) => {
  const [isRecording, setIsRecording] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [recognition, setRecognition] = useState(null);

  useEffect(() => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      const recognitionInstance = new SpeechRecognition();
      
      recognitionInstance.continuous = false;
      recognitionInstance.interimResults = false;
      recognitionInstance.lang = 'en-US';

      recognitionInstance.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setTranscript(transcript);
      };

      recognitionInstance.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        setIsRecording(false);
      };

      recognitionInstance.onend = () => {
        setIsRecording(false);
      };

      setRecognition(recognitionInstance);
    }
  }, []);

  const startRecording = () => {
    if (recognition) {
      recognition.start();
      setIsRecording(true);
      setTranscript('');
    }
  };

  const stopRecording = () => {
    if (recognition) {
      recognition.stop();
      setIsRecording(false);
    }
  };

  const handleConfirm = () => {
    // Parse the transcript to extract transaction details
    const parsedData = parseVoiceTranscript(transcript);
    onTranscript(parsedData);
    onClose();
  };

  const parseVoiceTranscript = (text) => {
    // Simple parsing logic - can be enhanced with AI
    const amountRegex = /(?:₹|\$|rupees?)\s*(\d+(?:\.\d{2})?)/i;
    const amountMatch = text.match(amountRegex);
    
    return {
      amount: amountMatch ? amountMatch[1] : '',
      description: text,
      category: 'Other', // Default category
      merchant: extractMerchant(text)
    };
  };

  const extractMerchant = (text) => {
    const merchants = ['starbucks', 'amazon', 'walmart', 'target', 'costco'];
    const found = merchants.find(merchant => text.toLowerCase().includes(merchant));
    return found ? found.charAt(0).toUpperCase() + found.slice(1) : '';
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Mic className="h-5 w-5" />
              Voice Input
            </div>
            <Button variant="ghost" size="sm" onClick={onClose}>
              <X className="h-4 w-4" />
            </Button>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="text-center space-y-4">
            <p className="text-sm text-gray-600">
              Speak your transaction like: "I spent ₹500 on groceries today"
            </p>
            
            <div className="flex justify-center">
              <Button
                type="button"
                size="lg"
                onClick={isRecording ? stopRecording : startRecording}
                className={`relative ${isRecording ? 'bg-red-500 hover:bg-red-600' : ''}`}
              >
                <Mic className={`h-6 w-6 ${isRecording ? 'animate-pulse' : ''}`} />
                {isRecording && (
                  <span className="absolute -top-1 -right-1 flex h-3 w-3">
                    <span className="animate-ping absolute inline-flex h-3 w-3 rounded-full bg-red-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
                  </span>
                )}
              </Button>
            </div>

            {isRecording && (
              <div className="space-y-2">
                <div className="flex items-center justify-center gap-2">
                  <div className="animate-pulse flex space-x-1">
                    <div className="w-1 h-4 bg-red-500 animate-pulse"></div>
                    <div className="w-1 h-6 bg-red-500 animate-pulse delay-75"></div>
                    <div className="w-1 h-4 bg-red-500 animate-pulse delay-150"></div>
                    <div className="w-1 h-8 bg-red-500 animate-pulse delay-300"></div>
                    <div className="w-1 h-6 bg-red-500 animate-pulse delay-525"></div>
                  </div>
                  <span className="text-sm text-red-600">Listening...</span>
                </div>
              </div>
            )}

            {transcript && (
              <div className="space-y-4">
                <div className="p-3 bg-gray-50 rounded-lg">
                  <p className="text-sm">{transcript}</p>
                </div>
                <Button onClick={handleConfirm} className="w-full">
                  Use This Transaction
                </Button>
              </div>
            )}

            {!isRecording && !transcript && (
              <Button onClick={startRecording} variant="outline" className="w-full">
                Start Recording
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AddTransactionForm;
