import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Mail, Lock, User, Sparkles, TrendingUp, Shield, Wallet } from 'lucide-react';

const Signup = () => {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [agreeToTerms, setAgreeToTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation for empty fields
    if (!formData.fullName.trim() || !formData.email.trim() || !formData.password) {
      toast({
        title: "Validation Error",
        description: "All fields are required",
        variant: "destructive",
      });
      return;
    }

    if (!agreeToTerms) {
      toast({
        title: "Terms Required",
        description: "Please agree to the Terms of Service",
        variant: "destructive",
      });
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      toast({
        title: "Password Mismatch",
        description: "Passwords do not match",
        variant: "destructive",
      });
      return;
    }

    // Debug log
    console.log("Signup Payload:", {
      name: formData.fullName,
      email: formData.email,
      password: formData.password ? "[PRESENT]" : "[MISSING]"
    });

    setLoading(true);
    try {
      await register(formData.fullName, formData.email, formData.password);
      toast({
        title: "Account Created!",
        description: "Welcome to Track Smart! Your account has been created successfully.",
        variant: "success"
      });
      setTimeout(() => {
        navigate('/dashboard');
      }, 1000);
    } catch (error) {
      console.error('Signup error:', error);
      toast({
        title: "Signup Failed",
        description: error.message || "Failed to create account",
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
              Start your financial journey today. Track expenses, analyze spending, and achieve your goals.
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
                <User className="w-8 h-8 text-white" />
              </div>
              <div className="space-y-2">
                <CardTitle className="text-2xl font-bold text-white">Create Account</CardTitle>
                <p className="text-gray-400 text-sm">
                  Join Track Smart and start your financial journey
                </p>
              </div>
            </CardHeader>
            <CardContent className="p-6 pt-0">
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="fullName" className="text-gray-300 text-sm font-medium">Full Name</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <Input
                      id="fullName"
                      name="fullName"
                      type="text"
                      placeholder="Enter your full name"
                      value={formData.fullName}
                      onChange={handleChange}
                      required
                      className="pl-11 h-11 bg-[#0F0F12]/60 border-[#4A4560] text-white placeholder-gray-500 focus:ring-2 focus:ring-[#7B6FC9] focus:border-[#7B6FC9] rounded-xl transition-all"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="email" className="text-gray-300 text-sm font-medium">Email Address</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <Input
                      id="email"
                      name="email"
                      type="email"
                      placeholder="Enter your email"
                      value={formData.email}
                      onChange={handleChange}
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
                      name="password"
                      type={showPassword ? "text" : "password"}
                      placeholder="Create a password"
                      value={formData.password}
                      onChange={handleChange}
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

                <div className="space-y-2">
                  <Label htmlFor="confirmPassword" className="text-gray-300 text-sm font-medium">Confirm Password</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <Input
                      id="confirmPassword"
                      name="confirmPassword"
                      type={showConfirmPassword ? "text" : "password"}
                      placeholder="Confirm your password"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      required
                      className="pl-11 pr-11 h-11 bg-[#0F0F12]/60 border-[#4A4560] text-white placeholder-gray-500 focus:ring-2 focus:ring-[#7B6FC9] focus:border-[#7B6FC9] rounded-xl transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3 top-3 text-gray-400 hover:text-gray-300 transition-colors"
                    >
                      {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="terms"
                    checked={agreeToTerms}
                    onChange={(e) => setAgreeToTerms(e.target.checked)}
                    className="w-4 h-4 text-[#7B6FC9] bg-[#0F0F12]/60 border-[#4A4560] rounded focus:ring-[#7B6FC9]"
                    required
                  />
                  <Label htmlFor="terms" className="text-gray-300 text-sm">
                    I agree to the{" "}
                    <Link to="/terms" className="text-[#9C90E8] hover:text-[#7B6FC9] underline font-medium">
                      Terms of Service
                    </Link>
                  </Label>
                </div>

                <Button 
                  type="submit" 
                  className="w-full h-11 bg-gradient-to-r from-[#7B6FC9] to-[#9C90E8] hover:from-[#6B5FB9] hover:to-[#8C80D8] text-white rounded-xl font-semibold shadow-lg hover:shadow-xl hover:scale-[1.02] transition-all duration-300"
                  disabled={loading}
                >
                  {loading ? (
                    <div className="flex items-center justify-center gap-2">
                      <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                      <span>Creating Account...</span>
                    </div>
                  ) : (
                    "Create Account"
                  )}
                </Button>

                <div className="text-center pt-2">
                  <div className="text-gray-400 text-sm">
                    Already have an account?{" "}
                    <Link 
                      to="/login" 
                      className="text-[#9C90E8] hover:text-[#7B6FC9] font-semibold transition-colors"
                    >
                      Sign in
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

export default Signup;
