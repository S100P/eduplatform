package ru.s100p.user.repository;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.s100p.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.role"}) //подгружает роли одним запросом через @EntityGraph, чтобы избежать N+1.
    Optional<User> findWithRolesById(Long id);
}

