import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import Header from "./Header";
import AIChatAssistant from "@/components/ai/AIChatAssistant";

const Layout = () => {
  return (
    <div className="flex h-screen bg-gradient-to-br from-[#0F0F12] via-[#151520] to-[#1A1A2E]">
      {/* Sidebar with glassmorphism effect */}
      <div className="relative">
        <Sidebar />
      </div>
      
      {/* Main content area */}
      <div className="flex-1 flex flex-col overflow-hidden relative">
        {/* Header with backdrop blur */}
        <div className="relative z-10">
          <Header />
        </div>
        
        {/* Main content with dark background */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 relative bg-gradient-to-br from-[#0F0F12] via-[#151520] to-[#1A1A2E]">
          {/* Subtle grid pattern overlay */}
          <div className="absolute inset-0 opacity-[0.02]" style={{
            backgroundImage: `linear-gradient(rgba(123, 111, 201, 0.3) 1px, transparent 1px), linear-gradient(90deg, rgba(123, 111, 201, 0.3) 1px, transparent 1px)`,
            backgroundSize: '50px 50px'
          }}></div>
          
          {/* Content with glassmorphism card effect */}
          <div className="relative z-10">
            <Outlet />
          </div>
        </main>
      </div>
      
      {/* Global AI Assistant */}
      <AIChatAssistant />
    </div>
  );
};

export default Layout;
