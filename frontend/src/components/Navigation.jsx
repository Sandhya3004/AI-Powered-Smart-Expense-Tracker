import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { useTheme } from '@/context/ThemeContext';
import { 
  LayoutDashboard, 
  Wallet, 
  Receipt, 
  Bell, 
  Settings, 
  LogOut,
  Menu,
  X,
  Sun,
  Moon
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useState } from 'react';

const Navigation = () => {
  const { user, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/expenses', label: 'Expenses', icon: Wallet },
    { path: '/bills', label: 'Bills', icon: Receipt },
    { path: '/alerts', label: 'Alerts', icon: Bell },
    { path: '/settings', label: 'Settings', icon: Settings },
  ];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const toggleTheme = () => {
    setTheme(theme === 'light' ? 'dark' : 'light');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <>
      {/* Mobile Menu Button */}
      <div className="lg:hidden fixed top-4 left-4 z-50">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          className="bg-[#1E1E2A] border border-[#2C2C3A] text-white hover:bg-[#2A2540]"
        >
          {isMobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </Button>
      </div>

      {/* Sidebar Navigation */}
      <aside 
        className={`fixed left-0 top-0 h-full w-64 bg-gradient-to-b from-[#1E1E2A] to-[#151520] border-r border-[#2C2C3A] z-40 transition-transform duration-300 lg:translate-x-0 ${
          isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="p-6">
          {/* Logo */}
          <div className="flex items-center gap-3 mb-8">
            <div className="w-10 h-10 bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] rounded-lg flex items-center justify-center">
              <Wallet className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-white">TrackSmart</h1>
              <p className="text-xs text-gray-400">Expense Tracker</p>
            </div>
          </div>

          {/* User Info */}
          {user && (
            <div className="mb-6 p-4 bg-[#2A2540]/50 rounded-xl border border-[#3A3560]">
              <p className="text-sm text-gray-400">Welcome back,</p>
              <p className="text-white font-medium truncate">{user.name || user.email}</p>
            </div>
          )}

          {/* Navigation Links */}
          <nav className="space-y-2">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
                    isActive(item.path)
                      ? 'bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white shadow-lg'
                      : 'text-gray-400 hover:bg-[#2A2540] hover:text-white'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span className="font-medium">{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        {/* Logout & Theme Toggle */}
        <div className="absolute bottom-0 left-0 right-0 p-6 border-t border-[#2C2C3A] space-y-2">
          <Button
            onClick={toggleTheme}
            variant="ghost"
            className="w-full flex items-center gap-3 text-gray-400 hover:text-white hover:bg-[#2A2540] justify-start px-4"
          >
            {theme === 'light' ? <Moon className="w-5 h-5" /> : <Sun className="w-5 h-5" />}
            <span className="font-medium">{theme === 'light' ? 'Dark Mode' : 'Light Mode'}</span>
          </Button>
          <Button
            onClick={handleLogout}
            variant="ghost"
            className="w-full flex items-center gap-3 text-gray-400 hover:text-red-400 hover:bg-red-500/10 justify-start px-4"
          >
            <LogOut className="w-5 h-5" />
            <span className="font-medium">Logout</span>
          </Button>
        </div>
      </aside>

      {/* Mobile Overlay */}
      {isMobileMenuOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-30 lg:hidden"
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}
    </>
  );
};

export default Navigation;
