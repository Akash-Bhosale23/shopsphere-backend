package com.codenza.shopsphere.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shopSphereOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopSphere API")
                        .description("Backend REST API for the ShopSphere e-commerce application")
                        .version("v1"));
    }
}