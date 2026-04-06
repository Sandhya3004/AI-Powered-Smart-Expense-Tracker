from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any
import base64
import io
import re
from PIL import Image
import pytesseract
import uvicorn
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Expense AI Service", version="1.0.0")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://expense-tracker-api-1onn.onrender.com", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class VoiceInput(BaseModel):
    audioData: str
    format: str = "wav"

class VoiceResponse(BaseModel):
    success: bool
    data: Optional[Dict[str, Any]] = None
    transcription: Optional[str] = None
    amount: Optional[float] = None
    description: Optional[str] = None
    category: Optional[str] = None
    expenseDate: Optional[str] = None
    error: Optional[str] = None

class OCRResponse(BaseModel):
    success: bool
    extracted_data: Optional[Dict[str, Any]] = None
    error: Optional[str] = None

class HealthResponse(BaseModel):
    status: str

def extract_expense_from_text(text: str) -> Dict[str, Any]:
    """Extract expense information from transcribed text using simple patterns"""
    text = text.lower().strip()
    
    # Pattern to find amounts (e.g., "spent 500", "₹500", "500 rupees")
    amount_pattern = r'(?:spent|paid|cost|₹|rs\.?\s*|rupees?\s*|rs\.?\s*)\s*([0-9]+(?:\.[0-9]{1,2})?)'
    amount_match = re.search(amount_pattern, text)
    
    # Pattern to find common expense categories
    categories = {
        'food': ['food', 'dining', 'restaurant', 'groceries', 'meal', 'lunch', 'dinner'],
        'transport': ['uber', 'taxi', 'auto', 'rickshaw', 'metro', 'bus', 'fuel', 'petrol'],
        'shopping': ['shopping', 'mall', 'clothes', 'shoes', 'electronics', 'amazon', 'flipkart'],
        'utilities': ['electricity', 'water', 'phone', 'internet', 'bill', 'recharge'],
        'entertainment': ['movie', 'netflix', 'prime', 'game', 'subscription'],
        'health': ['medicine', 'doctor', 'hospital', 'pharmacy'],
        'other': []
    }
    
    amount = 0.0
    if amount_match:
        try:
            amount = float(amount_match.group(1))
        except ValueError:
            amount = 0.0
    
    # Find category
    category = "Other"
    for cat, keywords in categories.items():
        if any(keyword in text for keyword in keywords):
            category = cat.capitalize()
            break
    
    # Extract description (everything except the amount part)
    description = text
    if amount_match:
        description = re.sub(amount_pattern, '', description).strip()
    
    # Clean up description
    description = re.sub(r'(?:spent|paid|cost|₹|rs\.?\s*|rupees?\s*|rs\.?\s*)', '', description).strip()
    
    if not description:
        description = f"Expense of ₹{amount}"
    
    return {
        "amount": amount,
        "description": description,
        "category": category,
        "expenseDate": str(datetime.now().date())
    }

def extract_expense_from_text(text: str) -> Dict[str, Any]:
    """Extract expense information from text using simple pattern matching"""
    try:
        # Extract amount
        amount_patterns = [
            r'(?:spent|paid|cost)\s+(\$?)(\d+(?:\.\d{2})?)',
            r'(\d+(?:\.\d{2})?)\s*(?:dollars?|bucks?|rs|rupees?)',
            r'(\d+(?:\.\d{2})?)'
        ]
        
        amount = 0.0
        for pattern in amount_patterns:
            match = re.search(pattern, text.lower())
            if match:
                try:
                    amount = float(match.group(2) if match.lastindex and match.lastindex >= 2 else match.group(1))
                    break
                except (ValueError, IndexError):
                    continue
        
        # Extract category
        text_lower = text.lower()
        if any(keyword in text_lower for keyword in ['grocery', 'food', 'supermarket', 'restaurant']):
            category = "Food & Dining"
        elif any(keyword in text_lower for keyword in ['gas', 'petrol', 'fuel', 'uber', 'taxi']):
            category = "Transportation"
        elif any(keyword in text_lower for keyword in ['movie', 'netflix', 'entertainment']):
            category = "Entertainment"
        else:
            category = "Other"
        
        # Generate description
        description = text.strip() if text.strip() else "Voice expense"
        
        return {
            "amount": amount,
            "description": description,
            "category": category,
            "expenseDate": str(datetime.now().date())
        }
        
    except Exception as e:
        logger.error(f"Error extracting expense from text: {e}")
        return {
            "amount": 0.0,
            "description": "Voice expense",
            "category": "Other",
            "expenseDate": str(datetime.now().date())
        }

def preprocess_image(image_bytes: bytes) -> bytes:
    """Preprocess image for better OCR accuracy"""
    try:
        # Convert to PIL Image
        image = Image.open(io.BytesIO(image_bytes))
        
        # Convert to grayscale
        if image.mode != 'L':
            image = image.convert('L')
        
        # Increase contrast
        from PIL import ImageEnhance
        enhancer = ImageEnhance.Contrast(image)
        image = enhancer.enhance(2.0)
        
        # Convert back to bytes
        img_byte_arr = io.BytesIO()
        image.save(img_byte_arr, format='PNG')
        return img_byte_arr.getvalue()
    except Exception as e:
        logger.error(f"Image preprocessing failed: {e}")
        return image_bytes

