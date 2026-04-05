import numpy as np
import joblib
import os
from sklearn.linear_model import LinearRegression
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import pandas as pd
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')

# Try to import Prophet and TensorFlow for LSTM
try:
    from prophet import Prophet
    PROPHET_AVAILABLE = True
except ImportError:
    PROPHET_AVAILABLE = False

try:
    import tensorflow as tf
    from tensorflow.keras.models import Sequential
    from tensorflow.keras.layers import LSTM, Dense, Dropout
    from tensorflow.keras.optimizers import Adam
    TENSORFLOW_AVAILABLE = True
except ImportError:
    TENSORFLOW_AVAILABLE = False

MODEL_PATH = "models/prediction_model.pkl"
PROPHET_MODEL_PATH = "models/prophet_model.pkl"
LSTM_MODEL_PATH = "models/lstm_model.h5"
SCALER_PATH = "models/lstm_scaler.pkl"

class ExpensePredictor:
    def __init__(self):
        self.linear_model = None
        self.prophet_model = None
        self.lstm_model = None
        self.scaler = None
        self.last_training_data = None
        
    def prepare_time_series_data(self, monthly_totals):
        """Prepare data for time series forecasting"""
        if not monthly_totals or len(monthly_totals) < 3:
            return None
            
        # Convert to DataFrame for Prophet
        df = pd.DataFrame(monthly_totals, columns=['month_index', 'amount'])
        
        # Create proper dates (assuming month_index represents months from start)
        start_date = datetime.now() - pd.DateOffset(months=len(monthly_totals))
        df['ds'] = start_date + pd.to_timedelta(df['month_index'], unit='M')
        df['y'] = df['amount']
        
        return df[['ds', 'y']]
    
    def train_linear_model(self, monthly_totals):
        """Train traditional linear regression model"""
        if len(monthly_totals) < 3:
            return None, 0.0
            
        X = np.array([[m] for m, _ in monthly_totals])
        y = np.array([t for _, t in monthly_totals])
        
        model = LinearRegression()
        model.fit(X, y)
        
        # Calculate metrics
        predictions = model.predict(X)
        mse = mean_squared_error(y, predictions)
        mae = mean_absolute_error(y, predictions)
        r2 = r2_score(y, predictions)
        
        return model, {'mse': mse, 'mae': mae, 'r2': r2}
    
    def train_prophet_model(self, monthly_totals):
        """Train Prophet model for time series forecasting"""
        if not PROPHET_AVAILABLE or len(monthly_totals) < 3:
            return None, {}
            
        df = self.prepare_time_series_data(monthly_totals)
        if df is None:
            return None, {}
        
        try:
            model = Prophet(
                yearly_seasonality=True,
                monthly_seasonality=True,
                weekly_seasonality=False,  # Monthly data doesn't need weekly
                daily_seasonality=False,
                changepoint_prior_scale=0.05,
                seasonality_prior_scale=10.0
            )
            
            model.fit(df)
            
            # Calculate metrics
            forecast = model.predict(df)
            mse = mean_squared_error(df['y'], forecast['yhat'])
            mae = mean_absolute_error(df['y'], forecast['yhat'])
            
            return model, {'mse': mse, 'mae': mae}
        except Exception as e:
            print(f"Prophet training failed: {e}")
            return None, {}
    
    def prepare_lstm_data(self, data, look_back=3):
        """Prepare data for LSTM training"""
        if len(data) < look_back + 1:
            return None, None
            
        # Scale the data
        scaler = MinMaxScaler(feature_range=(0, 1))
        scaled_data = scaler.fit_transform(np.array(data).reshape(-1, 1))
        
        X, y = [], []
        for i in range(len(scaled_data) - look_back):
            X.append(scaled_data[i:(i + look_back), 0])
            y.append(scaled_data[i + look_back, 0])
        
        return np.array(X), np.array(y), scaler
    
    def train_lstm_model(self, monthly_totals, look_back=3, epochs=100):
        """Train LSTM model for time series forecasting"""
        if not TENSORFLOW_AVAILABLE or len(monthly_totals) < look_back + 1:
            return None, {}
        
        try:
            # Extract amounts
            amounts = [amount for _, amount in monthly_totals]
            
            # Prepare data
            result = self.prepare_lstm_data(amounts, look_back)
            if result[0] is None:
                return None, {}
                
            X, y, scaler = result
            
            # Reshape for LSTM [samples, timesteps, features]
            X = X.reshape((X.shape[0], X.shape[1], 1))
            
            # Build LSTM model
            model = Sequential([
                LSTM(50, return_sequences=True, input_shape=(look_back, 1)),
                Dropout(0.2),
                LSTM(50, return_sequences=False),
                Dropout(0.2),
                Dense(25),
                Dense(1)
            ])
            
            model.compile(optimizer=Adam(learning_rate=0.001), loss='mse')
            
            # Train model
            model.fit(X, y, epochs=epochs, batch_size=32, verbose=0)
            
            # Calculate metrics
            predictions = model.predict(X)
            predictions = scaler.inverse_transform(predictions)
            actual = scaler.inverse_transform(y.reshape(-1, 1))
            
            mse = mean_squared_error(actual, predictions)
            mae = mean_absolute_error(actual, predictions)
            
            return model, {'mse': mse, 'mae': mae, 'scaler': scaler}
            
        except Exception as e:
            print(f"LSTM training failed: {e}")
            return None, {}
    
    def train_ensemble_model(self, monthly_totals):
        """Train ensemble of models and select the best"""
        if len(monthly_totals) < 3:
            return None, "insufficient_data"
        
        models = {}
        metrics = {}
        
        # Train Linear Regression
        linear_model, linear_metrics = self.train_linear_model(monthly_totals)
        if linear_model:
            models['linear'] = linear_model
            metrics['linear'] = linear_metrics
        
        # Train Prophet
        prophet_model, prophet_metrics = self.train_prophet_model(monthly_totals)
        if prophet_model:
            models['prophet'] = prophet_model
            metrics['prophet'] = prophet_metrics
        
        # Train LSTM
        lstm_model, lstm_metrics = self.train_lstm_model(monthly_totals)
        if lstm_model:
            models['lstm'] = lstm_model
            self.scaler = lstm_metrics.get('scaler')
            metrics['lstm'] = lstm_metrics
        
        # Select best model based on MAE
        if not models:
            return None, "no_models_trained"
        
        best_model = min(metrics.keys(), key=lambda k: metrics[k].get('mae', float('inf')))
        
        self.linear_model = models.get('linear')
        self.prophet_model = models.get('prophet')
        self.lstm_model = models.get('lstm')
        self.last_training_data = monthly_totals
        
        return best_model, metrics
    
    def predict_next_month(self, last_month_index, model_type='ensemble'):
        """Predict next month's expenses"""
        if not self.last_training_data:
            return None, "no_training_data"
        
        predictions = {}
        
        # Linear Regression prediction
        if self.linear_model:
            try:
                next_month = [[last_month_index + 1]]
                linear_pred = self.linear_model.predict(next_month)[0]
                predictions['linear'] = max(0, linear_pred)  # Ensure non-negative
            except Exception:
                pass
        
        # Prophet prediction
        if self.prophet_model:
            try:
                # Create future dataframe
                future = self.prophet_model.make_future_dataframe(periods=1, freq='M')
                forecast = self.prophet_model.predict(future)
                prophet_pred = forecast.iloc[-1]['yhat']
                predictions['prophet'] = max(0, prophet_pred)
            except Exception:
                pass
        
        # LSTM prediction
        if self.lstm_model and self.scaler:
            try:
                # Get last look_back values
                look_back = 3
                amounts = [amount for _, amount in self.last_training_data[-look_back:]]
                scaled_data = self.scaler.transform(np.array(amounts).reshape(-1, 1))
                
                # Reshape for prediction
                X_pred = scaled_data.reshape(1, look_back, 1)
                scaled_pred = self.lstm_model.predict(X_pred, verbose=0)
                lstm_pred = self.scaler.inverse_transform(scaled_pred)[0][0]
                predictions['lstm'] = max(0, lstm_pred)
            except Exception:
                pass
        
        if not predictions:
            return None, "no_predictions"
        
        if model_type == 'ensemble' and len(predictions) > 1:
            # Weighted average based on inverse MAE (simple approach)
            # For now, use simple average
            ensemble_pred = np.mean(list(predictions.values()))
            return ensemble_pred, predictions
        elif model_type in predictions:
            return predictions[model_type], {model_type: predictions[model_type]}
        else:
            # Return best available prediction
            best_pred = max(predictions.values())
            return best_pred, predictions
    
    def predict_multiple_months(self, last_month_index, months_ahead=3):
        """Predict multiple months ahead"""
        predictions = []
        current_index = last_month_index
        
        for i in range(months_ahead):
            pred, details = self.predict_next_month(current_index)
            if pred is not None:
                predictions.append({
                    'month_index': current_index + 1,
                    'predicted_amount': pred,
                    'details': details
                })
                current_index += 1
            else:
                break
        
        return predictions
    
    def get_prediction_confidence(self, prediction, historical_data):
        """Calculate confidence score for prediction"""
        if not historical_data or len(historical_data) < 3:
            return 0.5
        
        # Calculate historical volatility
        amounts = [amount for _, amount in historical_data]
        volatility = np.std(amounts) / np.mean(amounts) if np.mean(amounts) > 0 else 1
        
        # Lower volatility = higher confidence
        base_confidence = max(0.3, min(0.9, 1 - volatility))
        
        # Adjust based on data length
        data_factor = min(1.0, len(historical_data) / 12)  # More data = more confidence
        
        confidence = base_confidence * data_factor
        return confidence

