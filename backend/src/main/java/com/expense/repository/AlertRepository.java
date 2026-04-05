package com.expense.repository;

import com.expense.entity.Alert;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserAndReadFalse(User user);
}