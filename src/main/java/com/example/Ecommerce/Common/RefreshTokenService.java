package com.example.Ecommerce.Common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.AppUser;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }

    @Transactional
    public RefreshToken storeRefreshToken(AppUser user, String refreshToken) {
        String jti = jwtUtils.extractJti(refreshToken);
        String tokenHash = hashToken(refreshToken);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setJti(jti);
        rt.setTokenHash(tokenHash);
        rt.setExpiresAt(LocalDateTime.now().plusSeconds(jwtUtils.getRefreshExpirationSeconds()));
        rt.setStatus(RefreshToken.Status.ACTIVE);

        return refreshTokenRepository.save(rt);
    }

    @Transactional
    public Optional<RefreshToken> validateAndRotate(String refreshToken) {
        String jti = jwtUtils.extractJti(refreshToken);
        String tokenHash = hashToken(refreshToken);

        RefreshToken stored = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (stored.getStatus() == RefreshToken.Status.REVOKED) {
            // Token reuse detected - revoke entire family
            revokeFamily(stored.getUser().getUserId(), stored.getId());
            throw new InvalidRefreshTokenException("Refresh token already revoked (potential reuse detected)");
        }

        if (stored.getStatus() == RefreshToken.Status.REPLACED) {
            // Token reuse detected - revoke entire family (someone used an old token)
            revokeFamily(stored.getUser().getUserId(), stored.getId());
            throw new InvalidRefreshTokenException("Refresh token already used (potential reuse detected)");
        }

        if (!stored.getTokenHash().equals(tokenHash)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        return Optional.of(stored);
    }

    @Transactional
    public void rotateToken(RefreshToken oldToken, String newRefreshToken) {
        String newJti = jwtUtils.extractJti(newRefreshToken);
        String newTokenHash = hashToken(newRefreshToken);

        // Store new token first
        RefreshToken newToken = new RefreshToken();
        newToken.setUser(oldToken.getUser());
        newToken.setJti(newJti);
        newToken.setTokenHash(newTokenHash);
        newToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtUtils.getRefreshExpirationSeconds()));
        newToken.setStatus(RefreshToken.Status.ACTIVE);
        newToken = refreshTokenRepository.save(newToken);

        // Mark old as replaced with new token's ID
        refreshTokenRepository.rotateToken(oldToken.getJti(), RefreshToken.Status.REPLACED, newToken.getId());
    }

    @Transactional
    public void revokeToken(String refreshToken) {
        String jti = jwtUtils.extractJti(refreshToken);
        refreshTokenRepository.updateStatusByJti(jti, RefreshToken.Status.REVOKED);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, RefreshToken.Status.REVOKED);
    }

    @Transactional
    public void revokeFamily(Long userId, Long triggeringTokenId) {
        // Revoke all tokens for this user
        revokeAllForUser(userId);
    }

    @Transactional
    public void cleanupExpired() {
        List<RefreshToken> expired = refreshTokenRepository.findExpiredTokens(LocalDateTime.now());
        for (RefreshToken rt : expired) {
            if (rt.getStatus() == RefreshToken.Status.ACTIVE) {
                rt.setStatus(RefreshToken.Status.REVOKED);
                refreshTokenRepository.save(rt);
            }
        }
    }

    public long getRefreshExpirationSeconds() {
        return jwtUtils.getRefreshExpirationMs() / 1000;
    }

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) {
            super(message);
        }
    }
}