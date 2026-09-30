package com.codenza.shopsphere.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codenza.shopsphere.dto.AuthResponse;
import com.codenza.shopsphere.dto.LoginRequest;
import com.codenza.shopsphere.dto.RegisterRequest;
import com.codenza.shopsphere.dto.UserResponse;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.enums.Role;
import com.codenza.shopsphere.exception.BadRequestException;
import com.codenza.shopsphere.exception.DuplicateResourceException;
import com.codenza.shopsphere.mapper.UserMapper;
import com.codenza.shopsphere.repository.UserRepository;
import com.codenza.shopsphere.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

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
    
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(), request.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token);
    }
}