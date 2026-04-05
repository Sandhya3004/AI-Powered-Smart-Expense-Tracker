import React, { useState, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Upload, Camera, FileText, AlertCircle } from 'lucide-react';
import OcrConfirmationModal from './OcrConfirmationModal';
import { api } from '@/api/api';

const ReceiptUploader = ({ onExpenseCreated }) => {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [extractedData, setExtractedData] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const handleFileSelect = (event) => {
    const selectedFile = event.target.files[0];
    if (selectedFile) {
      // Validate file type
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
      if (!validTypes.includes(selectedFile.type)) {
        setError('Please select a valid image file (JPEG, PNG, or WebP)');
        return;
      }

      // Validate file size (10MB max)
      if (selectedFile.size > 10 * 1024 * 1024) {
        setError('File size must be less than 10MB');
        return;
      }

      setFile(selectedFile);
      setError('');
    }
  };

  const handleDrop = (event) => {
    event.preventDefault();
    const droppedFile = event.dataTransfer.files[0];
    if (droppedFile) {
      handleFileSelect({ target: { files: [droppedFile] } });
    }
  };

  const handleDragOver = (event) => {
    event.preventDefault();
  };

  const handleUpload = async () => {
    if (!file) return;

    setLoading(true);
    setError('');

    try {
      const formData = new FormData();
      formData.append('file', file);

      // Upload to new backend endpoint
      const response = await api.post('/receipts/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      // Backend now returns { receipt }, but support both old and new responses
      const receipt = response.data?.receipt || response.data;
      const ocrData = {
        amount: receipt?.amount || '',
        merchant: receipt?.merchant || '',
        date: receipt?.date || '',
        category: receipt?.category || '',
        confidence: receipt?.confidence || 0,
        extractedText: receipt?.extractedText || '',
      };

      setExtractedData(ocrData);
      setShowModal(true);
    } catch (error) {
      console.error('Error uploading receipt:', error);
      setError(error.response?.data?.error || 'Failed to upload and process receipt');
    } finally {
      setLoading(false);
    }
  };

  const handleModalConfirm = (confirmedData) => {
    // Create expense with confirmed data
    const expenseData = {
      amount: parseFloat(confirmedData.amount),
      type: 'EXPENSE',
      description: confirmedData.merchant || 'Receipt Expense',
      category: confirmedData.category || 'Other',
      merchant: confirmedData.merchant || '',
      date: confirmedData.date || new Date().toISOString().split('T')[0],
      source: 'receipt',
      notes: `Receipt uploaded: ${file.name}`,
      status: 'COMPLETED',
    };

    // Call the parent component to create the expense
    if (onExpenseCreated) {
      onExpenseCreated(expenseData);
    }

    // Reset state
    setShowModal(false);
    setFile(null);
    setExtractedData(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleModalCancel = () => {
    setShowModal(false);
    setExtractedData(null);
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <>
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Upload className="h-5 w-5" />
            Upload Receipt
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* File Upload Area */}
          <div
            className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-gray-400 transition-colors cursor-pointer"
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleFileSelect}
              className="hidden"
            />
            
            <div className="flex flex-col items-center space-y-4">
              {file ? (
                <>
                  <FileText className="h-12 w-12 text-green-600" />
                  <div className="text-sm">
                    <p className="font-medium text-green-600">{file.name}</p>
                    <p className="text-gray-500">{formatFileSize(file.size)}</p>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      setFile(null);
                      setError('');
                      if (fileInputRef.current) {
                        fileInputRef.current.value = '';
                      }
                    }}
                  >
                    Remove File
                  </Button>
                </>
              ) : (
                <>
                  <Upload className="h-12 w-12 text-gray-400" />
                  <div className="text-sm">
                    <p className="font-medium">Click to upload or drag and drop</p>
                    <p className="text-gray-500">JPEG, PNG, or WebP (MAX. 10MB)</p>
                  </div>
                  <Button variant="outline" size="sm">
                    <Camera className="h-4 w-4 mr-2" />
                    Select File
                  </Button>
                </>
              )}
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded-lg">
              <AlertCircle className="h-4 w-4 text-red-600" />
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          {/* Upload Button */}
          <div className="flex justify-end">
            <Button
              onClick={handleUpload}
              disabled={!file || loading}
              className="min-w-[120px]"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  Processing...
                </>
              ) : (
                <>
                  <Upload className="h-4 w-4 mr-2" />
                  Upload & Process
                </>
              )}
            </Button>
          </div>

          {/* Instructions */}
          <div className="text-sm text-gray-600 space-y-2">
            <p className="font-medium">How it works:</p>
            <ol className="list-decimal list-inside space-y-1">
              <li>Upload a receipt image (JPEG, PNG, or WebP)</li>
              <li>Our AI will extract amount, merchant, date, and category</li>
              <li>Review and edit the extracted information</li>
              <li>Confirm to create an expense automatically</li>
            </ol>
          </div>
        </CardContent>
      </Card>

      {/* OCR Confirmation Modal */}
      {showModal && extractedData && (
        <OcrConfirmationModal
          extractedData={extractedData}
          onConfirm={handleModalConfirm}
          onCancel={handleModalCancel}
          fileName={file?.name}
        />
      )}
    </>
  );
};

export default ReceiptUploader;
