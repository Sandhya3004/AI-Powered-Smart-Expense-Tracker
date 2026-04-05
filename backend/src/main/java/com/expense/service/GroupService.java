package com.expense.service;

import com.expense.dto.GroupDTO;
import com.expense.dto.GroupExpenseDTO;
import com.expense.dto.GroupMemberDTO;
import com.expense.entity.Group;
import com.expense.entity.GroupExpense;
import com.expense.entity.GroupMember;
import com.expense.entity.User;
import com.expense.repository.GroupExpenseRepository;
import com.expense.repository.GroupMemberRepository;
import com.expense.repository.GroupRepository;
import com.expense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public GroupDTO createGroup(String name, String description, String type, List<String> memberEmails) {
        User currentUser = authService.getCurrentUser();
        
        // Validation
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }
        
        String groupType = (type != null && !type.trim().isEmpty()) ? type.trim() : "other";
        
        Group group = Group.builder()
                .createdBy(currentUser)
                .name(name.trim())
                .description(description)
                .type(groupType)
                .build();
        
        Group savedGroup = groupRepository.save(group);
        
        // Add creator as admin member
        GroupMember creatorMember = GroupMember.builder()
                .group(savedGroup)
                .user(currentUser)
                .role("admin")
                .build();
        groupMemberRepository.save(creatorMember);
        
        // Add additional members if provided
        if (memberEmails != null && !memberEmails.isEmpty()) {
            for (String email : memberEmails) {
                if (email != null && !email.trim().isEmpty() && !email.equals(currentUser.getEmail())) {
                    try {
                        addMember(savedGroup.getId(), email.trim());
                    } catch (Exception e) {
                        log.warn("Failed to add member {} to group: {}", email, e.getMessage());
                        // Continue adding other members even if one fails
                    }
                }
            }
        }
        
        return convertToDTO(savedGroup);
    }

    // Keep old method for backward compatibility
    public GroupDTO createGroup(String name, String description, String type) {
        return createGroup(name, description, type, null);
    }

    public List<GroupDTO> getUserGroups() {
        User currentUser = authService.getCurrentUser();
        List<Group> groups = groupRepository.findByCreatedBy(currentUser);
        
        // Initialize lazy loaded users for all groups
        groups.forEach(group -> Hibernate.initialize(group.getCreatedBy()));
        
        return groups.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public GroupDTO getGroupById(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        // Initialize lazy loaded user
        Hibernate.initialize(group.getCreatedBy());
        
        return convertToDTO(group);
    }

    public GroupDTO updateGroup(Long id, String name, String description, String type) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        // Initialize lazy loaded user before accessing
        Hibernate.initialize(group.getCreatedBy());
        
        group.setName(name);
        group.setDescription(description);
        group.setType(type);
        
        Group updatedGroup = groupRepository.save(group);
        return convertToDTO(updatedGroup);
    }

    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        groupRepository.delete(group);
    }

    public GroupMemberDTO addMember(Long groupId, String email) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        User userToAdd = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Check if user is already a member
        if (groupMemberRepository.existsByGroupAndUser(group, userToAdd)) {
            throw new RuntimeException("User is already a member of this group");
        }
        
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(userToAdd)
                .role("member")
                .build();
        
        GroupMember savedMember = groupMemberRepository.save(member);
        log.info("Added member {} to group {}", email, groupId);
        
        return convertToMemberDTO(savedMember);
    }

    public void removeMember(Long groupId, Long memberId) {
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        // Verify the member belongs to the specified group
        if (!member.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Member does not belong to this group");
        }
        
        groupMemberRepository.delete(member);
        log.info("Removed member {} from group {}", memberId, groupId);
    }

    public List<GroupMemberDTO> getGroupMembers(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        
        // Initialize lazy loaded relationships
        members.forEach(member -> {
            Hibernate.initialize(member.getUser());
        });
        
        return members.stream()
                .map(this::convertToMemberDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> addGroupExpense(Long groupId, String description, BigDecimal amount, String category) {
        User currentUser = authService.getCurrentUser();
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        // Check if user is a member of the group
        boolean isMember = groupMemberRepository.existsByGroupAndUser(group, currentUser);
        if (!isMember) {
            throw new RuntimeException("User is not a member of this group");
        }
        
        GroupExpense expense = GroupExpense.builder()
                .group(group)
                .description(description)
                .totalAmount(amount)
                .category(category != null ? category : "Other")
                .paidBy(currentUser.getName())
                .createdBy(currentUser)
                .expenseDate(LocalDate.now())
                .currency("INR")
                .status("ACTIVE")
                .build();
        
        GroupExpense savedExpense = groupExpenseRepository.save(expense);
        
        return convertExpenseToMap(savedExpense);
    }

    public List<Map<String, Object>> getGroupExpenses(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupExpense> expenses = groupExpenseRepository.findByGroup(group);
        
        // Initialize lazy loaded relationships
        expenses.forEach(expense -> {
            Hibernate.initialize(expense.getGroup());
            Hibernate.initialize(expense.getCreatedBy());
        });
        
        return expenses.stream()
                .map(this::convertExpenseToMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> importGroupExpenses(Long groupId, MultipartFile file) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        try {
            List<GroupExpenseDTO> expenses = parseCSVFile(file);
            int totalRows = expenses.size();
            int imported = 0;
            int failed = 0;
            List<String> errors = new ArrayList<>();
            
            for (GroupExpenseDTO expenseDTO : expenses) {
                try {
                    GroupExpense expense = convertDTOToEntity(expenseDTO, group);
                    groupExpenseRepository.save(expense);
                    imported++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to import expense: " + expenseDTO.getDescription() + " - " + e.getMessage());
                }
            }
            
            return Map.of(
                "totalRows", totalRows,
                "imported", imported,
                "failed", failed,
                "errors", errors
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
        }
    }

    public byte[] exportGroupExpenses(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupExpense> expenses = groupExpenseRepository.findByGroup(group);
        
        // Initialize lazy loaded relationships
        expenses.forEach(expense -> {
            Hibernate.initialize(expense.getGroup());
            Hibernate.initialize(expense.getCreatedBy());
        });
        
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Date,Description,Amount,Category,Paid By\n");
        
        for (GroupExpense expense : expenses) {
            csvContent.append(String.format("%s,%s,%.2f,%s,%s\n",
                expense.getExpenseDate(),
                escapeCSV(expense.getDescription()),
                expense.getTotalAmount(),
                escapeCSV(expense.getCategory()),
                escapeCSV(expense.getPaidBy())
            ));
        }
        
        return csvContent.toString().getBytes();
    }

    public Map<String, Object> calculateSettlement(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupExpense> expenses = groupExpenseRepository.findByGroup(group);
        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        
        // Initialize lazy loaded relationships
        expenses.forEach(expense -> {
            Hibernate.initialize(expense.getCreatedBy());
        });
        members.forEach(member -> {
            Hibernate.initialize(member.getUser());
        });
        
        // Calculate total expenses
        BigDecimal totalExpenses = expenses.stream()
                .map(GroupExpense::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate per-person balance (simplified equal split)
        BigDecimal perPersonAmount = members.size() > 0 ? 
            totalExpenses.divide(BigDecimal.valueOf(members.size()), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;
        
        List<Map<String, Object>> balances = new ArrayList<>();
        
        for (GroupMember member : members) {
            // Calculate how much this person paid
            BigDecimal paidAmount = expenses.stream()
                    .filter(expense -> expense.getCreatedBy().getId().equals(member.getUser().getId()))
                    .map(GroupExpense::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate balance (paid - owed)
            BigDecimal balance = paidAmount.subtract(perPersonAmount);
            
            Map<String, Object> balanceInfo = new HashMap<>();
            balanceInfo.put("member", member.getUser().getName());
            balanceInfo.put("memberEmail", member.getUser().getEmail());
            balanceInfo.put("paid", paidAmount);
            balanceInfo.put("owed", perPersonAmount);
            balanceInfo.put("balance", balance);
            balanceInfo.put("owes", balance.compareTo(BigDecimal.ZERO) < 0);
            
            balances.add(balanceInfo);
        }
        
        return Map.of(
            "balances", balances,
            "totalExpenses", totalExpenses,
            "memberCount", members.size(),
            "perPersonAmount", perPersonAmount
        );
    }

    private List<GroupExpenseDTO> parseCSVFile(MultipartFile file) throws Exception {
        List<GroupExpenseDTO> expenses = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip header
                    continue;
                }
                
                String[] values = line.split(",");
                if (values.length >= 5) {
                    try {
                        GroupExpenseDTO expense = GroupExpenseDTO.fromCSV(values);
                        expenses.add(expense);
                    } catch (Exception e) {
                        log.warn("Skipping invalid CSV line: {}", line);
                    }
                }
            }
        }
        
        return expenses;
    }

    private GroupExpense convertDTOToEntity(GroupExpenseDTO dto, Group group) {
        User currentUser = authService.getCurrentUser();
        
        return GroupExpense.builder()
                .group(group)
                .description(dto.getDescription())
                .totalAmount(dto.getTotalAmount())
                .category(dto.getCategory())
                .paidBy(dto.getPaidBy())
                .expenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : LocalDate.now())
                .currency(dto.getCurrency())
                .status("ACTIVE")
                .createdBy(currentUser)
                .build();
    }

    private Map<String, Object> convertExpenseToMap(GroupExpense expense) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", expense.getId());
        map.put("description", expense.getDescription());
        map.put("amount", expense.getTotalAmount());
        map.put("category", expense.getCategory());
        map.put("paidBy", expense.getPaidBy());
        map.put("date", expense.getExpenseDate());
        map.put("groupId", expense.getGroup().getId());
        map.put("createdBy", expense.getCreatedBy().getEmail());
        map.put("createdAt", expense.getCreatedAt());
        map.put("status", expense.getStatus());
        return map;
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private GroupDTO convertToDTO(Group group) {
        User createdBy = group.getCreatedBy();
        
        return GroupDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .type(group.getType())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .createdByEmail(createdBy != null ? createdBy.getEmail() : null)
                .createdByName(createdBy != null ? createdBy.getName() : null)
                .build();
    }

    private GroupMemberDTO convertToMemberDTO(GroupMember member) {
        User user = member.getUser();
        Group group = member.getGroup();
        
        return GroupMemberDTO.builder()
                .id(member.getId())
                .groupId(group != null ? group.getId() : null)
                .userId(user != null ? user.getId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userName(user != null ? user.getName() : null)
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
