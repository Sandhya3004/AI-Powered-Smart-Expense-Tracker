import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const AuthCard = ({ title, subtitle, children, icon: Icon }) => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#0F0F12] via-[#151520] to-[#1A1A2E] p-4">
      <div className="w-full max-w-md">
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560] backdrop-blur-lg shadow-[0_8px_30px_rgba(123,111,201,0.25)] hover:scale-[1.02] transition-all duration-300 rounded-2xl overflow-hidden">
          <CardHeader className="text-center space-y-4 pb-6">
            {Icon && (
              <div className="mx-auto w-16 h-16 bg-gradient-to-br from-[#7B6FC9] to-[#9C90E8] rounded-full flex items-center justify-center shadow-lg">
                <Icon className="w-8 h-8 text-white" />
              </div>
            )}
            <div className="space-y-2">
              <CardTitle className="text-2xl font-bold text-white">{title}</CardTitle>
              <CardDescription className="text-gray-400 text-sm">
                {subtitle}
              </CardDescription>
            </div>
          </CardHeader>
          <CardContent className="p-6 pt-0">
            {children}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default AuthCard;
