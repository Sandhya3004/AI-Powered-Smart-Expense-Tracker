import { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import VoiceInputButton from '@/components/expense/VoiceInputButton';
import ReceiptScanner from '@/components/expense/ReceiptScanner';
import CSVImporter from '@/components/expense/CSVImporter';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { api, expenseService } from '@/api/api';
import { formatCurrency } from '@/utils/formatters';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Mic, Camera, Upload, Trash2, Receipt, X, Loader2, Plus, TrendingDown, TrendingUp, Calendar, DollarSign } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogDescription } from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

const Expenses = () => {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showOCRModal, setShowOCRModal] = useState(false);
  const [receiptFile, setReceiptFile] = useState(null);
  const [receiptDescription, setReceiptDescription] = useState('');
  const [receiptAmount, setReceiptAmount] = useState('');
  const [receiptCategory, setReceiptCategory] = useState('');
  const [ocrLoading, setOcrLoading] = useState(false);
  const [showAddExpenseModal, setShowAddExpenseModal] = useState(false);
  const [showEditExpenseModal, setShowEditExpenseModal] = useState(false);
  const [editingExpense, setEditingExpense] = useState(null);
  const [newExpense, setNewExpense] = useState({
    description: '',
    amount: '',
    category: '',
    type: 'EXPENSE',
    date: new Date().toISOString().split('T')[0]
  });
  const [editExpense, setEditExpense] = useState({
    description: '',
    amount: '',
    category: '',
    type: 'EXPENSE',
    date: new Date().toISOString().split('T')[0]
  });
  const [showVoiceDialog, setShowVoiceDialog] = useState(false);
  const [showReceiptDialog, setShowReceiptDialog] = useState(false);
  const [showCSVDialog, setShowCSVDialog] = useState(false);
  const [voiceText, setVoiceText] = useState('');
  const [voiceLoading, setVoiceLoading] = useState(false);
  const [receiptLoading, setReceiptLoading] = useState(false);
  const [csvFile, setCsvFile] = useState(null);
  const [csvLoading, setCsvLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const { user } = useAuth();
  const { toast } = useToast();

  // Fetch expenses from API
  useEffect(() => {
    fetchExpenses();
    
    // Listen for refresh events
    const handleRefresh = () => fetchExpenses();
    window.addEventListener('refreshTransactions', handleRefresh);
    
    return () => {
      window.removeEventListener('refreshTransactions', handleRefresh);
    };
  }, []);

  const fetchExpenses = async () => {
    try {
      setLoading(true);
      const response = await expenseService.getAll(0, 50);
      // Handle different response structures: { data: { content: [...] } } or { content: [...] } or [...]
      const expensesData = response?.data?.content || response?.content || response?.data || response || [];
      setExpenses(Array.isArray(expensesData) ? expensesData : []);
    } catch (error) {
      console.error('Failed to fetch expenses:', error);
      toast({
        title: 'Error',
        description: 'Failed to fetch expenses. Please try again.',
        variant: 'destructive'
      });
      setExpenses([]);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    try {
      await api.delete(`/expenses/${id}`);
      setExpenses(prev => prev.filter(expense => expense.id !== id));
      toast({
        title: "Success",
        description: "Expense deleted successfully",
      });
    } catch (error) {
      console.error('Failed to delete expense:', error);
      toast({
        title: "Error",
        description: "Failed to delete expense",
        variant: "destructive",
      });
    }
  };

  const handleVoiceExpenseCreated = (expense) => {
    // Ensure expense has a valid id from backend, or generate temporary one
    const expenseId = expense.id || expense._id || expense.uuid || 
                      (expenses.length ? Math.max(...expenses.map((e) => e.id || 0)) + 1 : 1);
    
    const newExpenseWithId = {
      ...expense,
      id: expenseId,
      expenseDate: expense.expenseDate || expense.date || new Date().toISOString().split('T')[0],
    };
    
    setExpenses(prev => [newExpenseWithId, ...prev]);

    toast({
      title: 'Voice expense added',
      description: 'Your voice expense has been saved successfully.',
    });
  };

  const handleVoiceSubmit = async () => {
    if (!voiceText.trim()) {
      toast({ title: 'Error', description: 'Please enter voice text', variant: 'destructive' });
      return;
    }
    setVoiceLoading(true);
    try {
      const response = await api.post('/expenses/voice', { text: voiceText });
      const expense = response.data?.data || response.data;
      if (expense) {
        handleVoiceExpenseCreated(expense);
        setVoiceText('');
        setShowVoiceDialog(false);
        toast({ title: 'Success', description: `Voice expense added: ${expense.description} - ${formatCurrency(expense.amount)}` });
      } else {
        throw new Error('Invalid response from server');
      }
    } catch (error) {
      console.error('Voice processing failed:', error);
      const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Failed to process voice input';
      toast({ title: 'Error', description: errorMsg, variant: 'destructive' });
    } finally {
      setVoiceLoading(false);
    }
  };

  const handleVoiceInput = () => {
    // Check browser support for Speech Recognition
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
      toast({ 
        title: 'Not Supported', 
        description: 'Speech recognition is not supported in this browser. Please use Chrome or Edge.',
        variant: 'destructive'
      });
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.lang = 'en-IN'; // Indian English
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onstart = () => {
      console.log('Voice recording started...');
      setVoiceLoading(true);
      toast({ title: 'Listening...', description: 'Speak your expense description' });
    };

    recognition.onresult = async (event) => {
      const text = event.results[0][0].transcript;
      console.log('Voice Text:', text);
      setVoiceText(text);
      
      // Automatically process the voice input
      try {
        const response = await api.post('/expenses/voice', { text });
        const expense = response.data?.data || response.data;
        
        if (expense) {
          // Ensure expense has id
          if (!expense.id && !expense._id) {
            expense.id = Date.now();
          }
          
          // Add to UI immediately
          setExpenses(prev => [expense, ...prev]);
          
          toast({ 
            title: 'Voice Expense Added', 
            description: `${expense.description} - ${formatCurrency(expense.amount)}` 
          });
          
          setVoiceText('');
          setShowVoiceDialog(false);
        }
      } catch (error) {
        console.error('Voice processing failed:', error);
        toast({ 
          title: 'Error', 
          description: 'Failed to process voice input. Please try again or enter manually.',
          variant: 'destructive'
        });
      } finally {
        setVoiceLoading(false);
      }
    };

    recognition.onerror = (event) => {
      console.error('Voice error:', event.error);
      setVoiceLoading(false);
      toast({ 
        title: 'Voice Error', 
        description: `Error: ${event.error}. Please try again.`,
        variant: 'destructive'
      });
    };

    recognition.onend = () => {
      console.log('Voice recording ended');
      if (voiceLoading) {
        setVoiceLoading(false);
      }
    };

    recognition.start();
  };

  const handleReceiptUpload = async () => {
    if (!receiptFile) {
      toast({ title: 'Error', description: 'Please select a receipt file', variant: 'destructive' });
      return;
    }
    setReceiptLoading(true);
    try {
      const formData = new FormData();
      formData.append('receipt', receiptFile);
      const response = await api.post('/expenses/upload-receipt', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      const expense = response.data?.data || response.data;
      if (expense) {
        handleVoiceExpenseCreated(expense);
        setReceiptFile(null);
        setShowReceiptDialog(false);
        toast({ title: 'Success', description: `Receipt processed: ${expense.description} - ${formatCurrency(expense.amount)}` });
      } else {
        throw new Error('Invalid response from server');
      }
    } catch (error) {
      console.error('Receipt upload failed:', error);
      const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Failed to process receipt';
      toast({ title: 'Error', description: errorMsg, variant: 'destructive' });
    } finally {
      setReceiptLoading(false);
    }
  };

  const handleCSVUpload = async () => {
    if (!csvFile) {
      toast({ title: 'Error', description: 'Please select a CSV file', variant: 'destructive' });
      return;
    }
    setCsvLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', csvFile);
      const response = await api.post('/expenses/import-csv', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      const result = response.data?.data || response.data;
      const successCount = result?.successful || result?.imported || 0;
      const failCount = result?.failed || 0;
      
      // Refresh expense list
      await fetchExpenses();
      
      setCsvFile(null);
      setShowCSVDialog(false);
      
      toast({ 
        title: 'CSV Import Complete', 
        description: `Successfully imported ${successCount} expenses${failCount > 0 ? `, ${failCount} failed` : ''}` 
      });
    } catch (error) {
      console.error('CSV import failed:', error);
      const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Failed to import CSV';
      toast({ title: 'Error', description: errorMsg, variant: 'destructive' });
    } finally {
      setCsvLoading(false);
    }
  };

  const handleAddExpense = async () => {
    if (!newExpense.description || !newExpense.amount || !newExpense.category) {
      toast({
        title: 'Validation Error',
        description: 'Please fill in all required fields',
        variant: 'destructive',
      });
      return;
    }

    try {
      const response = await expenseService.create({
        description: newExpense.description,
        amount: parseFloat(newExpense.amount),
        category: newExpense.category,
        expenseDate: newExpense.date,
        type: newExpense.type || 'EXPENSE',
        source: 'manual',
        currency: 'INR',
        paymentType: 'Cash',
        account: 'Cash',
        status: 'COMPLETED'
      });

      // Option 1 (BEST): Add expense from response directly to state
      const createdExpense = response?.data || response;
      
      // Ensure expense has a valid id for React key
      if (!createdExpense.id && !createdExpense._id) {
        console.warn('Created expense missing id:', createdExpense);
        createdExpense.id = Date.now(); // Temporary id for UI
      }
      
      // Add to UI immediately (real-time update)
      setExpenses(prev => [createdExpense, ...prev]);
      
      // Reset form
      setNewExpense({
        description: '',
        amount: '',
        category: '',
        type: 'EXPENSE',
        date: new Date().toISOString().split('T')[0]
      });
      setShowAddExpenseModal(false);

      toast({
        title: 'Success',
        description: 'Expense added successfully',
      });
    } catch (error) {
      console.error('Failed to add expense:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to add expense',
        variant: 'destructive',
      });
    }
  };

  const handleEditClick = (expense) => {
    setEditingExpense(expense);
    setEditExpense({
      description: expense.description || '',
      amount: expense.amount?.toString() || '',
      category: expense.category || '',
      type: expense.type || 'EXPENSE',
      date: expense.expenseDate || expense.date || new Date().toISOString().split('T')[0]
    });
    setShowEditExpenseModal(true);
  };

  const handleUpdateExpense = async () => {
    if (!editExpense.description || !editExpense.amount || !editExpense.category) {
      toast({
        title: 'Validation Error',
        description: 'Please fill in all required fields',
        variant: 'destructive',
      });
      return;
    }

    try {
      const response = await expenseService.update(editingExpense.id, {
        description: editExpense.description,
        amount: parseFloat(editExpense.amount),
        category: editExpense.category,
        expenseDate: editExpense.date,
        type: editExpense.type || 'EXPENSE',
        source: editingExpense.source || 'manual',
        currency: 'INR',
        paymentType: 'Cash',
        account: 'Cash',
        status: 'COMPLETED'
      });

      const updatedExpense = response?.data?.data || response?.data || response;
      
      // Update the expense in the list with new data taking precedence
      setExpenses(prev => prev.map(e => {
        if (e.id === editingExpense.id || e._id === editingExpense.id) {
          return { ...e, ...updatedExpense, id: editingExpense.id };
        }
        return e;
      }));
      
      setShowEditExpenseModal(false);
      setEditingExpense(null);

      toast({
        title: 'Success',
        description: 'Expense updated successfully',
      });
    } catch (error) {
      console.error('Failed to update expense:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to update expense',
        variant: 'destructive',
      });
    }
  };

  const handleReceiptFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      setReceiptFile(file);
    }
  };

  const handleProcessWithOCR = async () => {
    if (!receiptFile) {
      toast({
        title: 'No receipt file',
        description: 'Please upload a receipt image first.',
        variant: 'destructive',
      });
      return;
    }

    setOcrLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', receiptFile);

      const response = await api.post('/receipts/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      const receipt = response.data?.receipt || response.data;
      const scannedDescription = receipt?.merchant || 'Receipt Expense';
      const scannedAmount = receipt?.amount || 0;
      const scannedCategory = receipt?.category || 'Other';

      setReceiptDescription(scannedDescription);
      setReceiptAmount(scannedAmount);
      setReceiptCategory(scannedCategory);

      // Add it automatically to the expense list
      const newId = expenses.length ? Math.max(...expenses.map((e) => e.id || 0)) + 1 : 1;
      const newExpenseObj = {
        id: newId,
        description: scannedDescription,
        amount: Number(scannedAmount),
        category: scannedCategory,
        expenseDate: receipt?.date || new Date().toISOString().split('T')[0],
        type: 'EXPENSE',
        source: 'receipt',
      };

      setExpenses(prev => [newExpenseObj, ...prev]);

      toast({
        title: 'Receipt processed',
        description: `Added ${formatCurrency(newExpenseObj.amount)} to your expenses`,
      });
      setShowOCRModal(false);
      setReceiptFile(null);
      setReceiptDescription('');
      setReceiptAmount('');
      setReceiptCategory('');

    } catch (error) {
      console.error('OCR processing failed:', error);
      toast({
        title: 'Error',
        description: 'Failed to process receipt OCR. Make sure ai-service is running.',
        variant: 'destructive',
      });
    } finally {
      setOcrLoading(false);
    }
  };

  const handleCSVFileChange = (event) => {
    const file = event.target.files[0];
    if (file && file.type === 'text/csv') {
      setCsvFile(file);
    }
  };

  const handleImportCSV = async () => {
    if (!csvFile) {
      toast({
        title: 'No CSV file',
        description: 'Please upload a CSV file first.',
        variant: 'destructive',
      });
      return;
    }

    setCsvLoading(true);
    try {
      const text = await csvFile.text();
      const lines = text.split('\n').filter(line => line.trim());
      const headers = lines[0].split(',').map(h => h.trim());
      
      const expenses = [];
      for (let i = 1; i < lines.length; i++) {
        const values = lines[i].split(',');
        if (values.length >= 4) {
          expenses.push({
            description: values[0]?.trim() || '',
            amount: parseFloat(values[1]) || 0,
            category: values[2]?.trim() || 'Other',
            date: values[3]?.trim() || new Date().toISOString().split('T')[0]
          });
        }
      }

      // Batch create expenses
      const results = await Promise.allSettled(
        expenses.map(expense => 
          expenseService.create({
            description: expense.description,
            amount: expense.amount,
            category: expense.category,
            expenseDate: expense.date,
            type: 'EXPENSE',
            source: 'csv',
            currency: 'INR',
            paymentType: 'Cash',
            account: 'Cash',
            status: 'COMPLETED'
          })
        )
      );

      const successCount = results.filter(r => r.status === 'fulfilled').length;
      const failureCount = results.filter(r => r.status === 'rejected').length;

      // Refresh expenses list
      fetchExpenses();

      toast({
        title: 'CSV Import Complete',
        description: `Successfully imported ${successCount} expenses. ${failureCount > 0 ? `${failureCount} failed.` : ''}`,
        variant: failureCount > 0 ? 'destructive' : 'default',
      });

    } catch (error) {
      console.error('CSV import failed:', error);
      toast({
        title: 'Import Error',
        description: 'Failed to import CSV file. Please check the format.',
        variant: 'destructive',
      });
    } finally {
      setCsvLoading(false);
      setCsvFile(null);
      setShowCSVDialog(false);
    }
  };


  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Expenses</h1>
          <p className="text-gray-400 mt-1">Track and manage your expenses</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          {/* Voice Input Button */}
          <Dialog open={showVoiceDialog} onOpenChange={setShowVoiceDialog}>
            <DialogTrigger asChild>
              <Button 
                variant="outline" 
                className="bg-gradient-to-r from-indigo-500/20 to-purple-500/20 border-indigo-500/50 text-indigo-300 hover:bg-indigo-500/30 hover:border-indigo-400 hover:scale-105 transition-all duration-300 rounded-xl shadow-lg shadow-indigo-500/10 hover:shadow-indigo-500/20 px-5 py-3 text-base font-medium"
              >
                <Mic className="w-5 h-5 mr-2 text-indigo-400" />
                Voice
              </Button>
            </DialogTrigger>
            <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
              <DialogHeader>
                <DialogTitle>Voice Input</DialogTitle>
                <DialogDescription className="text-gray-400">
                  Describe your expense verbally and we'll process it for you.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 mt-4">
                <textarea
                  value={voiceText}
                  onChange={(e) => setVoiceText(e.target.value)}
                  placeholder="Describe your expense (e.g., 'spent 50 rupees on lunch')"
                  className="w-full h-32 bg-[#0F0F12]/60 border-[#4A4560] text-white rounded-lg p-3 resize-none"
                />
                <Button 
                  onClick={handleVoiceInput} 
                  disabled={voiceLoading}
                  className="w-full bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white"
                >
                  {voiceLoading ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Listening...</>
                  ) : (
                    <><Mic className="w-4 h-4 mr-2" /> 🎤 Process Voice</>
                  )}
                </Button>
              </div>
            </DialogContent>
          </Dialog>

          {/* Receipt Scanner Button */}
          <Dialog open={showReceiptDialog} onOpenChange={setShowReceiptDialog}>
            <DialogTrigger asChild>
              <Button 
                variant="outline" 
                className="bg-gradient-to-r from-amber-500/20 to-orange-500/20 border-amber-500/50 text-amber-300 hover:bg-amber-500/30 hover:border-amber-400 hover:scale-105 transition-all duration-300 rounded-xl shadow-lg shadow-amber-500/10 hover:shadow-amber-500/20 px-5 py-3 text-base font-medium"
              >
                <Camera className="w-5 h-5 mr-2 text-amber-400" />
                Scan
              </Button>
            </DialogTrigger>
            <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
              <DialogHeader>
                <DialogTitle>Upload Receipt</DialogTitle>
                <DialogDescription className="text-gray-400">
                  Upload a receipt image and we'll extract the expense details automatically.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 mt-4">
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => setReceiptFile(e.target.files[0])}
                  className="w-full p-3 bg-[#0F0F12]/60 border-[#4A4560] rounded-lg text-white file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-[#7B6FC9] file:text-white hover:file:bg-[#6B5FB9]"
                />
                <Button 
                  onClick={handleReceiptUpload} 
                  disabled={receiptLoading || !receiptFile}
                  className="w-full bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white"
                >
                  {receiptLoading ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Processing...</>
                  ) : (
                    <><Camera className="w-4 h-4 mr-2" /> Process Receipt</>
                  )}
                </Button>
              </div>
            </DialogContent>
          </Dialog>

          {/* CSV Upload Button */}
          <Dialog open={showCSVDialog} onOpenChange={setShowCSVDialog}>
            <DialogTrigger asChild>
              <Button 
                variant="outline" 
                className="bg-gradient-to-r from-emerald-500/20 to-teal-500/20 border-emerald-500/50 text-emerald-300 hover:bg-emerald-500/30 hover:border-emerald-400 hover:scale-105 transition-all duration-300 rounded-xl shadow-lg shadow-emerald-500/10 hover:shadow-emerald-500/20 px-5 py-3 text-base font-medium"
              >
                <Upload className="w-5 h-5 mr-2 text-emerald-400" />
                CSV
              </Button>
            </DialogTrigger>
            <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
              <DialogHeader>
                <DialogTitle>Import CSV</DialogTitle>
                <DialogDescription className="text-gray-400">
                  Upload a CSV file with your expenses to bulk import them.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 mt-4">
                <input
                  type="file"
                  accept=".csv"
                  onChange={(e) => setCsvFile(e.target.files[0])}
                  className="w-full p-3 bg-[#0F0F12]/60 border-[#4A4560] rounded-lg text-white file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-[#7B6FC9] file:text-white hover:file:bg-[#6B5FB9]"
                />
                <Button 
                  onClick={handleCSVUpload} 
                  disabled={csvLoading || !csvFile}
                  className="w-full bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white"
                >
                  {csvLoading ? (
                    <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Importing...</>
                  ) : (
                    <><Upload className="w-4 h-4 mr-2" /> Import CSV</>
                  )}
                </Button>
              </div>
            </DialogContent>
          </Dialog>

          {/* Add Expense Button */}
          <Button 
            onClick={() => setShowAddExpenseModal(true)}
            className="bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#8B7FD9] hover:to-[#ACA0F8] text-white transition-all duration-300 rounded-xl shadow-lg shadow-purple-500/20 hover:shadow-purple-500/40 hover:scale-105 px-6 py-3 text-base font-semibold"
          >
            <Plus className="w-5 h-5 mr-2" />
            Add Expense
          </Button>
        </div>
      </div>

      {/* Summary Cards - Improved Design */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card className="bg-gradient-to-br from-red-500/20 to-red-600/10 border-red-500/30 hover:shadow-lg hover:shadow-red-500/20 transition-all duration-300 rounded-2xl">
          <CardHeader className="pb-3">
            <CardTitle className="text-red-300 text-sm font-medium flex items-center gap-2">
              <TrendingDown className="w-4 h-4" />
              Total Expenses
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {formatCurrency(expenses.filter(e => e.type === 'EXPENSE' || !e.type).reduce((sum, exp) => sum + (exp.amount || 0), 0))}
            </p>
            <p className="text-sm text-red-300/70 mt-1">{expenses.filter(e => e.type === 'EXPENSE' || !e.type).length} transactions</p>
          </CardContent>
        </Card>
        
        <Card className="bg-gradient-to-br from-green-500/20 to-green-600/10 border-green-500/30 hover:shadow-lg hover:shadow-green-500/20 transition-all duration-300 rounded-2xl">
          <CardHeader className="pb-3">
            <CardTitle className="text-green-300 text-sm font-medium flex items-center gap-2">
              <TrendingUp className="w-4 h-4" />
              Total Income
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {formatCurrency(expenses.filter(e => e.type === 'INCOME').reduce((sum, exp) => sum + (exp.amount || 0), 0))}
            </p>
            <p className="text-sm text-green-300/70 mt-1">{expenses.filter(e => e.type === 'INCOME').length} transactions</p>
          </CardContent>
        </Card>
        
        <Card className="bg-gradient-to-br from-blue-500/20 to-blue-600/10 border-blue-500/30 hover:shadow-lg hover:shadow-blue-500/20 transition-all duration-300 rounded-2xl">
          <CardHeader className="pb-3">
            <CardTitle className="text-blue-300 text-sm font-medium flex items-center gap-2">
              <DollarSign className="w-4 h-4" />
              Balance
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {formatCurrency(
                expenses.filter(e => e.type === 'INCOME').reduce((sum, exp) => sum + (exp.amount || 0), 0) -
                expenses.filter(e => e.type === 'EXPENSE' || !e.type).reduce((sum, exp) => sum + (exp.amount || 0), 0)
              )}
            </p>
            <p className="text-sm text-blue-300/70 mt-1">Net balance</p>
          </CardContent>
        </Card>
        
        <Card className="bg-gradient-to-br from-purple-500/20 to-purple-600/10 border-purple-500/30 hover:shadow-lg hover:shadow-purple-500/20 transition-all duration-300 rounded-2xl">
          <CardHeader className="pb-3">
            <CardTitle className="text-purple-300 text-sm font-medium flex items-center gap-2">
              <Calendar className="w-4 h-4" />
              This Month
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {formatCurrency(expenses.filter(exp => {
                const expDate = new Date(exp.expenseDate || exp.date);
                const now = new Date();
                return expDate.getMonth() === now.getMonth() && expDate.getFullYear() === now.getFullYear();
              }).reduce((sum, exp) => sum + (exp.amount || 0), 0))}
            </p>
            <p className="text-sm text-purple-300/70 mt-1">
              {expenses.filter(exp => {
                const expDate = new Date(exp.expenseDate || exp.date);
                const now = new Date();
                return expDate.getMonth() === now.getMonth() && expDate.getFullYear() === now.getFullYear();
              }).length} transactions
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Expenses List - Improved with Income/Expense styling */}
      {(!expenses || expenses.length === 0) ? (
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560] rounded-2xl shadow-xl">
          <CardContent className="py-12 text-center">
            <Receipt className="w-12 h-12 mx-auto mb-4 text-gray-400 opacity-50" />
            <h3 className="text-lg font-medium text-white mb-2">No expenses yet</h3>
            <p className="text-gray-400 mb-4">Start tracking your expenses by adding your first one</p>
            <Button 
              onClick={() => setShowAddExpenseModal(true)}
              className="bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white rounded-xl"
            >
              <Plus className="w-4 h-4 mr-2" />
              Add First Expense
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4">
          {expenses.map((expense, index) => {
            const isIncome = expense.type === 'INCOME';
            // Use multiple fallback strategies for unique key
            const uniqueKey = expense.id || expense._id || expense.uuid || `expense-${index}-${Date.now()}`;
            return (
              <Card 
                key={uniqueKey} 
                className={`bg-gradient-to-br ${isIncome ? 'from-green-500/10 to-emerald-600/5 border-green-500/20' : 'from-[#2A2540] to-[#322B55] border-[#3A3560]'} hover:border-[#7B6FC9]/50 transition-all duration-300 rounded-2xl shadow-lg hover:shadow-xl`}
              >
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${isIncome ? 'bg-green-500/20' : 'bg-[#7B6FC9]/20'}`}>
                        <Receipt className={`w-6 h-6 ${isIncome ? 'text-green-400' : 'text-[#9C90E8]'}`} />
                      </div>
                      <div>
                        <h3 className="font-semibold text-white">{expense.description}</h3>
                        <div className="flex items-center gap-2 text-sm text-gray-400">
                          <span>{expense.category}</span>
                          <span>•</span>
                          <span>{new Date(expense.expenseDate || expense.date).toLocaleDateString()}</span>
                          {expense.type && (
                            <>
                              <span>•</span>
                              <span className={`px-2 py-0.5 rounded-full text-xs ${isIncome ? 'bg-green-500/20 text-green-300' : 'bg-red-500/20 text-red-300'}`}>
                                {expense.type}
                              </span>
                            </>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-4">
                      <div className="text-right">
                        <p className={`text-xl font-bold ${isIncome ? 'text-green-400' : 'text-red-400'}`}>
                          {isIncome ? '+' : '-'}{formatCurrency(expense.amount)}
                        </p>
                        <Badge variant="outline" className="border-[#3A3560] text-gray-400 text-xs">
                          {expense.source || 'manual'}
                        </Badge>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleEditClick(expense)}
                        className="icon-btn icon-btn-edit"
                        title="Edit"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                        </svg>
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDelete(expense.id)}
                        className="icon-btn icon-btn-delete"
                        title="Delete"
                      >
                        <Trash2 className="w-5 h-5" />
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Add Expense Modal */}
      {showAddExpenseModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
          <Card className="w-full max-w-md bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
            <CardHeader>
              <CardTitle className="text-white">Add Expense</CardTitle>
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={() => setShowAddExpenseModal(false)}
                className="icon-btn icon-btn-close absolute right-4 top-4"
                title="Close"
              >
                <X className="w-5 h-5" />
              </Button>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="expense-description" className="text-gray-300">Description *</Label>
                  <Input
                    id="expense-description"
                    placeholder="Enter expense description"
                    value={newExpense.description}
                    onChange={(e) => setNewExpense(prev => ({...prev, description: e.target.value}))}
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1"
                  />
                </div>
                <div className="space-y-2">
                  <Label>Amount (₹)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={newExpense.amount}
                    onChange={(e) => setNewExpense({ ...newExpense, amount: e.target.value })}
                    placeholder="0.00"
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                    required
                  />
                </div>
                <div>
                  <Label htmlFor="expense-category" className="text-gray-300">Category *</Label>
                  <Select value={newExpense.category} onValueChange={(value) => setNewExpense(prev => ({...prev, category: value}))}>
                    <SelectTrigger className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1">
                      <SelectValue placeholder="Select category" />
                    </SelectTrigger>
                    <SelectContent className="bg-[#2A2540] border-[#3A3560]">
                      <SelectItem value="Food & Dining" className="text-white">Food & Dining</SelectItem>
                      <SelectItem value="Transportation" className="text-white">Transportation</SelectItem>
                      <SelectItem value="Shopping" className="text-white">Shopping</SelectItem>
                      <SelectItem value="Entertainment" className="text-white">Entertainment</SelectItem>
                      <SelectItem value="Bills & Utilities" className="text-white">Bills & Utilities</SelectItem>
                      <SelectItem value="Healthcare" className="text-white">Healthcare</SelectItem>
                      <SelectItem value="Education" className="text-white">Education</SelectItem>
                      <SelectItem value="Personal Care" className="text-white">Personal Care</SelectItem>
                      <SelectItem value="Other" className="text-white">Other</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="expense-type" className="text-gray-300">Type *</Label>
                  <Select value={newExpense.type} onValueChange={(value) => setNewExpense(prev => ({...prev, type: value}))}>
                    <SelectTrigger className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1">
                      <SelectValue placeholder="Select type" />
                    </SelectTrigger>
                    <SelectContent className="bg-[#2A2540] border-[#3A3560]">
                      <SelectItem value="EXPENSE" className="text-white">
                        <span className="flex items-center gap-2">
                          <span className="w-2 h-2 rounded-full bg-red-400"></span>
                          Expense
                        </span>
                      </SelectItem>
                      <SelectItem value="INCOME" className="text-white">
                        <span className="flex items-center gap-2">
                          <span className="w-2 h-2 rounded-full bg-green-400"></span>
                          Income
                        </span>
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="expense-date" className="text-gray-300">Date</Label>
                  <Input
                    id="expense-date"
                    type="date"
                    value={newExpense.date}
                    onChange={(e) => setNewExpense(prev => ({...prev, date: e.target.value}))}
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1"
                  />
                </div>
                <div className="flex gap-2 pt-2">
                  <Button
                    variant="outline"
                    onClick={() => setShowAddExpenseModal(false)}
                    className="flex-1 bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleAddExpense}
                    disabled={!newExpense.description || !newExpense.amount || !newExpense.category || !newExpense.type}
                    className="flex-1 bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white hover:from-[#6B5FB9] hover:to-[#8C80D8]"
                  >
                    Add Expense
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Edit Expense Modal */}
      {showEditExpenseModal && editingExpense && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4 backdrop-blur-sm">
          <Card className="w-full max-w-md bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
            <CardHeader>
              <CardTitle className="text-white">Edit Expense</CardTitle>
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={() => setShowEditExpenseModal(false)}
                className="icon-btn icon-btn-close absolute right-4 top-4"
                title="Close"
              >
                <X className="w-5 h-5" />
              </Button>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="edit-expense-description" className="text-gray-300">Description *</Label>
                  <Input
                    id="edit-expense-description"
                    placeholder="Enter expense description"
                    value={editExpense.description}
                    onChange={(e) => setEditExpense(prev => ({...prev, description: e.target.value}))}
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1"
                  />
                </div>
                <div className="space-y-2">
                  <Label>Amount (₹)</Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={editExpense.amount}
                    onChange={(e) => setEditExpense(prev => ({...prev, amount: e.target.value}))}
                    placeholder="0.00"
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                    required
                  />
                </div>
                <div>
                  <Label htmlFor="edit-expense-category" className="text-gray-300">Category *</Label>
                  <Select value={editExpense.category} onValueChange={(value) => setEditExpense(prev => ({...prev, category: value}))}>
                    <SelectTrigger className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1">
                      <SelectValue placeholder="Select category" />
                    </SelectTrigger>
                    <SelectContent className="bg-[#2A2540] border-[#3A3560]">
                      <SelectItem value="Food & Dining" className="text-white">Food & Dining</SelectItem>
                      <SelectItem value="Transportation" className="text-white">Transportation</SelectItem>
                      <SelectItem value="Shopping" className="text-white">Shopping</SelectItem>
                      <SelectItem value="Entertainment" className="text-white">Entertainment</SelectItem>
                      <SelectItem value="Bills & Utilities" className="text-white">Bills & Utilities</SelectItem>
                      <SelectItem value="Healthcare" className="text-white">Healthcare</SelectItem>
                      <SelectItem value="Education" className="text-white">Education</SelectItem>
                      <SelectItem value="Personal Care" className="text-white">Personal Care</SelectItem>
                      <SelectItem value="Other" className="text-white">Other</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="edit-expense-type" className="text-gray-300">Type *</Label>
                  <Select value={editExpense.type} onValueChange={(value) => setEditExpense(prev => ({...prev, type: value}))}>
                    <SelectTrigger className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1">
                      <SelectValue placeholder="Select type" />
                    </SelectTrigger>
                    <SelectContent className="bg-[#2A2540] border-[#3A3560]">
                      <SelectItem value="EXPENSE" className="text-white">
                        <span className="flex items-center gap-2">
                          <span className="w-2 h-2 rounded-full bg-red-400"></span>
                          Expense
                        </span>
                      </SelectItem>
                      <SelectItem value="INCOME" className="text-white">
                        <span className="flex items-center gap-2">
                          <span className="w-2 h-2 rounded-full bg-green-400"></span>
                          Income
                        </span>
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="edit-expense-date" className="text-gray-300">Date</Label>
                  <Input
                    id="edit-expense-date"
                    type="date"
                    value={editExpense.date}
                    onChange={(e) => setEditExpense(prev => ({...prev, date: e.target.value}))}
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white h-11 mt-1"
                  />
                </div>
                <div className="flex gap-2 pt-2">
                  <Button
                    variant="outline"
                    onClick={() => setShowEditExpenseModal(false)}
                    className="flex-1 bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleUpdateExpense}
                    disabled={!editExpense.description || !editExpense.amount || !editExpense.category || !editExpense.type}
                    className="flex-1 bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white hover:from-[#6B5FB9] hover:to-[#8C80D8]"
                  >
                    Update Expense
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

export default Expenses;
