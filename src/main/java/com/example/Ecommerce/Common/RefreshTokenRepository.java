package com.example.Ecommerce.Common;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.Ecommerce.AppUser.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findByUserAndStatus(AppUser user, RefreshToken.Status status);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.status = :status WHERE rt.jti = :jti")
    int updateStatusByJti(@Param("jti") String jti, @Param("status") RefreshToken.Status status);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.status = :status, rt.replacedById = :replacedById WHERE rt.jti = :jti")
    int rotateToken(@Param("jti") String jti, @Param("status") RefreshToken.Status status, @Param("replacedById") Long replacedById);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.status = :status WHERE rt.user.id = :userId")
    int revokeAllForUser(@Param("userId") Long userId, @Param("status") RefreshToken.Status status);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.status = :status WHERE rt.familyId = :familyId")
    int revokeAllForFamily(@Param("familyId") String familyId, @Param("status") RefreshToken.Status status);

    List<RefreshToken> findByExpiresAtBefore(LocalDateTime time);

}