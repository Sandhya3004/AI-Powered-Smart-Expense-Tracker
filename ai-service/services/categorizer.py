import joblib
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import make_pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
import os
import re
from typing import Dict, List, Tuple
import numpy as np

MODEL_PATH = "models/categorization_model.pkl"
FINBERT_MODEL_PATH = "models/finbert_categorizer.pkl"

# Enhanced training data with more categories
TRAIN_DATA = [
    # Food & Dining
    ("starbucks coffee", "Food & Dining"),
    ("mcdonald's lunch", "Food & Dining"),
    ("restaurant dinner", "Food & Dining"),
    ("pizza delivery", "Food & Dining"),
    ("grocery store", "Food & Dining"),
    ("walmart groceries", "Food & Dining"),
    ("whole foods market", "Food & Dining"),
    ("food delivery", "Food & Dining"),
    ("cafe", "Food & Dining"),
    ("bakery", "Food & Dining"),
    
    # Shopping
    ("amazon books", "Shopping"),
    ("amazon purchase", "Shopping"),
    ("walmart shopping", "Shopping"),
    ("target store", "Shopping"),
    ("clothing store", "Shopping"),
    ("electronics purchase", "Shopping"),
    ("online shopping", "Shopping"),
    ("mall", "Shopping"),
    ("retail store", "Shopping"),
    
    # Transportation
    ("uber ride", "Transportation"),
    ("lyft ride", "Transportation"),
    ("taxi fare", "Transportation"),
    ("bus ticket", "Transportation"),
    ("train ticket", "Transportation"),
    ("metro card", "Transportation"),
    ("gas station", "Transportation"),
    ("petrol station", "Transportation"),
    ("parking fee", "Transportation"),
    ("toll booth", "Transportation"),
    ("airport parking", "Transportation"),
    
    # Entertainment
    ("netflix subscription", "Entertainment"),
    ("spotify premium", "Entertainment"),
    ("movie theater", "Entertainment"),
    ("concert tickets", "Entertainment"),
    ("gaming subscription", "Entertainment"),
    ("streaming service", "Entertainment"),
    ("cinema", "Entertainment"),
    ("event tickets", "Entertainment"),
    
    # Bills & Utilities
    ("electric bill", "Bills & Utilities"),
    ("water bill", "Bills & Utilities"),
    ("internet bill", "Bills & Utilities"),
    ("phone bill", "Bills & Utilities"),
    ("gas bill", "Bills & Utilities"),
    ("cable tv", "Bills & Utilities"),
    ("rent payment", "Bills & Utilities"),
    ("mortgage payment", "Bills & Utilities"),
    ("insurance premium", "Bills & Utilities"),
    
    # Healthcare
    ("doctor visit", "Healthcare"),
    ("pharmacy purchase", "Healthcare"),
    ("hospital bill", "Healthcare"),
    ("dental care", "Healthcare"),
    ("medical prescription", "Healthcare"),
    ("health insurance", "Healthcare"),
    ("gym membership", "Healthcare"),
    
    # Education
    ("course fees", "Education"),
    ("textbook purchase", "Education"),
    ("tuition payment", "Education"),
    ("online course", "Education"),
    ("school supplies", "Education"),
    ("library fees", "Education"),
    
    # Travel
    ("flight booking", "Travel"),
    ("hotel reservation", "Travel"),
    ("airbnb booking", "Travel"),
    ("vacation rental", "Travel"),
    ("travel insurance", "Travel"),
    ("car rental", "Travel"),
    ("travel agency", "Travel"),
    
    # Business & Work
    ("office supplies", "Business & Work"),
    ("software subscription", "Business & Work"),
    ("business meeting", "Business & Work"),
    ("client dinner", "Business & Work"),
    ("conference fees", "Business & Work"),
    ("professional services", "Business & Work"),
    
    # Personal Care
    ("haircut", "Personal Care"),
    ("salon services", "Personal Care"),
    ("cosmetics purchase", "Personal Care"),
    ("skincare products", "Personal Care"),
    ("personal grooming", "Personal Care"),
]

# Category keywords for rule-based categorization
CATEGORY_KEYWORDS = {
    "Food & Dining": ["food", "restaurant", "coffee", "lunch", "dinner", "breakfast", "grocery", "pizza", "cafe", "bakery", "meal", "eat", "dining"],
    "Shopping": ["amazon", "walmart", "target", "store", "shop", "purchase", "buy", "retail", "mall", "clothing", "electronics"],
    "Transportation": ["uber", "lyft", "taxi", "bus", "train", "metro", "gas", "petrol", "parking", "toll", "ride", "transport"],
    "Entertainment": ["netflix", "spotify", "movie", "concert", "gaming", "streaming", "cinema", "theater", "event", "ticket"],
    "Bills & Utilities": ["bill", "utility", "electric", "water", "internet", "phone", "rent", "mortgage", "insurance", "cable"],
    "Healthcare": ["doctor", "hospital", "pharmacy", "medical", "dental", "health", "gym", "medicine", "prescription"],
    "Education": ["course", "education", "school", "tuition", "book", "learning", "college", "university", "training"],
    "Travel": ["flight", "hotel", "airbnb", "vacation", "travel", "booking", "rental", "trip", "airport"],
    "Business & Work": ["business", "office", "work", "professional", "client", "meeting", "conference", "software", "subscription"],
    "Personal Care": ["haircut", "salon", "cosmetics", "skincare", "grooming", "beauty", "personal care"],
}

