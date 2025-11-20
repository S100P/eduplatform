package ru.s100p.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.s100p.shared.constants.ApiConstants;
import ru.s100p.shared.dto.ApiResponse;
import ru.s100p.shared.dto.UserDto;
import ru.s100p.user.dto.request.ChangePasswordRequest;
import ru.s100p.user.dto.request.LoginRequest;
import ru.s100p.user.dto.request.RefreshTokenRequest;
import ru.s100p.user.dto.request.RegisterRequest;
import ru.s100p.user.dto.response.AuthResponse;
import ru.s100p.user.service.AuthService;
import ru.s100p.user.service.UserService;

import java.net.URI;
import java.security.Principal;

/**
 * Контроллер для аутентификации и управления аккаунтом.
 * <p>
 * Эндпоинты в этом контроллере делятся на две категории:
 * <p>
 * 1. <b>Публичные:</b> /register, /login, /refresh, /forgot-password, /reset-password.
 *    Они доступны всем и являются точкой входа в систему.
 *    <p>
 * 2. <b>Защищенные:</b> /logout, /change-password, /resend-verification, /me.
 *    Они требуют валидного токена и используют {@link Authentication} объект,
 *    заполненный {@link ru.s100p.user.security.HeaderAuthenticationFilter}, для получения информации о пользователе.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Запрос на регистрацию пользователя: {}", request.getEmail());
        UserDto user = userService.registerUser(request);
        AuthResponse authResponse = authService.generateAuthResponse(user);
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Пользователь успешно зарегистрирован")
                .data(authResponse)
                .build();
        return ResponseEntity
                .created(URI.create(ApiConstants.API_V1 + "/users/" + user.id()))
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Попытка входа: {}", request.getUsernameOrEmail());
        AuthResponse authResponse = authService.authenticate(request);
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Вход выполнен успешно")
                .data(authResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Запрос на обновление токена");
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Токен успешно обновлен")
                .data(authResponse)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Выход из системы.
     * <p>
     * Принимает токен для добавления его в черный список.
     * ID пользователя для отзыва refresh-токена берется из {@link Authentication},
     * что является более безопасным подходом.
     * </p>
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(ApiConstants.AUTHORIZATION_HEADER) String authorizationHeader,
            @RequestParam(required = false, defaultValue = "false") boolean allDevices) {

        String jwt = authorizationHeader.replace(ApiConstants.BEARER_PREFIX, "");

        if (allDevices) {
            authService.logoutFromAllDevices(jwt);
            log.info("Выход из всех устройств");
        } else {
            authService.logout(jwt);
            log.info("Выход из текущего устройства");
        }

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Выход выполнен успешно")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Смена пароля для аутентифицированного пользователя.
     * ID пользователя берется из {@link Principal}, что гарантирует,
     * что пользователь может сменить пароль только себе.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        Long userId = Long.parseLong(principal.getName());
        userService.changePassword(userId, request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Пароль успешно изменен")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
        log.info("Запрос на сброс пароля для: {}", email);
        authService.initiatePasswordReset(email);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Инструкции по сбросу пароля отправлены на email")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Пароль успешно сброшен")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        log.info("Верификация email по токену");
        authService.verifyEmail(token);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Email успешно подтвержден")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Повторная отправка письма для верификации.
     * ID пользователя берется из {@link Principal}.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        authService.resendVerificationEmail(userId);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Письмо для верификации отправлено повторно")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        boolean available = authService.isUsernameAvailable(username);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .data(available)
                .message(available ? "Username доступен" : "Username уже занят")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .data(available)
                .message(available ? "Email доступен" : "Email уже используется")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Получение информации о текущем аутентифицированном пользователе.
     * ID пользователя берется из {@link Principal}.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        // Второй аргумент userId - для проверки прав доступа, если она потребуется в будущем
        UserDto user = userService.getUserById(userId, userId);
        ApiResponse<UserDto> response = ApiResponse.<UserDto>builder()
                .success(true)
                .data(user)
                .build();
        return ResponseEntity.ok(response);
    }
}
