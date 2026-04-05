import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Mail, Lock, Sparkles, TrendingUp, Shield, Wallet } from 'lucide-react';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(email, password);
      toast({
        title: "Welcome Back!",
        description: "Successfully logged in to TrackSmart",
        variant: "success"
      });
      setTimeout(() => {
        navigate('/dashboard');
      }, 1000);
    } catch (error) {
      console.error('Login error:', error);
      toast({
        title: "Login Failed",
        description: error.message || "Invalid credentials",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col md:flex-row">
      {/* LEFT SIDE - Branding */}
      <div className="w-full md:w-1/2 bg-gradient-to-br from-[#7B6FC9] via-[#6B5FB9] to-[#5A4FA8] text-white flex flex-col justify-center items-center p-8 md:p-16">
        <div className="max-w-md text-center">
          <div className="mb-8">
            <div className="w-20 h-20 mx-auto bg-white/20 rounded-2xl flex items-center justify-center mb-6 backdrop-blur-sm">
              <TrendingUp className="w-10 h-10 text-white" />
            </div>
            <h1 className="text-4xl md:text-5xl font-bold mb-4">Track Smart</h1>
            <p className="text-lg text-white/80 leading-relaxed">
              Manage your expenses smartly, track spending, analyze insights, and stay financially strong.
            </p>
          </div>
          
          <div className="grid grid-cols-3 gap-4 mt-8">
            <div className="bg-white/10 rounded-xl p-4 backdrop-blur-sm">
              <Wallet className="w-6 h-6 mx-auto mb-2" />
              <p className="text-sm text-white/80">Track Expenses</p>
            </div>
            <div className="bg-white/10 rounded-xl p-4 backdrop-blur-sm">
              <TrendingUp className="w-6 h-6 mx-auto mb-2" />
              <p className="text-sm text-white/80">Analytics</p>
            </div>
            <div className="bg-white/10 rounded-xl p-4 backdrop-blur-sm">
              <Shield className="w-6 h-6 mx-auto mb-2" />
              <p className="text-sm text-white/80">Secure</p>
            </div>
          </div>
        </div>
      </div>

      {/* RIGHT SIDE - Form */}
      <div className="w-full md:w-1/2 flex items-center justify-center bg-gradient-to-br from-[#0F0F12] via-[#151520] to-[#1A1A2E] p-4 md:p-8">
        <div className="w-full max-w-md">
          <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560] backdrop-blur-lg shadow-[0_8px_30px_rgba(123,111,201,0.25)] rounded-2xl overflow-hidden">
            <CardHeader className="text-center space-y-4 pb-6">
              <div className="mx-auto w-16 h-16 bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] rounded-full flex items-center justify-center shadow-lg">
                <Sparkles className="w-8 h-8 text-white" />
              </div>
              <div className="space-y-2">
                <CardTitle className="text-2xl font-bold text-white">Welcome Back</CardTitle>
                <p className="text-gray-400 text-sm">
                  Sign in to access your TrackSmart dashboard
                </p>
              </div>
            </CardHeader>
            <CardContent className="p-6 pt-0">
              <form onSubmit={handleSubmit} className="space-y-5">
                <div className="space-y-2">
                  <Label htmlFor="email" className="text-gray-300 text-sm font-medium">Email Address</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <Input
                      id="email"
                      type="email"
                      placeholder="Enter your email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      className="pl-11 h-11 bg-[#0F0F12]/60 border-[#4A4560] text-white placeholder-gray-500 focus:ring-2 focus:ring-[#7B6FC9] focus:border-[#7B6FC9] rounded-xl transition-all"
                    />
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="password" className="text-gray-300 text-sm font-medium">Password</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <Input
                      id="password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Enter your password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      className="pl-11 pr-11 h-11 bg-[#0F0F12]/60 border-[#4A4560] text-white placeholder-gray-500 focus:ring-2 focus:ring-[#7B6FC9] focus:border-[#7B6FC9] rounded-xl transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-3 text-gray-400 hover:text-gray-300 transition-colors"
                    >
                      {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  </div>
                </div>

                <Button 
                  type="submit" 
                  className="w-full h-11 bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white rounded-xl font-semibold shadow-lg hover:shadow-xl hover:scale-[1.02] transition-all duration-300"
                  disabled={loading}
                >
                  {loading ? (
                    <div className="flex items-center justify-center gap-2">
                      <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                      <span>Signing In...</span>
                    </div>
                  ) : (
                    "Sign In"
                  )}
                </Button>

                <div className="text-center space-y-3 pt-2">
                  <Link 
                    to="/forgot-password" 
                    className="text-[#9C90E8] hover:text-[#7B6FC9] text-sm transition-colors font-medium"
                  >
                    Forgot your password?
                  </Link>
                  <div className="text-gray-400 text-sm">
                    Don't have an account?{" "}
                    <Link 
                      to="/signup" 
                      className="text-[#9C90E8] hover:text-[#7B6FC9] font-semibold transition-colors"
                    >
                      Sign up
                    </Link>
                  </div>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default Login;
