import { useState, useEffect } from 'react';
import { useToast } from '@/hooks/use-toast';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Bell, AlertTriangle, CheckCircle2, Clock, TrendingUp, Wallet } from 'lucide-react';
import { alertService } from '@/api/api';
import { formatCurrency } from '@/utils/formatters';

const Alerts = () => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  useEffect(() => {
    fetchAlerts();
  }, []);

  const fetchAlerts = async () => {
    try {
      setLoading(true);
      const response = await alertService.getAll();
      setAlerts(response || []);
    } catch (error) {
      console.error('Failed to fetch alerts:', error);
      // Fallback mock data
      setAlerts([
        { id: 1, title: 'Budget Warning', message: 'You have spent 80% of your monthly budget', type: 'WARNING', read: false, createdAt: '2024-02-10T10:00:00' },
        { id: 2, title: 'Bill Due', message: 'Electric bill of $120 is due in 3 days', type: 'INFO', read: false, createdAt: '2024-02-09T15:30:00' },
        { id: 3, title: 'Large Expense', message: 'Unusual expense of $500 detected', type: 'ALERT', read: true, createdAt: '2024-02-08T09:15:00' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = async (id) => {
    try {
      await alertService.markAsRead(id);
      setAlerts(alerts.map(alert => 
        alert.id === id ? { ...alert, read: true } : alert
      ));
      toast({
        title: 'Success',
        description: 'Alert marked as read',
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to mark alert as read',
        variant: 'destructive',
      });
    }
  };

  const markAllAsRead = async () => {
    const unreadAlerts = alerts.filter(a => !a.read);
    await Promise.all(unreadAlerts.map(a => alertService.markAsRead(a.id)));
    setAlerts(alerts.map(alert => ({ ...alert, read: true })));
    toast({
      title: 'Success',
      description: 'All alerts marked as read',
    });
  };

  const getAlertIcon = (type) => {
    switch (type) {
      case 'WARNING':
        return <AlertTriangle className="w-5 h-5" />;
      case 'ALERT':
        return <TrendingUp className="w-5 h-5" />;
      case 'SUCCESS':
        return <CheckCircle2 className="w-5 h-5" />;
      default:
        return <Bell className="w-5 h-5" />;
    }
  };

  const getAlertColor = (type) => {
    switch (type) {
      case 'WARNING':
        return 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20';
      case 'ALERT':
        return 'text-red-400 bg-red-400/10 border-red-400/20';
      case 'SUCCESS':
        return 'text-green-400 bg-green-400/10 border-green-400/20';
      default:
        return 'text-blue-400 bg-blue-400/10 border-blue-400/20';
    }
  };

  const unreadCount = alerts.filter(a => !a.read).length;

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
          <h1 className="text-3xl font-bold text-white">Alerts</h1>
          <p className="text-gray-400 mt-1">Stay informed about your finances</p>
        </div>
        {unreadCount > 0 && (
          <Button
            onClick={markAllAsRead}
            variant="outline"
            className="border-[#3A3560] text-gray-300 hover:bg-[#2A2540] hover:text-white"
          >
            <CheckCircle2 className="w-4 h-4 mr-2" />
            Mark all as read
          </Button>
        )}
      </div>

      {/* Stats */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <Bell className="w-4 h-4" />
              Total Alerts
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">{alerts.length}</p>
          </CardContent>
        </Card>
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <AlertTriangle className="w-4 h-4" />
              Unread
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">{unreadCount}</p>
          </CardContent>
        </Card>
        <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
          <CardHeader className="pb-3">
            <CardTitle className="text-gray-400 text-sm font-medium flex items-center gap-2">
              <Wallet className="w-4 h-4" />
              Budget Alerts
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold text-white">
              {alerts.filter(a => a.type === 'WARNING' && !a.read).length}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Alerts List */}
      <Card className="bg-gradient-to-br from-[#2A2540] to-[#322B55] border-[#3A3560]">
        <CardHeader>
          <CardTitle className="text-white">Recent Alerts</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {alerts.length === 0 ? (
              <div className="text-center py-8 text-gray-400">
                <Bell className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>No alerts yet</p>
                <p className="text-sm">You're all caught up!</p>
              </div>
            ) : (
              alerts.map((alert) => (
                <div
                  key={alert.id}
                  className={`flex items-start gap-4 p-4 rounded-xl border transition-all ${
                    alert.read 
                      ? 'bg-[#1E1E2A]/30 border-[#3A3560]/30' 
                      : 'bg-[#1E1E2A]/60 border-[#3A3560]'
                  }`}
                >
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${getAlertColor(alert.type)}`}>
                    {getAlertIcon(alert.type)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className={`font-medium ${alert.read ? 'text-gray-400' : 'text-white'}`}>
                          {alert.title}
                        </p>
                        <p className="text-sm text-gray-400 mt-1">{alert.message}</p>
                      </div>
                      {!alert.read && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => markAsRead(alert.id)}
                          className="text-[#9C90E8] hover:text-[#7B6FC9] hover:bg-[#7B6FC9]/10 shrink-0"
                        >
                          <CheckCircle2 className="w-4 h-4" />
                        </Button>
                      )}
                    </div>
                    <div className="flex items-center gap-2 mt-2 text-xs text-gray-500">
                      <Clock className="w-3 h-3" />
                      {new Date(alert.createdAt).toLocaleDateString()}
                      {!alert.read && (
                        <span className="px-2 py-0.5 rounded-full bg-[#7B6FC9]/20 text-[#9C90E8]">
                          New
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default Alerts;
