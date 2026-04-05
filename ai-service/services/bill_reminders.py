import redis
import json
from typing import Dict, List, Optional
from datetime import datetime, timedelta
from dataclasses import dataclass, asdict
from enum import Enum
import uuid
import os

# Try to import Celery
try:
    from celery import Celery
    from celery.schedules import crontab
    CELERY_AVAILABLE = True
except ImportError:
    CELERY_AVAILABLE = False

class ReminderType(Enum):
    BILL_DUE = "bill_due"
    SUBSCRIPTION_RENEWAL = "subscription_renewal"
    BUDGET_ALERT = "budget_alert"
    SAVINGS_GOAL = "savings_goal"
    CUSTOM = "custom"

class ReminderFrequency(Enum):
    ONCE = "once"
    DAILY = "daily"
    WEEKLY = "weekly"
    MONTHLY = "monthly"
    QUARTERLY = "quarterly"
    YEARLY = "yearly"

class ReminderStatus(Enum):
    PENDING = "pending"
    SENT = "sent"
    SNOOZED = "snoozed"
    COMPLETED = "completed"
    CANCELLED = "cancelled"

@dataclass
class BillReminder:
    reminder_id: str
    user_id: int
    title: str
    description: str
    amount: float
    due_date: datetime
    reminder_type: ReminderType
    frequency: ReminderFrequency
    status: ReminderStatus
    notification_channels: List[str]  # email, sms, push, in_app
    created_at: datetime
    updated_at: datetime
    next_reminder: Optional[datetime] = None
    snooze_until: Optional[datetime] = None
    metadata: Dict = None

