import { formatCurrency } from "@/utils/formatters";
import { TrendingUp, TrendingDown } from "lucide-react";

const StatCard = ({ title, value, icon, trend, subtitle }) => {
  const getTrendIcon = () => {
    if (trend === 'up') return <TrendingUp className="w-4 h-4 text-green-500" />;
    if (trend === 'down') return <TrendingDown className="w-4 h-4 text-red-500" />;
    return null;
  };

  const getTrendColor = () => {
    if (trend === 'up') return 'text-green-500';
    if (trend === 'down') return 'text-red-500';
    return '';
  };

  return (
    <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm hover:shadow-md transition-all duration-200 hover:scale-105 border border-gray-100 dark:border-gray-700">
      <div className="flex items-center justify-between mb-2">
        <span className="text-3xl">{icon}</span>
        {trend && getTrendIcon()}
      </div>
      <div>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{title}</p>
        <p className="text-2xl font-bold text-gray-900 dark:text-white">
          {typeof value === "number" ? formatCurrency(value) : value}
        </p>
        {subtitle && (
          <p className={`text-xs mt-1 ${getTrendColor()}`}>{subtitle}</p>
        )}
      </div>
    </div>
  );
};

export default StatCard;
