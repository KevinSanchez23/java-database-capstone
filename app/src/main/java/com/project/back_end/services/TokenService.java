package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

@Component
public class TokenService {

    private static final long TOKEN_VALIDITY_DAYS = 7;

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final String jwtSecret;

    public TokenService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            @Value("${jwt.secret}") String jwtSecret) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.jwtSecret = jwtSecret;
    }

    public String generateToken(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier is required to generate a token.");
        }

        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(identifier)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractIdentifier(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(normalizeToken(token))
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token, String user) {
        if (token == null || token.isBlank() || user == null || user.isBlank()) {
            return false;
        }

        try {
            String identifier = extractIdentifier(token);
            if (identifier == null || identifier.isBlank()) {
                return false;
            }

            return switch (user.trim().toLowerCase(Locale.ROOT)) {
                case "admin" -> adminRepository.findByUsername(identifier) != null;
                case "doctor" -> doctorRepository.findByEmail(identifier) != null;
                case "patient", "loggedpatient" -> patientRepository.findByEmail(identifier) != null;
                default -> false;
            };
        } catch (Exception exception) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required.");
        }

        String normalizedToken = token.trim();
        if (normalizedToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
            normalizedToken = normalizedToken.substring(7).trim();
        }
        return normalizedToken;
    }
}
