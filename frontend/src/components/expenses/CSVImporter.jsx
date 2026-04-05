import React, { useState } from 'react';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Upload, X, FileText, Check } from 'lucide-react';
import { api } from '@/api/api';

const CSVImporter = ({ onImportComplete }) => {
  const [showModal, setShowModal] = useState(false);
  const [csvFile, setCsvFile] = useState(null);
  const [isImporting, setIsImporting] = useState(false);
  const [importResults, setImportResults] = useState(null);
  const { toast } = useToast();

  const handleFileChange = (event) => {
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

    setIsImporting(true);
    try {
      const text = await csvFile.text();
      const lines = text.split('\n').filter(line => line.trim());
      
      if (lines.length < 2) {
        toast({
          title: 'Invalid CSV',
          description: 'CSV file must have at least 2 rows (header + 1 data row).',
          variant: 'destructive',
        });
        return;
      }

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
          api.post('/expenses', {
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

      setImportResults({
        total: expenses.length,
        success: successCount,
        failed: failureCount
      });

      toast({
        title: 'CSV Import Complete',
        description: `Successfully imported ${successCount} expenses. ${failureCount > 0 ? `${failureCount} failed.` : ''}`,
        variant: failureCount > 0 ? 'destructive' : 'default',
      });

      // Notify parent component
      if (onImportComplete) {
        onImportComplete({
          totalImported: successCount,
          totalFailed: failureCount
        });
      }

      // Trigger refresh for expense list
      window.dispatchEvent(new CustomEvent('refreshTransactions'));

    } catch (error) {
      console.error('CSV import failed:', error);
      toast({
        title: 'Import Error',
        description: 'Failed to import CSV file. Please check the format.',
        variant: 'destructive',
      });
    } finally {
      setIsImporting(false);
      setCsvFile(null);
      setShowModal(false);
      setImportResults(null);
    }
  };

  return (
    <>
      <Button
        onClick={() => setShowModal(true)}
        variant="outline"
        className="flex items-center gap-2"
      >
        <FileText className="w-4 h-4" />
        Import CSV
      </Button>

      {/* CSV Import Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Upload className="w-5 h-5" />
                Import CSV File
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
                  <Label htmlFor="csv-upload">Upload CSV File</Label>
                  <Input
                    id="csv-upload"
                    type="file"
                    accept=".csv"
                    className="cursor-pointer"
                    onChange={handleFileChange}
                  />
                </div>
                <div className="text-sm text-gray-500">
                  <p>CSV format: Date,Description,Amount,Category,Notes</p>
                  <p>First row should contain headers.</p>
                  <p>Maximum 100 rows allowed.</p>
                </div>
                
                <Button
                  className="w-full"
                  onClick={handleImportCSV}
                  disabled={isImporting || !csvFile}
                >
                  {isImporting ? (
                    <>
                      <span className="animate-spin inline-block h-4 w-4 border-b-2 border-white rounded-full mr-2" />
                        Importing...
                    </>
                  ) : (
                    <>
                      <Upload className="w-4 h-4 mr-2" />
                      Import CSV
                    </>
                  )}
                </Button>

                {/* Import Results */}
                {importResults && (
                  <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                    <h4 className="font-medium mb-2">Import Results</h4>
                    <div className="grid grid-cols-3 gap-4 text-sm">
                      <div className="text-center">
                        <p className="font-medium">Total Rows</p>
                        <p className="text-lg font-bold">{importResults.total}</p>
                      </div>
                      <div className="text-center">
                        <p className="font-medium text-green-600">Success</p>
                        <p className="text-lg font-bold">{importResults.success}</p>
                      </div>
                      <div className="text-center">
                        <p className="font-medium text-red-600">Failed</p>
                        <p className="text-lg font-bold">{importResults.failed}</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </>
  );
};

export default CSVImporter;
