package com.buihien.fileserver.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Base64 encoded secret key for HS256 (fs_secret_key_1234567890_asdfghjkl_zxcvbnm_qwertyuiop)
    private static final String SECRET_KEY = "ZnNfc2VjcmV0X2tleV8xMjM0NTY3ODkwX2FzZGZnaGprbF96eGN2Ym5tX3F3ZXJ0eXVpb3A=";

    // Access Token duration: 15 minutes (900,000 ms)
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000;

    // Refresh Token duration: 7 days (604,800,000 ms)
    private static final long REFRESH_TOKEN_EXPIRATION = 7L * 24 * 60 * 60 * 1000;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(String username, List<String> roles, List<String> permissions) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", roles);
        extraClaims.put("permissions", permissions);
        extraClaims.put("type", "ACCESS");
        return buildToken(extraClaims, username, ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "REFRESH");
        return buildToken(extraClaims, username, REFRESH_TOKEN_EXPIRATION);
    }

    private String buildToken(Map<String, Object> extraClaims, String username, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String getTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractClaim(token, claims -> claims.get("permissions", List.class));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
