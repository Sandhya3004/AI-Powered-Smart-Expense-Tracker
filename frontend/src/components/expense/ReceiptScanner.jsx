import { useState, useRef } from 'react';
import { useToast } from '@/hooks/use-toast';
import { api } from '@/api/api';
import { Camera, Upload, CheckCircle, AlertCircle, Loader2, Image as ImageIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const ReceiptScanner = ({ onExpenseCreated }) => {
  const [isScanning, setIsScanning] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const [scanResult, setScanResult] = useState(null);
  const fileInputRef = useRef(null);
  const { toast } = useToast();

  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (file) {
      if (file.type.startsWith('image/')) {
        previewAndScan(file);
      } else {
        toast({
          title: "Invalid File",
          description: "Please select an image file (JPEG, PNG, or WebP)",
          variant: "destructive",
        });
      }
    }
  };

  const previewAndScan = (file) => {
    // Create preview
    const reader = new FileReader();
    reader.onload = (e) => {
      setPreviewImage(e.target.result);
      scanReceipt(file);
    };
    reader.readAsDataURL(file);
  };

  const scanReceipt = async (file) => {
    setIsScanning(true);
    setScanResult(null);

    try {
      const formData = new FormData();
      formData.append('receipt', file);

      const response = await api.post('/expenses/upload-receipt', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (response.data && response.data.success) {
        const expense = response.data.data;
        
        setScanResult({
          success: true,
          expense: expense,
          message: 'Receipt scanned successfully'
        });
        
        toast({
          title: "Scan Complete",
          description: `Expense created: ${expense.description} - ₹${expense.amount}`,
        });
        
        if (onExpenseCreated) {
          onExpenseCreated(expense);
        }
      } else {
        setScanResult({
          success: false,
          message: response.data.message || 'Failed to scan receipt'
        });
      }
    } catch (error) {
      console.error('Receipt scan error:', error);
      setScanResult({
        success: false,
        message: error.response?.data?.message || "Failed to scan receipt"
      });
    } finally {
      setIsScanning(false);
    }
  };

  const handleDragOver = (event) => {
    event.preventDefault();
    event.stopPropagation();
  };

  const handleDrop = (event) => {
    event.preventDefault();
    event.stopPropagation();
    
    const files = event.dataTransfer.files;
    if (files.length > 0) {
      const file = files[0];
      if (file.type.startsWith('image/')) {
        previewAndScan(file);
      } else {
        toast({
          title: "Invalid File",
          description: "Please select an image file (JPEG, PNG, or WebP)",
          variant: "destructive",
        });
      }
    }
  };

  const triggerFileSelect = () => {
    fileInputRef.current?.click();
  };

  const resetScanner = () => {
    setPreviewImage(null);
    setScanResult(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <Card className="bg-[#1E1E2A] border-[#2C2C3A] shadow-[0_8px_20px_rgba(0,0,0,0.4)] rounded-[16px] hover:shadow-[0_12px_30px_rgba(123,111,201,0.3)] hover:transform hover:translateY-[-4px] transition-all duration-300">
      <CardHeader>
        <CardTitle className="text-white flex items-center gap-2">
          <Camera className="w-5 h-5 text-[#9C90E8]" />
          Receipt Scanner
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Upload Area */}
        <div
          className="border-2 border-dashed border-[#2C2C3A] rounded-lg p-8 text-center cursor-pointer hover:border-[#7B6FC9] transition-colors duration-300"
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          onClick={triggerFileSelect}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleFileSelect}
            className="hidden"
            disabled={isScanning}
          />
          
          {isScanning ? (
            <div className="space-y-4">
              <Loader2 className="w-12 h-12 mx-auto text-[#9C90E8] animate-spin" />
              <p className="text-gray-400">Scanning receipt with AI...</p>
              <p className="text-sm text-gray-500">Extracting text and amounts using OCR</p>
            </div>
          ) : previewImage ? (
            <div className="space-y-4">
              <img 
                src={previewImage} 
                alt="Receipt preview" 
                className="max-h-48 mx-auto rounded-lg shadow-md"
              />
              <p className="text-gray-400">Click to scan or drag new image</p>
            </div>
          ) : (
            <div className="space-y-4">
              <Upload className="w-12 h-12 mx-auto text-gray-400" />
              <p className="text-gray-400">Drop receipt image here or click to browse</p>
              <p className="text-sm text-gray-500">Supported: JPEG, PNG, WebP (Max 5MB)</p>
            </div>
          )}
        </div>

        {/* Scan Result */}
        {scanResult && (
          <div className="mt-6 space-y-4">
            <div className={`p-4 rounded-lg border ${
              scanResult.success 
                ? 'bg-green-900/20 border-green-500/30' 
                : 'bg-red-900/20 border-red-500/30'
            }`}>
              <div className="flex items-center gap-3">
                {scanResult.success ? (
                  <CheckCircle className="w-6 h-6 text-green-400" />
                ) : (
                  <AlertCircle className="w-6 h-6 text-red-400" />
                )}
                <div>
                  <h4 className={`font-medium ${
                    scanResult.success ? 'text-green-400' : 'text-red-400'
                  }`}>
                    {scanResult.success ? 'Scan Successful!' : 'Scan Failed'}
                  </h4>
                  <p className="text-sm text-gray-300 mt-1">
                    {scanResult.message}
                  </p>
                </div>
              </div>
            </div>

            {/* Extracted Data */}
            {scanResult.success && scanResult.expense && (
              <div className="mt-4 p-4 bg-[#0F0F12] rounded-lg">
                <h4 className="text-white font-medium mb-3 flex items-center gap-2">
                  <ImageIcon className="w-4 h-4 text-[#9C90E8]" />
                  Extracted Information:
                </h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-400">Merchant:</span>
                    <span className="text-white font-medium">{scanResult.expense.merchant || 'Not detected'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">Amount:</span>
                    <span className="text-white font-semibold text-lg">₹{scanResult.expense.amount}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">Date:</span>
                    <span className="text-white font-medium">{scanResult.expense.expenseDate || 'Not detected'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">Category:</span>
                    <span className="text-white font-medium">{scanResult.expense.category || 'Other'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">Description:</span>
                    <span className="text-white">{scanResult.expense.description || 'Scanned expense'}</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Instructions */}
        <div className="mt-6 p-4 bg-[#0F0F12] rounded-lg">
          <h4 className="text-white font-medium mb-3 flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-[#9C90E8]" />
            Tips for Best Results:
          </h4>
          <div className="text-sm text-gray-300 space-y-2">
            <p>• Ensure receipt is well-lit and clearly visible</p>
            <p>• Place camera directly above receipt, avoid shadows</p>
            <p>• Make sure total amount is clearly visible</p>
            <p>• Supported formats: Store receipts, restaurant bills, gas stations</p>
            <p>• AI will automatically extract merchant, amount, date, and category</p>
          </div>
        </div>

        {/* Reset Button */}
        {(previewImage || scanResult) && (
          <div className="mt-4">
            <Button
              variant="outline"
              className="w-full border-[#2C2C3A] text-gray-300 hover:border-[#7B6FC9] hover:text-[#9C90E8]"
              onClick={resetScanner}
              disabled={isScanning}
            >
              <Upload className="w-4 h-4 mr-2" />
              Scan Another Receipt
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default ReceiptScanner;
