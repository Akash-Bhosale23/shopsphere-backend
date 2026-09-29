package com.codenza.shopsphere.dto;

import com.codenza.shopsphere.enums.Role;

public record UserResponse(Long id, String name, String email, Role role) {
}