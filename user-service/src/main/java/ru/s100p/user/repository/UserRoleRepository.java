package ru.s100p.user.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.s100p.user.entity.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUser_Id(Long userId);

    List<UserRole> findByRole_Id(Long roleId);

    Optional<UserRole> findByUser_IdAndRole_Id(Long userId, Long roleId);

    void deleteByUser_IdAndRole_Id(Long userId, Long roleId);
}

