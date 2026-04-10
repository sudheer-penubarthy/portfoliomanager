package com.sudheer.portfoliotracker.api.controller;

import com.sudheer.portfoliotracker.api.dto.UserDto;
import com.sudheer.portfoliotracker.exception.ResourceNotFoundException;
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.PortfolioUser;
import com.sudheer.portfoliotracker.repository.PortfolioUserRepository;
import com.sudheer.portfoliotracker.service.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and token management endpoints")
public class AuthController {

    private final PortfolioUserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public AuthController(PortfolioUserRepository userRepository, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns JWT tokens for authentication"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully with tokens",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data (email exists, invalid PAN, etc.)",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        // ...existing code...
        log.info("User registration request for email: {}", registrationRequest.email());

        // Check if user already exists
        if (userRepository.findByEmail(registrationRequest.email()).isPresent()) {
            log.warn("Registration failed: email already exists - {}", registrationRequest.email());
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        // Validate PAN format (10 characters, alphanumeric)
        if (!registrationRequest.pan().matches("^[A-Z0-9]{10}$")) {
            log.warn("Registration failed: invalid PAN - {}", registrationRequest.pan());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid PAN format"));
        }

        // Create new user
        PortfolioUser user = PortfolioUser.builder()
                .email(registrationRequest.email())
                .pan(registrationRequest.pan())
                .name(registrationRequest.firstName() + " " + registrationRequest.lastName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PortfolioUser savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Generate JWT tokens
        String accessToken = jwtTokenService.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken(savedUser.getId(), savedUser.getEmail());

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedUser.getId());
        response.put("email", savedUser.getEmail());
        response.put("name", savedUser.getName());
        response.put("pan", savedUser.getPan());
        response.put("token", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("expiresIn", 1800);  // 30 minutes in seconds
        response.put("tokenType", "Bearer");

        return ResponseEntity.created(URI.create("/api/users/" + savedUser.getId())).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user and returns JWT access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful, tokens returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid email or password"
            )
    })
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("User login request for email: {}", loginRequest.email());

        // Find user by email
        Optional<PortfolioUser> userOpt = userRepository.findByEmail(loginRequest.email());
        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found - {}", loginRequest.email());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email or password"));
        }

        PortfolioUser user = userOpt.get();

        // TODO: Verify password hash when password hashing is implemented
        // For now, just return success if user exists
        log.info("User logged in successfully: {}", user.getId());

        // Generate JWT tokens
        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId(), user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("pan", user.getPan());
        response.put("token", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("expiresIn", 1800);  // 30 minutes in seconds
        response.put("tokenType", "Bearer");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a refresh token for a new access token. Access tokens expire after 30 minutes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "New access token issued",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired refresh token"
            )
    })
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();
        log.info("Token refresh request received");

        // Validate refresh token
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Token refresh failed: refresh token is empty");
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
        }

        // Validate token
        if (jwtTokenService.isTokenExpired(refreshToken)) {
            log.warn("Token refresh failed: refresh token is expired");
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token has expired. Please login again"));
        }

        // Verify it's a refresh token
        String tokenType = jwtTokenService.extractTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            log.warn("Token refresh failed: provided token is not a refresh token");
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token type. Please provide a refresh token"));
        }

        // Extract user info from refresh token
        Long userId = jwtTokenService.extractUserId(refreshToken);
        String email = jwtTokenService.extractEmail(refreshToken);

        if (userId == null || email == null) {
            log.warn("Token refresh failed: unable to extract user info from refresh token");
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid refresh token"));
        }

        // Generate new access token
        String newAccessToken = jwtTokenService.generateAccessToken(userId, email);

        log.info("Token refreshed successfully for user: {}", email);

        Map<String, Object> response = new HashMap<>();
        response.put("token", newAccessToken);
        response.put("expiresIn", 1800);  // 30 minutes in seconds
        response.put("tokenType", "Bearer");

        return ResponseEntity.ok(response);
    }

    /*
     * @param userId the user ID
     * @return 200 OK with user data
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestParam Long userId) {
        log.debug("Fetching user profile for ID: {}", userId);

        PortfolioUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        UserDto dto = new UserDto();
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPan(user.getPan());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setKycStatus(user.getKycStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        return ResponseEntity.ok(dto);
    }

    /**
     * Update user profile
     *
     * @param userId  the user ID
     * @param userDto the updated user data
     * @return 200 OK with updated user data
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestParam Long userId, @Valid @RequestBody UserDto userDto) {
        log.info("Updating user profile for ID: {}", userId);

        PortfolioUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        // Update fields if provided
        if (userDto.getPhone() != null) {
            user.setPhone(userDto.getPhone());
        }
        if (userDto.getAddress() != null) {
            user.setAddress(userDto.getAddress());
        }
        if (userDto.getKycStatus() != null) {
            user.setKycStatus(userDto.getKycStatus());
        }

        user.setUpdatedAt(LocalDateTime.now());
        PortfolioUser updated = userRepository.save(user);

        UserDto response = new UserDto();
        response.setEmail(updated.getEmail());
        response.setName(updated.getName());
        response.setPan(updated.getPan());
        response.setPhone(updated.getPhone());
        response.setAddress(updated.getAddress());
        response.setKycStatus(updated.getKycStatus());
        response.setCreatedAt(updated.getCreatedAt());
        response.setUpdatedAt(updated.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    // ============== Request/Response DTOs ==============

    public record RegistrationRequest(
            @Email @NotBlank String email,
            @NotBlank String pan,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String password
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required") String refreshToken
    ) {
    }
}

