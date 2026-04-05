import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor, GradientBoostingClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score, accuracy_score
import joblib
import os
from typing import Dict, List, Tuple, Optional
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')

MODEL_PATH = "models/budget_model.pkl"
SCALER_PATH = "models/budget_scaler.pkl"
CATEGORY_ENCODER_PATH = "models/category_encoder.pkl"

class AIBudgetPlanner:
    def __init__(self):
        self.budget_model = None
        self.category_model = None
        self.scaler = None
        self.category_encoder = None
        self.historical_data = None
        
    def prepare_features(self, expenses_df: pd.DataFrame) -> pd.DataFrame:
        """Prepare features for budget prediction"""
        features = []
        
        # Group by user and month to get monthly spending patterns
        expenses_df['date'] = pd.to_datetime(expenses_df['date'])
        expenses_df['month'] = expenses_df['date'].dt.month
        expenses_df['year'] = expenses_df['date'].dt.year
        
        # Monthly aggregations
        monthly_stats = expenses_df.groupby(['user_id', 'year', 'month']).agg({
            'amount': ['sum', 'mean', 'count', 'std'],
            'category': lambda x: x.nunique()
        }).reset_index()
        
        # Flatten column names
        monthly_stats.columns = ['user_id', 'year', 'month', 'total_spent', 'avg_transaction', 
                               'transaction_count', 'spending_volatility', 'unique_categories']
        
        # Add temporal features
        monthly_stats['quarter'] = monthly_stats['month'] // 3 + 1
        monthly_stats['is_holiday_season'] = monthly_stats['month'].isin([11, 12, 1]).astype(int)
        monthly_stats['is_summer'] = monthly_stats['month'].isin([6, 7, 8]).astype(int)
        
        # Add spending ratios
        monthly_stats['avg_daily_spending'] = monthly_stats['total_spent'] / 30
        monthly_stats['spending_per_transaction'] = monthly_stats['total_spent'] / monthly_stats['transaction_count']
        
        # Fill NaN values
        monthly_stats = monthly_stats.fillna(0)
        
        return monthly_stats
    
    def prepare_category_features(self, expenses_df: pd.DataFrame) -> pd.DataFrame:
        """Prepare features for category-wise budget allocation"""
        # Category-wise monthly spending
        expenses_df['date'] = pd.to_datetime(expenses_df['date'])
        expenses_df['month'] = expenses_df['date'].dt.month
        
        category_monthly = expenses_df.groupby(['user_id', 'category', 'month']).agg({
            'amount': ['sum', 'mean', 'count']
        }).reset_index()
        
        category_monthly.columns = ['user_id', 'category', 'month', 'category_total', 
                                  'category_avg', 'category_count']
        
        # Add category importance features
        total_monthly = expenses_df.groupby(['user_id', 'month'])['amount'].sum().reset_index()
        total_monthly.columns = ['user_id', 'month', 'monthly_total']
        
        category_monthly = category_monthly.merge(total_monthly, on=['user_id', 'month'])
        category_monthly['category_percentage'] = category_monthly['category_total'] / category_monthly['monthly_total']
        
        # Add temporal features
        category_monthly['quarter'] = category_monthly['month'] // 3 + 1
        category_monthly['is_holiday_season'] = category_monthly['month'].isin([11, 12, 1]).astype(int)
        
        return category_monthly.fillna(0)
    
    def train_budget_model(self, expenses_df: pd.DataFrame, income_df: pd.DataFrame = None):
        """Train Gradient Boosting model for budget prediction"""
        if expenses_df.empty:
            return False, "No expense data available"
        
        # Prepare features
        features_df = self.prepare_features(expenses_df)
        
        if features_df.empty:
            return False, "Insufficient data for feature preparation"
        
        # Prepare target variable (next month's spending)
        features_df = features_df.sort_values(['user_id', 'year', 'month'])
        features_df['next_month_spending'] = features_df.groupby('user_id')['total_spent'].shift(-1)
        
        # Remove last month (no target available)
        features_df = features_df.dropna(subset=['next_month_spending'])
        
        if len(features_df) < 3:
            return False, "Insufficient historical data"
        
        # Select features for training
        feature_columns = ['total_spent', 'avg_transaction', 'transaction_count', 
                          'spending_volatility', 'unique_categories', 'quarter',
                          'is_holiday_season', 'is_summer', 'avg_daily_spending',
                          'spending_per_transaction']
        
        X = features_df[feature_columns]
        y = features_df['next_month_spending']
        
        # Scale features
        self.scaler = StandardScaler()
        X_scaled = self.scaler.fit_transform(X)
        
        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X_scaled, y, test_size=0.2, random_state=42
        )
        
        # Train Gradient Boosting Regressor
        self.budget_model = GradientBoostingRegressor(
            n_estimators=200,
            learning_rate=0.05,
            max_depth=6,
            min_samples_split=5,
            min_samples_leaf=2,
            subsample=0.8,
            random_state=42
        )
        
        self.budget_model.fit(X_train, y_train)
        
        # Evaluate model
        y_pred = self.budget_model.predict(X_test)
        mae = mean_absolute_error(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)
        r2 = r2_score(y_test, y_pred)
        
        # Cross-validation
        cv_scores = cross_val_score(self.budget_model, X_scaled, y, cv=5, scoring='neg_mean_absolute_error')
        
        self.historical_data = features_df
        
        # Save models
        os.makedirs("models", exist_ok=True)
        joblib.dump(self.budget_model, MODEL_PATH)
        joblib.dump(self.scaler, SCALER_PATH)
        
        metrics = {
            'mae': mae,
            'mse': mse,
            'r2': r2,
            'cv_mae_mean': -cv_scores.mean(),
            'cv_mae_std': cv_scores.std(),
            'training_samples': len(X_train),
            'test_samples': len(X_test)
        }
        
        return True, metrics
    
    def train_category_allocation_model(self, expenses_df: pd.DataFrame):
        """Train model for category-wise budget allocation"""
        if expenses_df.empty:
            return False, "No expense data available"
        
        # Prepare category features
        category_features = self.prepare_category_features(expenses_df)
        
        if category_features.empty:
            return False, "Insufficient data for category analysis"
        
        # Encode categories
        self.category_encoder = LabelEncoder()
        category_features['category_encoded'] = self.category_encoder.fit_transform(category_features['category'])
        
        # Prepare features
        feature_columns = ['category_total', 'category_avg', 'category_count',
                          'category_percentage', 'quarter', 'is_holiday_season']
        
        X = category_features[feature_columns]
        y = category_features['category_encoded']
        
        # Scale features
        X_scaled = self.scaler.transform(X) if self.scaler else StandardScaler().fit_transform(X)
        
        # Train Gradient Boosting Classifier
        self.category_model = GradientBoostingClassifier(
            n_estimators=100,
            learning_rate=0.1,
            max_depth=4,
            random_state=42
        )
        
        self.category_model.fit(X_scaled, y)
        
        # Save category encoder
        joblib.dump(self.category_encoder, CATEGORY_ENCODER_PATH)
        
        return True, "Category allocation model trained successfully"
    
    def generate_budget_suggestions(self, user_id: int, monthly_income: float, 
                                  target_savings_rate: float = 0.2) -> Dict:
        """Generate AI-powered budget suggestions"""
        if not self.budget_model or not self.scaler:
            return {"error": "Budget model not trained"}
        
        # Get user's recent spending patterns
        if self.historical_data is None:
            return {"error": "No historical data available"}
        
        user_data = self.historical_data[self.historical_data['user_id'] == user_id]
        
        if user_data.empty:
            return {"error": "No data available for user"}
        
        # Get most recent month data
        recent_month = user_data.iloc[-1]
        
        # Prepare features for prediction
        feature_columns = ['total_spent', 'avg_transaction', 'transaction_count', 
                          'spending_volatility', 'unique_categories', 'quarter',
                          'is_holiday_season', 'is_summer', 'avg_daily_spending',
                          'spending_per_transaction']
        
        features = recent_month[feature_columns].values.reshape(1, -1)
        features_scaled = self.scaler.transform(features)
        
        # Predict next month's spending
        predicted_spending = self.budget_model.predict(features_scaled)[0]
        
        # Calculate budget allocations
        target_savings = monthly_income * target_savings_rate
        available_for_spending = monthly_income - target_savings
        
        # Budget optimization
        if predicted_spending > available_for_spending:
            # Need to reduce spending
            reduction_factor = available_for_spending / predicted_spending
            optimized_spending = available_for_spending
            recommendations = self._generate_reduction_recommendations(user_data, reduction_factor)
        else:
            # Can maintain or increase spending
            optimized_spending = min(predicted_spending, available_for_spending)
            surplus = available_for_spending - optimized_spending
            recommendations = self._generate_optimization_recommendations(user_data, surplus)
        
        # Category-wise allocation
        category_allocation = self._allocate_budget_by_category(user_id, optimized_spending)
        
        return {
            'monthly_income': monthly_income,
            'predicted_spending': predicted_spending,
            'target_savings_rate': target_savings_rate,
            'target_savings': target_savings,
            'optimized_spending': optimized_spending,
            'available_surplus': available_for_spending - optimized_spending,
            'category_allocation': category_allocation,
            'recommendations': recommendations,
            'confidence_score': self._calculate_budget_confidence(recent_month),
            'risk_level': self._assess_risk_level(predicted_spending, monthly_income)
        }
    
    def _generate_reduction_recommendations(self, user_data: pd.DataFrame, reduction_factor: float) -> List[Dict]:
        """Generate recommendations for spending reduction"""
        recommendations = []
        
        # Analyze spending by category
        category_spending = user_data.groupby('category')['amount'].sum().sort_values(ascending=False)
        
        for category, amount in category_spending.head(3).items():
            if amount > 0:
                reduced_amount = amount * reduction_factor
                savings = amount - reduced_amount
                
                recommendations.append({
                    'category': category,
                    'current_spending': amount,
                    'recommended_spending': reduced_amount,
                    'potential_savings': savings,
                    'action': f"Reduce {category} spending by {(1-reduction_factor)*100:.1f}%"
                })
        
        # Add general recommendations
        recommendations.extend([
            {'action': 'Review subscriptions and cancel unused ones', 'potential_savings': 50},
            {'action': 'Cook at home more often to reduce food expenses', 'potential_savings': 100},
            {'action': 'Use public transportation instead of ride-sharing', 'potential_savings': 80}
        ])
        
        return recommendations
    
    def _generate_optimization_recommendations(self, user_data: pd.DataFrame, surplus: float) -> List[Dict]:
        """Generate recommendations for optimizing surplus"""
        recommendations = []
        
        if surplus > 100:
            recommendations.append({
                'action': 'Increase emergency fund contribution',
                'amount': min(surplus * 0.3, 200),
                'priority': 'high'
            })
        
        if surplus > 50:
            recommendations.append({
                'action': 'Increase investment contributions',
                'amount': min(surplus * 0.4, 150),
                'priority': 'medium'
            })
        
        recommendations.append({
            'action': 'Allocate to long-term goals',
            'amount': surplus * 0.2,
            'priority': 'medium'
        })
        
        return recommendations
    
    def _allocate_budget_by_category(self, user_id: int, total_budget: float) -> Dict:
        """Allocate budget across categories based on historical patterns"""
        if self.historical_data is None:
            return {}
        
        user_data = self.historical_data[self.historical_data['user_id'] == user_id]
        
        if user_data.empty:
            return {}
        
        # Get category spending patterns
        # Note: This is a simplified version - in practice, you'd need category-level data
        category_patterns = {
            'Food & Dining': 0.25,
            'Transportation': 0.15,
            'Shopping': 0.20,
            'Entertainment': 0.10,
            'Bills & Utilities': 0.20,
            'Healthcare': 0.05,
            'Other': 0.05
        }
        
        allocation = {}
        for category, percentage in category_patterns.items():
            allocation[category] = {
                'budget_amount': total_budget * percentage,
                'percentage': percentage * 100,
                'recommended_limit': total_budget * percentage * 1.1  # 10% buffer
            }
        
        return allocation
    
    def _calculate_budget_confidence(self, recent_data: pd.Series) -> float:
        """Calculate confidence score for budget predictions"""
        # Base confidence on data quality and volatility
        volatility = recent_data.get('spending_volatility', 0)
        transaction_count = recent_data.get('transaction_count', 0)
        
        # Lower volatility and more transactions = higher confidence
        confidence = 0.7  # Base confidence
        
        if volatility > 0:
            confidence -= min(volatility / 1000, 0.3)  # Reduce confidence for high volatility
        
        if transaction_count > 0:
            confidence += min(transaction_count / 50, 0.2)  # Increase confidence for more transactions
        
        return max(0.3, min(0.95, confidence))
    
    def _assess_risk_level(self, predicted_spending: float, monthly_income: float) -> str:
        """Assess risk level of budget plan"""
        spending_ratio = predicted_spending / monthly_income if monthly_income > 0 else 1
        
        if spending_ratio > 0.9:
            return "high"
        elif spending_ratio > 0.7:
            return "medium"
        else:
            return "low"
    
    def analyze_budget_performance(self, user_id: int, actual_spending: float, 
                                budget_target: float) -> Dict:
        """Analyze budget performance and provide insights"""
        variance = actual_spending - budget_target
        variance_percentage = (variance / budget_target) * 100 if budget_target > 0 else 0
        
        performance = {
            'budget_target': budget_target,
            'actual_spending': actual_spending,
            'variance': variance,
            'variance_percentage': variance_percentage,
            'status': 'on_track' if abs(variance_percentage) < 10 else 'off_track'
        }
        
        # Add insights
        if variance_percentage > 10:
            performance['insight'] = "Overspending detected. Consider reviewing recent expenses."
            performance['recommendation'] = "Reduce discretionary spending in the next period."
        elif variance_percentage < -10:
            performance['insight'] = "Underspending achieved. Great job on budget discipline!"
            performance['recommendation'] = "Consider allocating surplus to savings or investments."
        else:
            performance['insight'] = "Budget performance is within acceptable range."
            performance['recommendation'] = "Maintain current spending habits."
        
        return performance

