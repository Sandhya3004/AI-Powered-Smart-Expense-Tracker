import { useState, useRef } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import ToggleSwitch from '@/components/ui/ToggleSwitch';
import { api } from '@/api/api';
import { useToast } from '@/hooks/use-toast';
import { Loader2, Camera, Moon, Sun, User, Lock, Bell, Palette } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogDescription } from '@/components/ui/dialog';

const Settings = () => {
  const { user, setUser } = useAuth();
  const { toast } = useToast();
  const fileInputRef = useRef(null);

  // Profile states
  const [profile, setProfile] = useState({
    name: user?.name || '',
    email: user?.email || ''
  });
  const [isEditingProfile, setIsEditingProfile] = useState(false);
  const [isUploadingImage, setIsUploadingImage] = useState(false);

  // Password states
  const [passwordData, setPasswordData] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);

  // Preferences states
  const [preferences, setPreferences] = useState({
    notifications: user?.notificationsEnabled ?? true,
    budgetAlerts: user?.budgetAlerts ?? true,
    darkMode: document.documentElement.classList.contains('dark')
  });
  const [isSavingPreferences, setIsSavingPreferences] = useState(false);

  // Handle profile image upload
  const handleImageUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    console.log('Selected file:', file.name, 'Type:', file.type, 'Size:', file.size);

    // Validate file type
    if (!file.type.startsWith('image/')) {
      toast({
        title: 'Invalid file type',
        description: 'Please upload an image file (JPG, PNG, GIF)',
        variant: 'destructive'
      });
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      toast({
        title: 'File too large',
        description: 'Image must be less than 5MB',
        variant: 'destructive'
      });
      return;
    }

    setIsUploadingImage(true);
    try {
      const formData = new FormData();
      formData.append('file', file);

      console.log('Uploading to /auth/upload-profile...');
      
      const response = await api.post('/auth/upload-profile', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      console.log('Upload response:', response.data);
      
      // Extract image URL from response
      let imageUrl = null;
      if (response.data?.success && response.data?.data) {
        imageUrl = response.data.data;
      } else if (typeof response.data === 'string') {
        imageUrl = response.data;
      } else if (response.data?.data) {
        imageUrl = response.data.data;
      }

      if (!imageUrl) {
        throw new Error('No image URL returned from server');
      }

      console.log('Image URL:', imageUrl);
      
      // Update user context with new image
      setUser(prev => ({ ...prev, profileImage: imageUrl }));

      toast({
        title: 'Success',
        description: 'Profile image updated successfully'
      });
    } catch (error) {
      console.error('Failed to upload image:', error);
      console.error('Error response:', error.response?.data);
      toast({
        title: 'Error',
        description: error.response?.data?.message || error.message || 'Failed to upload image',
        variant: 'destructive'
      });
    } finally {
      setIsUploadingImage(false);
      // Clear the input so same file can be selected again
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  // Handle profile update
  const handleUpdateProfile = async () => {
    if (!profile.name.trim() || !profile.email.trim()) {
      toast({
        title: 'Validation Error',
        description: 'Name and email are required',
        variant: 'destructive'
      });
      return;
    }

    try {
      const response = await api.put('/auth/profile', {
        name: profile.name.trim(),
        email: profile.email.trim(),
        profileImage: user?.profileImage || null
      });

      // Update user context
      setUser(prev => ({ ...prev, name: profile.name, email: profile.email }));

      toast({
        title: 'Success',
        description: response.data?.message || 'Profile updated successfully'
      });
      setIsEditingProfile(false);
    } catch (error) {
      console.error('Failed to update profile:', error);
      if (error.response?.data?.message) {
        toast({
          title: 'Error',
          description: error.response.data.message,
          variant: 'destructive'
        });
      } else {
        toast({
          title: 'Error',
          description: 'Failed to update profile',
          variant: 'destructive'
        });
      }
    }
  };

  // Handle password change
  const handleChangePassword = async () => {
    // Validation
    if (!passwordData.oldPassword || !passwordData.newPassword || !passwordData.confirmPassword) {
      toast({
        title: 'Validation Error',
        description: 'All password fields are required',
        variant: 'destructive'
      });
      return;
    }

    if (passwordData.newPassword.length < 6) {
      toast({
        title: 'Validation Error',
        description: 'New password must be at least 6 characters',
        variant: 'destructive'
      });
      return;
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      toast({
        title: 'Validation Error',
        description: 'New passwords do not match',
        variant: 'destructive'
      });
      return;
    }

    setIsChangingPassword(true);
    try {
      await api.post('/auth/change-password', {
        oldPassword: passwordData.oldPassword,
        newPassword: passwordData.newPassword
      });

      toast({
        title: 'Success',
        description: 'Password changed successfully'
      });

      // Reset form and close dialog
      setPasswordData({ oldPassword: '', newPassword: '', confirmPassword: '' });
      setShowPasswordDialog(false);
    } catch (error) {
      console.error('Failed to change password:', error);
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to change password. Please check your old password.',
        variant: 'destructive'
      });
    } finally {
      setIsChangingPassword(false);
    }
  };

  // Handle preferences toggle
  const handleTogglePreference = async (key, value) => {
    const newPreferences = { ...preferences, [key]: value };
    setPreferences(newPreferences);

    // Handle theme toggle immediately
    if (key === 'darkMode') {
      if (value) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    }

    setIsSavingPreferences(true);
    try {
      await api.post('/auth/settings', {
        notificationsEnabled: newPreferences.notifications,
        pushNotifications: newPreferences.notifications,
        budgetAlerts: newPreferences.budgetAlerts,
        darkMode: newPreferences.darkMode
      });

      toast({
        title: 'Success',
        description: 'Preferences saved'
      });
    } catch (error) {
      console.error('Failed to save preferences:', error);
      if (error.response?.data?.message) {
        toast({
          title: 'Error',
          description: error.response.data.message,
          variant: 'destructive'
        });
      } else {
        toast({
          title: 'Error',
          description: 'Failed to save preferences',
          variant: 'destructive'
        });
      }
      // Revert on error
      setPreferences(preferences);
    } finally {
      setIsSavingPreferences(false);
    }
  };

  return (
    <div className="space-y-6 p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Settings</h1>
          <p className="text-gray-400 mt-1">Manage your account and preferences</p>
        </div>
      </div>

      {/* Profile Section */}
      <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
        <CardHeader>
          <CardTitle className="text-white flex items-center gap-2">
            <User className="w-5 h-5 text-[#7B6FC9]" />
            Profile Information
          </CardTitle>
          <CardDescription className="text-gray-400">
            Manage your profile image and personal details
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Profile Image */}
          <div className="flex items-center gap-4">
            <div className="relative">
              <div className="w-24 h-24 rounded-full bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] flex items-center justify-center overflow-hidden">
                {user?.profileImage ? (
                  <img 
                    src={user.profileImage} 
                    alt="Profile" 
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <span className="text-3xl font-bold text-white">
                    {user?.name?.charAt(0)?.toUpperCase() || 'U'}
                  </span>
                )}
              </div>
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploadingImage}
                className="absolute bottom-0 right-0 w-8 h-8 bg-[#7B6FC9] rounded-full flex items-center justify-center hover:bg-[#6B5FB9] transition-colors"
              >
                {isUploadingImage ? (
                  <Loader2 className="w-4 h-4 text-white animate-spin" />
                ) : (
                  <Camera className="w-4 h-4 text-white" />
                )}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleImageUpload}
                className="hidden"
              />
            </div>
            <div>
              <h3 className="text-white font-medium">{user?.name}</h3>
              <p className="text-gray-400 text-sm">{user?.email}</p>
            </div>
          </div>

          {/* Profile Edit Form */}
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <Label htmlFor="name" className="text-gray-300">Name</Label>
              <Input
                id="name"
                value={profile.name}
                onChange={(e) => setProfile(prev => ({ ...prev, name: e.target.value }))}
                disabled={!isEditingProfile}
                className="bg-[#0F0F12]/60 border-[#4A4560] text-white mt-1"
              />
            </div>
            <div>
              <Label htmlFor="email" className="text-gray-300">Email</Label>
              <Input
                id="email"
                type="email"
                value={profile.email}
                onChange={(e) => setProfile(prev => ({ ...prev, email: e.target.value }))}
                disabled={!isEditingProfile}
                className="bg-[#0F0F12]/60 border-[#4A4560] text-white mt-1"
              />
            </div>
          </div>

          <div className="flex gap-2">
            {isEditingProfile ? (
              <>
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsEditingProfile(false);
                    setProfile({ name: user?.name || '', email: user?.email || '' });
                  }}
                  className="bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleUpdateProfile}
                  className="bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white"
                >
                  Save Changes
                </Button>
              </>
            ) : (
              <Button
                onClick={() => setIsEditingProfile(true)}
                variant="outline"
                className="bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
              >
                Edit Profile
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Change Password Section */}
      <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
        <CardHeader>
          <CardTitle className="text-white flex items-center gap-2">
            <Lock className="w-5 h-5 text-[#7B6FC9]" />
            Security
          </CardTitle>
          <CardDescription className="text-gray-400">
            Update your password to keep your account secure
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Dialog open={showPasswordDialog} onOpenChange={setShowPasswordDialog}>
            <DialogTrigger asChild>
              <Button
                variant="outline"
                className="bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
              >
                Change Password
              </Button>
            </DialogTrigger>
            <DialogContent className="bg-[#2A2540] border-[#3A3560] text-white">
              <DialogHeader>
                <DialogTitle>Change Password</DialogTitle>
                <DialogDescription className="text-gray-400">
                  Update your password to keep your account secure
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 mt-4">
                <div>
                  <Label htmlFor="oldPassword" className="text-gray-300">Current Password</Label>
                  <Input
                    id="oldPassword"
                    type="password"
                    value={passwordData.oldPassword}
                    onChange={(e) => setPasswordData(prev => ({ ...prev, oldPassword: e.target.value }))}
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white mt-1"
                    placeholder="Enter current password"
                  />
                </div>
                <div>
                  <Label htmlFor="newPassword" className="text-gray-300">New Password</Label>
                  <Input
                    id="newPassword"
                    type="password"
                    value={passwordData.newPassword}
                    onChange={(e) => setPasswordData(prev => ({ ...prev, newPassword: e.target.value }))}
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white mt-1"
                    placeholder="Enter new password (min 6 characters)"
                  />
                </div>
                <div>
                  <Label htmlFor="confirmPassword" className="text-gray-300">Confirm New Password</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={passwordData.confirmPassword}
                    onChange={(e) => setPasswordData(prev => ({ ...prev, confirmPassword: e.target.value }))}
                    className="bg-[#0F0F12]/60 border-[#4A4560] text-white mt-1"
                    placeholder="Confirm new password"
                  />
                </div>
                <div className="flex gap-2 pt-2">
                  <Button
                    variant="outline"
                    onClick={() => setShowPasswordDialog(false)}
                    className="flex-1 bg-[#2A2540] border-[#3A3560] text-white hover:bg-[#3A3560]"
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleChangePassword}
                    disabled={isChangingPassword}
                    className="flex-1 bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white"
                  >
                    {isChangingPassword ? (
                      <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Changing...</>
                    ) : (
                      'Change Password'
                    )}
                  </Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
        </CardContent>
      </Card>

      {/* Preferences Section */}
      <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
        <CardHeader>
          <CardTitle className="text-white flex items-center gap-2">
            <Palette className="w-5 h-5 text-[#7B6FC9]" />
            Preferences
          </CardTitle>
          <CardDescription className="text-gray-400">
            Customize your app experience
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Notifications Toggle */}
          <div className="flex items-center justify-between p-4 bg-[#1E1E2A]/50 rounded-xl">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[#7B6FC9]/20 flex items-center justify-center">
                <Bell className="w-5 h-5 text-[#7B6FC9]" />
              </div>
              <div>
                <p className="text-white font-medium">Notifications</p>
                <p className="text-sm text-gray-400">Receive email notifications</p>
              </div>
            </div>
            <ToggleSwitch
              enabled={preferences.notifications}
              onChange={(checked) => handleTogglePreference('notifications', checked)}
              disabled={isSavingPreferences}
            />
          </div>

          {/* Budget Alerts Toggle */}
          <div className="flex items-center justify-between p-4 bg-[#1E1E2A]/50 rounded-xl">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[#4ADE80]/20 flex items-center justify-center">
                <Bell className="w-5 h-5 text-[#4ADE80]" />
              </div>
              <div>
                <p className="text-white font-medium">Budget Alerts</p>
                <p className="text-sm text-gray-400">Get alerts when budget is exceeded</p>
              </div>
            </div>
            <ToggleSwitch
              enabled={preferences.budgetAlerts}
              onChange={(checked) => handleTogglePreference('budgetAlerts', checked)}
              disabled={isSavingPreferences}
            />
          </div>

          {/* Dark Mode Toggle */}
          <div className="flex items-center justify-between p-4 bg-[#1E1E2A]/50 rounded-xl">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[#FBBF24]/20 flex items-center justify-center">
                {preferences.darkMode ? (
                  <Moon className="w-5 h-5 text-[#FBBF24]" />
                ) : (
                  <Sun className="w-5 h-5 text-[#FBBF24]" />
                )}
              </div>
              <div>
                <p className="text-white font-medium">Dark Mode</p>
                <p className="text-sm text-gray-400">Toggle dark/light theme</p>
              </div>
            </div>
            <ToggleSwitch
              enabled={preferences.darkMode}
              onChange={(checked) => handleTogglePreference('darkMode', checked)}
              disabled={isSavingPreferences}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Settings;