def extract_receipt_data(image_bytes: bytes) -> Dict[str, Any]:
    """Extract data from receipt image using OCR with improved processing"""
    try:
        # Preprocess image
        processed_bytes = preprocess_image(image_bytes)
        
        # Perform OCR with multiple configurations
        text_psm6 = pytesseract.image_to_string(
            Image.open(io.BytesIO(processed_bytes)),
            config='--psm 6 --oem 3 -c tessedit_char_whitelist 0123456789.,₹Rs'
        )
        
        text_psm4 = pytesseract.image_to_string(
            Image.open(io.BytesIO(processed_bytes)),
            config='--psm 4 --oem 3'
        )
        
        # Use the better result
        text = text_psm6 if len(text_psm6.strip()) > len(text_psm4.strip()) else text_psm4
        
        logger.info(f"OCR extracted text: {text}")
        
        if not text or len(text.strip()) < 3:
            return {"error": "No readable text found in receipt"}
        
        # Try to extract merchant (first line or first words)
        lines = [line.strip() for line in text.split('\n') if line.strip()]
        merchant = lines[0] if lines else "Unknown Merchant"
        
        # Extract amount with multiple patterns
        amount_patterns = [
            r'(?:₹|rs\.?\s*|rupees?\s*|rs\.?\s*)\s*([0-9]+(?:\.[0-9]{1,2})?)',
            r'(?:total|amount|sum)\s*[:\-]?\s*([0-9]+(?:\.[0-9]{1,2})?)',
            r'([0-9]+(?:\.[0-9]{1,2})?)\s*(?:₹|rs|rupees)'
        ]
        
        total_amount = 0.0
        for pattern in amount_patterns:
            amount_match = re.search(pattern, text, re.IGNORECASE)
            if amount_match:
                try:
                    total_amount = float(amount_match.group(1))
                    break
                except ValueError:
                    continue
        
        # Try to extract date with multiple patterns
        date_patterns = [
            r'\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b',
            r'\b(\d{4}[/-]\d{1,2}[/-]\d{1,2})\b',
            r'(?:date|dated?)\s*[:\-]?\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})'
        ]
        
        date = str(datetime.now().date())
        for pattern in date_patterns:
            date_match = re.search(pattern, text)
            if date_match:
                date = date_match.group(1)
                break
        
        # Generate description from merchant and amount
        description = f"Purchase at {merchant}" if merchant != "Unknown Merchant" else "Receipt Purchase"
        
        # Try to categorize based on merchant name
        merchant_lower = merchant.lower()
        if any(keyword in merchant_lower for keyword in ['restaurant', 'cafe', 'food', 'dining']):
            category = "Food & Dining"
        elif any(keyword in merchant_lower for keyword in ['petrol', 'gas', 'fuel', 'oil']):
            category = "Transportation"
        elif any(keyword in merchant_lower for keyword in ['supermarket', 'grocery', 'store']):
            category = "Groceries"
        elif any(keyword in merchant_lower for keyword in ['pharmacy', 'medical', 'medicine']):
            category = "Healthcare"
        else:
            category = "Other"
        
        return {
            "merchant": merchant,
            "total_amount": total_amount,
            "date": date,
            "description": description,
            "category": category
        }
        
    except Exception as e:
        logger.error(f"OCR processing failed: {e}")
        return {"error": f"OCR processing failed: {str(e)}"}

@app.post("/nlp/process-voice", response_model=VoiceResponse)
async def process_voice(request: VoiceInput):
    """Process voice input and extract expense information"""
    try:
        logger.info(f"Processing voice input with format: {request.format}")
        
        # Validate input
        if not request.audioData:
            return VoiceResponse(success=False, error="Audio data is required")
        
        # Decode base64 audio data
        try:
            audio_bytes = base64.b64decode(request.audioData)
            logger.info(f"Audio decoded, size: {len(audio_bytes)} bytes")
        except Exception as e:
            logger.error(f"Failed to decode audio data: {e}")
            return VoiceResponse(success=False, error="Invalid audio data format")
        
        # For now, we'll use a mock transcription
        # In a real implementation, you would use speech recognition here
        mock_transcription = "I spent 500 on groceries today"
        logger.info(f"Mock transcription: {mock_transcription}")
        
        # Extract expense information from transcription
        extracted_data = extract_expense_from_text(mock_transcription)
        
        return VoiceResponse(
            success=True,
            data=extracted_data,
            transcription=mock_transcription,
            **extracted_data
        )
        
    except Exception as e:
        logger.error(f"Voice processing failed: {e}")
        return VoiceResponse(success=False, error=f"Voice processing failed: {str(e)}")

@app.post("/ocr/receipt", response_model=OCRResponse)
async def ocr_receipt(receipt: UploadFile = File(...)):
    """Process receipt image and extract expense information"""
    try:
        logger.info(f"Processing receipt image: {receipt.filename}")
        
        # Validate file
        if not receipt:
            return OCRResponse(success=False, error="No file provided")
        
        # Validate file type
        if receipt.content_type and not receipt.content_type.startswith('image/'):
            return OCRResponse(success=False, error="Invalid file type. Please upload an image.")
        
        # Read image bytes
        image_bytes = await receipt.read()
        
        if len(image_bytes) == 0:
            return OCRResponse(success=False, error="Empty file provided")
        
        # Extract data using OCR
        extracted_data = extract_receipt_data(image_bytes)
        
        if "error" in extracted_data:
            return OCRResponse(success=False, error=extracted_data["error"])
        
        return OCRResponse(
            success=True,
            extracted_data=extracted_data
        )
        
    except Exception as e:
        logger.error(f"OCR processing failed: {e}")
        return OCRResponse(success=False, error=f"OCR processing failed: {str(e)}")

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    return HealthResponse(status="healthy")

if __name__ == "__main__":
    uvicorn.run(
        "app:app",
        host="0.0.0.0",
        port=5000,
        reload=True,
        limit_max_request_body_size=10_485_760  # 10 MB
    )
