package com.expense.service;

import com.expense.config.JwtUtil;
import com.expense.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JWTService {

    private final JwtUtil jwtUtil;

    public String generateToken(User user) {
        return jwtUtil.generateToken(user);
    }

    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        return jwtUtil.validateToken(token, userDetails);
    }
}
