package com.expense.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bill_reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user"})
public class BillReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "bill_name", nullable = false)
    private String billName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String category;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "recurrence_pattern")
    private String recurrencePattern;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPaid = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bill_reminder_days", joinColumns = @JoinColumn(name = "bill_reminder_id"))
    @Column(name = "reminder_day")
    @Builder.Default
    private List<Integer> reminderDays = List.of(1, 3, 7);

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "status")
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (isPaid) {
            status = "PAID";
        } else if (dueDate.isBefore(LocalDate.now())) {
            status = "OVERDUE";
        } else {
            status = "PENDING";
        }
    }
}
