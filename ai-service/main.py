import re
from datetime import datetime
from fastapi import FastAPI, File, UploadFile, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict
import uvicorn

# Try to import services with graceful fallbacks
try:
    from services import ocr_service, voice_service, budget_planner, group_expenses, bill_reminders, categorizer, anomaly_detector, predictor
    SERVICES_AVAILABLE = True
except ImportError as e:
    print(f"Warning: Some services not available: {e}")
    SERVICES_AVAILABLE = False
    # Create stub modules
    class StubService:
        @staticmethod
        def categorize(*args, **kwargs):
            return "Other"
        @staticmethod
        def detect_anomalies(*args, **kwargs):
            return []
        @staticmethod
        def parse_voice_expense(*args, **kwargs):
            return {'amount': None, 'category': None, 'description': None, 'date': None, 'merchant': None, 'confidence': 0.0}
        @staticmethod
        def extract_receipt_data(*args, **kwargs):
            return {'amount': 0.0, 'merchant': 'Unknown', 'date': datetime.now().strftime('%Y-%m-%d'), 'description': 'Receipt Expense', 'category': 'Other'}
        @staticmethod
        def extract_text_from_image(*args, **kwargs):
            return ""
        @staticmethod
        def extract_expense_from_conversation(*args, **kwargs):
            return []
        @staticmethod
        def train_budget_model(*args, **kwargs):
            return False, "Service unavailable"
        @staticmethod
        def generate_budget_suggestions(*args, **kwargs):
            return {"error": "Budget service unavailable"}
        @staticmethod
        def analyze_budget_performance(*args, **kwargs):
            return {"error": "Budget service unavailable"}
        @staticmethod
        def get_budget_recommendations_for_user(*args, **kwargs):
            return {"recommendations": []}
        @staticmethod
        def create_expense_group(*args, **kwargs):
            return "stub-group-id"
        @staticmethod
        def add_group_expense(*args, **kwargs):
            return "stub-expense-id"
        @staticmethod
        def get_group_summary(*args, **kwargs):
            return {"error": "Group service unavailable"}
        @staticmethod
        def set_group_expense(*args, **kwargs):
            return False
        @staticmethod
        def get_group_manager(*args, **kwargs):
            return None
        @staticmethod
        def create_bill_reminder(*args, **kwargs):
            return "stub-reminder-id"
        @staticmethod
        def get_user_bill_reminders(*args, **kwargs):
            return []
        @staticmethod
        def get_upcoming_bills(*args, **kwargs):
            return []
        @staticmethod
        def snooze_bill_reminder(*args, **kwargs):
            return False
        @staticmethod
        def get_bill_reminder_statistics(*args, **kwargs):
            return {}
        @staticmethod
        def train_model(*args, **kwargs):
            return None, {}
        @staticmethod
        def predict_next_month(*args, **kwargs):
            return None, "Service unavailable"
        @staticmethod
        def forecast_expenses(*args, **kwargs):
            return [], "unavailable", {}

    ocr_service = StubService()
    voice_service = StubService()
    budget_planner = StubService()
    group_expenses = StubService()
    bill_reminders = StubService()
    categorizer = StubService()
    anomaly_detector = StubService()
    predictor = StubService()

app = FastAPI(title="Expense AI Microservice")

class CategorizeRequest(BaseModel):
    description: str

class CategorizeResponse(BaseModel):
    category: str

class AnomalyRequest(BaseModel):
    amounts: List[float]

class AnomalyResponse(BaseModel):
    anomaly_indices: List[int]

class PredictRequest(BaseModel):
    monthly_totals: List[float]  # amounts for past months, in order
    last_month_index: int

class PredictResponse(BaseModel):
    predicted_amount: Optional[float]
    message: str

class VoiceRequest(BaseModel):
    text: str

class VoiceResponse(BaseModel):
    amount: Optional[float]
    category: Optional[str]
    description: Optional[str]
    date: Optional[str]
    merchant: Optional[str]
    confidence: float
    command: Optional[str]

@app.post("/categorize", response_model=CategorizeResponse)
async def categorize_endpoint(req: CategorizeRequest):
    category = categorizer.categorize(req.description)
    return CategorizeResponse(category=category)

@app.post("/anomaly", response_model=AnomalyResponse)
async def anomaly_endpoint(req: AnomalyRequest):
    indices = anomaly_detector.detect_anomalies(req.amounts)
    return AnomalyResponse(anomaly_indices=indices)

