package com.expense.service;

import com.expense.dto.GroupExpenseDTO;
import com.expense.entity.Group;
import com.expense.entity.GroupExpense;
import com.expense.entity.User;
import com.expense.repository.GroupExpenseRepository;
import com.expense.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupExpenseService {

    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupRepository groupRepository;
    private final AuthService authService;

    /**
     * Create a new group expense
     */
    public GroupExpense createGroupExpense(GroupExpenseDTO dto) {
        User currentUser = authService.getCurrentUser();
        
        // Find the group by name or use provided group ID
        Group group = findGroupFromDTO(dto);
        
        GroupExpense groupExpense = GroupExpense.builder()
                .group(group)
                .description(dto.getDescription())
                .totalAmount(dto.getTotalAmount())
                .category(dto.getCategory())
                .paidBy(dto.getPaidBy())
                .expenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : java.time.LocalDate.now())
                .createdBy(currentUser)
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .status("ACTIVE")
                .build();
        
        return groupExpenseRepository.save(groupExpense);
    }

    /**
     * Get all group expenses for current user
     */
    public List<GroupExpense> getUserGroupExpenses() {
        User currentUser = authService.getCurrentUser();
        return groupExpenseRepository.findByParticipantsContaining(currentUser);
    }

    /**
     * Get group expense by ID
     */
    public GroupExpense getGroupExpense(Long id) {
        User currentUser = authService.getCurrentUser();
        GroupExpense groupExpense = groupExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group expense not found"));
        
        // Verify user is a participant
        if (!groupExpense.getParticipants().contains(currentUser)) {
            throw new RuntimeException("Access denied: User is not a participant");
        }
        
        return groupExpense;
    }

    /**
     * Update group expense
     */
    public GroupExpense updateGroupExpense(Long id, GroupExpenseDTO dto) {
        GroupExpense existingExpense = getGroupExpense(id);
        User currentUser = authService.getCurrentUser();
        
        // Only creator can update
        if (!existingExpense.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the creator can update this group expense");
        }
        
        // Update the group if needed
        if (dto.getGroupName() != null) {
            Group group = findGroupFromDTO(dto);
            existingExpense.setGroup(group);
        }
        
        existingExpense.setDescription(dto.getDescription());
        existingExpense.setTotalAmount(dto.getTotalAmount());
        existingExpense.setCategory(dto.getCategory());
        existingExpense.setPaidBy(dto.getPaidBy());
        existingExpense.setExpenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : java.time.LocalDate.now());
        existingExpense.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "INR");
        existingExpense.setUpdatedAt(LocalDateTime.now());
        
        return groupExpenseRepository.save(existingExpense);
    }

    /**
     * Delete group expense
     */
    public void deleteGroupExpense(Long id) {
        GroupExpense groupExpense = getGroupExpense(id);
        User currentUser = authService.getCurrentUser();
        
        // Only creator can delete
        if (!groupExpense.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the creator can delete this group expense");
        }
        
        groupExpenseRepository.delete(groupExpense);
    }

    /**
     * Settle user's share of group expense
     */
    public GroupExpense settleExpense(Long id, String paymentMethod) {
        GroupExpense groupExpense = getGroupExpense(id);
        User currentUser = authService.getCurrentUser();
        
        // Check if user is a participant
        if (!groupExpense.getParticipants().contains(currentUser)) {
            throw new RuntimeException("User is not a participant");
        }
        
        // Add to settled amounts
        if (groupExpense.getSettledAmounts() == null) {
            groupExpense.setSettledAmounts(new HashMap<>());
        }
        
        BigDecimal userShare = groupExpense.getSplitAmounts().get(currentUser.getId());
        groupExpense.getSettledAmounts().put(currentUser.getId(), userShare);
        
        // Mark as settled for this user
        if (groupExpense.getSettlementStatus() == null) {
            groupExpense.setSettlementStatus(new HashMap<>());
        }
        groupExpense.getSettlementStatus().put(currentUser.getId(), "SETTLED");
        
        groupExpense.setUpdatedAt(LocalDateTime.now());
        
        return groupExpenseRepository.save(groupExpense);
    }

    /**
     * Get settlement summary for a group expense
     */
    public Map<String, Object> getSettlementSummary(Long id) {
        GroupExpense groupExpense = getGroupExpense(id);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("groupName", groupExpense.getGroup().getName());
        summary.put("totalAmount", groupExpense.getTotalAmount());
        summary.put("currency", groupExpense.getCurrency());
        summary.put("participantCount", groupExpense.getParticipants().size());
        summary.put("splitAmount", groupExpense.getTotalAmount().divide(
            BigDecimal.valueOf(groupExpense.getParticipants().size()), 2, BigDecimal.ROUND_HALF_UP
        ));
        
        // Calculate settlement status
        List<Map<String, Object>> participantStatuses = new ArrayList<>();
        BigDecimal totalSettled = BigDecimal.ZERO;
        
        for (User participant : groupExpense.getParticipants()) {
            Map<String, Object> status = new HashMap<>();
            status.put("userId", participant.getId());
            status.put("name", participant.getName());
            status.put("email", participant.getEmail());
            status.put("splitAmount", groupExpense.getSplitAmounts().get(participant));
            
            BigDecimal settledAmount = groupExpense.getSettledAmounts() != null ? 
                groupExpense.getSettledAmounts().getOrDefault(participant, BigDecimal.ZERO) : BigDecimal.ZERO;
            status.put("settledAmount", settledAmount);
            status.put("remainingAmount", groupExpense.getSplitAmounts().get(participant).subtract(settledAmount));
            status.put("status", groupExpense.getSettlementStatus() != null ? 
                groupExpense.getSettlementStatus().getOrDefault(participant, "PENDING") : "PENDING");
            
            participantStatuses.add(status);
            totalSettled = totalSettled.add(settledAmount);
        }
        
        summary.put("participants", participantStatuses);
        summary.put("totalSettled", totalSettled);
        summary.put("remainingAmount", groupExpense.getTotalAmount().subtract(totalSettled));
        summary.put("isFullySettled", totalSettled.compareTo(groupExpense.getTotalAmount()) >= 0);
        
        return summary;
    }

    /**
     * Get all group expenses where user owes money
     */
    public List<GroupExpense> getOutstandingExpenses() {
        User currentUser = authService.getCurrentUser();
        return groupExpenseRepository.findByParticipantsContaining(currentUser)
                .stream()
                .filter(expense -> {
                    BigDecimal settled = expense.getSettledAmounts() != null ? 
                        expense.getSettledAmounts().getOrDefault(currentUser, BigDecimal.ZERO) : BigDecimal.ZERO;
                    BigDecimal split = expense.getSplitAmounts().get(currentUser);
                    return settled.compareTo(split) < 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get group expenses created by user
     */
    public List<GroupExpense> getCreatedExpenses() {
        User currentUser = authService.getCurrentUser();
        return groupExpenseRepository.findByCreatedByOrderByCreatedAtDesc(currentUser);
    }

    /**
     * Calculate total amount owed by user across all group expenses
     */
    public Map<String, Object> getUserSettlementSummary() {
        User currentUser = authService.getCurrentUser();
        List<GroupExpense> userExpenses = groupExpenseRepository.findByParticipantsContaining(currentUser);
        
        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalToReceive = BigDecimal.ZERO;
        int activeExpenses = 0;
        int settledExpenses = 0;
        
        for (GroupExpense expense : userExpenses) {
            BigDecimal settled = expense.getSettledAmounts() != null ? 
                expense.getSettledAmounts().getOrDefault(currentUser, BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal split = expense.getSplitAmounts().get(currentUser);
            BigDecimal remaining = split.subtract(settled);
            
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                totalOwed = totalOwed.add(remaining);
                activeExpenses++;
            } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                totalToReceive = totalToReceive.add(remaining.abs());
            } else {
                settledExpenses++;
            }
        }
        
        return Map.of(
            "totalOwed", totalOwed,
            "totalToReceive", totalToReceive,
            "activeExpenses", activeExpenses,
            "settledExpenses", settledExpenses,
            "netPosition", totalToReceive.subtract(totalOwed)
        );
    }

    /**
     * Add participant to existing group expense
     */
    public GroupExpense addParticipant(Long id, String participantEmail) {
        GroupExpense groupExpense = getGroupExpense(id);
        User currentUser = authService.getCurrentUser();
        
        // Only creator can add participants
        if (!groupExpense.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only creator can add participants");
        }
        
        // For now, we'll just log this since participant management is complex
        // In a full implementation, you would:
        // 1. Find user by email
        // 2. Add to participants set
        // 3. Recalculate split amounts
        log.info("Adding participant {} to group expense {}", participantEmail, id);
        
        return groupExpenseRepository.save(groupExpense);
    }

    /**
     * Remove participant from group expense
     */
    public GroupExpense removeParticipant(Long id, String participantEmail) {
        GroupExpense groupExpense = getGroupExpense(id);
        User currentUser = authService.getCurrentUser();
        
        // Only creator can remove participants
        if (!groupExpense.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only creator can remove participants");
        }
        
        // For now, we'll just log this since participant management is complex
        // In a full implementation, you would:
        // 1. Find participant in participants set
        // 2. Remove from set
        // 3. Recalculate split amounts
        // 4. Update settlement status
        log.info("Removing participant {} from group expense {}", participantEmail, id);
        
        return groupExpenseRepository.save(groupExpense);
    }

    private Group findGroupFromDTO(GroupExpenseDTO dto) {
        if (dto.getGroupId() != null) {
            return groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found"));
        } else if (dto.getGroupName() != null) {
            return groupRepository.findByName(dto.getGroupName())
                    .orElseThrow(() -> new RuntimeException("Group not found with name: " + dto.getGroupName()));
        } else {
            throw new RuntimeException("Group ID or name is required");
        }
    }
}
