package com.expense.controller;

import com.expense.dto.GroupExpenseDTO;
import com.expense.entity.GroupExpense;
import com.expense.service.GroupExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-expenses")
@RequiredArgsConstructor
public class GroupExpenseController {

    private final GroupExpenseService groupExpenseService;

    @PostMapping
    public ResponseEntity<GroupExpense> createGroupExpense(@RequestBody GroupExpenseDTO dto) {
        GroupExpense groupExpense = groupExpenseService.createGroupExpense(dto);
        return ResponseEntity.ok(groupExpense);
    }

    @GetMapping
    public ResponseEntity<List<GroupExpense>> getUserGroupExpenses() {
        List<GroupExpense> groupExpenses = groupExpenseService.getUserGroupExpenses();
        return ResponseEntity.ok(groupExpenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupExpense> getGroupExpense(@PathVariable Long id) {
        GroupExpense groupExpense = groupExpenseService.getGroupExpense(id);
        return ResponseEntity.ok(groupExpense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupExpense> updateGroupExpense(@PathVariable Long id, @RequestBody GroupExpenseDTO dto) {
        GroupExpense groupExpense = groupExpenseService.updateGroupExpense(id, dto);
        return ResponseEntity.ok(groupExpense);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupExpense(@PathVariable Long id) {
        groupExpenseService.deleteGroupExpense(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<GroupExpense> settleExpense(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String paymentMethod = request.get("paymentMethod");
        GroupExpense groupExpense = groupExpenseService.settleExpense(id, paymentMethod);
        return ResponseEntity.ok(groupExpense);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getSettlementSummary(@PathVariable Long id) {
        Map<String, Object> summary = groupExpenseService.getSettlementSummary(id);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/outstanding")
    public ResponseEntity<List<GroupExpense>> getOutstandingExpenses() {
        List<GroupExpense> outstandingExpenses = groupExpenseService.getOutstandingExpenses();
        return ResponseEntity.ok(outstandingExpenses);
    }

    @GetMapping("/created")
    public ResponseEntity<List<GroupExpense>> getCreatedExpenses() {
        List<GroupExpense> createdExpenses = groupExpenseService.getCreatedExpenses();
        return ResponseEntity.ok(createdExpenses);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getUserSettlementSummary() {
        Map<String, Object> summary = groupExpenseService.getUserSettlementSummary();
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<GroupExpense> addParticipant(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String participantEmail = request.get("participantEmail");
        GroupExpense groupExpense = groupExpenseService.addParticipant(id, participantEmail);
        return ResponseEntity.ok(groupExpense);
    }

    @DeleteMapping("/{id}/participants")
    public ResponseEntity<GroupExpense> removeParticipant(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String participantEmail = request.get("participantEmail");
        GroupExpense groupExpense = groupExpenseService.removeParticipant(id, participantEmail);
        return ResponseEntity.ok(groupExpense);
    }

    @GetMapping("/active")
    public ResponseEntity<List<GroupExpense>> getActiveGroupExpenses() {
        // This would need to be implemented in GroupExpenseService
        return ResponseEntity.ok().build();
    }
}
