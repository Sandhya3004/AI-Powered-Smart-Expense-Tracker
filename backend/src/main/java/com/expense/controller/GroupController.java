package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.dto.CreateGroupRequest;
import com.expense.dto.GroupDTO;
import com.expense.dto.GroupMemberDTO;
import com.expense.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController extends BaseController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse<GroupDTO>> createGroup(@RequestBody CreateGroupRequest request) {
        try {
            log.info("Creating group for user: {} with name: {}", getCurrentUserId(), request.getName());
            
            // Validation
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Group name is required"));
            }
            
            // Set default type if not provided
            String type = request.getType();
            if (type == null || type.trim().isEmpty()) {
                type = "other";
            }
            
            GroupDTO group = groupService.createGroup(
                    request.getName().trim(), 
                    request.getDescription(), 
                    type,
                    request.getMemberEmails()
            );
            return ResponseEntity.ok(ApiResponse.success(group, "Group created successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating group", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create group", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create group: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupDTO>>> getUserGroups() {
        try {
            log.info("Fetching groups for user: {}", getCurrentUserId());
            List<GroupDTO> groups = groupService.getUserGroups();
            return ResponseEntity.ok(ApiResponse.success(groups, "Groups fetched successfully"));
        } catch (Exception e) {
            log.error("Failed to fetch groups", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch groups: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupDTO>> getGroup(@PathVariable Long id) {
        try {
            log.info("Fetching group: {} for user: {}", id, getCurrentUserId());
            GroupDTO group = groupService.getGroupById(id);
            return ResponseEntity.ok(ApiResponse.success(group, "Group fetched successfully"));
        } catch (Exception e) {
            log.error("Failed to fetch group", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch group: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupDTO>> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            log.info("Updating group: {} for user: {}", id, getCurrentUserId());
            
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String type = (String) request.get("type");
            
            GroupDTO group = groupService.updateGroup(id, name, description, type);
            return ResponseEntity.ok(ApiResponse.success(group, "Group updated successfully"));
        } catch (Exception e) {
            log.error("Failed to update group", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update group: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        try {
            log.info("Deleting group: {} for user: {}", id, getCurrentUserId());
            groupService.deleteGroup(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Group deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete group", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete group: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<GroupMemberDTO>> addMember(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            log.info("Adding member to group: {} for user: {}", id, getCurrentUserId());
            
            String email = (String) request.get("email");
            
            // Validate email
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email is required"));
            }
            
            // Basic email format validation
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid email format"));
            }
            
            GroupMemberDTO member = groupService.addMember(id, email.trim());
            return ResponseEntity.ok(ApiResponse.success(member, "Member added successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Validation error adding member: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error adding member: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to add member", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to add member: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberDTO>>> getGroupMembers(@PathVariable Long id) {
        try {
            log.info("Fetching members for group: {} for user: {}", id, getCurrentUserId());
            List<GroupMemberDTO> members = groupService.getGroupMembers(id);
            return ResponseEntity.ok(ApiResponse.success(members, "Group members fetched successfully"));
        } catch (Exception e) {
            log.error("Failed to fetch group members", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch group members: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        try {
            log.info("Removing member: {} from group: {} for user: {}", memberId, id, getCurrentUserId());
            groupService.removeMember(id, memberId);
            return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
        } catch (Exception e) {
            log.error("Failed to remove member", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to remove member: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/expenses")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addGroupExpense(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("Adding expense to group: {} for user: {}", id, getCurrentUserId());
            
            // Validate required fields
            String description = (String) request.get("description");
            Object amountObj = request.get("amount");
            String category = (String) request.get("category");
            
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Description is required"));
            }
            
            if (amountObj == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Amount is required"));
            }
            
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid amount format"));
            }
            
            Map<String, Object> expense = groupService.addGroupExpense(id, description, amount, category);
            return ResponseEntity.ok(ApiResponse.success(expense, "Expense added successfully"));
        } catch (RuntimeException e) {
            log.error("Error adding expense: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to add expense", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to add expense: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/expenses")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGroupExpenses(@PathVariable Long id) {
        try {
            log.info("Fetching expenses for group: {} for user: {}", id, getCurrentUserId());
            List<Map<String, Object>> expenses = groupService.getGroupExpenses(id);
            return ResponseEntity.ok(ApiResponse.success(expenses, "Group expenses fetched successfully"));
        } catch (Exception e) {
            log.error("Failed to fetch group expenses", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch group expenses: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> settleGroupExpenses(@PathVariable Long id) {
        try {
            log.info("Calculating settlement for group: {} for user: {}", id, getCurrentUserId());
            Map<String, Object> settlement = groupService.calculateSettlement(id);
            return ResponseEntity.ok(ApiResponse.success(settlement, "Settlement calculated successfully"));
        } catch (Exception e) {
            log.error("Failed to calculate settlement", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to calculate settlement: " + e.getMessage()));
        }
    }

    // AI Integration endpoint for group expense analysis
    @PostMapping("/{id}/ai-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeGroupExpenses(@PathVariable Long id) {
        try {
            log.info("AI analysis for group: {} for user: {}", id, getCurrentUserId());
            
            // This would integrate with AI service for group expense analysis
            Map<String, Object> analysis = Map.of(
                "totalExpenses", 0.0,
                "averagePerPerson", 0.0,
                "topCategories", List.of(),
                "recommendations", List.of("Add more expenses for better analysis")
            );
            
            return ResponseEntity.ok(ApiResponse.success(analysis, "Group analysis completed"));
        } catch (Exception e) {
            log.error("Failed to analyze group expenses", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to analyze group expenses: " + e.getMessage()));
        }
    }

    // File operation endpoint for group expense import
    @PostMapping("/{id}/import-expenses")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importGroupExpenses(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Importing expenses for group: {} for user: {}", id, getCurrentUserId());
            
            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No file provided"));
            }
            
            Map<String, Object> result = groupService.importGroupExpenses(id, file);
            return ResponseEntity.ok(ApiResponse.success(result, "Group expenses import completed"));
        } catch (Exception e) {
            log.error("Failed to import group expenses", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to import group expenses: " + e.getMessage()));
        }
    }

    // File operation endpoint for group expense export
    @GetMapping("/{id}/export-expenses")
    public ResponseEntity<byte[]> exportGroupExpenses(@PathVariable Long id) {
        try {
            log.info("Exporting expenses for group: {} for user: {}", id, getCurrentUserId());
            
            byte[] csvContent = groupService.exportGroupExpenses(id);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=group-expenses.csv")
                    .header("Content-Type", "text/csv")
                    .body(csvContent);
        } catch (Exception e) {
            log.error("Failed to export group expenses", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
