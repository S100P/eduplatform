package ru.s100p.user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.s100p.shared.constants.ApiConstants;
import ru.s100p.shared.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Тестовый контроллер для проверки работы JWT аутентификации и авторизации
 * Удалить в продакшене!
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/test")
public class TestSecurityController {

    /**
     * Публичный endpoint - доступен всем
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<String>> publicEndpoint() {
        log.info("Вызван публичный endpoint");

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Это публичный endpoint, доступный всем")
                        .data("Public data")
                        .build()
        );
    }

    /**
     * Защищенный endpoint - требует аутентификации
     */
    @GetMapping("/authenticated")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authenticatedEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Вызван защищенный endpoint пользователем: {}", auth.getName());

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", auth.getName());
        userData.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        userData.put("isAuthenticated", auth.isAuthenticated());

        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Вы успешно аутентифицированы")
                        .data(userData)
                        .build()
        );
    }

    /**
     * Endpoint только для студентов
     */
    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> studentEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Student endpoint вызван пользователем: {}", auth.getName());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Доступ разрешен - вы студент")
                        .data("Student-only content")
                        .build()
        );
    }

    /**
     * Endpoint только для инструкторов
     */
    @GetMapping("/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<String>> instructorEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Instructor endpoint вызван пользователем: {}", auth.getName());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Доступ разрешен - вы инструктор")
                        .data("Instructor-only content")
                        .build()
        );
    }

    /**
     * Endpoint только для администраторов
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin endpoint вызван пользователем: {}", auth.getName());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Доступ разрешен - вы администратор")
                        .data("Admin-only content")
                        .build()
        );
    }

    /**
     * Endpoint для инструкторов и админов
     */
    @GetMapping("/instructor-or-admin")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> instructorOrAdminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Instructor/Admin endpoint вызван пользователем: {}", auth.getName());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Доступ разрешен - вы инструктор или администратор")
                        .data("Instructor or Admin content")
                        .build()
        );
    }

    /**
     * Endpoint с проверкой конкретного permission
     */
    @GetMapping("/create-course")
    @PreAuthorize("hasAuthority('CREATE_COURSE')")
    public ResponseEntity<ApiResponse<String>> createCourseEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Create course endpoint вызван пользователем: {}", auth.getName());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("У вас есть право создавать курсы")
                        .data("Course creation allowed")
                        .build()
        );
    }

    /**
     * Endpoint для проверки всех authorities пользователя
     */
    @GetMapping("/my-authorities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put("principal", auth.getPrincipal().getClass().getSimpleName());
        authInfo.put("name", auth.getName());
        authInfo.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.toList()));
        authInfo.put("isAuthenticated", auth.isAuthenticated());
        authInfo.put("details", auth.getDetails());

        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Ваши текущие права доступа")
                        .data(authInfo)
                        .build()
        );
    }
}