from typing import Dict, List, Optional, Tuple
from datetime import datetime, timedelta
import uuid
from dataclasses import dataclass
from enum import Enum

class SplitType(Enum):
    EQUAL = "equal"
    PERCENTAGE = "percentage"
    CUSTOM = "custom"
    BY_ITEM = "by_item"

class SettlementStatus(Enum):
    PENDING = "pending"
    PARTIAL = "partial"
    COMPLETED = "completed"
    OVERDUE = "overdue"

@dataclass
class GroupMember:
    user_id: int
    name: str
    email: str
    phone: Optional[str] = None
    
@dataclass
class GroupExpense:
    expense_id: str
    group_id: str
    paid_by: int
    description: str
    amount: float
    category: str
    date: datetime
    split_type: SplitType
    participants: List[int]  # List of user_ids
    splits: Dict[int, float]  # user_id -> amount
    receipts: List[str] = None  # URLs to receipt images
    notes: str = ""
    
@dataclass
class Group:
    group_id: str
    name: str
    description: str
    created_by: int
    members: List[GroupMember]
    created_at: datetime
    is_active: bool = True
    
@dataclass
class Settlement:
    settlement_id: str
    group_id: str
    from_user: int
    to_user: int
    amount: float
    due_date: datetime
    status: SettlementStatus
    created_at: datetime
    settled_at: Optional[datetime] = None
    notes: str = ""

