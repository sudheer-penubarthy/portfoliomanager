package com.sudheer.portfoliotracker.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Service for generating and validating JWT tokens
 * Supports access tokens (30 minutes) and refresh tokens (7 days)
 */
@Slf4j
@Service
public class JwtTokenService {

    @Value("${jwt.secret:your-secret-key-minimum-32-characters-long-for-hs256}")
    private String secretKey;

    @Value("${jwt.access-token-expiration:1800000}")  // 30 minutes in milliseconds
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")  // 7 days in milliseconds
    private Long refreshTokenExpiration;

    /**
     * Generate an access token for a user
     *
     * @param userId the user ID
     * @param email  the user email
     * @return JWT access token
     */
    public String generateAccessToken(Long userId, String email) {
        return generateToken(userId, email, accessTokenExpiration, "access");
    }

    /**
     * Generate a refresh token for a user
     *
     * @param userId the user ID
     * @param email  the user email
     * @return JWT refresh token
     */
    public String generateRefreshToken(Long userId, String email) {
        return generateToken(userId, email, refreshTokenExpiration, "refresh");
    }

    /**
     * Generate a JWT token
     *
     * @param userId       the user ID
     * @param email        the user email
     * @param expiration   expiration time in milliseconds
     * @param tokenType    type of token (access or refresh)
     * @return JWT token
     */
    private String generateToken(Long userId, String email, Long expiration, String tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("tokenType", tokenType);

        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated {} token for user: {} (ID: {})", tokenType, email, userId);
        return token;
    }

    /**
     * Validate and extract claims from a token
     *
     * @param token the JWT token
     * @return Claims object or null if invalid
     */
    public Claims validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            log.warn("Token validation failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Check if a token is expired
     *
     * @param token the JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return true;
        }

        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    /**
     * Check if a token is about to expire (within 5 minutes)
     *
     * @param token the JWT token
     * @return true if token is about to expire
     */
    public boolean isTokenAboutToExpire(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return true;
        }

        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return false;
        }

        long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
        return timeUntilExpiration < 5 * 60 * 1000;  // Less than 5 minutes
    }

    /**
     * Extract user ID from token
     *
     * @param token the JWT token
     * @return user ID or null if invalid
     */
    public Long extractUserId(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("userId", Long.class);
    }

    /**
     * Extract email from token
     *
     * @param token the JWT token
     * @return email or null if invalid
     */
    public String extractEmail(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return null;
        }
        return claims.getSubject();
    }

    /**
     * Extract token type from token
     *
     * @param token the JWT token
     * @return token type (access or refresh) or null if invalid
     */
    public String extractTokenType(String token) {
        Claims claims = validateToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get("tokenType", String.class);
    }
}