@app.post("/predict", response_model=PredictResponse)
async def predict_endpoint(req: PredictRequest):
    try:
        # Prepare training data: list of (index, amount) tuples
        data = [(i, amt) for i, amt in enumerate(req.monthly_totals)]
        best_model, metrics = predictor.train_model(data)
        
        if best_model is None:
            return PredictResponse(predicted_amount=None, message="Insufficient data for prediction")
        
        pred, details = predictor.predict_next_month(req.last_month_index)
        return PredictResponse(predicted_amount=pred, message=f"Prediction generated using {best_model} model")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

class ForecastRequest(BaseModel):
    monthly_totals: List[float]
    months_ahead: int = 3

class ForecastResponse(BaseModel):
    forecasts: List[Dict]
    best_model: str
    metrics: Dict
    message: str

class BudgetTrainRequest(BaseModel):
    expenses: List[Dict]
    income: Optional[List[Dict]] = None

class BudgetRequest(BaseModel):
    user_id: int
    monthly_income: float
    target_savings_rate: float = 0.2

class BudgetResponse(BaseModel):
    monthly_income: float
    predicted_spending: float
    target_savings_rate: float
    target_savings: float
    optimized_spending: float
    available_surplus: float
    category_allocation: Dict
    recommendations: List[Dict]
    confidence_score: float
    risk_level: str

class GroupCreateRequest(BaseModel):
    name: str
    description: str
    created_by: int
    members: List[Dict]

class GroupExpenseRequest(BaseModel):
    group_id: str
    paid_by: int
    description: str
    amount: float
    category: str
    split_type: str
    participants: List[int]
    splits: Optional[Dict[int, float]] = None
    receipts: Optional[List[str]] = None
    notes: str = ""

class SettlementRequest(BaseModel):
    settlement_id: str
    amount: float
    notes: str = ""

class ReminderCreateRequest(BaseModel):
    user_id: int
    title: str
    description: str
    amount: float
    due_date: str
    reminder_type: str
    frequency: str
    notification_channels: List[str]
    metadata: Optional[Dict] = None

class ReminderSnoozeRequest(BaseModel):
    reminder_id: str
    snooze_days: int

