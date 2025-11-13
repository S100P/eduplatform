package ru.s100p.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Регистрация нового пользователя
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Запрос на регистрацию пользователя: {}", request.getEmail());

        UserDto user = userService.registerUser(request);
        AuthResponse authResponse = authService.generateAuthResponse(user);

        //см файл шпаргалку в корень проекта
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Пользователь успешно зарегистрирован")
                .data(authResponse)
                .build();

        return ResponseEntity
                .created(URI.create(ApiConstants.API_V1 + "/users/" + user.id()))
                .body(response);
    }

    /**
     * Вход в систему
     */
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

    /**
     * Обновление токена
     */
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
     * Выход из системы
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(ApiConstants.AUTHORIZATION_HEADER) String token,
            @RequestParam(required = false, defaultValue = "false") boolean allDevices) {

        String jwt = token.replace(ApiConstants.BEARER_PREFIX, "");

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
     * Смена пароля
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader(ApiConstants.AUTHORIZATION_HEADER) String token,
            @Valid @RequestBody ChangePasswordRequest request) {

        Long userId = authService.getUserIdFromToken(token.replace(ApiConstants.BEARER_PREFIX, ""));
        userService.changePassword(userId, request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Пароль успешно изменен")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Запрос на сброс пароля
     */
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

    /**
     * Сброс пароля по токену
     */
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

    /**
     * Подтверждение email
     */
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
     * Повторная отправка письма для верификации
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestHeader(ApiConstants.AUTHORIZATION_HEADER) String token) {

        Long userId = authService.getUserIdFromToken(token.replace(ApiConstants.BEARER_PREFIX, ""));
        authService.resendVerificationEmail(userId);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Письмо для верификации отправлено повторно")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Проверка доступности username
     */
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

    /**
     * Проверка доступности email
     */
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
     * Получение текущего пользователя по токену
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @RequestHeader(ApiConstants.AUTHORIZATION_HEADER) String token) {

        Long userId = authService.getUserIdFromToken(token.replace(ApiConstants.BEARER_PREFIX, ""));
        UserDto user = userService.getUserById(userId, userId);

        ApiResponse<UserDto> response = ApiResponse.<UserDto>builder()
                .success(true)
                .data(user)
                .build();

        return ResponseEntity.ok(response);
    }
}
