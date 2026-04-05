import numpy as np
import joblib
import os
from sklearn.ensemble import IsolationForest

MODEL_PATH = "models/anomaly_model.pkl"

def train_model(amounts):
    """amounts: list of expense amounts (numeric)"""
    X = np.array(amounts).reshape(-1, 1)
    model = IsolationForest(contamination=0.1, random_state=42)
    model.fit(X)
    os.makedirs("models", exist_ok=True)
    joblib.dump(model, MODEL_PATH)
    return model

def load_model():
    if os.path.exists(MODEL_PATH):
        return joblib.load(MODEL_PATH)
    else:
        return None

def detect_anomalies(amounts):
    """
    amounts: list of expense amounts to check.
    Returns a list of indices that are anomalies.
    """
    model = load_model()
    if model is None:
        # Not enough data to train – return empty
        return []
    X = np.array(amounts).reshape(-1, 1)
    preds = model.predict(X)  # -1 = anomaly, 1 = normal
    anomalies = [i for i, p in enumerate(preds) if p == -1]
    return anomalies
