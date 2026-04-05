package com.expense.repository;

import com.expense.entity.Group;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByCreatedBy(User user);

    // Temporarily commented out since members relationship is disabled
    // List<Group> findByMembers_User(User user);

    boolean existsByCreatedByAndName(User user, String name);
    
    Optional<Group> findByName(String name);
}
