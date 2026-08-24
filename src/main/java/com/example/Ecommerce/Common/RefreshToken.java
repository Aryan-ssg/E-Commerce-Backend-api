package com.example.Ecommerce.Common;

import java.time.LocalDateTime;

import com.example.Ecommerce.AppUser.AppUser;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_token_user_status", columnList = "user_id,status"),
        @Index(name = "idx_refresh_token_jti", columnList = "jti", unique = true),
        @Index(name = "idx_refresh_token_family", columnList = "family_id")
})
public class RefreshToken {

    public enum Status {
        ACTIVE,
        REVOKED,
        REPLACED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    private String jti;

    private String tokenHash;

    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private Long replacedById;

    private String familyId;

}