@app.post("/forecast", response_model=ForecastResponse)
async def forecast_endpoint(req: ForecastRequest):
    try:
        # Prepare training data
        data = [(i, amt) for i, amt in enumerate(req.monthly_totals)]
        
        forecasts, best_model, metrics = predictor.forecast_expenses(data, req.months_ahead)
        
        if not forecasts:
            return ForecastResponse(
                forecasts=[],
                best_model="none",
                metrics={},
                message="Insufficient data for forecasting"
            )
        
        return ForecastResponse(
            forecasts=forecasts,
            best_model=best_model,
            metrics=metrics,
            message=f"Forecast generated using {best_model} model"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/ocr")
async def ocr_endpoint(file: UploadFile = File(...)):
    contents = await file.read()
    try:
        # Use enhanced receipt data extraction
        receipt_data = ocr_service.extract_receipt_data(contents)
        return receipt_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/ocr/basic")
async def ocr_basic_endpoint(file: UploadFile = File(...)):
    contents = await file.read()
    try:
        text = ocr_service.extract_text_from_image(contents)
        # Simple regex extraction (improve as needed)
        amount_match = re.search(r'[\$\u20B9]\s*(\d+\.?\d*)', text)  # $ or ₹
        amount = float(amount_match.group(1)) if amount_match else None
        merchant_match = re.search(r'(?:at|from|store|merchant)[:\s]+([A-Za-z\s]+)', text, re.IGNORECASE)
        merchant = merchant_match.group(1).strip() if merchant_match else "Unknown"
        date_match = re.search(r'(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})', text)
        date = date_match.group(1) if date_match else None
        return {"amount": amount, "merchant": merchant, "date": date, "raw_text": text}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/nlp/process-voice")
async def nlp_process_voice_endpoint(req: VoiceRequest):
    try:
        # Parse voice input using NLP
        expense_data = voice_service.parse_voice_expense(req.text)
        
        return {
            "success": True,
            "data": {
                "amount": expense_data.get('amount'),
                "description": expense_data.get('description'),
                "category": expense_data.get('category'),
                "merchant": expense_data.get('merchant'),
                "date": expense_data.get('date')
            }
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "data": {
                "amount": 0.0,
                "description": "Voice Expense",
                "category": "Other",
                "date": datetime.now().strftime("%Y-%m-%d")
            }
        }

@app.post("/ocr/receipt")
async def ocr_receipt_endpoint(file: UploadFile = File(...)):
    try:
        contents = await file.read()
        
        # Use enhanced receipt data extraction
        receipt_data = ocr_service.extract_receipt_data(contents)
        
        return {
            "success": True,
            "extracted_data": {
                "merchant": receipt_data.get("merchant", "Unknown"),
                "total_amount": receipt_data.get("amount", 0.0),
                "date": receipt_data.get("date", datetime.now().strftime("%Y-%m-%d")),
                "description": receipt_data.get("description", "Receipt Expense"),
                "category": receipt_data.get("category", "Other")
            }
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "extracted_data": {
                "merchant": "Unknown",
                "total_amount": 0.0,
                "date": datetime.now().strftime("%Y-%m-%d"),
                "description": "Receipt Expense",
                "category": "Other"
            }
        }

@app.post("/voice/conversation")
async def voice_conversation_endpoint(req: VoiceRequest):
    try:
        expenses = voice_service.extract_expense_from_conversation(req.text)
        return {"expenses": expenses, "count": len(expenses)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/budget/train")
async def budget_train_endpoint(req: BudgetTrainRequest):
    try:
        success, result = budget_planner.train_budget_model(req.expenses, req.income)
        
        if success:
            return {"message": "Budget model trained successfully", "metrics": result}
        else:
            return {"message": "Budget model training failed", "error": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/budget/suggest", response_model=BudgetResponse)
async def budget_suggest_endpoint(req: BudgetRequest):
    try:
        suggestions = budget_planner.generate_budget_suggestions(
            req.user_id, 
            req.monthly_income, 
            req.target_savings_rate
        )
        
        if "error" in suggestions:
            raise HTTPException(status_code=400, detail=suggestions["error"])
        
        return BudgetResponse(**suggestions)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/budget/analyze")
async def budget_analyze_endpoint(user_id: int, actual_spending: float, budget_target: float):
    try:
        analysis = budget_planner.analyze_budget_performance(user_id, actual_spending, budget_target)
        return analysis
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/budget/recommendations")
async def budget_recommendations_endpoint(user_id: int, expenses: List[Dict]):
    try:
        recommendations = budget_planner.get_budget_recommendations_for_user(user_id, expenses)
        return recommendations
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/group/create")
async def group_create_endpoint(req: GroupCreateRequest):
    try:
        group_id = group_expenses.create_expense_group(
            req.name, req.description, req.created_by, req.members
        )
        return {"group_id": group_id, "message": "Group created successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/group/expense")
async def group_expense_endpoint(req: GroupExpenseRequest):
    try:
        expense_id = group_expenses.add_group_expense(
            req.group_id, req.paid_by, req.description, req.amount,
            req.category, req.split_type, req.participants, req.splits,
            req.receipts, req.notes
        )
        return {"expense_id": expense_id, "message": "Expense added successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/group/{group_id}/summary")
async def group_summary_endpoint(group_id: str):
    try:
        summary = group_expenses.get_group_summary(group_id)
        return summary
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/group/settle")
async def group_settle_endpoint(req: SettlementRequest):
    try:
        success = group_expenses.set_group_expense(req.settlement_id, req.amount, req.notes)
        return {"success": success, "message": "Settlement processed successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/group/{group_id}/balance")
async def group_balance_endpoint(group_id: str):
    try:
        manager = group_expenses.get_group_manager()
        balances = manager.get_group_balance(group_id)
        return {"balances": balances}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/group/{group_id}/settlements")
async def group_settlements_endpoint(group_id: str):
    try:
        manager = group_expenses.get_group_manager()
        settlements = manager.get_settlement_summary(group_id)
        return {"settlements": settlements}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/reminder/create")
async def reminder_create_endpoint(req: ReminderCreateRequest):
    try:
        reminder_id = bill_reminders.create_bill_reminder(
            req.user_id, req.title, req.description, req.amount,
            req.due_date, req.reminder_type, req.frequency,
            req.notification_channels, req.metadata
        )
        return {"reminder_id": reminder_id, "message": "Reminder created successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/reminder/user/{user_id}")
async def reminder_user_endpoint(user_id: int, status: str = None):
    try:
        reminders = bill_reminders.get_user_bill_reminders(user_id, status)
        return {"reminders": reminders}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/reminder/upcoming/{user_id}")
async def reminder_upcoming_endpoint(user_id: int, days_ahead: int = 7):
    try:
        upcoming = bill_reminders.get_upcoming_bills(user_id, days_ahead)
        return {"upcoming_bills": upcoming, "count": len(upcoming)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/reminder/snooze")
async def reminder_snooze_endpoint(req: ReminderSnoozeRequest):
    try:
        success = bill_reminders.snooze_bill_reminder(req.reminder_id, req.snooze_days)
        return {"success": success, "message": "Reminder snoozed successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/reminder/stats/{user_id}")
async def reminder_stats_endpoint(user_id: int):
    try:
        stats = bill_reminders.get_bill_reminder_statistics(user_id)
        return stats
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "services_available": SERVICES_AVAILABLE,
        "timestamp": datetime.now().isoformat()
    }

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000)
