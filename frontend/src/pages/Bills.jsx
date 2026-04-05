import { useState, useEffect } from 'react';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogDescription } from '@/components/ui/dialog';
import { Plus, Receipt, Calendar, AlertCircle, CheckCircle2, Clock, Trash2, Loader2 } from 'lucide-react';
import { api } from '@/api/api';
import { formatCurrency } from '@/utils/formatters';

// Bill API service
const billService = {
  getAll: () => api.get('/bill-reminders'),
  getById: (id) => api.get(`/bill-reminders/${id}`),
  create: (data) => api.post('/bill-reminders', data),
  update: (id, data) => api.put(`/bill-reminders/${id}`, data),
  delete: (id) => api.delete(`/bill-reminders/${id}`),
  markAsPaid: (id) => api.post(`/bill-reminders/${id}/pay`),
  getUpcoming: () => api.get('/bill-reminders/upcoming'),
  getOverdue: () => api.get('/bill-reminders/overdue'),
};

const Bills = () => {
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editingBill, setEditingBill] = useState(null);
  const [newBill, setNewBill] = useState({
    billName: '',
    amount: '',
    dueDate: '',
    category: 'Utilities',
    description: ''
  });
  const [editBill, setEditBill] = useState({
    billName: '',
    amount: '',
    dueDate: '',
    category: 'Utilities',
    description: ''
  });
  const [deletingId, setDeletingId] = useState(null);
  const [payingId, setPayingId] = useState(null);
  const { toast } = useToast();

  useEffect(() => {
    fetchBills();
  }, []);

  const fetchBills = async () => {
    try {
      setLoading(true);
      const response = await billService.getAll();
      // Handle ApiResponse wrapper
      const billsData = response.data?.data || response.data || [];
      
      // Auto-calculate bill status based on due date and paid status
      const billsWithStatus = billsData.map(bill => ({
        ...bill,
        status: getBillStatus(bill)
      }));
      
      setBills(billsWithStatus);
    } catch (error) {
      console.error('Failed to fetch bills:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to fetch bills',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  // Auto-calculate bill status
  const getBillStatus = (bill) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(bill.dueDate);
    due.setHours(0, 0, 0, 0);
    
    if (bill.isPaid || bill.status === 'PAID') return 'PAID';
    if (due < today) return 'OVERDUE';
    return 'PENDING';
  };

  const handleAddBill = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        billName: newBill.billName,
        amount: parseFloat(newBill.amount),
        dueDate: newBill.dueDate,
        category: newBill.category,
        description: newBill.description || '',
        isPaid: false,
        status: 'PENDING',
        recurrencePattern: 'NONE',
        paymentMethod: 'ONLINE',
        reminderDays: [1, 3, 7]
      };
      
      const response = await billService.create(payload);
      
      if (response.data?.success) {
        const newBillData = response.data.data;
        setBills(prev => [{
          ...newBillData,
          status: getBillStatus(newBillData)
        }, ...prev]);
        
        toast({
          title: 'Success',
          description: 'Bill added successfully',
        });
        setShowAddDialog(false);
        setNewBill({ billName: '', amount: '', dueDate: '', category: 'Utilities', description: '' });
        fetchBills();
      } else {
        throw new Error(response.data?.message || 'Failed to add bill');
      }
    } catch (error) {
      console.error('Failed to add bill:', error);
      const isConflict = error.response?.status === 409;
      toast({
        title: isConflict ? 'Duplicate Bill' : 'Error',
        description: error.response?.data?.message || error.message || 'Failed to add bill',
        variant: 'destructive',
      });
    }
  };

  const handleDeleteBill = async (id) => {
    try {
      setDeletingId(id);
      const response = await billService.delete(id);
      
      if (response.data?.success) {
        setBills(prev => prev.filter(b => b.id !== id));
        toast({
          title: 'Success',
          description: 'Bill deleted successfully',
        });
      } else {
        throw new Error(response.data?.message || 'Failed to delete bill');
      }
    } catch (error) {
      console.error('Failed to delete bill:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to delete bill',
        variant: 'destructive',
      });
    } finally {
      setDeletingId(null);
    }
  };

  const handleMarkAsPaid = async (id) => {
    try {
      setPayingId(id);
      const response = await billService.markAsPaid(id);
      
      if (response.data?.success) {
        const updatedBill = response.data.data;
        setBills(prev => prev.map(b => b.id === id ? { 
          ...b, 
          ...updatedBill,
          isPaid: true, 
          status: 'PAID',
          paidDate: new Date().toISOString().split('T')[0]
        } : b));
        
        toast({
          title: 'Success',
          description: 'Bill marked as paid',
        });
      } else {
        throw new Error(response.data?.message || 'Failed to mark bill as paid');
      }
    } catch (error) {
      console.error('Failed to mark bill as paid:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to update bill',
        variant: 'destructive',
      });
    } finally {
      setPayingId(null);
    }
  };

  const handleEditClick = (bill) => {
    setEditingBill(bill);
    setEditBill({
      billName: bill.billName || bill.title || '',
      amount: bill.amount?.toString() || '',
      dueDate: bill.dueDate || '',
      category: bill.category || 'Utilities',
      description: bill.description || ''
    });
    setShowEditDialog(true);
  };

  const handleUpdateBill = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        billName: editBill.billName,
        amount: parseFloat(editBill.amount),
        dueDate: editBill.dueDate,
        category: editBill.category,
        description: editBill.description || ''
      };
      
      const response = await billService.update(editingBill.id, payload);
      
      if (response.data?.success) {
        const updatedBill = response.data.data;
        setBills(prev => prev.map(b => b.id === editingBill.id ? { 
          ...b, 
          ...updatedBill,
          status: getBillStatus(updatedBill)
        } : b));
        
        toast({
          title: 'Success',
          description: 'Bill updated successfully',
        });
        setShowEditDialog(false);
        setEditingBill(null);
      } else {
        throw new Error(response.data?.message || 'Failed to update bill');
      }
    } catch (error) {
      console.error('Failed to update bill:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to update bill',
        variant: 'destructive',
      });
    }
  };

  const getDaysUntil = (dueDate) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(dueDate);
    due.setHours(0, 0, 0, 0);
    const diffTime = due - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  const getStatusColor = (status, daysUntil) => {
    if (status === 'PAID') return 'text-green-400 bg-green-400/10';
    if (status === 'OVERDUE') return 'text-red-400 bg-red-400/10';
    if (daysUntil <= 3) return 'text-yellow-400 bg-yellow-400/10';
    return 'text-blue-400 bg-blue-400/10';
  };

  const getStatusIcon = (status, daysUntil) => {
    if (status === 'PAID') return <CheckCircle2 className="w-4 h-4" />;
    if (status === 'OVERDUE') return <AlertCircle className="w-4 h-4" />;
    return <Clock className="w-4 h-4" />;
  };

  const getStatusText = (status, daysUntil) => {
    if (status === 'PAID') return 'Paid';
    if (status === 'OVERDUE') return 'Overdue';
    if (daysUntil < 0) return 'Overdue';
    return 'Pending';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#7B6FC9]"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Bills</h1>
          <p className="text-gray-400 mt-1">Manage your recurring bills and payments</p>
        </div>
        <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
          <DialogTrigger asChild>
            <Button 
              className="bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white rounded-xl px-6 py-3 text-lg font-semibold shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-200"
            >
              <Plus className="w-5 h-5 mr-2" />
              Add Bill
            </Button>
          </DialogTrigger>
          <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
            <DialogHeader>
              <DialogTitle>Add New Bill</DialogTitle>
              <DialogDescription className="text-gray-400">
                Enter the bill details below
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleAddBill} className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label>Bill Name</Label>
                <Input
                  value={newBill.billName}
                  onChange={(e) => setNewBill({ ...newBill, billName: e.target.value })}
                  placeholder="e.g., Electric Bill"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Category</Label>
                <select
                  value={newBill.category}
                  onChange={(e) => setNewBill({ ...newBill, category: e.target.value })}
                  className="w-full bg-[#0F0F12]/60 border border-[#4A4560] text-white rounded-md px-3 py-2"
                >
                  <option value="Utilities">Utilities</option>
                  <option value="Rent">Rent</option>
                  <option value="Internet">Internet</option>
                  <option value="Mobile">Mobile</option>
                  <option value="Insurance">Insurance</option>
                  <option value="Entertainment">Entertainment</option>
                  <option value="Other">Other</option>
                </select>
              </div>
              <div className="space-y-2">
                <Label>Amount (₹)</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={newBill.amount}
                  onChange={(e) => setNewBill({ ...newBill, amount: e.target.value })}
                  placeholder="0.00"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Due Date</Label>
                <Input
                  type="date"
                  value={newBill.dueDate}
                  onChange={(e) => setNewBill({ ...newBill, dueDate: e.target.value })}
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <Button 
                type="submit" 
                className="w-full bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white py-3 text-lg font-semibold rounded-xl shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-200"
              >
                <Plus className="w-5 h-5 mr-2" />
                Add Bill
              </Button>
            </form>
          </DialogContent>
        </Dialog>

        {/* Edit Bill Dialog */}
        <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
          <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
            <DialogHeader>
              <DialogTitle>Edit Bill</DialogTitle>
              <DialogDescription className="text-gray-400">
                Update the bill details below
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleUpdateBill} className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label>Bill Name</Label>
                <Input
                  value={editBill.billName}
                  onChange={(e) => setEditBill({ ...editBill, billName: e.target.value })}
                  placeholder="e.g., Electric Bill"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Amount (₹)</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={editBill.amount}
                  onChange={(e) => setEditBill({ ...editBill, amount: e.target.value })}
                  placeholder="0.00"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Due Date</Label>
                <Input
                  type="date"
                  value={editBill.dueDate}
                  onChange={(e) => setEditBill({ ...editBill, dueDate: e.target.value })}
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Category</Label>
                <select
                  value={editBill.category}
                  onChange={(e) => setEditBill({ ...editBill, category: e.target.value })}
                  className="w-full bg-[#0F0F12]/60 border border-[#4A4560] text-white rounded-md px-3 py-2"
                >
                  <option value="Utilities">Utilities</option>
                  <option value="Rent">Rent</option>
                  <option value="Internet">Internet</option>
                  <option value="Mobile">Mobile</option>
                  <option value="Insurance">Insurance</option>
                  <option value="Entertainment">Entertainment</option>
                  <option value="Other">Other</option>
                </select>
              </div>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setShowEditDialog(false)}
                  className="flex-1 bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
                >
                  Cancel
                </Button>
                <Button 
                  type="submit" 
                  className="flex-1 bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white py-3 text-lg font-semibold rounded-xl"
                >
                  Update Bill
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <Receipt className="w-4 h-4" />
              Total Bills
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">{bills.length}</p>
          </CardContent>
        </Card>
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              Due Soon
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {bills.filter(b => b.status !== 'PAID' && getDaysUntil(b.dueDate) <= 3 && getDaysUntil(b.dueDate) >= 0).length}
            </p>
          </CardContent>
        </Card>
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <Calendar className="w-4 h-4" />
              Monthly Total
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {formatCurrency(bills.reduce((sum, b) => sum + (b.amount || 0), 0))}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Bills List */}
      <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
        <CardHeader>
          <CardTitle className="text-white">Upcoming Bills</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {bills.length === 0 ? (
              <div className="text-center py-8 text-gray-400">
                <Receipt className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>No bills added yet</p>
                <p className="text-sm">Add your first bill to get started</p>
              </div>
            ) : (
              bills.map((bill) => {
                const daysUntil = getDaysUntil(bill.dueDate);
                return (
                  <div
                    key={bill.id}
                    className="flex items-center justify-between p-4 bg-[#1E1E2A]/50 rounded-xl border border-[#3A3560]/50 hover:border-[#7B6FC9]/30 transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${getStatusColor(bill.status, daysUntil)}`}>
                        {getStatusIcon(bill.status, daysUntil)}
                      </div>
                      <div>
                        <p className="text-white font-medium">{bill.billName || bill.title}</p>
                        <p className="text-sm text-gray-400">{bill.category} • Due {daysUntil < 0 ? `${Math.abs(daysUntil)} days ago` : `in ${daysUntil} days`}</p>
                      </div>
                    </div>
                    <div className="text-right flex items-center gap-2">
                      <div>
                        <p className="text-white font-semibold">{formatCurrency(bill.amount)}</p>
                        <span className={`text-xs px-2 py-1 rounded-full ${getStatusColor(bill.status, daysUntil)}`}>
                          {getStatusText(bill.status, daysUntil)}
                        </span>
                      </div>
                      {bill.status !== 'PAID' && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleMarkAsPaid(bill.id)}
                          disabled={payingId === bill.id}
                          className="text-green-400 hover:text-green-300 hover:bg-green-500/10 transition-colors"
                          title="Mark as Paid"
                        >
                          {payingId === bill.id ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                          ) : (
                            <CheckCircle2 className="w-4 h-4" />
                          )}
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleEditClick(bill)}
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
                        onClick={() => handleDeleteBill(bill.id)}
                        disabled={deletingId === bill.id}
                        className="icon-btn icon-btn-delete"
                        title="Delete"
                      >
                        {deletingId === bill.id ? (
                          <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                          <Trash2 className="w-5 h-5" />
                        )}
                      </Button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Bills;
