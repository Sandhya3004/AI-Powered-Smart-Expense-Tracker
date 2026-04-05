import { useState, useEffect } from "react";
import { getExpenses } from '@/services/expenseService';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

const ExpenseList = () => {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchExpenses = async () => {
      try {
        const response = await getExpenses();
        // Handle different response formats
        const expensesData = response.data;
        if (Array.isArray(expensesData)) {
          setExpenses(expensesData.slice(0, 5)); // Show last 5 expenses
        } else if (expensesData && expensesData.content && Array.isArray(expensesData.content)) {
          // Handle paginated response
          setExpenses(expensesData.content.slice(0, 5));
        } else {
          setExpenses([]); // Empty array if no data
        }
      } catch (error) {
        console.error("Failed to fetch expenses:", error);
        setExpenses([]); // Set empty array on error
      } finally {
        setLoading(false);
      }
    };

    fetchExpenses();
  }, []);

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Recent Expenses</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex justify-between items-center">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-4 w-20" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent Expenses</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {expenses.map((expense) => (
            <div key={expense.id} className="flex justify-between items-center">
              <div>
                <p className="font-medium">{expense.description}</p>
                <p className="text-sm text-gray-500">{expense.category}</p>
              </div>
              <span className="font-semibold text-red-600">
                -${expense.amount.toFixed(2)}
              </span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};

export default ExpenseList;