class BillReminderManager:
    def __init__(self, redis_url: str = None):
        self.redis_client = None
        self.celery_app = None
        
        # Initialize Redis
        try:
            self.redis_client = redis.from_url(redis_url or os.getenv('REDIS_URL', 'redis://localhost:6379'))
            self.redis_client.ping()  # Test connection
        except Exception as e:
            print(f"Redis connection failed: {e}")
            self.redis_client = None
        
        # Initialize Celery
        if CELERY_AVAILABLE:
            try:
                self.celery_app = Celery(
                    'bill_reminders',
                    broker=redis_url or os.getenv('REDIS_URL', 'redis://localhost:6379'),
                    backend=redis_url or os.getenv('REDIS_URL', 'redis://localhost:6379')
                )
                self._setup_celery_tasks()
            except Exception as e:
                print(f"Celery setup failed: {e}")
                self.celery_app = None
    
    def _setup_celery_tasks(self):
        """Setup Celery tasks for scheduled reminders"""
        if not self.celery_app:
            return
        
        @self.celery_app.task
        def send_bill_reminder(reminder_id: str):
            """Send bill reminder notification"""
            try:
                reminder = self.get_reminder(reminder_id)
                if reminder and reminder.status == ReminderStatus.PENDING:
                    self._send_notification(reminder)
                    reminder.status = ReminderStatus.SENT
                    reminder.updated_at = datetime.now()
                    self.update_reminder(reminder)
                    
                    # Schedule next reminder if recurring
                    if reminder.frequency != ReminderFrequency.ONCE:
                        self._schedule_next_reminder(reminder)
            except Exception as e:
                print(f"Failed to send reminder {reminder_id}: {e}")
        
        @self.celery_app.task
        def check_overdue_bills():
            """Check for overdue bills and send alerts"""
            try:
                overdue_reminders = self.get_overdue_reminders()
                for reminder in overdue_reminders:
                    self._send_overdue_alert(reminder)
            except Exception as e:
                print(f"Failed to check overdue bills: {e}")
        
        # Schedule periodic tasks
        self.celery_app.conf.beat_schedule = {
            'check-overdue-bills': {
                'task': 'bill_reminders.check_overdue_bills',
                'schedule': crontab(hour=9, minute=0),  # Daily at 9 AM
            },
        }
    
    def create_reminder(self, user_id: int, title: str, description: str, 
                       amount: float, due_date: datetime, reminder_type: str,
                       frequency: str, notification_channels: List[str],
                       metadata: Dict = None) -> str:
        """Create a new bill reminder"""
        reminder_id = str(uuid.uuid4())
        
        reminder = BillReminder(
            reminder_id=reminder_id,
            user_id=user_id,
            title=title,
            description=description,
            amount=amount,
            due_date=due_date,
            reminder_type=ReminderType(reminder_type),
            frequency=ReminderFrequency(frequency),
            status=ReminderStatus.PENDING,
            notification_channels=notification_channels,
            created_at=datetime.now(),
            updated_at=datetime.now(),
            next_reminder=due_date,
            metadata=metadata or {}
        )
        
        # Save to Redis
        if self.redis_client:
            self._save_reminder_to_redis(reminder)
        
        # Schedule with Celery
        if self.celery_app and reminder.next_reminder:
            self._schedule_celery_task(reminder)
        
        return reminder_id
    
    def _save_reminder_to_redis(self, reminder: BillReminder):
        """Save reminder to Redis"""
        if not self.redis_client:
            return
        
        key = f"reminder:{reminder.reminder_id}"
        data = asdict(reminder)
        
        # Convert datetime objects to strings
        for field, value in data.items():
            if isinstance(value, datetime):
                data[field] = value.isoformat()
            elif isinstance(value, (ReminderType, ReminderFrequency, ReminderStatus)):
                data[field] = value.value
        
        self.redis_client.setex(key, 86400 * 365, json.dumps(data))  # 1 year expiry
        
        # Add to user's reminders list
        user_key = f"user_reminders:{reminder.user_id}"
        self.redis_client.sadd(user_key, reminder.reminder_id)
        self.redis_client.expire(user_key, 86400 * 365)
    
    def _schedule_celery_task(self, reminder: BillReminder):
        """Schedule reminder task with Celery"""
        if not self.celery_app or not reminder.next_reminder:
            return
        
        from celery.app.task import Task
        
        # Calculate delay until next reminder
        now = datetime.now()
        if reminder.next_reminder > now:
            delay = (reminder.next_reminder - now).total_seconds()
            
            # Schedule task
            self.celery_app.send_task(
                'bill_reminders.send_bill_reminder',
                args=[reminder.reminder_id],
                countdown=delay
            )
    
    def get_reminder(self, reminder_id: str) -> Optional[BillReminder]:
        """Get reminder by ID"""
        if not self.redis_client:
            return None
        
        key = f"reminder:{reminder_id}"
        data = self.redis_client.get(key)
        
        if not data:
            return None
        
        try:
            data_dict = json.loads(data)
            
            # Convert string dates back to datetime
            for field, value in data_dict.items():
                if field in ['due_date', 'created_at', 'updated_at', 'next_reminder', 'snooze_until']:
                    if value and isinstance(value, str):
                        data_dict[field] = datetime.fromisoformat(value)
                elif field in ['reminder_type', 'frequency', 'status']:
                    if field == 'reminder_type':
                        data_dict[field] = ReminderType(value)
                    elif field == 'frequency':
                        data_dict[field] = ReminderFrequency(value)
                    elif field == 'status':
                        data_dict[field] = ReminderStatus(value)
            
            return BillReminder(**data_dict)
        except Exception as e:
            print(f"Failed to parse reminder {reminder_id}: {e}")
            return None
    
    def update_reminder(self, reminder: BillReminder):
        """Update reminder"""
        reminder.updated_at = datetime.now()
        if self.redis_client:
            self._save_reminder_to_redis(reminder)
    
    def get_user_reminders(self, user_id: int, status: str = None) -> List[BillReminder]:
        """Get all reminders for a user"""
        if not self.redis_client:
            return []
        
        user_key = f"user_reminders:{user_id}"
        reminder_ids = self.redis_client.smembers(user_key)
        
        reminders = []
        for reminder_id in reminder_ids:
            reminder = self.get_reminder(reminder_id.decode())
            if reminder:
                if status is None or reminder.status.value == status:
                    reminders.append(reminder)
        
        return sorted(reminders, key=lambda x: x.due_date)
    
    def get_upcoming_reminders(self, user_id: int, days_ahead: int = 7) -> List[BillReminder]:
        """Get upcoming reminders for a user"""
        reminders = self.get_user_reminders(user_id, ReminderStatus.PENDING.value)
        now = datetime.now()
        cutoff = now + timedelta(days=days_ahead)
        
        upcoming = []
        for reminder in reminders:
            if reminder.next_reminder and now <= reminder.next_reminder <= cutoff:
                upcoming.append(reminder)
        
        return sorted(upcoming, key=lambda x: x.next_reminder)
    
    def get_overdue_reminders(self) -> List[BillReminder]:
        """Get all overdue reminders"""
        if not self.redis_client:
            return []
        
        # Get all reminder keys
        keys = self.redis_client.keys("reminder:*")
        overdue = []
        now = datetime.now()
        
        for key in keys:
            reminder_id = key.decode().split(":")[1]
            reminder = self.get_reminder(reminder_id)
            
            if (reminder and 
                reminder.status in [ReminderStatus.PENDING, ReminderStatus.SNOOZED] and
                reminder.due_date < now):
                overdue.append(reminder)
        
        return overdue
    
    def snooze_reminder(self, reminder_id: str, snooze_until: datetime) -> bool:
        """Snooze a reminder"""
        reminder = self.get_reminder(reminder_id)
        if not reminder:
            return False
        
        reminder.status = ReminderStatus.SNOOZED
        reminder.snooze_until = snooze_until
        reminder.next_reminder = snooze_until
        self.update_reminder(reminder)
        
        # Reschedule with Celery
        if self.celery_app:
            self._schedule_celery_task(reminder)
        
        return True
    
    def cancel_reminder(self, reminder_id: str) -> bool:
        """Cancel a reminder"""
        reminder = self.get_reminder(reminder_id)
        if not reminder:
            return False
        
        reminder.status = ReminderStatus.CANCELLED
        self.update_reminder(reminder)
        
        return True
    
    def mark_reminder_completed(self, reminder_id: str) -> bool:
        """Mark reminder as completed"""
        reminder = self.get_reminder(reminder_id)
        if not reminder:
            return False
        
        reminder.status = ReminderStatus.COMPLETED
        self.update_reminder(reminder)
        
        # Schedule next reminder if recurring
        if reminder.frequency != ReminderFrequency.ONCE:
            self._schedule_next_reminder(reminder)
        
        return True
    
    def _schedule_next_reminder(self, reminder: BillReminder):
        """Schedule next occurrence of recurring reminder"""
        if reminder.frequency == ReminderFrequency.ONCE:
            return
        
        next_date = self._calculate_next_date(reminder.due_date, reminder.frequency)
        reminder.next_reminder = next_date
        reminder.status = ReminderStatus.PENDING
        reminder.snooze_until = None
        self.update_reminder(reminder)
        
        # Schedule with Celery
        if self.celery_app:
            self._schedule_celery_task(reminder)
    
    def _calculate_next_date(self, current_date: datetime, frequency: ReminderFrequency) -> datetime:
        """Calculate next date based on frequency"""
        if frequency == ReminderFrequency.DAILY:
            return current_date + timedelta(days=1)
        elif frequency == ReminderFrequency.WEEKLY:
            return current_date + timedelta(weeks=1)
        elif frequency == ReminderFrequency.MONTHLY:
            return current_date + timedelta(days=30)  # Simplified
        elif frequency == ReminderFrequency.QUARTERLY:
            return current_date + timedelta(days=90)  # Simplified
        elif frequency == ReminderFrequency.YEARLY:
            return current_date + timedelta(days=365)
        else:
            return current_date
    
    def _send_notification(self, reminder: BillReminder):
        """Send notification (placeholder implementation)"""
        print(f"Sending reminder: {reminder.title} for user {reminder.user_id}")
        # In a real implementation, this would send email, SMS, push notification, etc.
        
        # Store notification log
        if self.redis_client:
            log_key = f"notification_log:{reminder.reminder_id}"
            log_data = {
                'reminder_id': reminder.reminder_id,
                'sent_at': datetime.now().isoformat(),
                'channels': reminder.notification_channels
            }
            self.redis_client.lpush(log_key, json.dumps(log_data))
            self.redis_client.expire(log_key, 86400 * 30)  # 30 days
    
    def _send_overdue_alert(self, reminder: BillReminder):
        """Send overdue alert"""
        print(f"OVERDUE ALERT: {reminder.title} for user {reminder.user_id}")
        # In a real implementation, this would send urgent notifications
    
    def get_reminder_statistics(self, user_id: int) -> Dict:
        """Get statistics for user's reminders"""
        reminders = self.get_user_reminders(user_id)
        
        stats = {
            'total': len(reminders),
            'pending': len([r for r in reminders if r.status == ReminderStatus.PENDING]),
            'sent': len([r for r in reminders if r.status == ReminderStatus.SENT]),
            'completed': len([r for r in reminders if r.status == ReminderStatus.COMPLETED]),
            'overdue': len([r for r in reminders if r.due_date < datetime.now() and r.status != ReminderStatus.COMPLETED]),
            'upcoming_7_days': len(self.get_upcoming_reminders(user_id, 7)),
            'upcoming_30_days': len(self.get_upcoming_reminders(user_id, 30))
        }
        
        # Calculate total amount of pending bills
        pending_amount = sum(r.amount for r in reminders if r.status == ReminderStatus.PENDING)
        stats['pending_amount'] = pending_amount
        
        return stats

