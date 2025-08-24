package ru.s100p.user.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.s100p.shared.constants.ErrorCodes;
import ru.s100p.shared.constants.SecurityConstants;
import ru.s100p.shared.dto.UserDto;
import ru.s100p.shared.exceptions.BusinessException;
import ru.s100p.user.dto.request.RegisterRequest;
import ru.s100p.user.entity.Role;
import ru.s100p.user.entity.User;
import ru.s100p.user.entity.UserRole;
import ru.s100p.user.kafka.UserServiceProducer;
import ru.s100p.user.mapper.UserMapper;
import ru.s100p.user.repository.RoleRepository;
import ru.s100p.user.repository.UserRepository;
import ru.s100p.user.repository.UserRoleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Validated
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserServiceProducer userServiceProducer;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto registerUser(RegisterRequest request) {
        // Проверка на существование
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email уже используется", ErrorCodes.USER_ALREADY_EXISTS);
        }

        // Создание пользователя
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Назначение роли по умолчанию
        User savedUser = userRepository.save(user);
        assignRole(savedUser.getId(), SecurityConstants.ROLE_STUDENT);

        // Публикация события
        userServiceProducer.publishUserRegistered(savedUser);

        return UserMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return UserMapper.toDto(user);
    }

    @Transactional
    public UserDto createUser(User user) {
        //TODO валидации можно добавить тут
        User saved = userRepository.save(user);
        return UserMapper.toDto(saved);
    }

    @Transactional
    public void assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));

        if (userRoleRepository.findByUser_IdAndRole_Id(userId, role.getId()).isEmpty()) {
            UserRole ur = new UserRole();
            ur.setUser(user);
            ur.setRole(role);
            ur.setAssignedAt(java.time.LocalDateTime.now());
            ur.setAssignedBy(user); //TODO пример — сам себе назначил
            userRoleRepository.save(ur);
        }
    }

    @Transactional
    public void revokeRole(Long userId, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleName));
        userRoleRepository.deleteByUser_IdAndRole_Id(userId, role.getId());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDto)
                .toList();
    }
}

