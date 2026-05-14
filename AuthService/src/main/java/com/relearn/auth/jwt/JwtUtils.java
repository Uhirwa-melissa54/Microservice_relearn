package com.relearn.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for all JWT operations:
 *  - Generating access tokens
 *  - Extracting claims (userId, email, role)
 *  - Validating tokens
 *
 * Uses HMAC-SHA256 (HS256) signing algorithm.
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // ----------------------------------------------------------------
    //  Token Generation
    // ----------------------------------------------------------------

    /**
     * Generates an access token for the given user.
     * Embeds userId, email, and role as custom claims.
     */
    public String generateAccessToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)                          // email is the subject
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ----------------------------------------------------------------
    //  Claims Extraction
    // ----------------------------------------------------------------

    /** Extract the email (subject) from the token */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extract the userId custom claim from the token */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class)).longValue();
    }

    /** Extract the role custom claim from the token */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /** Extract the expiration date from the token */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /** Generic claim extractor using a resolver function */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ----------------------------------------------------------------
    //  Token Validation
    // ----------------------------------------------------------------

    /**
     * Validates the token against the UserDetails loaded from the database.
     * Checks: email match + not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /** Returns true if the token's expiration date is in the past */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ----------------------------------------------------------------
    //  Internal Helpers
    // ----------------------------------------------------------------

    /** Parse and return all claims from the token */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** Decode the Base64 secret and build the HMAC signing key */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates a raw token string without needing UserDetails.
     * Used by the JWT filter to do a quick structural check.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
