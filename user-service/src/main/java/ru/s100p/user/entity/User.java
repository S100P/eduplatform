package ru.s100p.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_active", columnList = "is_active"),
                @Index(name = "idx_users_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 50, nullable = false, unique = true)
    String username;

    @Column(length = 100, nullable = false, unique = true)
    String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    String passwordHash;

    @Column(name = "first_name", length = 50)
    String firstName;

    @Column(name = "last_name", length = 50)
    String lastName;

    @Column(length = 20)
    String phone;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Lob
    String bio;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    Boolean isEmailVerified = false;

    @Column(name = "last_login")
    LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Связь с UserRole
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    Set<UserRole> roles = new HashSet<>();

    // Связь с RefreshToken
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    Set<RefreshToken> refreshTokens = new HashSet<>();

}
