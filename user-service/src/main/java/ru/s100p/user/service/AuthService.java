package ru.s100p.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import ru.s100p.user.security.CustomUserPrincipal;
import ru.s100p.user.security.JwtService;
import ru.s100p.user.security.TokenBlacklistService;

import java.time.LocalDateTime;
import java.util.UUID;

import static ru.s100p.shared.constants.ErrorCodes.UNAUTHENTICATED;

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

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        log.info("Попытка аутентификации: {}", request.getUsernameOrEmail());

        User user = userRepository.findByEmail(request.getUsernameOrEmail())
                .or(() -> userRepository.findByUsername(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BusinessException("Неверные учетные данные", ErrorCodes.INVALID_CREDENTIALS));

        if (!user.getIsActive()) {
            throw new BusinessException("Аккаунт деактивирован", ErrorCodes.ACCOUNT_DISABLED);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        request.getPassword()
                )
        );

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(authentication);
        long refreshExpiry = request.isRememberMe() ? REMEMBER_ME_EXPIRY : REFRESH_TOKEN_EXPIRY;
        var refreshTokenDto = refreshTokenService.createToken(user.getId(), refreshExpiry);

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

    @Transactional
    public AuthResponse generateAuthResponse(UserDto userDto) {
        User user = userRepository.findById(userDto.id())
                .orElseThrow(() -> new BusinessException("Пользователь не найден",
                        ErrorCodes.USER_NOT_FOUND));

        UserDetails userDetails = createUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,
                null, userDetails.getAuthorities());

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

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Запрос на обновление токена");

        var refreshTokenEntity = refreshTokenService.validateAndGetToken(refreshToken);

        User user = userRepository.findById(refreshTokenEntity.getUser().getId())
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));

        UserDetails userDetails = createUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = jwtService.generateAccessToken(authentication);

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

    @Transactional
    public void logout(String token) {
        log.info("Выход из системы");

        tokenBlacklistService.blacklistToken(token);
        Long userId = getUserIdFromSecurityContext();
        var activeTokens = refreshTokenService.getActiveTokens(userId);
        if (!activeTokens.isEmpty()) {
            refreshTokenService.revokeToken(activeTokens.get(0).token());
        }
        log.info("Пользователь с ID {} вышел из системы", userId);
    }

    @Transactional
    public void logoutFromAllDevices(String token) {
        log.info("Выход из всех устройств");

        Long userId = getUserIdFromSecurityContext();
        var activeTokens = refreshTokenService.getActiveTokens(userId);
        activeTokens.forEach(t -> refreshTokenService.revokeToken(t.token()));
        tokenBlacklistService.blacklistToken(token);
        log.info("Пользователь с ID {} вышел из всех устройств", userId);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Инициация сброса пароля для: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Пользователь с таким email не найден", ErrorCodes.USER_NOT_FOUND));
        String resetToken = UUID.randomUUID().toString();
        tokenBlacklistService.savePasswordResetToken(user.getId(), resetToken);
        emailVerificationService.sendPasswordResetEmail(user, resetToken);
        log.info("Email для сброса пароля отправлен на: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Сброс пароля по токену");
        Long userId = tokenBlacklistService.validatePasswordResetToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Пользователь не найден", ErrorCodes.USER_NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        tokenBlacklistService.deletePasswordResetToken(token);
        var activeTokens = refreshTokenService.getActiveTokens(user.getId());
        activeTokens.forEach(t -> refreshTokenService.revokeToken(t.token()));
        log.info("Пароль успешно сброшен для пользователя: {}", user.getUsername());
    }

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

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email.toLowerCase());
    }

    // ===== Вспомогательные методы =====

    private UserDetails createUserDetails(User user) {
        // Заменено создание стандартного User на наш CustomUserPrincipal,
        // чтобы обеспечить наличие ID и избежать ClassCastException.
        return CustomUserPrincipal.create(user);
    }

    private Long getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Отсутствует информация об аутентификации", UNAUTHENTICATED);
        }
        return Long.parseLong(authentication.getName());
    }
}
