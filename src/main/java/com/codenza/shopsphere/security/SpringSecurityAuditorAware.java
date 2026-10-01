package com.codenza.shopsphere.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("auditorProvider")
public class SpringSecurityAuditorAware implements AuditorAware<String>{

	 @Override
	    public Optional<String> getCurrentAuditor() {
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	        if (authentication == null || !authentication.isAuthenticated()
	                || "anonymousUser".equals(authentication.getPrincipal())) {
	            return Optional.of("SYSTEM");
	        }

	        return Optional.of(authentication.getName());
	    }
}
