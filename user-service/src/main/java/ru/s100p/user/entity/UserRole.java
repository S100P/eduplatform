package ru.s100p.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}),
        indexes = {
                @Index(name = "idx_user_roles_user", columnList = "user_id"),
                @Index(name = "idx_user_roles_role", columnList = "role_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    Role role;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    LocalDateTime assignedAt;

    @ManyToOne
    @JoinColumn(name = "assigned_by")
    User assignedBy;
}
