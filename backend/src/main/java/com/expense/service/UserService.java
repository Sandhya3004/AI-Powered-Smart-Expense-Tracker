package com.expense.service;

import com.expense.entity.User;
import com.expense.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("Not authenticated");
        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        } else if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found")).getId();
        }
        throw new RuntimeException("Unknown principal type");
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public String exportUserData(User user) {
        // Simple CSV export mock implementation
        return "id,email,name\n" + user.getId() + "," + user.getEmail() + "," + user.getName();
    }
}
