package com.codenza.shopsphere.mapper;

import org.springframework.stereotype.Component;

import com.codenza.shopsphere.dto.UserResponse;
import com.codenza.shopsphere.entity.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}