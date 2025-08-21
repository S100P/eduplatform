package ru.s100p.user.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.s100p.user.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}