# Global predictor instance
_predictor = None

def get_predictor():
    global _predictor
    if _predictor is None:
        _predictor = ExpensePredictor()
    return _predictor

def train_model(monthly_totals):
    """
    Train ensemble prediction model
    monthly_totals: list of (month_index, total) e.g. [(1, 1200), (2, 1350), (3, 1100)]
    """
    predictor = get_predictor()
    best_model, metrics = predictor.train_ensemble_model(monthly_totals)
    
    # Save models
    os.makedirs("models", exist_ok=True)
    
    if predictor.linear_model:
        joblib.dump(predictor.linear_model, MODEL_PATH)
    
    if predictor.prophet_model:
        joblib.dump(predictor.prophet_model, PROPHET_MODEL_PATH)
    
    if predictor.lstm_model:
        predictor.lstm_model.save(LSTM_MODEL_PATH)
        if predictor.scaler:
            joblib.dump(predictor.scaler, SCALER_PATH)
    
    return best_model, metrics

def load_model():
    """Load trained models"""
    predictor = get_predictor()
    
    try:
        if os.path.exists(MODEL_PATH):
            predictor.linear_model = joblib.load(MODEL_PATH)
    except Exception:
        pass
    
    try:
        if os.path.exists(PROPHET_MODEL_PATH):
            predictor.prophet_model = joblib.load(PROPHET_MODEL_PATH)
    except Exception:
        pass
    
    try:
        if os.path.exists(LSTM_MODEL_PATH) and TENSORFLOW_AVAILABLE:
            predictor.lstm_model = tf.keras.models.load_model(LSTM_MODEL_PATH)
        if os.path.exists(SCALER_PATH):
            predictor.scaler = joblib.load(SCALER_PATH)
    except Exception:
        pass
    
    return predictor