# Financial entity patterns for FinBERT-style categorization
FINANCIAL_ENTITIES = {
    "Food & Dining": ["restaurant", "food", "coffee", "meal", "dining"],
    "Transportation": ["transport", "vehicle", "fuel", "parking", "mobility"],
    "Shopping": ["retail", "ecommerce", "purchase", "consumer", "goods"],
    "Entertainment": ["media", "entertainment", "streaming", "content"],
    "Bills & Utilities": ["utility", "housing", "energy", "telecommunications"],
    "Healthcare": ["healthcare", "medical", "pharmaceutical", "wellness"],
    "Education": ["education", "learning", "academic", "training"],
    "Travel": ["travel", "hospitality", "accommodation", "transportation"],
    "Business": ["business", "corporate", "professional", "commercial"],
    "Personal": ["personal", "lifestyle", "care", "services"],
}

_model = None
_finbert_model = None

def train_model():
    """Train enhanced categorization model"""
    texts, labels = zip(*TRAIN_DATA)
    
    # Use RandomForest for better performance
    pipeline = make_pipeline(
        TfidfVectorizer(
            ngram_range=(1, 2),  # Include bigrams
            max_features=5000,
            stop_words='english'
        ),
        RandomForestClassifier(
            n_estimators=100,
            random_state=42,
            max_depth=10
        )
    )
    
    pipeline.fit(texts, labels)
    
    # Save model
    os.makedirs("models", exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    
    return pipeline

def load_model():
    """Load or train the categorization model"""
    global _model
    if _model is None:
        if os.path.exists(MODEL_PATH):
            _model = joblib.load(MODEL_PATH)
        else:
            _model = train_model()
    return _model

def rule_based_categorize(description: str) -> str:
    """Rule-based categorization as fallback"""
    description_lower = description.lower()
    
    for category, keywords in CATEGORY_KEYWORDS.items():
        if any(keyword in description_lower for keyword in keywords):
            return category
    
    return "Miscellaneous"

def extract_financial_entities(description: str) -> Dict[str, float]:
    """Extract financial entities (simplified FinBERT approach)"""
    description_lower = description.lower()
    entity_scores = {}
    
    for category, entities in FINANCIAL_ENTITIES.items():
        score = 0
        for entity in entities:
            if entity in description_lower:
                score += 1
        entity_scores[category] = score / len(entities)
    
    return entity_scores

def categorize(description: str, method: str = "hybrid") -> str:
    """
    Categorize expense description using hybrid approach
    method: "ml", "rule", "hybrid", "finbert"
    """
    description = description.strip().lower()
    
    if not description:
        return "Miscellaneous"
    
    if method == "rule":
        return rule_based_categorize(description)
    
    elif method == "finbert":
        # Simplified FinBERT-style categorization
        entity_scores = extract_financial_entities(description)
        if entity_scores:
            best_category = max(entity_scores, key=entity_scores.get)
            if entity_scores[best_category] > 0:
                return best_category
        return rule_based_categorize(description)
    
    elif method == "ml":
        model = load_model()
        return model.predict([description])[0]
    
    else:  # hybrid approach
        try:
            # Try ML model first
            model = load_model()
            ml_prediction = model.predict([description])[0]
            
            # Get confidence score
            proba = model.predict_proba([description])[0]
            confidence = max(proba)
            
            # If confidence is high, use ML prediction
            if confidence > 0.7:
                return ml_prediction
            
            # Otherwise, use rule-based as fallback
            rule_prediction = rule_based_categorize(description)
            
            # If rule-based gives a specific category (not miscellaneous), use it
            if rule_prediction != "Miscellaneous":
                return rule_prediction
            
            # Fall back to ML prediction
            return ml_prediction
            
        except Exception:
            return rule_based_categorize(description)

def get_category_confidence(description: str) -> Tuple[str, float]:
    """Get category and confidence score"""
    try:
        model = load_model()
        prediction = model.predict([description])[0]
        proba = model.predict_proba([description])[0]
        confidence = max(proba)
        return prediction, confidence
    except Exception:
        fallback = rule_based_categorize(description)
        return fallback, 0.5

def get_all_categories() -> List[str]:
    """Get list of all available categories"""
    return list(CATEGORY_KEYWORDS.keys()) + ["Miscellaneous"]

def add_training_data(description: str, category: str):
    """Add new training data and retrain model"""
    global TRAIN_DATA, _model
    
    TRAIN_DATA.append((description.lower(), category))
    
    # Retrain model with new data
    _model = train_model()

def analyze_spending_patterns(expenses: List[Dict]) -> Dict:
    """Analyze spending patterns across categories"""
    if not expenses:
        return {}
    
    category_totals = {}
    category_counts = {}
    
    for expense in expenses:
        category = expense.get('category', 'Miscellaneous')
        amount = float(expense.get('amount', 0))
        
        category_totals[category] = category_totals.get(category, 0) + amount
        category_counts[category] = category_counts.get(category, 0) + 1
    
    # Calculate averages and percentages
    total_spending = sum(category_totals.values())
    analysis = {}
    
    for category in category_totals:
        analysis[category] = {
            'total': category_totals[category],
            'count': category_counts[category],
            'average': category_totals[category] / category_counts[category],
            'percentage': (category_totals[category] / total_spending * 100) if total_spending > 0 else 0
        }
    
    return analysis
