import { useState, useRef } from 'react';
import { useToast } from '@/hooks/use-toast';
import { api } from '@/api/api';
import { Upload, FileText, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';

const CSVImporter = ({ onImportComplete }) => {
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [importResult, setImportResult] = useState(null);
  const fileInputRef = useRef(null);
  const { toast } = useToast();

  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    if (file) {
      if (!file.name.toLowerCase().endsWith('.csv')) {
        uploadCSV(file);
      } else {
        toast({
          title: "Invalid File",
          description: "Please select a CSV file (.csv extension)",
          variant: "destructive",
        });
      }
    }
  };

  const uploadCSV = async (file) => {
    setIsUploading(true);
    setUploadProgress(0);
    setImportResult(null);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await api.post('/expenses/upload-csv', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          setUploadProgress(progress);
        },
      });

      if (response.data && response.data.success) {
        const result = response.data.data;
        
        setImportResult(result);
        
        toast({
          title: "Import Complete",
          description: `Successfully imported ${result.imported} of ${result.totalRows} expenses`,
        });
        
        if (onImportComplete) {
          onImportComplete(result);
        }
      } else {
        throw new Error(response.data.message || 'Failed to import CSV');
      }
    } catch (error) {
      console.error('CSV import error:', error);
      toast({
        title: "Import Failed",
        description: error.response?.data?.message || "Failed to import CSV file",
        variant: "destructive",
      });
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
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
      if (file.name.toLowerCase().endsWith('.csv')) {
        uploadCSV(file);
      } else {
        toast({
          title: "Invalid File",
          description: "Please select a CSV file (.csv extension)",
          variant: "destructive",
        });
      }
    }
  };

  const triggerFileSelect = () => {
    fileInputRef.current?.click();
  };

  return (
    <Card className="bg-[#1E1E2A] border-[#2C2C3A] shadow-[0_8px_20px_rgba(0,0,0,0.4)] rounded-[16px] hover:shadow-[0_12px_30px_rgba(123,111,201,0.3)] hover:transform hover:translateY-[-4px] transition-all duration-300">
      <CardHeader>
        <CardTitle className="text-white flex items-center gap-2">
          <FileText className="w-5 h-5 text-[#9C90E8]" />
          CSV Import
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
            accept=".csv"
            onChange={handleFileSelect}
            className="hidden"
            disabled={isUploading}
          />
          
          {isUploading ? (
            <div className="space-y-4">
              <Loader2 className="w-12 h-12 mx-auto text-[#9C90E8] animate-spin" />
              <p className="text-gray-400">Processing CSV file...</p>
              <Progress value={uploadProgress} className="w-full h-2 bg-[#2C2C3A]" />
              <p className="text-sm text-gray-400">{uploadProgress}%</p>
            </div>
          ) : (
            <div className="space-y-4">
              <Upload className="w-12 h-12 mx-auto text-gray-400" />
              <p className="text-gray-400">Drop CSV file here or click to browse</p>
              <p className="text-sm text-gray-500">Maximum file size: 10MB</p>
            </div>
          )}
        </div>

        {/* Import Result */}
        {importResult && (
          <div className="mt-6 space-y-4">
            <div className="flex items-center justify-between p-4 bg-[#0F0F12] rounded-lg">
              <span className="text-gray-300">Total Rows:</span>
              <span className="text-white font-semibold">{importResult.totalRows}</span>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div className="flex items-center justify-between p-4 bg-green-900/20 rounded-lg border border-green-500/30">
                <span className="text-green-400">Imported:</span>
                <span className="text-green-400 font-semibold">{importResult.imported}</span>
              </div>
              
              <div className="flex items-center justify-between p-4 bg-red-900/20 rounded-lg border border-red-500/30">
                <span className="text-red-400">Failed:</span>
                <span className="text-red-400 font-semibold">{importResult.failed}</span>
              </div>
            </div>

            {/* Success Rate */}
            <div className="mt-4">
              <div className="flex items-center justify-between p-3 bg-[#0F0F12] rounded-lg">
                <span className="text-gray-300">Success Rate:</span>
                <span className={`font-semibold ${
                  importResult.imported / importResult.totalRows >= 0.9 ? 'text-green-400' : 'text-yellow-400'
                }`}>
                  {Math.round((importResult.imported / importResult.totalRows) * 100)}%
                </span>
              </div>
              <Progress 
                value={(importResult.imported / importResult.totalRows) * 100} 
                className="w-full h-2 bg-[#2C2C3A]" 
              />
            </div>

            {/* Errors */}
            {importResult.errors && importResult.errors.length > 0 && (
              <div className="mt-4">
                <h4 className="text-white font-medium mb-2 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-400" />
                  Import Errors ({importResult.errors.length})
                </h4>
                <div className="max-h-32 overflow-y-auto bg-[#0F0F12] rounded-lg p-3">
                  {importResult.errors.map((error, index) => (
                    <div key={index} className="text-sm text-red-400 mb-1">
                      {error}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Instructions */}
        <div className="mt-6 p-4 bg-[#0F0F12] rounded-lg">
          <h4 className="text-white font-medium mb-3 flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-[#9C90E8]" />
            CSV Format Requirements:
          </h4>
          <div className="text-sm text-gray-300 space-y-1">
            <p>• First row must contain headers: <code className="bg-[#2C2C3A] px-2 py-1 rounded text-[#9C90E8]">date,description,amount,category,paymentType</code></p>
            <p>• Date format: <code className="bg-[#2C2C3A] px-2 py-1 rounded text-[#9C90E8]">YYYY-MM-DD</code> (e.g., 2026-04-03)</p>
            <p>• Amount should be numeric (e.g., 50.50)</p>
            <p>• Category: Food & Dining, Transportation, Shopping, Entertainment, Bills & Utilities, Healthcare, Education, Travel, Investments, Savings, Other</p>
            <p>• Payment Type: Cash, Card, Bank Transfer, UPI, etc.</p>
          </div>
        </div>

        {/* Sample CSV Download */}
        <div className="mt-4">
          <Button
            variant="outline"
            className="w-full border-[#2C2C3A] text-gray-300 hover:border-[#7B6FC9] hover:text-[#9C90E8]"
            onClick={() => {
              // Create and download sample CSV
              const sampleCSV = `date,description,amount,category,paymentType
2026-04-01,Groceries,1500.50,Food & Dining,Cash
2026-04-02,Monthly Rent,25000.00,Bills & Utilities,Bank Transfer
2026-04-03,Uber Ride,350.00,Transportation,Card`;
              
              const blob = new Blob([sampleCSV], { type: 'text/csv' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = 'sample_expenses.csv';
              document.body.appendChild(a);
              a.click();
              document.body.removeChild(a);
              URL.revokeObjectURL(url);
              
              toast({
                title: "Sample Downloaded",
                description: "Sample CSV file has been downloaded",
              });
            }}
          >
            <FileText className="w-4 h-4 mr-2" />
            Download Sample CSV
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

export default CSVImporter;