# Global instance
_reminder_manager = None

def get_reminder_manager(redis_url: str = None):
    global _reminder_manager
    if _reminder_manager is None:
        _reminder_manager = BillReminderManager(redis_url)
    return _reminder_manager

# API-like functions
def create_bill_reminder(user_id: int, title: str, description: str, amount: float,
                       due_date: str, reminder_type: str, frequency: str,
                       notification_channels: List[str], metadata: Dict = None) -> str:
    """Create a new bill reminder"""
    manager = get_reminder_manager()
    due_datetime = datetime.fromisoformat(due_date)
    
    return manager.create_reminder(
        user_id, title, description, amount, due_datetime,
        reminder_type, frequency, notification_channels, metadata
    )

def get_user_bill_reminders(user_id: int, status: str = None) -> List[Dict]:
    """Get user's bill reminders"""
    manager = get_reminder_manager()
    reminders = manager.get_user_reminders(user_id, status)
    
    return [
        {
            'reminder_id': r.reminder_id,
            'title': r.title,
            'description': r.description,
            'amount': r.amount,
            'due_date': r.due_date.isoformat(),
            'reminder_type': r.reminder_type.value,
            'frequency': r.frequency.value,
            'status': r.status.value,
            'notification_channels': r.notification_channels,
            'next_reminder': r.next_reminder.isoformat() if r.next_reminder else None
        }
        for r in reminders
    ]

def get_upcoming_bills(user_id: int, days_ahead: int = 7) -> List[Dict]:
    """Get upcoming bills for a user"""
    manager = get_reminder_manager()
    reminders = manager.get_upcoming_reminders(user_id, days_ahead)
    
    return [
        {
            'reminder_id': r.reminder_id,
            'title': r.title,
            'amount': r.amount,
            'due_date': r.due_date.isoformat(),
            'days_until': (r.next_reminder - datetime.now()).days if r.next_reminder else 0
        }
        for r in reminders
    ]

def snooze_bill_reminder(reminder_id: str, snooze_days: int) -> bool:
    """Snooze a bill reminder"""
    manager = get_reminder_manager()
    snooze_until = datetime.now() + timedelta(days=snooze_days)
    
    return manager.snooze_reminder(reminder_id, snooze_until)

def get_bill_reminder_statistics(user_id: int) -> Dict:
    """Get bill reminder statistics for a user"""
    manager = get_reminder_manager()
    return manager.get_reminder_statistics(user_id)
