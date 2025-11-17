package ru.s100p.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.s100p.shared.constants.ErrorCodes;
import ru.s100p.shared.dto.UserDto;
import ru.s100p.shared.exceptions.BusinessException;
import ru.s100p.user.dto.request.LoginRequest;
import ru.s100p.user.dto.response.AuthResponse;
import ru.s100p.user.entity.User;
import ru.s100p.user.mapper.UserMapper;
import ru.s100p.user.repository.UserRepository;
import ru.s100p.user.security.JwtService;
import ru.s100p.user.security.TokenBlacklistService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    private static final long ACCESS_TOKEN_EXPIRY = 3600; // 1 час в секундах
    private static final long REFRESH_TOKEN_EXPIRY = 604800; // 7 дней в секундах
    private static final long REMEMBER_ME_EXPIRY = 2592000; // 30 дней в секундах

    /**
     * Аутентификация пользователя
     */
    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        log.info("Попытка аутентификации: {}", request.getUsernameOrEmail());

        // Найти пользователя по email или username
        User user = userRepository.findByEmail(request.getUsernameOrEmail())
                .or(() -> userRepository.findByUsername(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BusinessException("Неверные учетные данные", ErrorCodes.INVALID_CREDENTIALS));

        // Проверка активности аккаунта
        if (!user.getIsActive()) {
            throw new BusinessException("Аккаунт деактивирован", ErrorCodes.ACCOUNT_DISABLED);
        }

        // Аутентификация через Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        request.getPassword()
                )
        );

        // Обновление информации о последнем входе
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Генерация токенов
        String accessToken = jwtService.generateAccessToken(authentication);
        // Если стоить галочка "запомнить", то устанавливается срок в 30 дней, если нет, в 7
        long refreshExpiry = request.isRememberMe() ? REMEMBER_ME_EXPIRY : REFRESH_TOKEN_EXPIRY;
        var refreshTokenDto = refreshTokenService.createToken(user.getId(), refreshExpiry);

        // Определение, первый ли это вход. Этот этап нужен для того, чтобы приложение могло по-особому отреагировать на первый вход пользователя. Например, показать ему приветственное сообщение, предложить пройти обучение или заполнить профиль.
        boolean isFirstLogin = user.getLastLogin() == null ||
                user.getLastLogin().equals(user.getCreatedAt());

        log.info("Успешная аутентификация пользователя: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenDto.token())
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRY)
                .issuedAt(LocalDateTime.now())
                .user(UserMapper.toDto(user))
                .firstLogin(isFirstLogin)
                .emailVerified(user.getIsEmailVerified())
                .message("Вход выполнен успешно")
                .build();
    }

    /**
     * Генерация AuthResponse для нового пользователя
     */
    @Transactional
    public AuthResponse generateAuthResponse(UserDto userDto) {
        User user = userRepository.findById(userDto.id())
                .orElseThrow(() -> new BusinessException("Пользователь не найден",
                        ErrorCodes.USER_NOT_FOUND));

        /*
         * Создание UserDetails. UserDetails – это описание (контракт) сущности, которая
         * пытается получить доступ к приложению. Источник данных: Он содержит все данные,
         * необходимые для принятия решений об авторизации и для сравнения учетных данных.
         * UserDetails отвечает на вопрос, кто этот пользователь.
         */
        UserDetails userDetails = createUserDetails(user);
        /*
         * Создание Authentication объекта. Объект Authentication в Spring Security служит
         * центральным контейнером для всей информации, связанной с аутентификацией
         * пользователя. Объект Authentication – это доказательство того, что процесс
         * аутентификации был успешно завершен. Он отвечает на вопрос, был ли пользователь
         * успешно аутентифицирован. Это как штамп о въезде 🔖 в вашем паспорте — само по
         * себе наличие паспорта не означает, что вы внутри страны, но штамп доказывает
         * успешное прохождение контроля.
         *
         * Principal: Как правило, это объект UserDetails. Credentials: Пароль (до
         * аутентификации) или null (после аутентификации). [В вашем случае передается null,
         * так как аутентификация уже прошла (пользователь найден), и пароль не нужен для
         * создания токена.] isAuthenticated(): Самый важный флаг, который устанавливается в
         * true после успешной проверки.
         */
        /*
         * В методе UsernamePasswordAuthenticationToken флаг isAuthenticated() всегда
         * устанавливает true, т.к. вызывается после успешной регистрации и проверки в
         * (userService.registerUser).
         * "В этом контексте, вы сами подтверждаете статус аутентификации, создавая токен. Вы не поручаете проверку Spring Security,
         * а информируете его о том, что аутентификация уже состоялась."
         */
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,
                null, userDetails.getAuthorities());

        // Генерация токенов
        String accessToken = jwtService.generateAccessToken(authentication);
        var refreshTokenDto = refreshTokenService.createToken(user.getId(), REFRESH_TOKEN_EXPIRY);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenDto.token())
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRY)
                .issuedAt(LocalDateTime.now())
                .user(userDto)
                .firstLogin(true)
                .emailVerified(false)
                .message("Регистрация прошла успешно")
                .build();
    }

    /**
     * Обновление access токена через refresh токен
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Запрос на обновление токена");

        // Валидация refresh токена
        var refreshTokenEntity = refreshTokenService.validateAndGetToken(refreshToken);

        User user = userRepository.findById(refreshTokenEntity.getUser().getId())
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));

        // Создание нового access токена
        UserDetails userDetails = createUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = jwtService.generateAccessToken(authentication);

        // Опционально: ротация refresh токена
        refreshTokenService.revokeToken(refreshToken);
        var newRefreshToken = refreshTokenService.createToken(user.getId(), REFRESH_TOKEN_EXPIRY);

        log.info("Токен успешно обновлен для пользователя: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.token())
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_EXPIRY)
                .issuedAt(LocalDateTime.now())
                .user(UserMapper.toDto(user))
                .emailVerified(user.getIsEmailVerified())
                .message("Токен обновлен")
                .build();
    }

    /**
     * Выход из системы (добавление токена в черный список)
     */
    @Transactional
    public void logout(String token) {
        log.info("Выход из системы");

        // Проверка токена, чтобы отфильтровать в принципе "не наши" токены
        if (!jwtService.validateToken(token)) {
            log.warn("Попытка выхода с невалидным токеном.");
            throw new BusinessException("Невалидный токен", ErrorCodes.INVALID_TOKEN);
        }

        // Добавление токена в черный список
        tokenBlacklistService.blacklistToken(token);

        // Отзыв refresh токена
        String username = jwtService.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));

        // Отзываем только последний refresh токен
        var activeTokens = refreshTokenService.getActiveTokens(user.getId());
        if (!activeTokens.isEmpty()) {
            refreshTokenService.revokeToken(activeTokens.get(0).token());
        }

        log.info("Пользователь {} вышел из системы", username);
    }

    /**
     * Выход из всех устройств
     */
    @Transactional
    public void logoutFromAllDevices(String token) {
        log.info("Выход из всех устройств");

        String username = jwtService.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));

        // Отзываем все refresh токены пользователя
        var activeTokens = refreshTokenService.getActiveTokens(user.getId());
        activeTokens.forEach(t -> refreshTokenService.revokeToken(t.token()));

        // Добавляем текущий access токен в черный список
        tokenBlacklistService.blacklistToken(token);

        log.info("Пользователь {} вышел из всех устройств", username);
    }

    /**
     * Инициация сброса пароля
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Инициация сброса пароля для: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Пользователь с таким email не найден", ErrorCodes.USER_NOT_FOUND));

        // Генерация токена для сброса пароля
        String resetToken = UUID.randomUUID().toString();

        // Сохранение токена (можно использовать Redis или отдельную таблицу)
        tokenBlacklistService.savePasswordResetToken(user.getId(), resetToken);

        // Отправка email с инструкциями
        emailVerificationService.sendPasswordResetEmail(user, resetToken);

        log.info("Email для сброса пароля отправлен на: {}", email);
    }

    /**
     * Сброс пароля
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Сброс пароля по токену");

        // Валидация токена и получение userId
        Long userId = tokenBlacklistService.validatePasswordResetToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));

        // Обновление пароля
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Удаление токена сброса
        tokenBlacklistService.deletePasswordResetToken(token);

        // Отзыв всех refresh токенов (безопасность)
        var activeTokens = refreshTokenService.getActiveTokens(user.getId());
        activeTokens.forEach(t -> refreshTokenService.revokeToken(t.token()));

        log.info("Пароль успешно сброшен для пользователя: {}", user.getUsername());
    }

    /**
     * Верификация email
     */
    @Transactional
    public void verifyEmail(String token) {
        log.info("Верификация email по токену");

        Long userId = emailVerificationService.validateEmailToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));

        user.setIsEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Email верифицирован для пользователя: {}", user.getEmail());
    }

    /**
     * Повторная отправка письма для верификации
     */
    @Transactional
    public void resendVerificationEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));

        if (user.getIsEmailVerified()) {
            throw new BusinessException("Email уже подтвержден", "EMAIL_ALREADY_VERIFIED");
        }

        emailVerificationService.sendVerificationEmail(user);

        log.info("Письмо для верификации отправлено повторно: {}", user.getEmail());
    }

    /**
     * Получение userId из токена
     */
    public Long getUserIdFromToken(String token) {
        String username = jwtService.getUsernameFromToken(token);
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));
    }

    /**
     * Проверка доступности username
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Проверка доступности email
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email.toLowerCase());
    }

    // ===== Вспомогательные методы =====

    private UserDetails createUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(getUserAuthorities(user))
                .accountExpired(false)
                .accountLocked(!user.getIsActive())
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }

    private String[] getUserAuthorities(User user) {
        return user.getRoles().stream()
                .map(ur -> "ROLE_" + ur.getRole().getName())
                .toArray(String[]::new);
    }
}