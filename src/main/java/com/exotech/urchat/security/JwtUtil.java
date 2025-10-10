package com.exotech.urchat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtil {

    private String secret = "akncOANSclknscnLSCNONVSDcskdncvASNXOCKNKOnvksnvMKLCSNSO";

    private final long ACCESS_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000 * 7;
    private final long REFRESH_TOKEN_EXPIRATION = 365L * 24 * 60 * 60 * 1000; // 1 year

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public LocalDateTime getAccessTokenExpiry(String token) {
        Date expiry = extractExpiration(token);
        return LocalDateTime.ofInstant(expiry.toInstant(), ZoneId.systemDefault());
    }

    public LocalDateTime getRefreshTokenExpiry(String token) {
        Date expiry = extractExpiration(token);
        return LocalDateTime.ofInstant(expiry.toInstant(), ZoneId.systemDefault());
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateAccessToken(String username) {
        return createToken(username, ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(String username) {
        return createToken(username, REFRESH_TOKEN_EXPIRATION);
    }

    private String createToken(String subject, long expiration){
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token){
        try{
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e){
            return false;
        }
    }

}
