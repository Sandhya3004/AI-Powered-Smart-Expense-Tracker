import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { 
  CheckCircle, 
  AlertCircle, 
  FileText, 
  Calendar,
  DollarSign,
  Building,
  Tag
} from 'lucide-react';

const OcrConfirmationModal = ({ 
  extractedData, 
  onConfirm, 
  onCancel, 
  fileName 
}) => {
  const [formData, setFormData] = useState({
    amount: '',
    merchant: '',
    date: '',
    category: '',
  });

  const categories = [
    'Food & Dining', 
    'Transportation', 
    'Shopping', 
    'Entertainment',
    'Bills & Utilities', 
    'Healthcare', 
    'Education', 
    'Travel',
    'Investments', 
    'Savings', 
    'Other'
  ];

  // Initialize form with extracted data
  useEffect(() => {
    if (extractedData) {
      setFormData({
        amount: extractedData.amount || '',
        merchant: extractedData.merchant || '',
        date: extractedData.date || new Date().toISOString().split('T')[0],
        category: extractedData.category || 'Other',
      });
    }
  }, [extractedData]);

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleConfirm = () => {
    onConfirm(formData);
  };

  const getConfidenceColor = (confidence) => {
    if (confidence >= 0.8) return 'bg-green-100 text-green-800';
    if (confidence >= 0.6) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  };

  const getConfidenceText = (confidence) => {
    if (confidence >= 0.8) return 'High';
    if (confidence >= 0.6) return 'Medium';
    return 'Low';
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Confirm Receipt Details
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* File Info */}
          <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-2">
              <FileText className="h-4 w-4 text-gray-600" />
              <span className="text-sm font-medium">{fileName}</span>
            </div>
            <Badge className={getConfidenceColor(extractedData.confidence)}>
              <AlertCircle className="h-3 w-3 mr-1" />
              {getConfidenceText(extractedData.confidence)} Confidence
            </Badge>
          </div>

          {/* Extracted Text Preview */}
          {extractedData.extractedText && (
            <div className="space-y-2">
              <Label className="text-sm font-medium">Extracted Text</Label>
              <div className="p-3 bg-gray-50 rounded-lg text-sm text-gray-700 max-h-32 overflow-y-auto">
                {extractedData.extractedText}
              </div>
            </div>
          )}

          {/* Editable Form */}
          <div className="grid gap-4 md:grid-cols-2">
            {/* Amount */}
            <div className="space-y-2">
              <Label htmlFor="amount" className="flex items-center gap-2">
                <DollarSign className="h-4 w-4" />
                Amount *
              </Label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0.00"
                value={formData.amount}
                onChange={(e) => handleInputChange('amount', e.target.value)}
                className="text-lg font-medium"
              />
            </div>

            {/* Date */}
            <div className="space-y-2">
              <Label htmlFor="date" className="flex items-center gap-2">
                <Calendar className="h-4 w-4" />
                Date *
              </Label>
              <Input
                id="date"
                type="date"
                value={formData.date}
                onChange={(e) => handleInputChange('date', e.target.value)}
              />
            </div>

            {/* Merchant */}
            <div className="space-y-2">
              <Label htmlFor="merchant" className="flex items-center gap-2">
                <Building className="h-4 w-4" />
                Merchant/Payee *
              </Label>
              <Input
                id="merchant"
                placeholder="Enter merchant or payee"
                value={formData.merchant}
                onChange={(e) => handleInputChange('merchant', e.target.value)}
              />
            </div>

            {/* Category */}
            <div className="space-y-2">
              <Label htmlFor="category" className="flex items-center gap-2">
                <Tag className="h-4 w-4" />
                Category *
              </Label>
              <Select 
                value={formData.category} 
                onValueChange={(value) => handleInputChange('category', value)}
              >
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
          </div>

          {/* Preview */}
          <div className="space-y-2">
            <Label className="text-sm font-medium">Preview</Label>
            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <DollarSign className="h-4 w-4 text-blue-600" />
                  <span className="font-medium">
                    {formData.merchant || 'Untitled Expense'}
                  </span>
                </div>
                <div className="text-right">
                  <p className="font-bold text-lg text-red-600">
                    -${parseFloat(formData.amount || 0).toFixed(2)}
                  </p>
                  <p className="text-sm text-gray-600">
                    {formData.date ? new Date(formData.date).toLocaleDateString() : 'No date'}
                  </p>
                </div>
              </div>
              {formData.category && (
                <div className="mt-2">
                  <Badge variant="secondary">{formData.category}</Badge>
                </div>
              )}
            </div>
          </div>

          {/* Instructions */}
          <div className="text-sm text-gray-600 space-y-1">
            <p className="font-medium">Please review the extracted information:</p>
            <ul className="list-disc list-inside space-y-1">
              <li>Edit any fields that need correction</li>
              <li>Ensure all required fields (*) are filled</li>
              <li>Click "Confirm" to create the expense</li>
            </ul>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button 
              type="button" 
              variant="outline" 
              onClick={onCancel}
              className="min-w-[100px]"
            >
              Cancel
            </Button>
            <Button 
              type="button" 
              onClick={handleConfirm}
              disabled={!formData.amount || !formData.merchant || !formData.date}
              className="min-w-[120px]"
            >
              <CheckCircle className="h-4 w-4 mr-2" />
              Confirm & Create Expense
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default OcrConfirmationModal;
