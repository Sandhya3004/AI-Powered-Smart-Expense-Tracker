import easyocr
import numpy as np
import cv2
import pytesseract
import re
from PIL import Image
import io

# Initialize readers once (lazy loading)
_reader = None

def get_reader():
    global _reader
    if _reader is None:
        _reader = easyocr.Reader(['en'])  # only need English
    return _reader

def preprocess_image(image_bytes):
    """Preprocess image for better OCR accuracy"""
    # Convert bytes to PIL Image
    image = Image.open(io.BytesIO(image_bytes))
    
    # Convert to OpenCV format
    img_array = np.array(image)
    
    # Convert to grayscale
    if len(img_array.shape) == 3:
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
    else:
        gray = img_array
    
    # Apply adaptive thresholding
    thresh = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)
    
    # Noise removal
    kernel = np.ones((1,1), np.uint8)
    opening = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel)
    
    return opening

def extract_text_from_image(image_bytes, method='tesseract'):
    """Extract text from image using Tesseract or EasyOCR"""
    try:
        if method == 'tesseract':
            return extract_with_tesseract(image_bytes)
        else:
            return extract_with_easyocr(image_bytes)
    except Exception as e:
        # Fallback to alternative method
        if method == 'tesseract':
            return extract_with_easyocr(image_bytes)
        else:
            return extract_with_tesseract(image_bytes)

def extract_with_tesseract(image_bytes):
    """Extract text using Tesseract OCR"""
    # Preprocess image
    processed_img = preprocess_image(image_bytes)
    
    # Configure Tesseract for receipt recognition
    custom_config = r'--oem 3 --psm 6 -c tessedit_char_whitelist=0123456789.$₹ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz '
    
    # Extract text
    text = pytesseract.image_to_string(processed_img, config=custom_config)
    
    return text.strip()

def extract_with_easyocr(image_bytes):
    """Extract text using EasyOCR (fallback)"""
    # Convert bytes to numpy array (OpenCV format)
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    reader = get_reader()
    result = reader.readtext(img, detail=0)  # detail=0 returns only text
    return " ".join(result)

def extract_receipt_data(image_bytes):
    """Extract structured receipt data with enhanced parsing"""
    text = extract_text_from_image(image_bytes, method='tesseract')
    
    # Enhanced regex patterns for receipt parsing
    receipt_data = {
        'raw_text': text,
        'amount': None,
        'merchant': None,
        'date': None,
        'items': [],
        'tax': None,
        'total': None
    }
    
    # Extract amounts (multiple patterns)
    amount_patterns = [
        r'(?:total|amount|sum|pay|paid)[:\s]*[$₹]?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)',
        r'[$₹]\s*(\d+(?:,\d{3})*(?:\.\d{2})?)',
        r'(\d+(?:,\d{3})*(?:\.\d{2})?)\s*(?:usd|eur|gbp|inr|rs|rupees)',
    ]
    
    for pattern in amount_patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            amount_str = match.group(1).replace(',', '')
            try:
                receipt_data['amount'] = float(amount_str)
                break
            except ValueError:
                continue
    
    # Extract merchant name (first line or common patterns)
    lines = text.split('\n')
    for line in lines[:5]:  # Check first 5 lines
        line = line.strip()
        if len(line) > 3 and not re.search(r'\d', line) and not re.match(r'^[a-z]+$', line.lower()):
            receipt_data['merchant'] = line
            break
    
    # Extract date (multiple formats)
    date_patterns = [
        r'(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})',
        r'(\d{4}[/-]\d{1,2}[/-]\d{1,2})',
        r'(\d{1,2}\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\s+\d{2,4})',
    ]
    
    for pattern in date_patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            receipt_data['date'] = match.group(1)
            break
    
    # Extract line items
    item_pattern = r'(.+?)\s+(\d+(?:,\d{3})*(?:\.\d{2})?)\s*(?:x\s*(\d+))?'
    for match in re.finditer(item_pattern, text, re.MULTILINE):
        item_name = match.group(1).strip()
        price = float(match.group(2).replace(',', ''))
        quantity = int(match.group(3)) if match.group(3) else 1
        
        if len(item_name) > 2 and price > 0:
            receipt_data['items'].append({
                'name': item_name,
                'price': price,
                'quantity': quantity
            })
    
    return receipt_data
