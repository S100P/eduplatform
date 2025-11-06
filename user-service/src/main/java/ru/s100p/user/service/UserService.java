package ru.s100p.user.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.s100p.shared.constants.ErrorCodes;
import ru.s100p.shared.dto.PageResponse;
import ru.s100p.shared.dto.UserDto;
import ru.s100p.shared.exceptions.BusinessException;
import ru.s100p.shared.exceptions.ValidationException;
import ru.s100p.user.dto.request.ChangePasswordRequest;
import ru.s100p.user.dto.request.RegisterRequest;
import ru.s100p.user.dto.request.UpdateProfileRequest;
import ru.s100p.user.entity.Role;
import ru.s100p.user.entity.User;
import ru.s100p.user.entity.UserRole;
import ru.s100p.user.kafka.UserServiceProducer;
import ru.s100p.user.mapper.UserMapper;
import ru.s100p.user.repository.RoleRepository;
import ru.s100p.user.repository.UserRepository;
import ru.s100p.user.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceProducer userServiceProducer;
    private final EmailVerificationService emailVerificationService;

    /**
     * Регистрация нового пользователя
     */
    @Transactional
    public UserDto registerUser(RegisterRequest request) {
        log.info("Регистрация нового пользователя с email: {}", request.getEmail());

        // Валидация данных
        validateRegistrationRequest(request);

        // Проверка на существование пользователя
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email уже используется", ErrorCodes.USER_ALREADY_EXISTS);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username уже занят", ErrorCodes.USER_ALREADY_EXISTS);
        }

        // Создание нового пользователя
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setBio(request.getBio());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setIsActive(true);
        user.setIsEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Назначение роли
        String roleName = validateAndGetRole(request.getRequestedRole());
        assignRoleInternal(savedUser, roleName);

        // Отправка email для верификации
        emailVerificationService.sendVerificationEmail(savedUser);

        // Публикация события о регистрации
        userServiceProducer.publishUserRegistered(savedUser);

        log.info("Пользователь успешно зарегистрирован с ID: {}", savedUser.getId());

        return UserMapper.toDto(savedUser);
    }

    /**
     * Получение пользователя по ID с проверкой прав доступа
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id, Long requesterId) {
        log.debug("Получение пользователя с ID: {} запрошено пользователем: {}", id, requesterId);

        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + id));

        // Проверка прав доступа (пользователь может видеть только свой профиль или если он админ)
        if (!id.equals(requesterId) && !hasAdminRole(requesterId)) {
            // Возвращаем ограниченную информацию для других пользователей
            return UserMapper.toPublicDto(user);
        }

        return UserMapper.toDto(user);
    }

    /**
     * Обновление профиля пользователя
     */
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Обновление профиля пользователя: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Трекинг изменений для события
        Map<String, Object> changes = new HashMap<>();

        // Обновление полей
        if (request.getFirstName() != null && !request.getFirstName().equals(user.getFirstName())) {
            changes.put("firstName", request.getFirstName());
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null && !request.getLastName().equals(user.getLastName())) {
            changes.put("lastName", request.getLastName());
            user.setLastName(request.getLastName());
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            validatePhone(request.getPhone());
            changes.put("phone", request.getPhone());
            user.setPhone(request.getPhone());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
            changes.put("bio", true);
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
            changes.put("avatarUrl", true);
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
            changes.put("dateOfBirth", true);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Публикация события об обновлении профиля
        if (!changes.isEmpty()) {
            userServiceProducer.publishUserProfileUpdated(updatedUser, changes);
        }

        log.info("Профиль пользователя {} успешно обновлен", userId);

        return UserMapper.toDto(updatedUser);
    }

    /**
     * Смена пароля
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Смена пароля для пользователя: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new ValidationException("Пароли не совпадают",
                    Map.of("confirmNewPassword", "Пароли должны совпадать"));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Проверка текущего пароля
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Неверный текущий пароль", ErrorCodes.INVALID_CREDENTIALS);
        }

        // Проверка что новый пароль отличается от старого
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("Новый пароль должен отличаться от текущего", "SAME_PASSWORD");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Пароль успешно изменен для пользователя: {}", userId);
    }

    /**
     * Назначение роли пользователю (для админов)
     */
    @Transactional
    public void assignRole(Long userId, String roleName, Long assignedBy) {
        log.info("Назначение роли {} пользователю {} администратором {}", roleName, userId, assignedBy);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        User admin = userRepository.findById(assignedBy)
                .orElseThrow(() -> new EntityNotFoundException("Администратор не найден: " + assignedBy));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Роль не найдена: " + roleName));

        // Проверка, что роль еще не назначена
        if (userRoleRepository.findByUser_IdAndRole_Id(userId, role.getId()).isPresent()) {
            log.warn("Роль {} уже назначена пользователю {}", roleName, userId);
            return;
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedBy(admin);
        userRole.setAssignedAt(LocalDateTime.now());

        userRoleRepository.save(userRole);

        log.info("Роль {} успешно назначена пользователю {}", roleName, userId);
    }

    /**
     * Отзыв роли у пользователя
     */
    @Transactional
    public void revokeRole(Long userId, String roleName) {
        log.info("Отзыв роли {} у пользователя {}", roleName, userId);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Роль не найдена: " + roleName));

        // Проверка, что у пользователя останется хотя бы одна роль
        List<UserRole> userRoles = userRoleRepository.findByUser_Id(userId);
        if (userRoles.size() <= 1) {
            throw new BusinessException("Нельзя удалить последнюю роль пользователя", "LAST_ROLE");
        }

        userRoleRepository.deleteByUser_IdAndRole_Id(userId, role.getId());

        log.info("Роль {} успешно отозвана у пользователя {}", roleName, userId);
    }

//    TODO добавить создание объекта peageble,чтобы в контроллере были только параметры страницы в инте
//     /**
//     * Метод для поиска пользователей по текстовому запросу.
//     * @param searchTerm строка для поиска
//     * @param pageNumber номер страницы (начиная с 0)
//     * @param pageSize количество пользователей на странице
//     * @return страница с найденными пользователями
//    public Page<User> search(String searchTerm, int pageNumber, int pageSize) {
//
//        // 1. Создаем объект Pageable для пагинации и сортировки.
//        //    - pageNumber: номер страницы, которую мы хотим получить.
//        //    - pageSize: сколько записей будет на этой странице.
//        //    - Sort.by("username"): указываем, что результаты нужно отсортировать по полю 'username'.
//        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("username").ascending());
//
//        // 2. Вызываем метод репозитория, передавая ему поисковый запрос и объект Pageable.
//        //    Здесь мы ищем пользователей, у которых в одном из полей есть слово "admin".
//        //    Мы хотим получить первую страницу (индекс 0), на которой будет 10 пользователей.
//        Page<User> foundUsers = userRepository.searchUsers(searchTerm, pageable);
//
//        return foundUsers;
//    }*/

    /**
     * Поиск пользователей с пагинацией и фильтрацией
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDto> searchUsers(String query, Pageable pageable) {
        Page<User> usersPage;

        if (query != null && !query.trim().isEmpty()) {
            String searchQuery = "%" + query.toLowerCase() + "%";
            usersPage = userRepository.searchUsers(searchQuery, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }

        List<UserDto> userDtos = usersPage.getContent().stream()
                .map(UserMapper::toPublicDto)
                .collect(Collectors.toList());

        return PageResponse.<UserDto>builder()
                .content(userDtos)
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .first(usersPage.isFirst())
                .last(usersPage.isLast())
                .build();
    }

    /**
     * Деактивация аккаунта
     */
    @Transactional
    public void deactivateAccount(Long userId) {
        log.info("Деактивация аккаунта пользователя: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Публикация события о деактивации
        userServiceProducer.publishUserDeactivated(user);

        log.info("Аккаунт пользователя {} деактивирован", userId);
    }

    /**
     * Получение всех пользователей (для админов)
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDto)
                .toList();
    }

    // ===== Вспомогательные методы =====

    private void validateRegistrationRequest(RegisterRequest request) {
        Map<String, String> errors = new HashMap<>();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            errors.put("confirmPassword", "Пароли не совпадают");
        }

        if (!request.isTermsAccepted()) {
            errors.put("termsAccepted", "Необходимо принять условия использования");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Ошибка валидации данных регистрации", errors);
        }
    }

    private String validateAndGetRole(String requestedRole) {
        if (requestedRole == null || requestedRole.trim().isEmpty()) {
            return "STUDENT";
        }

        // Только админы могут создавать других админов и инструкторов
        Set<String> allowedRoles = Set.of("STUDENT", "GUEST");
        if (!allowedRoles.contains(requestedRole.toUpperCase())) {
            log.warn("Попытка зарегистрировать пользователя с ролью: {}", requestedRole);
            return "STUDENT";
        }

        return requestedRole.toUpperCase();
    }

    private void assignRoleInternal(User user, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Роль не найдена: " + roleName));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedBy(user); // При регистрации пользователь назначает роль сам себе
        userRole.setAssignedAt(LocalDateTime.now());

        userRoleRepository.save(userRole);
    }

    private boolean hasAdminRole(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUser_Id(userId);
        return userRoles.stream()
                .anyMatch(ur -> "ADMIN".equals(ur.getRole().getName()));
    }

    private void validatePhone(String phone) {
        if (phone != null && !phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new ValidationException("Некорректный формат телефона",
                    Map.of("phone", "Телефон должен соответствовать международному формату"));
        }
    }
}
