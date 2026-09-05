package io.commercedna.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT Token Generation and Validation Service.
 * Issues time-limited API tokens for authenticated merchants.
 */
@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final long TOKEN_VALIDITY_HOURS = 24;

    @Value("${commercedna.security.jwt-secret:commercedna_default_jwt_secret_change_in_production}")
    private String jwtSecret;

    public String generateToken(String merchantId, String merchantCode, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS);

        return Jwts.builder()
                .setSubject(merchantId)
                .claim("merchantCode", merchantCode)
                .claim("roles", roles)
                .claim("tokenId", UUID.randomUUID().toString())
                .claim("issuedAt", now.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    public String extractMerchantId(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }
}