def predict_next_month(last_month_index, model_type='ensemble'):
    """
    Predict next month's expenses
    last_month_index: the index of the most recent month
    model_type: 'linear', 'prophet', 'lstm', 'ensemble'
    """
    predictor = load_model()
    return predictor.predict_next_month(last_month_index, model_type)

def forecast_expenses(monthly_totals, months_ahead=3):
    """
    Forecast expenses for multiple months
    monthly_totals: list of (month_index, total)
    months_ahead: number of months to forecast
    """
    predictor = get_predictor()
    best_model, metrics = predictor.train_ensemble_model(monthly_totals)
    
    if best_model:
        last_index = monthly_totals[-1][0] if monthly_totals else 0
        forecasts = predictor.predict_multiple_months(last_index, months_ahead)
        
        # Add confidence scores
        for forecast in forecasts:
            forecast['confidence'] = predictor.get_prediction_confidence(
                forecast['predicted_amount'], 
                monthly_totals
            )
        
        return forecasts, best_model, metrics
    else:
        return [], "insufficient_data", {}

# Legacy functions for backward compatibility
def train_legacy_model(monthly_totals):
    """Legacy training function for backward compatibility"""
    if len(monthly_totals) < 3:
        return None
    X = np.array([[m] for m, _ in monthly_totals])
    y = np.array([t for _, t in monthly_totals])
    model = LinearRegression()
    model.fit(X, y)
    os.makedirs("models", exist_ok=True)
    joblib.dump(model, MODEL_PATH)
    return model

def load_legacy_model():
    if os.path.exists(MODEL_PATH):
        return joblib.load(MODEL_PATH)
    return None

def predict_legacy_next_month(last_month_index):
    model = load_legacy_model()
    if model is None:
        return None
    next_month = [[last_month_index + 1]]
    return model.predict(next_month)[0]
