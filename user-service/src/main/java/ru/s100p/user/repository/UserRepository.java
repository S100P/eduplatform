package ru.s100p.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.s100p.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Проверка существования
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // Поиск по уникальным полям
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    // Загрузка с ролями через EntityGraph для избежания N+1
    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<User> findWithRolesById(Long id);

    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<User> findWithRolesByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<User> findWithRolesByEmail(String email);

    // Поиск пользователей с фильтрацией
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(:query) OR " +
            "LOWER(u.email) LIKE LOWER(:query) OR " +
            "LOWER(u.firstName) LIKE LOWER(:query) OR " +
            "LOWER(u.lastName) LIKE LOWER(:query)")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    // Поиск активных пользователей
    Page<User> findByIsActive(Boolean isActive, Pageable pageable);

    // Поиск по роли
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.roles ur " +
            "JOIN ur.role r " +
            "WHERE r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    // Поиск пользователей, зарегистрированных в определенный период
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersRegisteredBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Поиск неверифицированных пользователей
    List<User> findByIsEmailVerifiedFalse();

    // Статистические запросы
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    Long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    Long countUsersRegisteredAfter(@Param("date") LocalDateTime date);

    // Поиск пользователей для отправки уведомлений
    @Query("SELECT u FROM User u WHERE " +
            "u.isActive = true AND " +
            "u.isEmailVerified = true AND " +
            "u.lastLogin < :inactiveDate")
    List<User> findInactiveUsers(@Param("inactiveDate") LocalDateTime inactiveDate);

    // Batch операции
    @Query("UPDATE User u SET u.isActive = false WHERE u.lastLogin < :inactiveDate")
    int deactivateInactiveUsers(@Param("inactiveDate") LocalDateTime inactiveDate);
}