class GroupExpenseManager:
    def __init__(self):
        self.groups: Dict[str, Group] = {}
        self.expenses: Dict[str, GroupExpense] = {}
        self.settlements: Dict[str, Settlement] = {}
        
    def create_group(self, name: str, description: str, created_by: int, 
                    members: List[GroupMember]) -> str:
        """Create a new expense group"""
        group_id = str(uuid.uuid4())
        
        # Ensure creator is in members
        if not any(m.user_id == created_by for m in members):
            creator_member = next((m for m in self.members if m.user_id == created_by), None)
            if creator_member:
                members.append(creator_member)
        
        group = Group(
            group_id=group_id,
            name=name,
            description=description,
            created_by=created_by,
            members=members,
            created_at=datetime.now()
        )
        
        self.groups[group_id] = group
        return group_id
    
    def add_expense(self, group_id: str, paid_by: int, description: str, 
                   amount: float, category: str, split_type: SplitType,
                   participants: List[int], splits: Dict[int, float] = None,
                   receipts: List[str] = None, notes: str = "") -> str:
        """Add a new group expense"""
        if group_id not in self.groups:
            raise ValueError("Group not found")
        
        group = self.groups[group_id]
        
        # Validate paid_by is a group member
        if not any(m.user_id == paid_by for m in group.members):
            raise ValueError("Payer is not a group member")
        
        # Validate participants are group members
        for participant_id in participants:
            if not any(m.user_id == participant_id for m in group.members):
                raise ValueError(f"Participant {participant_id} is not a group member")
        
        # Calculate splits if not provided
        if splits is None:
            splits = self._calculate_splits(amount, split_type, participants)
        
        # Validate splits sum to total amount
        if abs(sum(splits.values()) - amount) > 0.01:
            raise ValueError("Split amounts do not sum to total expense amount")
        
        expense_id = str(uuid.uuid4())
        expense = GroupExpense(
            expense_id=expense_id,
            group_id=group_id,
            paid_by=paid_by,
            description=description,
            amount=amount,
            category=category,
            date=datetime.now(),
            split_type=split_type,
            participants=participants,
            splits=splits,
            receipts=receipts or [],
            notes=notes
        )
        
        self.expenses[expense_id] = expense
        
        # Update settlements
        self._update_settlements(group_id, expense)
        
        return expense_id
    
    def _calculate_splits(self, amount: float, split_type: SplitType, 
                          participants: List[int]) -> Dict[int, float]:
        """Calculate expense splits based on split type"""
        splits = {}
        
        if split_type == SplitType.EQUAL:
            equal_split = amount / len(participants)
            splits = {pid: equal_split for pid in participants}
            
        elif split_type == SplitType.PERCENTAGE:
            # Default to equal percentage if not specified
            equal_percentage = 100 / len(participants)
            splits = {pid: amount * (equal_percentage / 100) for pid in participants}
            
        elif split_type == SplitType.CUSTOM:
            # Default to equal split for custom (should be provided by caller)
            equal_split = amount / len(participants)
            splits = {pid: equal_split for pid in participants}
            
        elif split_type == SplitType.BY_ITEM:
            # Default to equal split for by-item (should be provided by caller)
            equal_split = amount / len(participants)
            splits = {pid: equal_split for pid in participants}
        
        return splits
    
    def _update_settlements(self, group_id: str, expense: GroupExpense):
        """Update settlement records based on new expense"""
        paid_by = expense.paid_by
        splits = expense.splits
        
        for participant_id, amount in splits.items():
            if participant_id != paid_by:
                # Participant owes money to payer
                settlement_id = str(uuid.uuid4())
                settlement = Settlement(
                    settlement_id=settlement_id,
                    group_id=group_id,
                    from_user=participant_id,
                    to_user=paid_by,
                    amount=amount,
                    due_date=datetime.now() + timedelta(days=30),  # 30 days to settle
                    status=SettlementStatus.PENDING,
                    created_at=datetime.now()
                )
                self.settlements[settlement_id] = settlement
    
    def get_group_balance(self, group_id: str) -> Dict[int, float]:
        """Get net balance for each member in a group"""
        if group_id not in self.groups:
            raise ValueError("Group not found")
        
        balances = {}
        group = self.groups[group_id]
        
        # Initialize all members with 0 balance
        for member in group.members:
            balances[member.user_id] = 0.0
        
        # Calculate balances from expenses
        group_expenses = [e for e in self.expenses.values() if e.group_id == group_id]
        
        for expense in group_expenses:
            paid_by = expense.paid_by
            splits = expense.splits
            
            # Add total amount to payer's balance
            balances[paid_by] += expense.amount
            
            # Subtract each person's share
            for participant_id, amount in splits.items():
                balances[participant_id] -= amount
        
        return balances
    
    def get_settlement_summary(self, group_id: str) -> List[Dict]:
        """Get summary of who owes whom"""
        balances = self.get_group_balance(group_id)
        
        # Separate debtors and creditors
        debtors = [(uid, bal) for uid, bal in balances.items() if bal < 0]
        creditors = [(uid, bal) for uid, bal in balances.items() if bal > 0]
        
        # Sort by amount (descending for creditors, ascending for debtors)
        debtors.sort(key=lambda x: x[1])  # Most negative first
        creditors.sort(key=lambda x: x[1], reverse=True)  # Highest positive first
        
        settlements = []
        debtor_idx, creditor_idx = 0, 0
        
        while debtor_idx < len(debtors) and creditor_idx < len(creditors):
            debtor_id, debtor_amount = debtors[debtor_idx]
            creditor_id, creditor_amount = creditors[creditor_idx]
            
            # Calculate settlement amount
            settle_amount = min(abs(debtor_amount), creditor_amount)
            
            settlements.append({
                'from_user': debtor_id,
                'to_user': creditor_id,
                'amount': settle_amount,
                'from_user_balance': debtor_amount,
                'to_user_balance': creditor_amount
            })
            
            # Update balances
            debtor_amount += settle_amount
            creditor_amount -= settle_amount
            
            # Update lists
            if debtor_amount >= 0:
                debtor_idx += 1
            else:
                debtors[debtor_idx] = (debtor_id, debtor_amount)
            
            if creditor_amount <= 0:
                creditor_idx += 1
            else:
                creditors[creditor_idx] = (creditor_id, creditor_amount)
        
        return settlements
    
    def settle_expense(self, settlement_id: str, amount: float, notes: str = "") -> bool:
        """Record a settlement payment"""
        if settlement_id not in self.settlements:
            raise ValueError("Settlement not found")
        
        settlement = self.settlements[settlement_id]
        
        if settlement.status == SettlementStatus.COMPLETED:
            raise ValueError("Settlement already completed")
        
        if amount > settlement.amount:
            raise ValueError("Payment amount exceeds settlement amount")
        
        # Update settlement
        if amount >= settlement.amount:
            settlement.status = SettlementStatus.COMPLETED
            settlement.settled_at = datetime.now()
        else:
            settlement.status = SettlementStatus.PARTIAL
            settlement.amount -= amount
            
            # Create new settlement for remaining amount
            new_settlement_id = str(uuid.uuid4())
            new_settlement = Settlement(
                settlement_id=new_settlement_id,
                group_id=settlement.group_id,
                from_user=settlement.from_user,
                to_user=settlement.to_user,
                amount=settlement.amount - amount,
                due_date=settlement.due_date,
                status=SettlementStatus.PENDING,
                created_at=datetime.now(),
                notes=f"Remaining balance from {settlement_id}"
            )
            self.settlements[new_settlement_id] = new_settlement
        
        settlement.notes += f"\n{datetime.now().isoformat()}: Paid ${amount:.2f}. {notes}"
        
        return True
    
    def get_group_expenses(self, group_id: str, start_date: datetime = None, 
                          end_date: datetime = None) -> List[GroupExpense]:
        """Get expenses for a group with optional date filtering"""
        expenses = [e for e in self.expenses.values() if e.group_id == group_id]
        
        if start_date:
            expenses = [e for e in expenses if e.date >= start_date]
        
        if end_date:
            expenses = [e for e in expenses if e.date <= end_date]
        
        return sorted(expenses, key=lambda x: x.date, reverse=True)
    
    def get_member_expenses(self, group_id: str, user_id: int) -> List[GroupExpense]:
        """Get expenses for a specific member in a group"""
        group_expenses = self.get_group_expenses(group_id)
        
        return [
            e for e in group_expenses 
            if e.paid_by == user_id or user_id in e.participants
        ]
    
    def get_group_statistics(self, group_id: str) -> Dict:
        """Get statistics for a group"""
        if group_id not in self.groups:
            raise ValueError("Group not found")
        
        expenses = self.get_group_expenses(group_id)
        
        if not expenses:
            return {
                'total_expenses': 0,
                'total_amount': 0,
                'member_contributions': {},
                'category_breakdown': {},
                'average_expense': 0,
                'largest_expense': 0,
                'smallest_expense': 0
            }
        
        total_amount = sum(e.amount for e in expenses)
        member_contributions = {}
        category_breakdown = {}
        
        for expense in expenses:
            # Member contributions
            if expense.paid_by not in member_contributions:
                member_contributions[expense.paid_by] = 0
            member_contributions[expense.paid_by] += expense.amount
            
            # Category breakdown
            if expense.category not in category_breakdown:
                category_breakdown[expense.category] = 0
            category_breakdown[expense.category] += expense.amount
        
        amounts = [e.amount for e in expenses]
        
        return {
            'total_expenses': len(expenses),
            'total_amount': total_amount,
            'member_contributions': member_contributions,
            'category_breakdown': category_breakdown,
            'average_expense': total_amount / len(expenses),
            'largest_expense': max(amounts),
            'smallest_expense': min(amounts)
        }
    
    def add_member(self, group_id: str, member: GroupMember) -> bool:
        """Add a new member to a group"""
        if group_id not in self.groups:
            raise ValueError("Group not found")
        
        group = self.groups[group_id]
        
        # Check if member already exists
        if any(m.user_id == member.user_id for m in group.members):
            raise ValueError("Member already in group")
        
        group.members.append(member)
        return True
    
    def remove_member(self, group_id: str, user_id: int) -> bool:
        """Remove a member from a group (only if no pending settlements)"""
        if group_id not in self.groups:
            raise ValueError("Group not found")
        
        group = self.groups[group_id]
        
        # Check for pending settlements
        pending_settlements = [
            s for s in self.settlements.values() 
            if s.group_id == group_id and 
            (s.from_user == user_id or s.to_user == user_id) and
            s.status in [SettlementStatus.PENDING, SettlementStatus.PARTIAL]
        ]
        
        if pending_settlements:
            raise ValueError("Cannot remove member with pending settlements")
        
        # Remove member
        group.members = [m for m in group.members if m.user_id != user_id]
        return True
    
    def get_overdue_settlements(self, group_id: str = None) -> List[Settlement]:
        """Get overdue settlements"""
        now = datetime.now()
        settlements = self.settlements.values()
        
        if group_id:
            settlements = [s for s in settlements if s.group_id == group_id]
        
        overdue = [
            s for s in settlements 
            if s.due_date < now and s.status != SettlementStatus.COMPLETED
        ]
        
        return overdue

