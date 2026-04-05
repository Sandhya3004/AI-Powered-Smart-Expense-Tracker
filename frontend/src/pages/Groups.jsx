import { useState, useEffect } from 'react';
import { useToast } from '@/hooks/use-toast';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogDescription } from '@/components/ui/dialog';
import { Plus, Users, Trash2, Loader2, ArrowLeft, Wallet, UserPlus, Edit2, X } from 'lucide-react';
import { api } from '@/api/api';
import { formatCurrency } from '@/utils/formatters';

const Groups = () => {
  const { user } = useAuth();
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [editingGroup, setEditingGroup] = useState(null);
  const [groupMembers, setGroupMembers] = useState([]);
  const [groupExpenses, setGroupExpenses] = useState([]);
  const [groupLoading, setGroupLoading] = useState(false);
  const [showAddMemberDialog, setShowAddMemberDialog] = useState(false);
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [newGroup, setNewGroup] = useState({
    name: '',
    description: ''
  });
  const [newExpense, setNewExpense] = useState({
    description: '',
    amount: '',
    category: 'Food'
  });
  const [showExpenseDialog, setShowExpenseDialog] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [removingMemberId, setRemovingMemberId] = useState(null);
  const { toast } = useToast();

  useEffect(() => {
    fetchGroups();
  }, []);

  const fetchGroups = async () => {
    try {
      setLoading(true);
      const response = await api.get('/groups');
      const groupsData = response.data?.data || response.data || [];
      setGroups(groupsData);
    } catch (error) {
      console.error('Failed to fetch groups:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to fetch groups',
        variant: 'destructive'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    
    // Validation before sending
    if (!newGroup.name || newGroup.name.trim() === '') {
      toast({
        title: 'Validation Error',
        description: 'Group name is required',
        variant: 'destructive'
      });
      return;
    }
    
    try {
      // Ensure payload format is correct
      const payload = {
        name: newGroup.name.trim(),
        description: newGroup.description?.trim() || '',
        type: 'other', // Default type
        memberEmails: [] // Empty array for now
      };
      
      console.log('Creating group with payload:', payload);
      
      const response = await api.post('/groups', payload);
      console.log('Group created response:', response.data);
      
      const createdGroup = response.data?.data || response.data;
      
      if (!createdGroup) {
        throw new Error('Invalid response from server');
      }
      
      setGroups(prev => [createdGroup, ...prev]);
      toast({
        title: 'Success',
        description: 'Group created successfully',
      });
      setShowAddDialog(false);
      setNewGroup({ name: '', description: '' });
    } catch (error) {
      console.error('Failed to create group:', error);
      console.error('Error response:', error.response?.data);
      
      toast({
        title: 'Error',
        description: error.response?.data?.message || error.message || 'Failed to create group',
        variant: 'destructive',
      });
    }
  };

  const handleDeleteGroup = async (id) => {
    try {
      setDeletingId(id);
      await api.delete(`/groups/${id}`);
      setGroups(prev => prev.filter(g => g.id !== id));
      toast({
        title: 'Success',
        description: 'Group deleted successfully',
      });
    } catch (error) {
      console.error('Failed to delete group:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to delete group',
        variant: 'destructive',
      });
    } finally {
      setDeletingId(null);
    }
  };

  const handleUpdateGroup = async (e) => {
    e.preventDefault();
    if (!editingGroup || !editingGroup.name.trim()) {
      toast({
        title: 'Validation Error',
        description: 'Group name is required',
        variant: 'destructive'
      });
      return;
    }

    try {
      const payload = {
        name: editingGroup.name.trim(),
        description: editingGroup.description?.trim() || ''
      };

      const response = await api.put(`/groups/${editingGroup.id}`, payload);
      
      if (response.data?.success) {
        const updatedGroup = response.data?.data;
        setGroups(prev => prev.map(g => g.id === updatedGroup.id ? updatedGroup : g));
        toast({
          title: 'Success',
          description: 'Group updated successfully',
        });
        setShowEditDialog(false);
        setEditingGroup(null);
      } else {
        throw new Error(response.data?.message || 'Failed to update group');
      }
    } catch (error) {
      console.error('Failed to update group:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to update group',
        variant: 'destructive',
      });
    }
  };

  const handleRemoveMember = async (memberId) => {
    if (!selectedGroup) return;

    try {
      setRemovingMemberId(memberId);
      await api.delete(`/groups/${selectedGroup.id}/members/${memberId}`);
      
      setGroupMembers(prev => prev.filter(m => m.id !== memberId));
      toast({
        title: 'Success',
        description: 'Member removed successfully',
      });
      
      await fetchGroupDetails(selectedGroup.id);
    } catch (error) {
      console.error('Failed to remove member:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to remove member',
        variant: 'destructive',
      });
    } finally {
      setRemovingMemberId(null);
    }
  };

  // Check if current user is group owner
  const isGroupOwner = (group) => {
    return group?.ownerId === user?.id || group?.createdBy === user?.email;
  };

  const handleSelectGroup = async (group) => {
    setSelectedGroup(group);
    setGroupLoading(true);
    
    try {
      // Fetch members and expenses in parallel
      const [membersRes, expensesRes] = await Promise.all([
        api.get(`/groups/${group.id}/members`),
        api.get(`/groups/${group.id}/expenses`)
      ]);
      
      const membersData = membersRes.data?.data || membersRes.data || [];
      const expensesData = expensesRes.data?.data || expensesRes.data || [];
      
      setGroupMembers(membersData);
      setGroupExpenses(expensesData);
      
      console.log('Fetched members:', membersData);
      console.log('Fetched expenses:', expensesData);
    } catch (error) {
      console.error('Failed to fetch group details:', error);
      toast({
        title: 'Error',
        description: 'Failed to load group details',
        variant: 'destructive'
      });
    } finally {
      setGroupLoading(false);
    }
  };

  const fetchGroupDetails = async (groupId) => {
    try {
      const [membersRes, expensesRes] = await Promise.all([
        api.get(`/groups/${groupId}/members`),
        api.get(`/groups/${groupId}/expenses`)
      ]);
      
      const membersData = membersRes.data?.data || membersRes.data || [];
      const expensesData = expensesRes.data?.data || expensesRes.data || [];
      
      setGroupMembers(membersData);
      setGroupExpenses(expensesData);
    } catch (error) {
      console.error('Failed to refresh group details:', error);
    }
  };

  const handleAddExpense = async (e) => {
    e.preventDefault();
    if (!selectedGroup) return;
    
    try {
      const payload = {
        description: newExpense.description,
        amount: parseFloat(newExpense.amount),
        category: newExpense.category
      };
      
      console.log('Adding expense with payload:', payload);
      
      const response = await api.post(`/groups/${selectedGroup.id}/expenses`, payload);
      
      if (response.data?.success) {
        const newExpenseData = response.data?.data;
        
        // Add to expenses list
        setGroupExpenses(prev => [newExpenseData, ...prev]);
        
        toast({
          title: 'Success',
          description: 'Expense added to group',
        });
        setShowExpenseDialog(false);
        setNewExpense({ description: '', amount: '', category: 'Food' });
        
        // Refresh to get updated data
        await fetchGroupDetails(selectedGroup.id);
      } else {
        throw new Error(response.data?.message || 'Failed to add expense');
      }
    } catch (error) {
      console.error('Failed to add expense:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || error.message || 'Failed to add expense',
        variant: 'destructive',
      });
    }
  };

  const handleAddMember = async (e) => {
    e.preventDefault();
    if (!selectedGroup || !newMemberEmail.trim()) return;
    
    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(newMemberEmail.trim())) {
      toast({
        title: 'Validation Error',
        description: 'Please enter a valid email address',
        variant: 'destructive'
      });
      return;
    }
    
    try {
      const payload = {
        email: newMemberEmail.trim()
      };
      
      console.log('Adding member with payload:', payload);
      
      const response = await api.post(`/groups/${selectedGroup.id}/members`, payload);
      
      if (response.data?.success) {
        const newMember = response.data?.data;
        
        // Add to members list
        setGroupMembers(prev => [...prev, newMember]);
        
        toast({
          title: 'Success',
          description: 'Member added to group',
        });
        setShowAddMemberDialog(false);
        setNewMemberEmail('');
        
        // Refresh members list
        await fetchGroupDetails(selectedGroup.id);
      } else {
        throw new Error(response.data?.message || 'Failed to add member');
      }
    } catch (error) {
      console.error('Failed to add member:', error);
      console.error('Error response:', error.response?.data);
      
      const errorMsg = error.response?.data?.message || error.message || 'Failed to add member';
      
      // Check if user doesn't exist
      if (errorMsg.includes('User not found')) {
        toast({
          title: 'User Not Found',
          description: 'This email is not registered. Please ask them to sign up first.',
          variant: 'destructive',
        });
      } else {
        toast({
          title: 'Error',
          description: errorMsg,
          variant: 'destructive',
        });
      }
    }
  };

  const calculateSplit = () => {
    if (!groupExpenses || groupExpenses.length === 0) return [];
    
    const total = groupExpenses.reduce((sum, e) => sum + (parseFloat(e.amount) || 0), 0);
    const memberCount = groupMembers?.length || 1;
    const perPerson = total / memberCount;
    
    return groupMembers?.map(member => {
      // Calculate how much this member paid
      const paid = groupExpenses
        .filter(e => e.paidBy === member.userEmail || e.paidBy === member.userName || e.createdBy === member.userEmail)
        .reduce((sum, e) => sum + (parseFloat(e.amount) || 0), 0);
      
      return {
        ...member,
        name: member.userName || member.userEmail,
        owed: perPerson,
        paid: paid,
        balance: paid - perPerson
      };
    }) || [];
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#7B6FC9]"></div>
      </div>
    );
  }

  // Group Detail View
  if (selectedGroup) {
    const splitData = calculateSplit();
    const totalExpenses = groupExpenses.reduce((sum, e) => sum + (parseFloat(e.amount) || 0), 0);
    
    if (groupLoading) {
      return (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#7B6FC9]"></div>
        </div>
      );
    }
    
    return (
      <div className="space-y-6 p-6">
        {/* Header */}
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            onClick={() => setSelectedGroup(null)}
            className="text-gray-400 hover:text-white"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            Back
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-white">{selectedGroup.name}</h1>
            <p className="text-gray-400">{selectedGroup.description}</p>
          </div>
        </div>

        {/* Stats */}
        <div className="grid gap-4 md:grid-cols-3">
          <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
            <CardHeader className="pb-3">
              <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
                <Wallet className="w-4 h-4" />
                Total Expenses
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold text-white">
                {formatCurrency(totalExpenses)}
              </p>
            </CardContent>
          </Card>
          <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
            <CardHeader className="pb-3">
              <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
                <Users className="w-4 h-4" />
                Members
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold text-white">{groupMembers.length}</p>
            </CardContent>
          </Card>
          <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
            <CardHeader className="pb-3">
              <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
                <Wallet className="w-4 h-4" />
                Per Person
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold text-white">
                {formatCurrency(totalExpenses / (groupMembers.length || 1))}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Add Expense Button */}
        <div className="flex justify-end">
          <Dialog open={showExpenseDialog} onOpenChange={setShowExpenseDialog}>
            <DialogTrigger asChild>
              <Button 
                className="bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white rounded-xl px-6 py-3 text-lg font-semibold shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-200"
              >
                <Plus className="w-5 h-5 mr-2" />
                Add Group Expense
              </Button>
            </DialogTrigger>
            <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
              <DialogHeader>
                <DialogTitle>Add Group Expense</DialogTitle>
                <DialogDescription className="text-gray-400">
                  Add an expense to split with group members
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleAddExpense} className="space-y-4 mt-4">
                <div className="space-y-2">
                  <Label>Description</Label>
                  <Input
                    value={newExpense.description}
                    onChange={(e) => setNewExpense({ ...newExpense, description: e.target.value })}
                    placeholder="e.g., Dinner"
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                    required
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
                <div className="space-y-2">
                  <Label>Category</Label>
                  <select
                    value={newExpense.category}
                    onChange={(e) => setNewExpense({ ...newExpense, category: e.target.value })}
                    className="w-full bg-[#0F0F12]/60 border border-[#4A4560] text-white rounded-md px-3 py-2"
                  >
                    <option value="Food">Food</option>
                    <option value="Transport">Transport</option>
                    <option value="Entertainment">Entertainment</option>
                    <option value="Shopping">Shopping</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
                <Button 
                  type="submit" 
                  className="w-full bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white py-3 text-lg font-semibold rounded-xl shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-200"
                >
                  <Plus className="w-5 h-5 mr-2" />
                  Add Expense
                </Button>
              </form>
            </DialogContent>
          </Dialog>

          {/* Add Member Dialog */}
          <Dialog open={showAddMemberDialog} onOpenChange={setShowAddMemberDialog}>
            <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
              <DialogHeader>
                <DialogTitle>Add Member to Group</DialogTitle>
                <DialogDescription className="text-gray-400">
                  Enter the email of the user you want to add to this group
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleAddMember} className="space-y-4 mt-4">
                <div className="space-y-2">
                  <Label>Member Email</Label>
                  <Input
                    type="email"
                    value={newMemberEmail}
                    onChange={(e) => setNewMemberEmail(e.target.value)}
                    placeholder="e.g., friend@example.com"
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                    required
                  />
                </div>
                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setShowAddMemberDialog(false)}
                    className="flex-1 bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
                  >
                    Cancel
                  </Button>
                  <Button 
                    type="submit" 
                    className="flex-1 bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white py-3 text-lg font-semibold rounded-xl"
                  >
                    <UserPlus className="w-5 h-5 mr-2" />
                    Add Member
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* Members & Split */}
        <div className="grid gap-6 md:grid-cols-2">
          <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle className="text-white flex items-center gap-2">
                <Users className="w-5 h-5" />
                Members
              </CardTitle>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowAddMemberDialog(true)}
                className="text-[#9C90E8] hover:text-[#7B6FC9] hover:bg-[#7B6FC9]/10"
              >
                <UserPlus className="w-4 h-4 mr-1" />
                Add Member
              </Button>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {splitData.map((member, index) => (
                  <div key={index} className="flex items-center justify-between p-3 bg-[#1E1E2A]/50 rounded-xl">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] flex items-center justify-center text-white font-semibold">
                        {member.name?.charAt(0) || 'U'}
                      </div>
                      <div>
                        <p className="text-white font-medium">{member.name || member.email}</p>
                        <p className="text-sm text-gray-400">
                          Paid: {formatCurrency(member.paid)} • Owed: {formatCurrency(member.owed)}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`text-sm font-semibold ${member.paid >= member.owed ? 'text-green-400' : 'text-red-400'}`}>
                        {member.paid >= member.owed ? 'Settled' : `Owes ${formatCurrency(member.owed - member.paid)}`}
                      </span>
                      {isGroupOwner(selectedGroup) && member.id && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleRemoveMember(member.id)}
                          disabled={removingMemberId === member.id}
                          className="icon-btn icon-btn-delete"
                          title="Remove"
                        >
                          {removingMemberId === member.id ? (
                            <Loader2 className="w-5 h-5 animate-spin" />
                          ) : (
                            <X className="w-5 h-5" />
                          )}
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
            <CardHeader>
              <CardTitle className="text-white">Group Expenses</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {groupExpenses.length === 0 ? (
                  <div className="text-center py-8 text-gray-400">
                    <Wallet className="w-12 h-12 mx-auto mb-3 opacity-50" />
                    <p>No expenses yet</p>
                    <p className="text-sm">Add your first group expense</p>
                  </div>
                ) : (
                  groupExpenses.map((expense, index) => (
                    <div key={expense.id || index} className="flex items-center justify-between p-3 bg-[#1E1E2A]/50 rounded-xl">
                      <div>
                        <p className="text-white font-medium">{expense.description}</p>
                        <p className="text-sm text-gray-400">{expense.category} • {expense.paidBy || 'Unknown'}</p>
                      </div>
                      <span className="text-white font-semibold">{formatCurrency(expense.amount)}</span>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  // Groups List View
  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Groups</h1>
          <p className="text-gray-400 mt-1">Manage shared expenses with friends</p>
        </div>
        <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
          <DialogTrigger asChild>
            <Button 
              className="bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white rounded-xl px-6 py-3 text-lg font-semibold shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-200"
            >
              <Plus className="w-5 h-5 mr-2" />
              Create Group
            </Button>
          </DialogTrigger>
          <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
            <DialogHeader>
              <DialogTitle>Create New Group</DialogTitle>
              <DialogDescription className="text-gray-400">
                Create a group to split expenses with friends
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleCreateGroup} className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label>Group Name</Label>
                <Input
                  value={newGroup.name}
                  onChange={(e) => setNewGroup({ ...newGroup, name: e.target.value })}
                  placeholder="e.g., Trip to Goa"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Description</Label>
                <Input
                  value={newGroup.description}
                  onChange={(e) => setNewGroup({ ...newGroup, description: e.target.value })}
                  placeholder="e.g., Weekend trip expenses"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                />
              </div>
              <Button 
                type="submit" 
                className="w-full bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white py-3 text-lg font-semibold rounded-xl shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-200"
              >
                <Plus className="w-5 h-5 mr-2" />
                Create Group
              </Button>
            </form>
          </DialogContent>
        </Dialog>

        {/* Edit Group Dialog */}
        <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
          <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
            <DialogHeader>
              <DialogTitle>Edit Group</DialogTitle>
              <DialogDescription className="text-gray-400">
                Update group details
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleUpdateGroup} className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label>Group Name</Label>
                <Input
                  value={editingGroup?.name || ''}
                  onChange={(e) => setEditingGroup(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="e.g., Trip to Goa"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Description</Label>
                <Input
                  value={editingGroup?.description || ''}
                  onChange={(e) => setEditingGroup(prev => ({ ...prev, description: e.target.value }))}
                  placeholder="e.g., Weekend trip expenses"
                  className="bg-[#0F0F12]/60 border-[#4A4560] text-white"
                />
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
                  <Edit2 className="w-5 h-5 mr-2" />
                  Update Group
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
              <Users className="w-4 h-4" />
              Total Groups
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">{groups.length}</p>
          </CardContent>
        </Card>
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <Wallet className="w-4 h-4" />
              Total Shared
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {formatCurrency(
                groups.reduce((sum, g) => 
                  sum + (g.expenses?.reduce((eSum, e) => eSum + (e.amount || 0), 0) || 0), 0
                )
              )}
            </p>
          </CardContent>
        </Card>
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <UserPlus className="w-4 h-4" />
              Total Members
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {groups.reduce((sum, g) => sum + (g.members?.length || 0), 0)}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Groups Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {groups.length === 0 ? (
          <div className="col-span-full text-center py-12 text-gray-400">
            <Users className="w-16 h-16 mx-auto mb-4 opacity-50" />
            <p className="text-lg">No groups yet</p>
            <p className="text-sm">Create your first group to start splitting expenses</p>
          </div>
        ) : (
          groups.map((group) => (
            <Card 
              key={group.id} 
              className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560] hover:border-[#7B6FC9]/50 transition-all cursor-pointer group"
              onClick={() => handleSelectGroup(group)}
            >
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] flex items-center justify-center">
                    <Users className="w-6 h-6 text-white" />
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      {isGroupOwner(group) && (
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={(e) => {
                          e.stopPropagation();
                          setEditingGroup(group);
                          setShowEditDialog(true);
                        }}
                        className="icon-btn icon-btn-edit"
                        title="Edit"
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                        </svg>
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteGroup(group.id);
                      }}
                      disabled={deletingId === group.id}
                      className="icon-btn icon-btn-delete"
                      title="Delete"
                    >
                      {deletingId === group.id ? (
                        <Loader2 className="w-5 h-5 animate-spin" />
                      ) : (
                        <Trash2 className="w-5 h-5" />
                      )}
                    </Button>
                  </div>
                </div>
                <CardTitle className="text-white mt-3">{group.name}</CardTitle>
                <p className="text-sm text-gray-400">{group.description}</p>
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-gray-400">
                    <Users className="w-4 h-4" />
                    <span>{group.members?.length || 0} members</span>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-gray-400">Total</p>
                    <p className="text-lg font-semibold text-white">
                      {formatCurrency(
                        group.expenses?.reduce((sum, e) => sum + (e.amount || 0), 0) || 0
                      )}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};

export default Groups;
