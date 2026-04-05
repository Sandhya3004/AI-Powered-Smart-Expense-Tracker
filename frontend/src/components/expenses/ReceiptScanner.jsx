import React, { useState } from 'react';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Camera, X, Check, AlertTriangle } from 'lucide-react';
import { api } from '@/api/api';

const ReceiptScanner = ({ onExpenseCreated }) => {
  const [showModal, setShowModal] = useState(false);
  const [receiptFile, setReceiptFile] = useState(null);
  const [extractedData, setExtractedData] = useState({
    merchant: '',
    amount: '',
    description: '',
    category: '',
    date: new Date().toISOString().split('T')[0]
  });
  const [isProcessing, setIsProcessing] = useState(false);
  const { toast } = useToast();

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      setReceiptFile(file);
    // Auto-extract basic info from filename
      const filename = file.name.toLowerCase();
      let category = 'Other';
      let merchant = '';

      if (filename.includes('restaurant') || filename.includes('food')) {
        category = 'Food & Dining';
      } else if (filename.includes('gas') || filename.includes('petrol')) {
        category = 'Transportation';
      } else if (filename.includes('walmart') || filename.includes('target')) {
        category = 'Shopping';
        merchant = filename.includes('walmart') ? 'Walmart' : 'Target';
      }

      setExtractedData(prev => ({
        ...prev,
        category,
        merchant
      }));
    }
  };

  const handleScanReceipt = async () => {
    if (!receiptFile) {
      toast({
        title: 'No file',
        description: 'Please upload a receipt image first.',
        variant: 'destructive',
      });
      return;
    }

    setIsProcessing(true);
    try {
      const formData = new FormData();
      formData.append('file', receiptFile);

      // Send to AI OCR service
      const response = await api.post('/ocr/receipt', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (response.data && response.data.success) {
        const ocrData = response.data.extracted_data;
        
        setExtractedData({
          merchant: ocrData.merchant || extractedData.merchant,
          amount: ocrData.total_amount || extractedData.amount,
          description: ocrData.description || extractedData.description,
          category: ocrData.category || extractedData.category,
          date: ocrData.date || extractedData.date
        });

        toast({
          title: 'Receipt Scanned',
          description: 'Receipt data extracted successfully. Please review and confirm.',
        });
      } else {
        toast({
          title: 'Scanning Failed',
          description: response.data?.error || 'Failed to scan receipt. Please try again.',
          variant: 'destructive',
        });
      }
    } catch (error) {
      console.error('OCR processing failed:', error);
      toast({
        title: 'Error',
        description: 'Failed to scan receipt. Please try again.',
        variant: 'destructive',
      });
    } finally {
      setIsProcessing(false);
    }
  };

  const handleConfirmExpense = async () => {
    try {
      const response = await api.post('/expenses', {
        description: extractedData.description,
        amount: parseFloat(extractedData.amount),
        category: extractedData.category,
        expenseDate: extractedData.date,
        type: 'EXPENSE',
        source: 'ocr',
        merchant: extractedData.merchant,
        currency: 'INR',
        paymentType: 'Cash',
        account: 'Cash',
        status: 'COMPLETED'
      });

      if (response.data) {
        const newExpense = response.data;
        
        // Notify parent component
        if (onExpenseCreated) {
          onExpenseCreated({
            ...newExpense,
            expenseDate: newExpense.expenseDate || newExpense.date
          });
        }

        toast({
          title: 'Success',
          description: `Added ₹${newExpense.amount.toFixed(2)} expense from ${extractedData.merchant || 'receipt'}`,
        });

        // Close modal and reset
        setShowModal(false);
        setReceiptFile(null);
        setExtractedData({
          merchant: '',
          amount: '',
          description: '',
          category: '',
          date: new Date().toISOString().split('T')[0]
        });

        // Trigger refresh for expense list
        window.dispatchEvent(new CustomEvent('refreshTransactions'));
      }
    } catch (error) {
      console.error('Failed to create expense from receipt:', error);
      toast({
        title: 'Error',
        description: 'Failed to create expense from receipt. Please try again.',
        variant: 'destructive',
      });
    }
  };

  return (
    <>
      <Button
        onClick={() => setShowModal(true)}
        variant="outline"
        className="flex items-center gap-2"
      >
        <Camera className="w-4 h-4" />
        Scan Receipt
      </Button>

      {/* Receipt Scanner Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Camera className="w-5 h-5" />
                Scan Receipt
              </CardTitle>
              <Button 
                variant="ghost" 
                size="sm" 
                onClick={() => setShowModal(false)}
                className="absolute right-4 top-4"
              >
                <X className="w-4 h-4" />
              </Button>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="receipt-upload">Upload Receipt Image</Label>
                  <Input
                    id="receipt-upload"
                    type="file"
                    accept="image/*"
                    className="cursor-pointer"
                    onChange={handleFileChange}
                  />
                </div>
                
                <Button
                  className="w-full"
                  onClick={handleScanReceipt}
                  disabled={isProcessing || !receiptFile}
                >
                  {isProcessing ? (
                    <>
                      <span className="animate-spin inline-block h-4 w-4 border-b-2 border-white rounded-full mr-2" />
                      Scanning...
                    </>
                  ) : (
                    <>
                      <Camera className="w-4 h-4 mr-2" />
                      Scan with AI OCR
                    </>
                  )}
                </Button>

                {/* Extracted Data Display */}
                {(extractedData.merchant || extractedData.amount) && (
                  <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                    <h4 className="font-medium mb-3">Extracted Information</h4>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <Label>Merchant</Label>
                        <Input
                          value={extractedData.merchant}
                          onChange={(e) => setExtractedData(prev => ({...prev, merchant: e.target.value}))}
                          placeholder="Store name"
                        />
                      </div>
                      <div>
                        <Label>Amount</Label>
                        <Input
                          type="number"
                          value={extractedData.amount}
                          onChange={(e) => setExtractedData(prev => ({...prev, amount: e.target.value}))}
                          placeholder="0.00"
                          step="0.01"
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <Label>Category</Label>
                        <Select value={extractedData.category} onValueChange={(value) => setExtractedData(prev => ({...prev, category: value}))}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select category" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="Food & Dining">Food & Dining</SelectItem>
                            <SelectItem value="Transportation">Transportation</SelectItem>
                            <SelectItem value="Shopping">Shopping</SelectItem>
                            <SelectItem value="Entertainment">Entertainment</SelectItem>
                            <SelectItem value="Bills & Utilities">Bills & Utilities</SelectItem>
                            <SelectItem value="Healthcare">Healthcare</SelectItem>
                            <SelectItem value="Other">Other</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div>
                        <Label>Description</Label>
                        <Textarea
                          value={extractedData.description}
                          onChange={(e) => setExtractedData(prev => ({...prev, description: e.target.value}))}
                          placeholder="Enter expense description"
                          rows={3}
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <Label>Date</Label>
                        <Input
                          type="date"
                          value={extractedData.date}
                          onChange={(e) => setExtractedData(prev => ({...prev, date: e.target.value}))}
                        />
                      </div>
                    </div>
                  </div>
                )}

                <div className="flex gap-2 mt-6">
                  <Button
                    variant="outline"
                    onClick={() => setShowModal(false)}
                    className="flex-1"
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleConfirmExpense}
                    disabled={!extractedData.amount || isProcessing}
                    className="flex-1"
                  >
                    {isProcessing ? (
                      <>
                        <span className="animate-spin inline-block h-4 w-4 border-b-2 border-white rounded-full mr-2" />
                          Creating...
                      </>
                    ) : (
                      <>
                        <Check className="w-4 h-4 mr-2" />
                        Create Expense
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
};

export default ReceiptScanner;
