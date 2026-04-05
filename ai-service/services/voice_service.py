import stanza
import re
from typing import Dict, List, Optional
import spacy

# Initialize NLP models
_nlp = None
_stanza_pipeline = None

def get_spacy_model():
    global _nlp
    if _nlp is None:
        try:
            _nlp = spacy.load("en_core_web_sm")
        except OSError:
            # Fallback to basic processing if model not available
            _nlp = None
    return _nlp

def get_stanza_pipeline():
    global _stanza_pipeline
    if _stanza_pipeline is None:
        try:
            stanza.download('en')  # Download English models
            _stanza_pipeline = stanza.Pipeline('en', processors='tokenize,pos,ner')
        except Exception:
            _stanza_pipeline = None
    return _stanza_pipeline

def parse_voice_expense(text: str) -> Dict:
    """Parse voice input to extract expense information"""
    result = {
        'amount': None,
        'category': None,
        'description': None,
        'date': None,
        'merchant': None,
        'confidence': 0.0
    }
    
    # Convert to lowercase for processing
    text_lower = text.lower()
    
    # Extract amount using multiple patterns
    amount_patterns = [
        r'(\d+(?:\.\d{2})?)\s*(?:dollars?|bucks?|rupees?|rs|₹|\$)',
        r'(?:spent|paid|cost)\s+(\d+(?:\.\d{2})?)',
        r'(\d+(?:\.\d{2})?)\s*(?:for|on)',
        r'(\d+(?:\.\d{2})?)',
    ]
    
    for pattern in amount_patterns:
        match = re.search(pattern, text_lower)
        if match:
            try:
                result['amount'] = float(match.group(1))
                result['confidence'] += 0.3
                break
            except ValueError:
                continue
    
    # Extract category using keyword mapping
    category_keywords = {
        'food': ['food', 'lunch', 'dinner', 'breakfast', 'restaurant', 'cafe', 'coffee', 'meal', 'eat', 'ate'],
        'transport': ['uber', 'taxi', 'cab', 'bus', 'train', 'metro', 'gas', 'petrol', 'fuel', 'parking', 'toll'],
        'shopping': ['bought', 'shopping', 'store', 'mall', 'amazon', 'flipkart', 'purchase', 'clothes', 'shoes'],
        'entertainment': ['movie', 'cinema', 'netflix', 'spotify', 'game', 'concert', 'show', 'ticket'],
        'bills': ['electricity', 'water', 'internet', 'phone', 'rent', 'mortgage', 'insurance', 'bill'],
        'healthcare': ['doctor', 'medicine', 'hospital', 'pharmacy', 'medical', 'health'],
        'education': ['course', 'book', 'tuition', 'school', 'college', 'education'],
        'travel': ['flight', 'hotel', 'trip', 'vacation', 'travel', 'booking'],
    }
    
    for category, keywords in category_keywords.items():
        if any(keyword in text_lower for keyword in keywords):
            result['category'] = category
            result['confidence'] += 0.2
            break
    
    # Extract merchant/vendor names
    merchant_patterns = [
        r'(?:at|from|in)\s+([a-z\s]{3,20})\s*(?:for|spent|paid)',
        r'([a-z\s]{3,20})\s+(?:store|shop|restaurant|cafe)',
    ]
    
    for pattern in merchant_patterns:
        match = re.search(pattern, text_lower)
        if match:
            merchant = match.group(1).strip().title()
            if len(merchant) > 2:
                result['merchant'] = merchant
                result['confidence'] += 0.1
                break
    
    # Extract description (everything else)
    # Remove amount and common words to get description
    cleaned_text = text
    if result['amount']:
        cleaned_text = re.sub(str(result['amount']), '', cleaned_text, flags=re.IGNORECASE)
    
    # Remove common expense words
    expense_words = ['spent', 'paid', 'cost', 'dollars', 'rupees', 'bucks', 'for', 'on', 'at', 'from']
    for word in expense_words:
        cleaned_text = re.sub(rf'\b{word}\b', '', cleaned_text, flags=re.IGNORECASE)
    
    result['description'] = cleaned_text.strip() if cleaned_text.strip() else text
    
    # Use Stanza for enhanced NER if available
    if get_stanza_pipeline():
        try:
            doc = _stanza_pipeline(text)
            for ent in doc.ents:
                if ent.type == 'MONEY' and not result['amount']:
                    try:
                        result['amount'] = float(re.sub(r'[^\d.]', '', ent.text))
                        result['confidence'] += 0.2
                    except ValueError:
                        pass
                elif ent.type == 'ORG' and not result['merchant']:
                    result['merchant'] = ent.text
                    result['confidence'] += 0.1
        except Exception:
            pass
    
    # Normalize confidence
    result['confidence'] = min(result['confidence'], 1.0)
    
    return result

def extract_expense_from_conversation(text: str) -> List[Dict]:
    """Extract multiple expenses from a conversational text"""
    sentences = re.split(r'[.!?]+', text)
    expenses = []
    
    for sentence in sentences:
        sentence = sentence.strip()
        if len(sentence) > 5:  # Skip very short sentences
            expense = parse_voice_expense(sentence)
            if expense['amount'] and expense['confidence'] > 0.3:
                expenses.append(expense)
    
    return expenses

def voice_command_recognition(text: str) -> Dict:
    """Recognize voice commands for expense management"""
    text_lower = text.lower()
    
    commands = {
        'add_expense': ['add expense', 'new expense', 'spent', 'paid for'],
        'show_expenses': ['show expenses', 'list expenses', 'my expenses', 'recent expenses'],
        'show_budget': ['show budget', 'budget status', 'how much left'],
        'categorize': ['categorize', 'category'],
        'delete': ['delete', 'remove'],
        'help': ['help', 'what can i say'],
    }
    
    for command, patterns in commands.items():
        if any(pattern in text_lower for pattern in patterns):
            return {'command': command, 'text': text}
    
    return {'command': 'unknown', 'text': text}
