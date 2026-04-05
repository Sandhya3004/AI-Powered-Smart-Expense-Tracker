import { NavLink, useNavigate } from "react-router-dom";
import { 
  LayoutDashboard, 
  Wallet, 
  Receipt, 
  Settings, 
  LogOut,
  Menu,
  Brain,
  Target,
  Users
} from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";

const navigation = [
  { name: "Dashboard", to: "/dashboard", icon: LayoutDashboard },
  { name: "Expenses", to: "/expenses", icon: Wallet },
  { name: "Bills", to: "/bills", icon: Receipt },
  { name: "Groups", to: "/groups", icon: Users },
  { name: "Insights", to: "/insights", icon: Brain },
  { name: "Settings", to: "/settings", icon: Settings },
];

const Sidebar = () => {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <aside
      className={cn(
        "bg-gradient-to-b from-[#1E1E2A] to-[#151520] backdrop-blur-xl border-r border-[#2C2C3A] transition-all duration-300 flex flex-col h-screen shadow-2xl",
        collapsed ? "w-20" : "w-64"
      )}
    >
      {/* Logo Section */}
      <div className="flex items-center justify-between h-16 px-4 border-b border-[#2C2C3A]">
        {!collapsed && (
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] rounded-lg flex items-center justify-center shadow-lg">
              <Target className="w-5 h-5 text-white" />
            </div>
            <div>
              <span className="font-bold text-lg text-white">
                TrackSmart
              </span>
              <p className="text-xs text-gray-400">Expense Tracker</p>
            </div>
          </div>
        )}
        {collapsed && (
          <div className="w-full flex justify-center">
            <div className="w-10 h-10 bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] rounded-lg flex items-center justify-center">
              <Target className="w-5 h-5 text-white" />
            </div>
          </div>
        )}
        <button 
          onClick={() => setCollapsed(!collapsed)} 
          className="p-2 rounded-lg hover:bg-[#2A2540] text-gray-400 hover:text-white transition-colors"
        >
          <Menu size={20} />
        </button>
      </div>

      {/* User Info */}
      {!collapsed && user && (
        <div className="mx-4 mt-4 p-3 bg-[#2A2540]/60 rounded-xl border border-[#3A3560]">
          <p className="text-xs text-gray-400">Welcome back,</p>
          <p className="text-sm text-white font-medium truncate">{user.name || user.email}</p>
        </div>
      )}

      {/* Navigation */}
      <nav className="mt-6 px-2 flex-1">
        {navigation.map((item) => (
          <NavLink
            key={item.name}
            to={item.to}
            className={({ isActive }) =>
              cn(
                "group flex items-center px-3 py-3 rounded-xl text-gray-400 hover:text-white hover:bg-[#2A2540] transition-all duration-200 mb-2",
                isActive && "bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] text-white shadow-lg shadow-[#7B6FC9]/25"
              )
            }
          >
            {({ isActive }) => (
              <>
                <div className={cn(
                  "flex items-center justify-center w-9 h-9 rounded-lg transition-colors",
                  isActive ? 
                    "bg-white/20 text-white" : 
                    "bg-[#2A2540]/50 text-gray-400 group-hover:bg-[#3A3560] group-hover:text-white"
                )}>
                  <item.icon size={18} />
                </div>
                {!collapsed && (
                  <span className="ml-3 font-medium">{item.name}</span>
                )}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* Bottom Section - Logout */}
      <div className="p-4 border-t border-[#2C2C3A]">
        <Button
          onClick={handleLogout}
          variant="ghost"
          className={cn(
            "w-full flex items-center gap-3 text-gray-400 hover:text-red-400 hover:bg-red-500/10 justify-start",
            collapsed && "justify-center px-2"
          )}
        >
          <LogOut className="w-5 h-5" />
          {!collapsed && <span className="font-medium">Logout</span>}
        </Button>
      </div>
    </aside>
  );
};

export default Sidebar;
