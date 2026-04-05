package com.expense.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "group_expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GroupExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "expenses"})
    private Group group;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private String category;

    private String paidBy; // Name of person who paid

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "groupExpenses"})
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_expense_participants",
        joinColumns = @JoinColumn(name = "group_expense_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "groupExpenses"})
    private Set<User> participants;

    @ElementCollection
    @CollectionTable(name = "group_expense_splits", joinColumns = @JoinColumn(name = "group_expense_id"))
    @MapKeyColumn(name = "user_id")
    @MapKeyClass(Long.class)
    @Column(name = "split_amount")
    private Map<Long, BigDecimal> splitAmounts;

    @ElementCollection
    @CollectionTable(name = "group_expense_settlements", joinColumns = @JoinColumn(name = "group_expense_id"))
    @MapKeyColumn(name = "user_id")
    @MapKeyClass(Long.class)
    @Column(name = "settled_amount")
    private Map<Long, BigDecimal> settledAmounts;

    @ElementCollection
    @CollectionTable(name = "group_expense_settlement_status", joinColumns = @JoinColumn(name = "group_expense_id"))
    @MapKeyColumn(name = "user_id")
    @MapKeyClass(Long.class)
    @Column(name = "settlement_status")
    private Map<Long, String> settlementStatus;

    @Column(name = "currency")
    private String currency = "INR";

    private String status; // ACTIVE, SETTLED, CANCELLED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (expenseDate == null) {
            expenseDate = LocalDate.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
