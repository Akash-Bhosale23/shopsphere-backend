package com.codenza.shopsphere.dto;

public record AuthResponse(String token, String tokenType) {
    
	public AuthResponse(String token) {
		
        this(token, "Bearer");
    }
}