# Global budget planner instance
_budget_planner = None

def get_budget_planner():
    global _budget_planner
    if _budget_planner is None:
        _budget_planner = AIBudgetPlanner()
    return _budget_planner

def train_budget_model(expenses_data: List[Dict], income_data: List[Dict] = None):
    """Train AI budget planning model"""
    planner = get_budget_planner()
    
    # Convert to DataFrame
    expenses_df = pd.DataFrame(expenses_data)
    income_df = pd.DataFrame(income_data) if income_data else pd.DataFrame()
    
    success, result = planner.train_budget_model(expenses_df, income_df)
    
    if success:
        # Also train category allocation model
        planner.train_category_allocation_model(expenses_df)
    
    return success, result

def generate_budget_suggestions(user_id: int, monthly_income: float, 
                              target_savings_rate: float = 0.2):
    """Generate AI-powered budget suggestions"""
    planner = get_budget_planner()
    return planner.generate_budget_suggestions(user_id, monthly_income, target_savings_rate)

def analyze_budget_performance(user_id: int, actual_spending: float, budget_target: float):
    """Analyze budget performance"""
    planner = get_budget_planner()
    return planner.analyze_budget_performance(user_id, actual_spending, budget_target)

def load_budget_models():
    """Load pre-trained budget models"""
    planner = get_budget_planner()
    
    try:
        if os.path.exists(MODEL_PATH):
            planner.budget_model = joblib.load(MODEL_PATH)
        if os.path.exists(SCALER_PATH):
            planner.scaler = joblib.load(SCALER_PATH)
        if os.path.exists(CATEGORY_ENCODER_PATH):
            planner.category_encoder = joblib.load(CATEGORY_ENCODER_PATH)
        return True
    except Exception:
        return False

def get_budget_recommendations_for_user(user_id: int, expenses: List[Dict]) -> Dict:
    """Get personalized budget recommendations for a user"""
    if not expenses:
        return {"error": "No expense data available"}
    
    # Calculate current spending patterns
    expenses_df = pd.DataFrame(expenses)
    total_spending = expenses_df['amount'].sum()
    
    # Estimate monthly income (simplified - in practice, this would come from user data)
    estimated_income = total_spending * 1.5  # Assume spending is 2/3 of income
    
    return generate_budget_suggestions(user_id, estimated_income)