# Global instance
_group_manager = None

def get_group_manager():
    global _group_manager
    if _group_manager is None:
        _group_manager = GroupExpenseManager()
    return _group_manager

# API-like functions
def create_expense_group(name: str, description: str, created_by: int, 
                        members: List[Dict]) -> str:
    """Create a new expense group"""
    manager = get_group_manager()
    member_objects = [GroupMember(**m) for m in members]
    return manager.create_group(name, description, created_by, member_objects)

def add_group_expense(group_id: str, paid_by: int, description: str, 
                     amount: float, category: str, split_type: str,
                     participants: List[int], splits: Dict[int, float] = None,
                     receipts: List[str] = None, notes: str = "") -> str:
    """Add a new group expense"""
    manager = get_group_manager()
    split_enum = SplitType(split_type)
    return manager.add_expense(
        group_id, paid_by, description, amount, category, 
        split_enum, participants, splits, receipts, notes
    )

def get_group_summary(group_id: str) -> Dict:
    """Get comprehensive group summary"""
    manager = get_group_manager()
    
    if group_id not in manager.groups:
        raise ValueError("Group not found")
    
    group = manager.groups[group_id]
    balances = manager.get_group_balance(group_id)
    settlements = manager.get_settlement_summary(group_id)
    statistics = manager.get_group_statistics(group_id)
    overdue = manager.get_overdue_settlements(group_id)
    
    return {
        'group': {
            'group_id': group.group_id,
            'name': group.name,
            'description': group.description,
            'created_by': group.created_by,
            'members': [
                {
                    'user_id': m.user_id,
                    'name': m.name,
                    'email': m.email
                } for m in group.members
            ],
            'created_at': group.created_at.isoformat(),
            'is_active': group.is_active
        },
        'balances': balances,
        'settlements': settlements,
        'statistics': statistics,
        'overdue_settlements': len(overdue)
    }
