package com.expense.repository;

import com.expense.entity.Group;
import com.expense.entity.GroupMember;
import com.expense.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroup(Group group);

    Optional<GroupMember> findByGroupAndUser(Group group, User user);

    void deleteByGroupAndUser(Group group, User user);

    boolean existsByGroupAndUser(Group group, User user);
}
