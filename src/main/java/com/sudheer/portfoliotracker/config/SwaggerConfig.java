package com.sudheer.portfoliotracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration for Portfolio Manager API
 * Provides comprehensive API documentation and Swagger UI
 */
@Configuration
public class SwaggerConfig {

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.portfoliomanager.com")
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for authentication. Use Bearer token in Authorization header.")));
    }

    private Info apiInfo() {
        return new Info()
                .title("Portfolio Manager API")
                .description("Comprehensive REST API for investment portfolio management\n\n" +
                        "## Features\n" +
                        "- User authentication with JWT tokens\n" +
                        "- Multi-file upload support (CSV, ZIP)\n" +
                        "- AMFI NAV data synchronization\n" +
                        "- Portfolio analysis and reporting\n" +
                        "- Transaction management\n" +
                        "- Goal tracking and monitoring\n\n" +
                        "## Authentication\n" +
                        "This API uses JWT Bearer token authentication. " +
                        "Obtain a token via `/api/auth/login` endpoint and include it in all requests " +
                        "using the Authorization header: `Authorization: Bearer <token>`\n\n" +
                        "## Token Refresh\n" +
                        "Access tokens expire after 30 minutes. Use the refresh token endpoint " +
                        "to obtain a new access token before expiration.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Portfolio Manager Support")
                        .email("support@portfoliomanager.com")
                        .url("https://portfoliomanager.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }
}

