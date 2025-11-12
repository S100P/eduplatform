package ru.s100p.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username не может быть пустым")
    @Size(min = 3, max = 50, message = "Username должен быть от 3 до 50 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username может содержать только буквы, цифры и подчеркивание")
    private String username;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный email адрес")
    private String email;

    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
            message = "Пароль должен содержать минимум одну цифру, одну строчную букву, одну заглавную букву и один специальный символ")
    private String password;

    @NotBlank(message = "Подтверждение пароля не может быть пустым")
    private String confirmPassword;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private String firstName;

    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(min = 2, max = 50, message = "Фамилия должна быть от 2 до 50 символов")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный формат телефона")
    private String phone;

    // Опциональные поля
    private String bio;
    private String avatarUrl;

    // Роль по умолчанию будет STUDENT, но можно указать при создании инструктора
    @Builder.Default
    private String requestedRole = "STUDENT";

    // Согласие с условиями использования
    private boolean termsAccepted;

    // Подписка на рассылку
    private boolean newsletterSubscribed;
}