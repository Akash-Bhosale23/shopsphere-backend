package com.codenza.shopsphere.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codenza.shopsphere.dto.RegisterRequest;
import com.codenza.shopsphere.dto.UserResponse;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.enums.Role;
import com.codenza.shopsphere.exception.BadRequestException;
import com.codenza.shopsphere.exception.DuplicateResourceException;
import com.codenza.shopsphere.mapper.UserMapper;
import com.codenza.shopsphere.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Registering as ADMIN is not allowed");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? Role.CUSTOMER : request.role());

        return userMapper.toResponse(userRepository.save(user));
    }
}